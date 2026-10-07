// Package consent handles consent and revocation logic for sessions.
package consent

import (
	"crypto/ed25519"
	"encoding/hex"
	"fmt"
	"time"
)

// RevocationReason explains why a session was revoked.
type RevocationReason string

const (
	ExplicitRevocation RevocationReason = "EXPLICIT"
	Compromised        RevocationReason = "COMPROMISED"
	ScopeViolation     RevocationReason = "SCOPE_VIOLATION"
	PolicyChange       RevocationReason = "POLICY_CHANGE"
	TokenExpired       RevocationReason = "TOKEN_EXPIRED"
)

// SessionRevocation records the revocation of a session with cryptographic proof.
type SessionRevocation struct {
	SessionID  string           `json:"sessionId"`
	Reason     RevocationReason `json:"reason"`
	Timestamp  int64            `json:"timestamp"` // Unix milliseconds, from OS not input
	Evidence   string           `json:"evidence"` // Hex-encoded Ed25519 signature
	RevokedBy  string           `json:"revokedBy"`
}

// RevocationLog manages immutable append-only revocation records.
type RevocationLog struct {
	revocations []SessionRevocation
	signingKey  ed25519.PrivateKey
}

// NewRevocationLog creates a new revocation log with the provided signing key.
func NewRevocationLog(signingKey ed25519.PrivateKey) *RevocationLog {
	return &RevocationLog{
		revocations: make([]SessionRevocation, 0),
		signingKey:  signingKey,
	}
}

// Revoke adds a new revocation entry with cryptographic signature.
// The signature is computed over a canonical representation of the revocation.
// Returns the signed revocation entry.
func (rl *RevocationLog) Revoke(sessionID string, reason RevocationReason, revokedBy string) (*SessionRevocation, error) {
	if sessionID == "" {
		return nil, fmt.Errorf("sessionID cannot be empty")
	}
	if revokedBy == "" {
		return nil, fmt.Errorf("revokedBy cannot be empty")
	}

	// Use OS time to prevent timestamp manipulation
	now := time.Now().UnixMilli()

	// Create canonical message: sessionId|reason|timestamp|revokedBy
	message := fmt.Sprintf("%s|%s|%d|%s", sessionID, reason, now, revokedBy)

	// Sign with Ed25519
	signature := ed25519.Sign(rl.signingKey, []byte(message))
	signatureHex := hex.EncodeToString(signature)

	rev := SessionRevocation{
		SessionID:  sessionID,
		Reason:     reason,
		Timestamp:  now,
		Evidence:   signatureHex,
		RevokedBy:  revokedBy,
	}

	// Append to immutable log (never update or delete)
	rl.revocations = append(rl.revocations, rev)

	return &rev, nil
}

// Get returns a revocation by sessionID if it exists (most recent only).
func (rl *RevocationLog) Get(sessionID string) *SessionRevocation {
	for i := len(rl.revocations) - 1; i >= 0; i-- {
		if rl.revocations[i].SessionID == sessionID {
			return &rl.revocations[i]
		}
	}
	return nil
}

// IsRevoked checks if a session has been revoked.
func (rl *RevocationLog) IsRevoked(sessionID string) bool {
	return rl.Get(sessionID) != nil
}

// GetAll returns all revocations in order.
func (rl *RevocationLog) GetAll() []SessionRevocation {
	result := make([]SessionRevocation, len(rl.revocations))
	copy(result, rl.revocations)
	return result
}

// Verify checks the signature of a revocation entry.
func (rev *SessionRevocation) Verify(publicKey ed25519.PublicKey) error {
	if rev.Evidence == "" {
		return fmt.Errorf("revocation has no evidence")
	}

	signature, err := hex.DecodeString(rev.Evidence)
	if err != nil {
		return fmt.Errorf("invalid evidence encoding: %w", err)
	}

	message := fmt.Sprintf("%s|%s|%d|%s", rev.SessionID, rev.Reason, rev.Timestamp, rev.RevokedBy)

	if !ed25519.Verify(publicKey, []byte(message), signature) {
		return fmt.Errorf("revocation signature verification failed")
	}

	return nil
}
