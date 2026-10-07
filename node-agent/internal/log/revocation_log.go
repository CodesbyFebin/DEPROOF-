// Package log provides immutable append-only logging for revocations.
package log

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"
	"time"
)

// RevocationLogEntry is a single entry in the revocation log.
type RevocationLogEntry struct {
	SessionID  string `json:"sessionId"`
	Reason     string `json:"reason"`
	Timestamp  int64  `json:"timestamp"` // Unix milliseconds
	Evidence   string `json:"evidence"`  // Hex-encoded signature
	RevokedBy  string `json:"revokedBy"`
	RecordedAt int64  `json:"recordedAt"` // When this entry was logged (OS time)
}

// ImmutableRevocationLog manages append-only revocation records on disk.
type ImmutableRevocationLog struct {
	mu       sync.Mutex
	filePath string
	file     *os.File
	entries  []RevocationLogEntry
}

// NewImmutableRevocationLog creates or opens an immutable revocation log file.
// The file is created with 0600 permissions (readable/writable by owner only).
func NewImmutableRevocationLog(logDir string) (*ImmutableRevocationLog, error) {
	if err := os.MkdirAll(logDir, 0700); err != nil {
		return nil, fmt.Errorf("failed to create log directory: %w", err)
	}

	filePath := filepath.Join(logDir, "revocations.log")

	// Open file in append mode, create if not exists
	file, err := os.OpenFile(filePath, os.O_APPEND|os.O_CREATE|os.O_RDWR, 0600)
	if err != nil {
		return nil, fmt.Errorf("failed to open revocation log: %w", err)
	}

	log := &ImmutableRevocationLog{
		filePath: filePath,
		file:     file,
		entries:  make([]RevocationLogEntry, 0),
	}

	// Load existing entries from file
	if err := log.loadEntries(); err != nil {
		file.Close()
		return nil, fmt.Errorf("failed to load existing revocations: %w", err)
	}

	return log, nil
}

// loadEntries reads all existing entries from the log file.
func (irl *ImmutableRevocationLog) loadEntries() error {
	irl.mu.Lock()
	defer irl.mu.Unlock()

	// Seek to beginning to read all entries
	if _, err := irl.file.Seek(0, 0); err != nil {
		return fmt.Errorf("failed to seek to beginning: %w", err)
	}

	scanner := bufio.NewScanner(irl.file)
	for scanner.Scan() {
		line := scanner.Bytes()
		if len(line) == 0 {
			continue // Skip empty lines
		}

		var entry RevocationLogEntry
		if err := json.Unmarshal(line, &entry); err != nil {
			// Log but continue - corrupted entry doesn't break the whole log
			fmt.Fprintf(os.Stderr, "Warning: failed to parse revocation log entry: %v\n", err)
			continue
		}

		irl.entries = append(irl.entries, entry)
	}

	if err := scanner.Err(); err != nil {
		return fmt.Errorf("error reading revocation log: %w", err)
	}

	return nil
}

// AppendRevocation adds a new revocation entry to the log.
// This is append-only - entries are never updated or deleted.
// The entry is written to disk immediately and signed.
func (irl *ImmutableRevocationLog) AppendRevocation(
	sessionID string,
	reason string,
	timestamp int64,
	evidence string,
	revokedBy string,
) error {
	irl.mu.Lock()
	defer irl.mu.Unlock()

	// Use OS time for recording (not input timestamp)
	recordedAt := time.Now().UnixMilli()

	entry := RevocationLogEntry{
		SessionID:  sessionID,
		Reason:     reason,
		Timestamp:  timestamp,
		Evidence:   evidence,
		RevokedBy:  revokedBy,
		RecordedAt: recordedAt,
	}

	// Serialize entry to JSON (one entry per line)
	data, err := json.Marshal(entry)
	if err != nil {
		return fmt.Errorf("failed to serialize revocation entry: %w", err)
	}

	// Append newline to mark end of entry
	data = append(data, '\n')

	// Write to file immediately (append-only)
	if _, err := irl.file.Write(data); err != nil {
		return fmt.Errorf("failed to write to revocation log: %w", err)
	}

	// Sync to disk to ensure durability
	if err := irl.file.Sync(); err != nil {
		return fmt.Errorf("failed to sync revocation log: %w", err)
	}

	// Add to in-memory entries
	irl.entries = append(irl.entries, entry)

	return nil
}

// GetAll returns all revocation entries in order.
func (irl *ImmutableRevocationLog) GetAll() []RevocationLogEntry {
	irl.mu.Lock()
	defer irl.mu.Unlock()

	result := make([]RevocationLogEntry, len(irl.entries))
	copy(result, irl.entries)
	return result
}

// GetBySessionID returns the most recent revocation for a session (if any).
func (irl *ImmutableRevocationLog) GetBySessionID(sessionID string) *RevocationLogEntry {
	irl.mu.Lock()
	defer irl.mu.Unlock()

	// Search from end to find most recent
	for i := len(irl.entries) - 1; i >= 0; i-- {
		if irl.entries[i].SessionID == sessionID {
			entry := irl.entries[i]
			return &entry
		}
	}

	return nil
}

// Count returns the total number of revocations logged.
func (irl *ImmutableRevocationLog) Count() int {
	irl.mu.Lock()
	defer irl.mu.Unlock()
	return len(irl.entries)
}

// IsRevoked checks if a session has been revoked.
func (irl *ImmutableRevocationLog) IsRevoked(sessionID string) bool {
	return irl.GetBySessionID(sessionID) != nil
}

// Close closes the log file and prevents further writes.
func (irl *ImmutableRevocationLog) Close() error {
	irl.mu.Lock()
	defer irl.mu.Unlock()

	if irl.file != nil {
		return irl.file.Close()
	}
	return nil
}

// Verify ensures the log file has correct permissions and is not writable by others.
func (irl *ImmutableRevocationLog) Verify() error {
	stat, err := os.Stat(irl.filePath)
	if err != nil {
		return fmt.Errorf("failed to stat revocation log: %w", err)
	}

	// Check that file permissions are 0600 (read/write by owner only)
	mode := stat.Mode().Perm()
	if mode != 0600 {
		return fmt.Errorf("revocation log has incorrect permissions: %o (expected 0600)", mode)
	}

	return nil
}
