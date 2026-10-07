// Package consent defines scope constraints for delegated actions.
package consent

import (
	"fmt"
	"strconv"
	"time"
)

// ActionType defines the kinds of actions that can be delegated.
type ActionType string

const (
	ActionTransfer       ActionType = "TRANSFER"
	ActionStake          ActionType = "STAKE"
	ActionUnstake        ActionType = "UNSTAKE"
	ActionClaim          ActionType = "CLAIM"
	ActionSwap           ActionType = "SWAP"
	ActionReadOnly       ActionType = "READ_ONLY"
	ActionProofSubmit    ActionType = "PROOF_SUBMIT"
	ActionOperationPoll  ActionType = "OPERATION_POLL"
)

// ActionConstraints defines runtime limits for delegated actions.
type ActionConstraints struct {
	MaxTransactionSize   int64             `json:"maxTransactionSize"`   // Bytes
	MaxInstructionCount  int64             `json:"maxInstructionCount"`  // Count
	TokenTransferMaxAmount string           `json:"tokenTransferMaxAmount"` // Raw amount as string
	StakingPoolWhitelist []string          `json:"stakingPoolWhitelist"`  // Allowed pool addresses
	ClusterRestriction   string            `json:"clusterRestriction"`   // "devnet", "testnet", "mainnet"
	OperationTimeoutSec  int64             `json:"operationTimeoutSec"`  // Timeout for operation completion
	MaxConcurrentOps     int64             `json:"maxConcurrentOps"`     // Max concurrent operations
}

// DelegatedActionScope defines what actions can be performed in a session and under what constraints.
type DelegatedActionScope struct {
	SessionID         string             `json:"sessionId"`
	AllowedActions    []ActionType       `json:"allowedActions"`
	Constraints       ActionConstraints  `json:"constraints"`
	ExpiresAt         int64              `json:"expiresAt"` // Unix seconds
	CreatedAt         int64              `json:"createdAt"` // Unix seconds
	DelegatedBy       string             `json:"delegatedBy"` // Node authority
	RateLimitPerMin   int64              `json:"rateLimitPerMin"`
	CurrentRequestCount int64            `json:"currentRequestCount"` // For tracking rate limit
	LastRequestAt     int64              `json:"lastRequestAt"`       // Unix milliseconds
}

// NewDelegatedActionScope creates a new scope with defaults.
func NewDelegatedActionScope(sessionID string, allowedActions []ActionType, expiresIn time.Duration) *DelegatedActionScope {
	now := time.Now()
	return &DelegatedActionScope{
		SessionID:      sessionID,
		AllowedActions: allowedActions,
		ExpiresAt:      now.Add(expiresIn).Unix(),
		CreatedAt:      now.Unix(),
		Constraints: ActionConstraints{
			MaxTransactionSize:   1280,        // Standard Solana transaction max
			MaxInstructionCount:  4,
			TokenTransferMaxAmount: "1000000000", // 1 token with 9 decimals
			ClusterRestriction:   "devnet",
			OperationTimeoutSec:  300,
			MaxConcurrentOps:     1,
		},
		RateLimitPerMin:     10,
		CurrentRequestCount: 0,
	}
}

// IsExpired checks if the scope has expired.
func (s *DelegatedActionScope) IsExpired() bool {
	return time.Now().Unix() > s.ExpiresAt
}

// IsActionAllowed checks if an action type is in the allowed set.
func (s *DelegatedActionScope) IsActionAllowed(action ActionType) bool {
	for _, allowed := range s.AllowedActions {
		if allowed == action {
			return true
		}
	}
	return false
}

// ValidateAction performs comprehensive validation of an action against scope constraints.
func (s *DelegatedActionScope) ValidateAction(action ActionType, txSize int64, instructionCount int64) error {
	// Check expiration
	if s.IsExpired() {
		return fmt.Errorf("scope expired")
	}

	// Check action permission
	if !s.IsActionAllowed(action) {
		return fmt.Errorf("action %s not allowed in scope", action)
	}

	// Check transaction size
	if txSize > s.Constraints.MaxTransactionSize {
		return fmt.Errorf("transaction size %d exceeds max %d", txSize, s.Constraints.MaxTransactionSize)
	}

	// Check instruction count
	if instructionCount > s.Constraints.MaxInstructionCount {
		return fmt.Errorf("instruction count %d exceeds max %d", instructionCount, s.Constraints.MaxInstructionCount)
	}

	// Check rate limiting
	now := time.Now().UnixMilli()
	oneMinutesAgo := now - 60000 // 60 seconds in ms

	// Reset counter if minute has passed
	if s.LastRequestAt < oneMinutesAgo {
		s.CurrentRequestCount = 0
	}

	s.CurrentRequestCount++
	if s.CurrentRequestCount > s.RateLimitPerMin {
		return fmt.Errorf("rate limit exceeded: %d requests per minute", s.RateLimitPerMin)
	}

	s.LastRequestAt = now

	return nil
}

// ClusterRestrictionViolation checks if an action targets the wrong cluster.
func (s *DelegatedActionScope) ClusterRestrictionViolation(targetCluster string) error {
	if s.Constraints.ClusterRestriction != "" && s.Constraints.ClusterRestriction != targetCluster {
		return fmt.Errorf("cluster restriction violated: scope allows %s but action targets %s",
			s.Constraints.ClusterRestriction, targetCluster)
	}
	return nil
}

// TokenTransferValidation validates token transfer against constraints.
func (s *DelegatedActionScope) TokenTransferValidation(amount string, destinationAddress string, poolWhitelist []string) error {
	// Validate action is permitted
	if err := s.ValidateAction(ActionTransfer, 0, 1); err != nil {
		return err
	}

	// Compare amounts numerically (parse as integers to avoid floating point)
	amountInt := parseStringInt(amount)
	maxAmountInt := parseStringInt(s.Constraints.TokenTransferMaxAmount)
	if amountInt > maxAmountInt {
		return fmt.Errorf("transfer amount %s exceeds max %s", amount, s.Constraints.TokenTransferMaxAmount)
	}

	// Validate destination is in whitelist if whitelist exists
	if len(s.Constraints.StakingPoolWhitelist) > 0 {
		allowed := false
		for _, addr := range s.Constraints.StakingPoolWhitelist {
			if addr == destinationAddress {
				allowed = true
				break
			}
		}
		if !allowed {
			return fmt.Errorf("destination %s not in whitelist", destinationAddress)
		}
	}

	return nil
}

// StakingActionValidation validates staking operations against constraints.
func (s *DelegatedActionScope) StakingActionValidation(poolId string, action ActionType) error {
	// Validate staking action is permitted
	if !s.IsActionAllowed(action) {
		return fmt.Errorf("staking action %s not allowed in scope", action)
	}

	// Validate pool is whitelisted
	if len(s.Constraints.StakingPoolWhitelist) > 0 {
		allowed := false
		for _, addr := range s.Constraints.StakingPoolWhitelist {
			if addr == poolId {
				allowed = true
				break
			}
		}
		if !allowed {
			return fmt.Errorf("pool %s not in whitelist", poolId)
		}
	}

	return s.ValidateAction(action, 0, 1)
}

// parseStringInt parses a string as a 64-bit integer, defaulting to 0 on error.
func parseStringInt(s string) int64 {
	val, err := strconv.ParseInt(s, 10, 64)
	if err != nil {
		return 0
	}
	return val
}
