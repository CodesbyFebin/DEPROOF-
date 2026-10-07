// Package consent provides session validation logic.
package consent

import (
	"fmt"
)

// SessionValidator validates commands against session scope and revocation status.
type SessionValidator struct {
	revocationLog *RevocationLog
	scopes        map[string]*DelegatedActionScope // sessionID -> scope
}

// NewSessionValidator creates a new session validator.
func NewSessionValidator(revocationLog *RevocationLog) *SessionValidator {
	return &SessionValidator{
		revocationLog: revocationLog,
		scopes:        make(map[string]*DelegatedActionScope),
	}
}

// RegisterScope registers a new scope for a session.
func (sv *SessionValidator) RegisterScope(scope *DelegatedActionScope) error {
	if scope.SessionID == "" {
		return fmt.Errorf("scope requires sessionID")
	}
	sv.scopes[scope.SessionID] = scope
	return nil
}

// ValidateSessionExists checks if a session exists and is not revoked.
func (sv *SessionValidator) ValidateSessionExists(sessionID string) error {
	// Check revocation status first
	if sv.revocationLog.IsRevoked(sessionID) {
		rev := sv.revocationLog.Get(sessionID)
		return fmt.Errorf("session %s is revoked: %s", sessionID, rev.Reason)
	}

	// Check if scope exists
	if _, exists := sv.scopes[sessionID]; !exists {
		return fmt.Errorf("session %s does not exist", sessionID)
	}

	return nil
}

// ValidateAction checks if an action is allowed for a session.
// Returns explicit reason if rejected.
func (sv *SessionValidator) ValidateAction(
	sessionID string,
	action ActionType,
	txSize int64,
	instructionCount int64,
) error {
	// Validate session exists and not revoked
	if err := sv.ValidateSessionExists(sessionID); err != nil {
		return err
	}

	scope := sv.scopes[sessionID]

	// Validate action against scope constraints
	return scope.ValidateAction(action, txSize, instructionCount)
}

// ValidateClusterAction checks if an action targets the correct cluster.
func (sv *SessionValidator) ValidateClusterAction(sessionID string, targetCluster string) error {
	if err := sv.ValidateSessionExists(sessionID); err != nil {
		return err
	}

	scope := sv.scopes[sessionID]
	return scope.ClusterRestrictionViolation(targetCluster)
}

// ValidateTokenTransfer validates a token transfer operation.
func (sv *SessionValidator) ValidateTokenTransfer(
	sessionID string,
	amount string,
	destinationAddress string,
) error {
	if err := sv.ValidateSessionExists(sessionID); err != nil {
		return err
	}

	scope := sv.scopes[sessionID]
	return scope.TokenTransferValidation(amount, destinationAddress, scope.Constraints.StakingPoolWhitelist)
}

// ValidateStakingAction validates a staking operation.
func (sv *SessionValidator) ValidateStakingAction(
	sessionID string,
	poolId string,
	action ActionType,
) error {
	if err := sv.ValidateSessionExists(sessionID); err != nil {
		return err
	}

	scope := sv.scopes[sessionID]
	return scope.StakingActionValidation(poolId, action)
}

// GetRevocationReason returns the reason a session was revoked, or empty string if not revoked.
func (sv *SessionValidator) GetRevocationReason(sessionID string) string {
	rev := sv.revocationLog.Get(sessionID)
	if rev == nil {
		return ""
	}
	return string(rev.Reason)
}

// GetScope returns the scope for a session if it exists.
func (sv *SessionValidator) GetScope(sessionID string) (*DelegatedActionScope, error) {
	if _, exists := sv.scopes[sessionID]; !exists {
		return nil, fmt.Errorf("scope for session %s not found", sessionID)
	}
	return sv.scopes[sessionID], nil
}

// RevokeSession revokes a session and returns the revocation.
func (sv *SessionValidator) RevokeSession(sessionID string, reason RevocationReason, revokedBy string) (*SessionRevocation, error) {
	rev, err := sv.revocationLog.Revoke(sessionID, reason, revokedBy)
	if err != nil {
		return nil, err
	}

	// Keep scope but mark as revoked for audit trail
	// Don't delete the scope mapping
	return rev, nil
}
