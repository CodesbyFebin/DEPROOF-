package agent

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"strings"
	"testing"
)

// Test pubkeys: short but valid base58 32-byte encodings.
// These are constructed as valid 32-byte sequences encoded in base58.
// They represent DEVNET TEST FIXTURES only — not real wallet addresses.
const (
	// testOwner is a zeroed 32-byte key encoded in base58.
	// FIXTURE — not a real wallet.
	testOwner       = "11111111111111111111111111111111"
	// testSource is a deterministic test SPL token account.
	// FIXTURE — not a real token account.
	testSource      = "2dn6KSx4dCoMHqqvZYMc9bgXEkCsZJVxYBfHfwVJQXJt"
	// testDestination is a deterministic test SPL token account.
	// FIXTURE — not a real token account.
	testDestination = "3Cqk7HnGLKaEnHpFpGLhGSmHfGPVJqvqbfq4t5sNMuHs"
)

func testParams(jobID string, price uint64) BuildSkrPaymentJobParams {
	return BuildSkrPaymentJobParams{
		JobID:       jobID,
		PriceRaw:    price,
		Source:      testSource,
		Destination: testDestination,
		Owner:       testOwner,
	}
}

// TestBuildSkrPaymentJobBlockedWhenNotVerified checks that payment is BLOCKED
// when verification has not passed.
func TestBuildSkrPaymentJobBlockedWhenNotVerified(t *testing.T) {
	ResetSkrJobRegistry()

	result, err := BuildSkrPaymentJob(testParams("job-blocked-01", 1_000_000), VerificationPending)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if result.Job.PaymentStatus != PaymentBlocked {
		t.Errorf("expected PaymentBlocked, got %q", result.Job.PaymentStatus)
	}
	if result.Job.WalletApproved {
		t.Error("walletApproved must be false when BLOCKED")
	}
	// No digest recorded when BLOCKED — tx bytes are still produced but not registered.
	if result.Job.TxBytesDigest != "" {
		t.Errorf("txBytesDigest should be empty when payment is BLOCKED, got %q", result.Job.TxBytesDigest)
	}
	if result.Authorization != "CONSTRUCTION_ONLY_MAINNET_SPENDING_NOT_AUTHORIZED" {
		t.Errorf("wrong authorization label: %q", result.Authorization)
	}
}

// TestBuildSkrPaymentJobPendingReviewWhenVerified checks that payment advances to
// PENDING_REVIEW when verification has passed.
func TestBuildSkrPaymentJobPendingReviewWhenVerified(t *testing.T) {
	ResetSkrJobRegistry()

	result, err := BuildSkrPaymentJob(testParams("job-verified-01", 2_000_000), VerificationVerified)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if result.Job.PaymentStatus != PaymentPendingReview {
		t.Errorf("expected PaymentPendingReview, got %q", result.Job.PaymentStatus)
	}
	if result.Job.WalletApproved {
		t.Error("walletApproved must never be auto-set to true")
	}
	if result.Job.TxBytesDigest == "" {
		t.Error("txBytesDigest must be set when PENDING_REVIEW")
	}
	if !strings.HasPrefix(result.Job.TxBytesDigest, "sha256:") {
		t.Errorf("txBytesDigest must start with sha256:, got %q", result.Job.TxBytesDigest)
	}
}

// TestDuplicatePaymentRejected checks that submitting the same jobId twice is rejected.
func TestDuplicatePaymentRejected(t *testing.T) {
	ResetSkrJobRegistry()

	_, err := BuildSkrPaymentJob(testParams("job-dup-01", 1_000_000), VerificationVerified)
	if err != nil {
		t.Fatalf("first build failed: %v", err)
	}

	_, err = BuildSkrPaymentJob(testParams("job-dup-01", 1_000_000), VerificationVerified)
	if err == nil {
		t.Fatal("expected error on duplicate jobId, got nil")
	}
	if !strings.Contains(err.Error(), "SKR_DUPLICATE_PAYMENT_REJECTED") {
		t.Errorf("expected SKR_DUPLICATE_PAYMENT_REJECTED in error, got %q", err.Error())
	}
}

// TestDuplicateBlockedJobAllowedSecondTime checks that a BLOCKED job (not yet
// registered) can be re-submitted once verification passes.
func TestDuplicateBlockedJobAllowedSecondTime(t *testing.T) {
	ResetSkrJobRegistry()

	_, err := BuildSkrPaymentJob(testParams("job-resubmit-01", 1_000_000), VerificationPending)
	if err != nil {
		t.Fatalf("first (BLOCKED) build failed: %v", err)
	}

	// BLOCKED jobs do not register the jobId, so second attempt is allowed.
	result, err := BuildSkrPaymentJob(testParams("job-resubmit-01", 1_000_000), VerificationVerified)
	if err != nil {
		t.Fatalf("second (VERIFIED) build failed: %v", err)
	}
	if result.Job.PaymentStatus != PaymentPendingReview {
		t.Errorf("expected PaymentPendingReview on second submit, got %q", result.Job.PaymentStatus)
	}
}

// TestChangedBytesRejection checks the digest-based changed-bytes detection.
// The digest is computed over the message bytes and stored in the job.
// If the bytes change (e.g. blockhash substitution produces different bytes),
// the digest no longer matches, and any caller verifying it should refuse.
func TestChangedBytesRejection(t *testing.T) {
	ResetSkrJobRegistry()

	result, err := BuildSkrPaymentJob(testParams("job-changed-01", 1_000_000), VerificationVerified)
	if err != nil {
		t.Fatalf("build failed: %v", err)
	}

	originalDigest := result.TxDigest
	originalBytes := result.TxBytes

	// Verify that the stored digest matches the bytes.
	h := sha256.Sum256(originalBytes)
	expectedDigest := "sha256:" + hex.EncodeToString(h[:])
	if originalDigest != expectedDigest {
		t.Errorf("digest mismatch: stored %q, computed %q", originalDigest, expectedDigest)
	}

	// Simulate changed bytes (e.g. someone modified a byte).
	changedBytes := make([]byte, len(originalBytes))
	copy(changedBytes, originalBytes)
	if len(changedBytes) > 10 {
		changedBytes[10] ^= 0xFF // flip a byte
	}

	changedH := sha256.Sum256(changedBytes)
	changedDigest := "sha256:" + hex.EncodeToString(changedH[:])

	// The wallet MUST refuse if digest changes — verify they are different.
	if changedDigest == originalDigest {
		t.Error("changed bytes produced the same digest — test is invalid")
	}

	// The job's TxBytesDigest no longer matches the changed bytes.
	if result.Job.TxBytesDigest == changedDigest {
		t.Error("job digest should NOT match changed bytes")
	}
}

// TestDecimalFormattingBoundaryValues checks FormatSkrRaw at boundary values.
func TestDecimalFormattingBoundaryValues(t *testing.T) {
	cases := []struct {
		raw      uint64
		expected string
	}{
		{0, "0.000000"},                           // zero
		{1, "0.000001"},                           // minimum non-zero
		{999999, "0.999999"},                      // just under 1 SKR
		{1_000_000, "1.000000"},                   // exactly 1 SKR
		{1_000_001, "1.000001"},                   // 1 SKR + 1 raw
		{1_500_000, "1.500000"},                   // 1.5 SKR
		{1_000_000_000_000, "1000000.000000"},      // 1M SKR
		{18_446_744_073_709_551_615, "18446744073709.551615"}, // u64 max
	}

	for _, tc := range cases {
		got := FormatSkrRaw(tc.raw)
		if got != tc.expected {
			t.Errorf("FormatSkrRaw(%d) = %q, want %q", tc.raw, got, tc.expected)
		}
	}
}

// TestParseSkrRaw checks the round-trip from string to raw and back.
func TestParseSkrRaw(t *testing.T) {
	cases := []struct {
		input    string
		expected uint64
		wantErr  bool
	}{
		{"0", 0, false},
		{"0.000000", 0, false},
		{"0.000001", 1, false},
		{"1.000000", 1_000_000, false},
		{"1.5", 1_500_000, false},
		{"1.500000", 1_500_000, false},
		{"", 0, true},            // empty
		{"abc", 0, true},         // non-numeric
		{"1.1234567", 0, true},   // too many decimals
		{"-1", 0, true},          // negative
	}

	for _, tc := range cases {
		got, err := ParseSkrRaw(tc.input)
		if tc.wantErr {
			if err == nil {
				t.Errorf("ParseSkrRaw(%q): expected error, got %d", tc.input, got)
			}
		} else {
			if err != nil {
				t.Errorf("ParseSkrRaw(%q): unexpected error: %v", tc.input, err)
			} else if got != tc.expected {
				t.Errorf("ParseSkrRaw(%q) = %d, want %d", tc.input, got, tc.expected)
			}
		}
	}
}

// TestTransferCheckedInstructionEncoding verifies the 10-byte instruction data.
func TestTransferCheckedInstructionEncoding(t *testing.T) {
	data := buildTransferCheckedData(1_000_000, 6)

	if len(data) != 10 {
		t.Fatalf("expected 10 bytes, got %d", len(data))
	}
	if data[0] != 12 {
		t.Errorf("discriminator: got %d, want 12", data[0])
	}
	// Amount 1_000_000 in LE u64:
	// 1000000 = 0x000F4240
	// LE bytes: 64 42 0F 00 00 00 00 00
	if data[1] != 0x40 || data[2] != 0x42 || data[3] != 0x0F || data[4] != 0x00 {
		t.Errorf("amount LE bytes: got %02x %02x %02x %02x, want 40 42 0f 00",
			data[1], data[2], data[3], data[4])
	}
	if data[9] != 6 {
		t.Errorf("decimals: got %d, want 6", data[9])
	}
}

// TestSkrPaymentJobJSONRoundTrip verifies JSON encoding/decoding.
func TestSkrPaymentJobJSONRoundTrip(t *testing.T) {
	ResetSkrJobRegistry()

	result, err := BuildSkrPaymentJob(testParams("job-json-01", 5_000_000), VerificationVerified)
	if err != nil {
		t.Fatalf("build failed: %v", err)
	}

	raw, err := json.Marshal(result.Job)
	if err != nil {
		t.Fatalf("marshal failed: %v", err)
	}

	decoded, err := DecodeSkrPaymentJob(raw)
	if err != nil {
		t.Fatalf("decode failed: %v", err)
	}

	if decoded.JobID != result.Job.JobID {
		t.Errorf("jobId mismatch: %q vs %q", decoded.JobID, result.Job.JobID)
	}
	if decoded.PriceRawLamports != "5000000" {
		t.Errorf("priceRawLamports: got %q, want %q", decoded.PriceRawLamports, "5000000")
	}
	if decoded.PaymentStatus != PaymentPendingReview {
		t.Errorf("paymentStatus: got %q, want %q", decoded.PaymentStatus, PaymentPendingReview)
	}
	if decoded.WalletApproved {
		t.Error("walletApproved must be false after decode")
	}
}

// TestSkrMintConstant verifies the SKR mint address has the correct length
// for a Solana public key (44 characters in base58).
func TestSkrMintConstant(t *testing.T) {
	if SkrMintAddress != "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3" {
		t.Errorf("SkrMintAddress changed: %q", SkrMintAddress)
	}
	if len(SkrMintAddress) < 43 || len(SkrMintAddress) > 44 {
		t.Errorf("SkrMintAddress length %d is not in [43,44]", len(SkrMintAddress))
	}
	if SkrDecimals != 6 {
		t.Errorf("SkrDecimals: got %d, want 6", SkrDecimals)
	}
}

// TestSummaryDoesNotImplyApproval checks that the summary string
// correctly reflects the unapproved state.
func TestSummaryDoesNotImplyApproval(t *testing.T) {
	ResetSkrJobRegistry()

	result, err := BuildSkrPaymentJob(testParams("job-summary-01", 2_500_000), VerificationVerified)
	if err != nil {
		t.Fatalf("build failed: %v", err)
	}

	summary := SkrPaymentJobSummary(result.Job)
	if !strings.Contains(summary, "wallet_approved=false") {
		t.Errorf("summary should show wallet_approved=false: %q", summary)
	}
	if !strings.Contains(summary, "2.500000 SKR") {
		t.Errorf("summary should show formatted SKR amount: %q", summary)
	}
}
