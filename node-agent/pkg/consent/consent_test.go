package consent_test

import (
	"crypto/ed25519"
	"crypto/rand"
	"fmt"
	"testing"
	"time"

	"deproof.local/node-agent/internal/consent"
)

// setupKeys generates a test key pair.
func setupKeys() (ed25519.PublicKey, ed25519.PrivateKey) {
	pub, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		panic(fmt.Sprintf("failed to generate key: %v", err))
	}
	return pub, priv
}

// Test_RevocationLog_Revoke tests creating and verifying revocation entries.
func Test_RevocationLog_Revoke(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)

	// Test successful revocation
	rev, err := revLog.Revoke("session-123", consent.ExplicitRevocation, "user@example.com")
	if err != nil {
		t.Fatalf("failed to revoke: %v", err)
	}

	if rev.SessionID != "session-123" {
		t.Errorf("expected sessionID session-123, got %s", rev.SessionID)
	}

	if rev.Reason != consent.ExplicitRevocation {
		t.Errorf("expected reason EXPLICIT, got %s", rev.Reason)
	}

	if rev.Evidence == "" {
		t.Error("evidence should not be empty")
	}

	// Test empty sessionID
	_, err = revLog.Revoke("", consent.ExplicitRevocation, "user@example.com")
	if err == nil {
		t.Error("expected error for empty sessionID")
	}

	// Test empty revokedBy
	_, err = revLog.Revoke("session-123", consent.ExplicitRevocation, "")
	if err == nil {
		t.Error("expected error for empty revokedBy")
	}
}

// Test_RevocationLog_IsRevoked tests revocation status checking.
func Test_RevocationLog_IsRevoked(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)

	// Initially not revoked
	if revLog.IsRevoked("session-123") {
		t.Error("session should not be revoked initially")
	}

	// Revoke it
	revLog.Revoke("session-123", consent.Compromised, "admin")
	if !revLog.IsRevoked("session-123") {
		t.Error("session should be revoked after revocation")
	}

	// Other sessions should not be revoked
	if revLog.IsRevoked("session-999") {
		t.Error("other sessions should not be revoked")
	}
}

// Test_RevocationLog_GetAll tests retrieving all revocations.
func Test_RevocationLog_GetAll(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)

	revLog.Revoke("session-1", consent.ExplicitRevocation, "user1@example.com")
	revLog.Revoke("session-2", consent.Compromised, "user2@example.com")
	revLog.Revoke("session-3", consent.ScopeViolation, "user3@example.com")

	all := revLog.GetAll()
	if len(all) != 3 {
		t.Errorf("expected 3 revocations, got %d", len(all))
	}
}

// Test_RevocationSignatureVerification tests signature verification.
func Test_RevocationSignatureVerification(t *testing.T) {
	pub, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)

	rev, _ := revLog.Revoke("session-123", consent.ExplicitRevocation, "admin")

	// Should verify with correct public key
	err := rev.Verify(pub)
	if err != nil {
		t.Fatalf("signature verification failed: %v", err)
	}

	// Should fail with wrong public key
	_, wrongPriv := setupKeys()
	wrongPub := wrongPriv.Public().(ed25519.PublicKey)

	err = rev.Verify(wrongPub)
	if err == nil {
		t.Error("signature verification should fail with wrong key")
	}
}

// Test_DelegatedActionScope_IsExpired tests scope expiration.
func Test_DelegatedActionScope_IsExpired(t *testing.T) {
	// Future scope should not be expired
	future := consent.NewDelegatedActionScope("session-123", []consent.ActionType{consent.ActionTransfer}, time.Hour)
	if future.IsExpired() {
		t.Error("future scope should not be expired")
	}

	// Past scope should be expired
	past := consent.NewDelegatedActionScope("session-456", []consent.ActionType{consent.ActionTransfer}, -time.Hour)
	if !past.IsExpired() {
		t.Error("past scope should be expired")
	}
}

// Test_DelegatedActionScope_IsActionAllowed tests action permission checking.
func Test_DelegatedActionScope_IsActionAllowed(t *testing.T) {
	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer, consent.ActionStake},
		time.Hour,
	)

	if !scope.IsActionAllowed(consent.ActionTransfer) {
		t.Error("transfer should be allowed")
	}

	if !scope.IsActionAllowed(consent.ActionStake) {
		t.Error("stake should be allowed")
	}

	if scope.IsActionAllowed(consent.ActionUnstake) {
		t.Error("unstake should not be allowed")
	}
}

// Test_DelegatedActionScope_ValidateAction tests comprehensive action validation.
func Test_DelegatedActionScope_ValidateAction(t *testing.T) {
	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)

	// Valid action
	err := scope.ValidateAction(consent.ActionTransfer, 500, 1)
	if err != nil {
		t.Errorf("valid action should not error: %v", err)
	}

	// Disallowed action
	err = scope.ValidateAction(consent.ActionStake, 500, 1)
	if err == nil {
		t.Error("disallowed action should error")
	}

	// Transaction too large
	err = scope.ValidateAction(consent.ActionTransfer, 2000, 1)
	if err == nil {
		t.Error("oversized transaction should error")
	}

	// Too many instructions
	err = scope.ValidateAction(consent.ActionTransfer, 500, 10)
	if err == nil {
		t.Error("too many instructions should error")
	}
}

// Test_DelegatedActionScope_ClusterRestriction tests cluster protection.
func Test_DelegatedActionScope_ClusterRestriction(t *testing.T) {
	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)
	scope.Constraints.ClusterRestriction = "devnet"

	// Correct cluster should pass
	err := scope.ClusterRestrictionViolation("devnet")
	if err != nil {
		t.Errorf("correct cluster should not error: %v", err)
	}

	// Wrong cluster should fail
	err = scope.ClusterRestrictionViolation("mainnet")
	if err == nil {
		t.Error("wrong cluster should error")
	}
}

// Test_DelegatedActionScope_TokenTransferValidation tests token transfer validation.
func Test_DelegatedActionScope_TokenTransferValidation(t *testing.T) {
	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)
	scope.Constraints.TokenTransferMaxAmount = "1000"
	scope.Constraints.StakingPoolWhitelist = []string{"pool-addr-1", "pool-addr-2"}

	// Valid transfer
	err := scope.TokenTransferValidation("500", "pool-addr-1", scope.Constraints.StakingPoolWhitelist)
	if err != nil {
		t.Errorf("valid transfer should not error: %v", err)
	}

	// Amount exceeds max
	err = scope.TokenTransferValidation("2000", "pool-addr-1", scope.Constraints.StakingPoolWhitelist)
	if err == nil {
		t.Error("exceeding transfer should error")
	}

	// Destination not in whitelist
	err = scope.TokenTransferValidation("500", "pool-addr-3", scope.Constraints.StakingPoolWhitelist)
	if err == nil {
		t.Error("non-whitelisted destination should error")
	}
}

// Test_SessionValidator_ValidateSessionExists tests session existence checking.
func Test_SessionValidator_ValidateSessionExists(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)
	validator := consent.NewSessionValidator(revLog)

	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)
	validator.RegisterScope(scope)

	// Existing session should be valid
	err := validator.ValidateSessionExists("session-123")
	if err != nil {
		t.Errorf("existing session should be valid: %v", err)
	}

	// Non-existing session should fail
	err = validator.ValidateSessionExists("session-999")
	if err == nil {
		t.Error("non-existing session should fail")
	}

	// Revoked session should fail
	revLog.Revoke("session-123", consent.ExplicitRevocation, "admin")
	err = validator.ValidateSessionExists("session-123")
	if err == nil {
		t.Error("revoked session should fail")
	}
}

// Test_SessionValidator_ValidateAction tests action validation through validator.
func Test_SessionValidator_ValidateAction(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)
	validator := consent.NewSessionValidator(revLog)

	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)
	validator.RegisterScope(scope)

	// Valid action
	err := validator.ValidateAction("session-123", consent.ActionTransfer, 500, 1)
	if err != nil {
		t.Errorf("valid action should not error: %v", err)
	}

	// Action on non-existing session
	err = validator.ValidateAction("session-999", consent.ActionTransfer, 500, 1)
	if err == nil {
		t.Error("non-existing session should fail")
	}

	// Revoke and test
	revLog.Revoke("session-123", consent.ExplicitRevocation, "admin")
	err = validator.ValidateAction("session-123", consent.ActionTransfer, 500, 1)
	if err == nil {
		t.Error("revoked session should fail")
	}
}

// Test_SessionValidator_CrossClusterProtection tests mainnet protection.
func Test_SessionValidator_CrossClusterProtection(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)
	validator := consent.NewSessionValidator(revLog)

	// Devnet scope
	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionStake},
		time.Hour,
	)
	scope.Constraints.ClusterRestriction = "devnet"
	validator.RegisterScope(scope)

	// Devnet action should pass
	err := validator.ValidateClusterAction("session-123", "devnet")
	if err != nil {
		t.Errorf("devnet action should not error: %v", err)
	}

	// Mainnet action should fail (cross-cluster protection)
	err = validator.ValidateClusterAction("session-123", "mainnet")
	if err == nil {
		t.Error("cross-cluster action should fail")
	}
}

// Test_SessionValidator_RevokeSession tests revoking sessions.
func Test_SessionValidator_RevokeSession(t *testing.T) {
	_, priv := setupKeys()
	revLog := consent.NewRevocationLog(priv)
	validator := consent.NewSessionValidator(revLog)

	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)
	validator.RegisterScope(scope)

	// Revoke through validator
	rev, err := validator.RevokeSession("session-123", consent.ScopeViolation, "admin")
	if err != nil {
		t.Fatalf("failed to revoke: %v", err)
	}

	if rev.Reason != consent.ScopeViolation {
		t.Errorf("expected reason SCOPE_VIOLATION, got %s", rev.Reason)
	}

	// Session should be revoked
	err = validator.ValidateSessionExists("session-123")
	if err == nil {
		t.Error("revoked session should fail validation")
	}
}

// Test_RateLimiting tests rate limit enforcement.
func Test_RateLimiting(t *testing.T) {
	scope := consent.NewDelegatedActionScope(
		"session-123",
		[]consent.ActionType{consent.ActionTransfer},
		time.Hour,
	)
	scope.RateLimitPerMin = 3

	// Should allow up to rate limit
	for i := 0; i < 3; i++ {
		err := scope.ValidateAction(consent.ActionTransfer, 500, 1)
		if err != nil {
			t.Errorf("action %d should be allowed: %v", i+1, err)
		}
	}

	// Fourth request should fail
	err := scope.ValidateAction(consent.ActionTransfer, 500, 1)
	if err == nil {
		t.Error("fourth request should exceed rate limit")
	}
}
