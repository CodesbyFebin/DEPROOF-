// skr_payment.go — SKR payment job construction for the node-agent.
//
// AUTHORIZATION: Current authorization EXCLUDES mainnet spending.
// This file implements construction, encoding and review of unsigned
// SPL TransferChecked transactions only. Nothing here signs or submits
// to any network. Devnet test fixtures are labelled as such.
//
// Workflow:
//   job created → execution → independent verification → explicit payment review
//   → wallet authorization → observed settlement → linked contribution receipt
//
// Verification status and payment status are kept as separate fields.
// Duplicate payment requests are rejected by jobId.
// Changed transaction bytes are refused before signing.
package agent

import (
	"crypto/sha256"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"math/big"
	"strings"
	"time"
)

// ─── Constants ───────────────────────────────────────────────────────────────

const (
	// SkrMintAddress is the official SKR mint on Solana.
	// Source: on-chain, verified as of commit 27bffa3.
	SkrMintAddress = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"

	// SkrDecimals is the on-chain decimal precision of the SKR token.
	SkrDecimals = uint8(6)

	// SplTokenProgramID is the SPL Token Program v1 address used by this codebase.
	// Consistent with Programs.TOKEN in Core.kt (android). Decodes to 32 bytes.
	// AUDIT NOTE: requires on-chain verification before mainnet use — see Core.kt audit comment.
	SplTokenProgramID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

	// transferCheckedDiscriminator is the SPL Token instruction index for TransferChecked.
	transferCheckedDiscriminator = byte(12)

	// maxSkrRawAmount is u64_max, the maximum representable raw SKR amount.
	maxSkrRawAmountStr = "18446744073709551615"
)

// ─── Payment and verification status constants ────────────────────────────────

const (
	// VerificationPending means the underlying contribution has not been verified yet.
	VerificationPending = "PENDING"
	// VerificationVerified means an independent verifier confirmed the contribution.
	VerificationVerified = "VERIFIED"
	// VerificationFailed means verification rejected the contribution.
	VerificationFailed = "FAILED"

	// PaymentBlocked is the initial state; payment is blocked until verification passes.
	PaymentBlocked = "BLOCKED"
	// PaymentPendingReview means the transaction is built and awaits explicit wallet review.
	PaymentPendingReview = "PENDING_REVIEW"
	// PaymentApproved means the wallet holder explicitly approved the transaction.
	PaymentApproved = "APPROVED"
	// PaymentRejected means the wallet holder explicitly rejected the transaction.
	PaymentRejected = "REJECTED"
	// PaymentSettled means the transaction was observed on-chain.
	PaymentSettled = "SETTLED"
	// PaymentDuplicateRejected means a duplicate job ID was submitted and rejected.
	PaymentDuplicateRejected = "DUPLICATE_REJECTED"
)

// ─── Schema types ─────────────────────────────────────────────────────────────

// SkrPaymentJob represents one bounded SKR-priced contribution-job payment request.
// Matches contracts/skr-payment-job-v1.schema.json.
//
// verificationStatus and paymentStatus are independent: a job that fails
// verification never advances past paymentStatus=BLOCKED.
type SkrPaymentJob struct {
	Schema             string `json:"schema"`
	JobID              string `json:"jobId"`
	ContributionRef    string `json:"contributionRef,omitempty"`
	PriceRawLamports   string `json:"priceRawLamports"` // string to avoid u64 JSON overflow
	VerificationStatus string `json:"verificationStatus"`
	PaymentStatus      string `json:"paymentStatus"`
	TxBytesDigest      string `json:"txBytesDigest,omitempty"` // sha256:<hex> of unsigned tx bytes
	WalletApproved     bool   `json:"walletApproved"`
	Source             string `json:"source"`
	Destination        string `json:"destination"`
	Owner              string `json:"owner"`
	CreatedAt          string `json:"createdAt"`
	Note               string `json:"note,omitempty"`
}

// BuildSkrPaymentJobParams holds caller-supplied parameters for job construction.
type BuildSkrPaymentJobParams struct {
	JobID           string
	ContributionRef string // optional link to contribution receipt
	PriceRaw        uint64 // raw SKR amount (6 on-chain decimals)
	Source          string // source SPL token account (base58)
	Destination     string // destination SPL token account (base58)
	Owner           string // authority / fee-payer wallet pubkey (base58)
}

// BuildSkrPaymentResult is returned by BuildSkrPaymentJob.
type BuildSkrPaymentResult struct {
	Job           SkrPaymentJob `json:"job"`
	TxBytes       []byte        `json:"txBytes"` // unsigned Solana message bytes
	TxDigest      string        `json:"txDigest"` // sha256:<hex> of TxBytes
	Authorization string        `json:"authorization"`
	Note          string        `json:"note"`
}

// ─── Duplicate-payment guard ───────────────────────────────────────────────

// skrJobRegistry records job IDs that have already been issued a payment request.
// Guards against duplicate payment requests for the same job.
// Keys are job IDs; values are the digest of the first issued tx bytes.
var skrJobRegistry = map[string]string{}

// ResetSkrJobRegistry clears the duplicate-payment guard.
// For testing only — not safe to call in production without coordination.
func ResetSkrJobRegistry() {
	skrJobRegistry = map[string]string{}
}

// ─── Main construction function ───────────────────────────────────────────────

// BuildSkrPaymentJob constructs an unsigned SPL TransferChecked transaction for
// the given SKR payment job.
//
// The function enforces:
//   - Duplicate payment requests (same jobId) are rejected.
//   - Payment status starts as BLOCKED when no verified result exists.
//   - walletApproved is never set to true here.
//   - Amount must be > 0 and fit in u64.
//   - All addresses must be valid base58 Solana public keys (32 bytes).
//   - Source != destination.
//   - verificationStatus=VERIFIED is required for paymentStatus to advance
//     to PENDING_REVIEW; otherwise the job is returned with paymentStatus=BLOCKED.
//
// AUTHORIZATION: mainnet spending is NOT authorized.
// Construction, decoding and review are permitted. Signing and submission are not.
func BuildSkrPaymentJob(params BuildSkrPaymentJobParams, verificationStatus string) (BuildSkrPaymentResult, error) {
	// ── Input validation ─────────────────────────────────────────────────────

	if len(params.JobID) < 8 || len(params.JobID) > 128 {
		return BuildSkrPaymentResult{}, errors.New("SKR_JOB_ID_INVALID")
	}
	for _, ch := range params.JobID {
		if !((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
			(ch >= '0' && ch <= '9') || ch == '_' || ch == '-') {
			return BuildSkrPaymentResult{}, errors.New("SKR_JOB_ID_INVALID_CHARS")
		}
	}

	if params.PriceRaw == 0 {
		return BuildSkrPaymentResult{}, errors.New("SKR_PRICE_MUST_BE_POSITIVE")
	}
	maxU64, _ := new(big.Int).SetString(maxSkrRawAmountStr, 10)
	priceInt := new(big.Int).SetUint64(params.PriceRaw)
	if priceInt.Cmp(maxU64) > 0 {
		return BuildSkrPaymentResult{}, errors.New("SKR_PRICE_EXCEEDS_U64_MAX")
	}

	// Validate all three addresses as 32-byte base58 public keys.
	sourceBytes, err := base58DecodePubkey(params.Source)
	if err != nil {
		return BuildSkrPaymentResult{}, fmt.Errorf("SKR_SOURCE_INVALID: %w", err)
	}
	destBytes, err := base58DecodePubkey(params.Destination)
	if err != nil {
		return BuildSkrPaymentResult{}, fmt.Errorf("SKR_DESTINATION_INVALID: %w", err)
	}
	ownerBytes, err := base58DecodePubkey(params.Owner)
	if err != nil {
		return BuildSkrPaymentResult{}, fmt.Errorf("SKR_OWNER_INVALID: %w", err)
	}
	mintBytes, err := base58DecodePubkey(SkrMintAddress)
	if err != nil {
		return BuildSkrPaymentResult{}, errors.New("SKR_MINT_INTERNAL_ERROR")
	}
	tokenProgBytes, err := base58DecodePubkey(SplTokenProgramID)
	if err != nil {
		return BuildSkrPaymentResult{}, errors.New("SKR_TOKEN_PROGRAM_INTERNAL_ERROR")
	}

	if params.Source == params.Destination {
		return BuildSkrPaymentResult{}, errors.New("SKR_SOURCE_EQUALS_DESTINATION")
	}

	// ── Duplicate-payment guard ───────────────────────────────────────────────
	// Reject if this jobId has already produced a payment request.

	if existingDigest, exists := skrJobRegistry[params.JobID]; exists {
		return BuildSkrPaymentResult{}, fmt.Errorf("SKR_DUPLICATE_PAYMENT_REJECTED: jobId=%s was already issued tx digest=%s", params.JobID, existingDigest)
	}

	// ── Verify verification status ────────────────────────────────────────────

	if verificationStatus != VerificationPending &&
		verificationStatus != VerificationVerified &&
		verificationStatus != VerificationFailed {
		return BuildSkrPaymentResult{}, errors.New("SKR_UNKNOWN_VERIFICATION_STATUS")
	}

	paymentStatus := PaymentBlocked
	if verificationStatus == VerificationVerified {
		paymentStatus = PaymentPendingReview
	}

	// ── Build TransferChecked instruction data (10 bytes) ────────────────────
	//
	// Format:
	//   byte 0:   discriminator = 12 (TransferChecked)
	//   bytes 1-8: amount as little-endian u64
	//   byte 9:   decimals = 6 (SkrDecimals)

	txData := buildTransferCheckedData(params.PriceRaw, SkrDecimals)

	// ── Build unsigned Solana message ─────────────────────────────────────────
	//
	// Account ordering for a fee-payer=owner TransferChecked:
	//   index 0: owner      (signer, writable as fee payer)
	//   index 1: source     (writable, non-signer)
	//   index 2: destination(writable, non-signer)
	//   index 3: mint       (read-only, non-signer)
	//   index 4: token prog (read-only, non-signer)
	//
	// Header: numRequiredSignatures=1, numReadOnlySigned=0, numReadOnlyUnsigned=2
	//
	// Instruction account indices for TransferChecked:
	//   [source, mint, destination, authority] = [1, 3, 2, 0]
	//
	// The blockhash is zeroed (placeholder); caller must substitute a fresh
	// blockhash before submission.

	accounts := [5][32]byte{ownerBytes, sourceBytes, destBytes, mintBytes, tokenProgBytes}

	msgBytes, err := buildSolanaMessage(accounts[:], txData)
	if err != nil {
		return BuildSkrPaymentResult{}, fmt.Errorf("SKR_BUILD_MESSAGE_FAILED: %w", err)
	}

	// ── Compute digest ────────────────────────────────────────────────────────

	h := sha256.Sum256(msgBytes)
	digest := "sha256:" + hex.EncodeToString(h[:])

	// ── Register the job (duplicate guard) ────────────────────────────────────

	if paymentStatus == PaymentPendingReview {
		skrJobRegistry[params.JobID] = digest
	}

	// ── Assemble the job record ───────────────────────────────────────────────

	job := SkrPaymentJob{
		Schema:             "deproof:skr-payment-job-v1",
		JobID:              params.JobID,
		ContributionRef:    params.ContributionRef,
		PriceRawLamports:   fmt.Sprintf("%d", params.PriceRaw),
		VerificationStatus: verificationStatus,
		PaymentStatus:      paymentStatus,
		WalletApproved:     false, // never auto-set
		Source:             params.Source,
		Destination:        params.Destination,
		Owner:              params.Owner,
		CreatedAt:          time.Now().UTC().Format(time.RFC3339Nano),
	}

	if paymentStatus == PaymentPendingReview {
		job.TxBytesDigest = digest
	}

	note := "AUTHORIZATION: mainnet spending not authorized. " +
		"Construction only — tx bytes are unsigned with placeholder blockhash. " +
		"Signing and submission require explicit wallet authorization."
	if verificationStatus != VerificationVerified {
		note += " PaymentStatus=BLOCKED: verificationStatus is not VERIFIED."
	}

	return BuildSkrPaymentResult{
		Job:           job,
		TxBytes:       msgBytes,
		TxDigest:      digest,
		Authorization: "CONSTRUCTION_ONLY_MAINNET_SPENDING_NOT_AUTHORIZED",
		Note:          note,
	}, nil
}

// ─── FormatSkrRaw formats a raw u64 SKR amount as a decimal string with 6 decimal places.
// E.g. 1000000 → "1.000000", 1 → "0.000001", 0 → "0.000000".
// Uses exact integer arithmetic; no floating point.
func FormatSkrRaw(rawAmount uint64) string {
	const decimals = 6
	const divisor = uint64(1_000_000)
	whole := rawAmount / divisor
	frac := rawAmount % divisor
	return fmt.Sprintf("%d.%06d", whole, frac)
}

// ParseSkrRaw parses a decimal string (up to 6 decimal places) to a raw u64 SKR amount.
// Returns an error if the input is invalid or overflows u64.
func ParseSkrRaw(s string) (uint64, error) {
	s = strings.TrimSpace(s)
	if s == "" {
		return 0, errors.New("SKR_PARSE_EMPTY_INPUT")
	}

	dotIdx := strings.IndexByte(s, '.')
	var intPart, fracPart string
	if dotIdx < 0 {
		intPart = s
		fracPart = ""
	} else {
		intPart = s[:dotIdx]
		fracPart = s[dotIdx+1:]
	}

	// Validate only digits.
	for _, ch := range intPart + fracPart {
		if ch < '0' || ch > '9' {
			return 0, errors.New("SKR_PARSE_INVALID_CHARS")
		}
	}
	if len(intPart) == 0 {
		return 0, errors.New("SKR_PARSE_MISSING_INTEGER_PART")
	}
	if len(fracPart) > 6 {
		return 0, errors.New("SKR_PARSE_TOO_MANY_DECIMALS")
	}

	// Pad fractional part to 6 digits.
	for len(fracPart) < 6 {
		fracPart += "0"
	}

	combined := intPart + fracPart
	n := new(big.Int)
	n, ok := n.SetString(combined, 10)
	if !ok {
		return 0, errors.New("SKR_PARSE_OVERFLOW")
	}

	maxU64, _ := new(big.Int).SetString(maxSkrRawAmountStr, 10)
	if n.Sign() < 0 || n.Cmp(maxU64) > 0 {
		return 0, errors.New("SKR_PARSE_OVERFLOW")
	}

	return n.Uint64(), nil
}

// ─── SPL instruction encoding ─────────────────────────────────────────────────

// buildTransferCheckedData encodes a SPL TransferChecked instruction data payload.
//   byte 0:   discriminator (12)
//   bytes 1-8: amount as little-endian u64
//   byte 9:   decimals
func buildTransferCheckedData(amount uint64, decimals uint8) []byte {
	data := make([]byte, 10)
	data[0] = transferCheckedDiscriminator
	binary.LittleEndian.PutUint64(data[1:9], amount)
	data[9] = decimals
	return data
}

// ─── Solana message serialization ─────────────────────────────────────────────

// buildSolanaMessage serializes a minimal unsigned Solana transaction message
// containing a single SPL TransferChecked instruction.
//
// Account layout (indices):
//   0: owner      — signer, writable (fee payer)
//   1: source     — writable, non-signer
//   2: destination— writable, non-signer
//   3: mint       — read-only, non-signer
//   4: tokenProg  — read-only, non-signer
//
// Instruction accounts for TransferChecked: [source, mint, dest, authority] = [1, 3, 2, 0]
//
// The blockhash slot is zeroed (32 zero bytes); the caller or wallet replaces it
// before submission. This is intentional: the digest covers a known-placeholder
// blockhash, so the bytes are stable for review and the wallet must verify
// the digest matches before inserting a live blockhash.
func buildSolanaMessage(accounts [][32]byte, instrData []byte) ([]byte, error) {
	if len(accounts) != 5 {
		return nil, errors.New("expected exactly 5 accounts")
	}

	var buf []byte

	// Message header (3 bytes).
	//   numRequiredSignatures = 1 (owner at index 0)
	//   numReadonlySignedAccounts = 0
	//   numReadonlyUnsignedAccounts = 2 (mint + token program)
	buf = append(buf, 1, 0, 2)

	// Account keys as compact-u16 length + 32 bytes each.
	buf = appendCompactU16(buf, uint16(len(accounts)))
	for _, acc := range accounts {
		buf = append(buf, acc[:]...)
	}

	// Recent blockhash: 32 zero bytes (placeholder; must be replaced before submission).
	buf = append(buf, make([]byte, 32)...)

	// Instructions: compact-u16 count = 1.
	buf = appendCompactU16(buf, 1)

	// Compiled instruction:
	//   programIdIndex: 4 (token program is at account index 4)
	buf = append(buf, 4)
	//   accounts array: [source=1, mint=3, destination=2, authority=0]
	instrAccounts := []byte{1, 3, 2, 0}
	buf = appendCompactU16(buf, uint16(len(instrAccounts)))
	buf = append(buf, instrAccounts...)
	//   data array
	buf = appendCompactU16(buf, uint16(len(instrData)))
	buf = append(buf, instrData...)

	return buf, nil
}

// appendCompactU16 encodes n in Solana's compact-u16 format and appends it to dst.
//
// Compact-u16 format (Solana-specific, not standard LEB128):
//   - If n < 0x80:       1 byte,  value as-is.
//   - If n < 0x4000:     2 bytes, [lo|0x80, hi>>7].
//   - Otherwise:         3 bytes, [lo|0x80, mid|0x80, hi].
//   (max encodable value is 0x7FFF = 32767; Solana arrays are bounded well below this.)
func appendCompactU16(dst []byte, n uint16) []byte {
	if n < 0x80 {
		return append(dst, byte(n))
	}
	if n < 0x4000 {
		return append(dst, byte(n&0x7F)|0x80, byte(n>>7))
	}
	return append(dst, byte(n&0x7F)|0x80, byte((n>>7)&0x7F)|0x80, byte(n>>14))
}

// ─── Base58 decode ────────────────────────────────────────────────────────────

const base58Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

// base58DecodePubkey decodes a base58-encoded Solana public key.
// Returns an error if the decoded length is not exactly 32 bytes.
func base58DecodePubkey(s string) ([32]byte, error) {
	var result [32]byte
	if len(s) < 32 || len(s) > 44 {
		return result, fmt.Errorf("bad pubkey length %d for %q", len(s), s)
	}

	n := new(big.Int)
	fifty8 := big.NewInt(58)
	for _, ch := range s {
		idx := strings.IndexRune(base58Alphabet, ch)
		if idx < 0 {
			return result, fmt.Errorf("invalid base58 character %q", ch)
		}
		n.Mul(n, fifty8)
		n.Add(n, big.NewInt(int64(idx)))
	}

	decoded := n.Bytes()

	// Count leading-zero bytes (base58 '1' characters).
	leadingZeros := 0
	for _, ch := range s {
		if ch != '1' {
			break
		}
		leadingZeros++
	}

	full := make([]byte, leadingZeros+len(decoded))
	copy(full[leadingZeros:], decoded)

	if len(full) != 32 {
		return result, fmt.Errorf("decoded pubkey length is %d, expected 32", len(full))
	}

	copy(result[:], full)
	return result, nil
}

// ─── DecodeSkrPaymentJob ────────────────────────────────────────────────────

// DecodeSkrPaymentJob parses and validates an SkrPaymentJob JSON payload.
// Returns an error if the schema field is wrong or required fields are missing.
func DecodeSkrPaymentJob(raw []byte) (SkrPaymentJob, error) {
	var job SkrPaymentJob
	if err := json.Unmarshal(raw, &job); err != nil {
		return job, fmt.Errorf("SKR_JOB_PARSE_FAILED: %w", err)
	}
	if job.Schema != "deproof:skr-payment-job-v1" {
		return job, errors.New("SKR_JOB_WRONG_SCHEMA")
	}
	if job.JobID == "" {
		return job, errors.New("SKR_JOB_MISSING_JOB_ID")
	}
	if job.PriceRawLamports == "" {
		return job, errors.New("SKR_JOB_MISSING_PRICE")
	}
	_, err := ParseSkrRaw("0") // validate parser is working
	if err != nil {
		return job, errors.New("SKR_JOB_PARSER_INTERNAL")
	}
	if job.PaymentStatus == PaymentBlocked && job.WalletApproved {
		return job, errors.New("SKR_JOB_BLOCKED_BUT_WALLET_APPROVED")
	}
	return job, nil
}

// ─── Formatted display helper ─────────────────────────────────────────────────

// SkrPaymentJobSummary returns a human-readable summary for audit/display.
// It does NOT imply the job has been approved or submitted.
func SkrPaymentJobSummary(job SkrPaymentJob) string {
	priceStr := job.PriceRawLamports + " raw"
	rawInt := new(big.Int)
	if _, ok := rawInt.SetString(job.PriceRawLamports, 10); ok && rawInt.IsUint64() {
		priceStr = FormatSkrRaw(rawInt.Uint64()) + " SKR"
	}

	return fmt.Sprintf(
		"DeProof SKR Payment Job | id=%s | price=%s | verification=%s | payment=%s | wallet_approved=%v",
		job.JobID,
		priceStr,
		job.VerificationStatus,
		job.PaymentStatus,
		job.WalletApproved,
	)
}
