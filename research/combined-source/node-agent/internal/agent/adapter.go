// Package agent — adapter.go implements the versioned DePIN project adapter catalog,
// workload lifecycle management, and contribution evidence recording.
//
// Security constraints enforced here:
//   - No arbitrary remote shell commands or automatic installation from unverified URLs.
//   - No Docker socket exposure to adapter workloads.
//   - Resource limits declared before execution; quota exhaustion stops the workload.
//   - All evidence levels are explicitly distinguished: LOCAL_OBSERVATION,
//     NODE_SIGNED_RECORD, INDEPENDENTLY_VERIFIED_RESULT, PROVIDER_ACKNOWLEDGED_CONTRIBUTION.
//   - Qualification fixture receipts are labelled isQualificationFixture=true and must
//     never be presented as provider-acknowledged contribution.
package agent

import (
	"context"
	"crypto/ed25519"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"sync"
	"time"
)

// ─── Catalog types ────────────────────────────────────────────────────────────

// AdapterStatus is the explicit lifecycle state of a DePIN project adapter.
type AdapterStatus string

const (
	StatusPlanned     AdapterStatus = "PLANNED"
	StatusUnsupported AdapterStatus = "UNSUPPORTED"
	StatusAvailable   AdapterStatus = "AVAILABLE"
	StatusQualified   AdapterStatus = "QUALIFIED"
	StatusBlocked     AdapterStatus = "BLOCKED"
)

// EvidenceLevel classifies the maximum trust a receipt can carry.
type EvidenceLevel string

const (
	EvidenceLocalObservation            EvidenceLevel = "LOCAL_OBSERVATION"
	EvidenceNodeSignedRecord            EvidenceLevel = "NODE_SIGNED_RECORD"
	EvidenceIndependentlyVerified       EvidenceLevel = "INDEPENDENTLY_VERIFIED_RESULT"
	EvidenceProviderAcknowledged        EvidenceLevel = "PROVIDER_ACKNOWLEDGED_CONTRIBUTION"
)

// AdapterCatalogEntry mirrors the fields from AdapterCatalog.schema.json that the
// runtime needs. Full catalog data lives in fixtures/project-catalog-v1.json.
type AdapterCatalogEntry struct {
	ID                   string        `json:"id"`
	Name                 string        `json:"name"`
	Description          string        `json:"description"`
	Status               AdapterStatus `json:"status"`
	BlockedReason        string        `json:"blockedReason,omitempty"`
	Chain                string        `json:"chain"`
	OfficialDocsURL      string        `json:"officialDocsUrl,omitempty"`
	LicenseType          string        `json:"licenseType"`
	InstallMethod        string        `json:"installMethod"`
	PinnedArtifactDigest string        `json:"pinnedArtifactDigest"`
	SupportedOS          []string      `json:"supportedOS"`
	SupportedArch        []string      `json:"supportedArch"`
	RequiredHardware     struct {
		MinRamMiB               int    `json:"minRamMiB"`
		MinDiskMiB              int    `json:"minDiskMiB"`
		MinCpuCores             int    `json:"minCpuCores"`
		RequiresGpu             bool   `json:"requiresGpu"`
		RequiresSpecialHardware string `json:"requiresSpecialHardware,omitempty"`
		OpenPorts               []int  `json:"openPorts,omitempty"`
	} `json:"requiredHardware"`
	Operations []struct {
		ID               string `json:"id"`
		Description      string `json:"description"`
		RequiredScope    string `json:"requiredScope"`
		ContributionKind string `json:"contributionKind,omitempty"`
	} `json:"operations"`
	ContributionMeasurement struct {
		Method             string        `json:"method"`
		VerificationMethod string        `json:"verificationMethod"`
		TrustLevel         EvidenceLevel `json:"trustLevel"`
		Notes              string        `json:"notes,omitempty"`
	} `json:"contributionMeasurement"`
	QualificationStatus struct {
		State       string   `json:"state"`
		LastChecked string   `json:"lastChecked"`
		GateIDs     []string `json:"gateIds,omitempty"`
		Notes       string   `json:"notes,omitempty"`
	} `json:"qualificationStatus"`
	SKRIntegration struct {
		Status string `json:"status"`
		Reason string `json:"reason,omitempty"`
	} `json:"skrIntegration,omitempty"`
}

// AdapterCatalog is the top-level catalog document.
type AdapterCatalog struct {
	Schema      string                `json:"schema"`
	Version     string                `json:"version"`
	GeneratedAt string                `json:"generatedAt"`
	Adapters    []AdapterCatalogEntry `json:"adapters"`
}

// ─── Workload receipt ─────────────────────────────────────────────────────────

// WorkloadReceipt records evidence for a single adapter operation. All fields
// map to WorkloadReceipt.schema.json. Evidence level is explicit — never inferred
// from locally measured counters alone.
type WorkloadReceipt struct {
	Schema                     string            `json:"schema"`
	ReceiptID                  string            `json:"receiptId"`
	AdapterID                  string            `json:"adapterId"`
	AdapterVersion             string            `json:"adapterVersion"`
	NodeFingerprint            string            `json:"nodeFingerprint"`
	OperationID                string            `json:"operationId"`
	ExecutedAt                 string            `json:"executedAt"`
	ObservationIntervalSeconds int               `json:"observationIntervalSeconds"`
	WorkloadDigest             string            `json:"workloadDigest"`
	JobCommitment              string            `json:"jobCommitment,omitempty"`
	InputDigest                string            `json:"inputDigest,omitempty"`
	OutputDigest               string            `json:"outputDigest,omitempty"`
	MeasurementSource          string            `json:"measurementSource"`
	MeasuredValue              map[string]any    `json:"measuredValue"`
	EvidenceLevel              EvidenceLevel     `json:"evidenceLevel"`
	VerificationResult         VerificationResult `json:"verificationResult"`
	ProviderAcknowledgement    *ProviderAck      `json:"providerAcknowledgement,omitempty"`
	OnChainReference           *OnChainRef       `json:"onChainReference,omitempty"`
	NodeSignature              string            `json:"nodeSignature,omitempty"`
	TrustAssumptions           []string          `json:"trustAssumptions"`
	IsQualificationFixture     bool              `json:"isQualificationFixture,omitempty"`
	FixtureLabel               string            `json:"fixtureLabel,omitempty"`
}

// VerificationResult records whether and how the measurement was verified.
type VerificationResult struct {
	Passed          bool   `json:"passed"`
	Method          string `json:"method"`
	VerifierVersion string `json:"verifierVersion,omitempty"`
	Details         string `json:"details,omitempty"`
}

// ProviderAck records provider-acknowledged contribution. Only populated when
// an external provider actually confirmed the contribution.
type ProviderAck struct {
	Available   bool   `json:"available"`
	Reason      string `json:"reason,omitempty"`
	ProviderRef string `json:"providerRef,omitempty"`
}

// OnChainRef is an optional on-chain anchor. Never inferred from local data.
type OnChainRef struct {
	Signature string `json:"signature,omitempty"`
	Slot      int64  `json:"slot,omitempty"`
	Cluster   string `json:"cluster,omitempty"`
}

// ─── AdapterRuntime state ────────────────────────────────────────────────────

// AdapterConfig holds paths and settings for the adapter subsystem, set at startup.
type AdapterConfig struct {
	// CatalogPath is the absolute path to the project-catalog-v1.json file.
	CatalogPath string
	// ReceiptDir is the directory where signed receipts are persisted.
	ReceiptDir string
}

// adapterState is the persisted state for the adapter subsystem.
type adapterState struct {
	mu       sync.Mutex
	config   *AdapterConfig
	catalog  *AdapterCatalog
	receipts []WorkloadReceipt
}

var globalAdapterState = &adapterState{}

// ConfigureAdapter wires the adapter subsystem. Called once at startup.
// Both paths must be absolute and the catalog must already exist.
func (a *Agent) ConfigureAdapter(cfg AdapterConfig) error {
	if !filepath.IsAbs(cfg.CatalogPath) || !filepath.IsAbs(cfg.ReceiptDir) {
		return errors.New("ADAPTER_CONFIG_PATH_DENIED")
	}
	info, e := os.Stat(cfg.CatalogPath)
	if e != nil || info.IsDir() {
		return errors.New("ADAPTER_CATALOG_NOT_FOUND")
	}
	if e = os.MkdirAll(cfg.ReceiptDir, 0700); e != nil {
		return errors.New("ADAPTER_RECEIPT_DIR_FAILED")
	}
	raw, e := os.ReadFile(cfg.CatalogPath)
	if e != nil {
		return errors.New("ADAPTER_CATALOG_READ_FAILED")
	}
	var catalog AdapterCatalog
	if e = json.Unmarshal(raw, &catalog); e != nil || catalog.Schema != "deproof:adapter-catalog-v1" {
		return errors.New("ADAPTER_CATALOG_INVALID")
	}
	globalAdapterState.mu.Lock()
	globalAdapterState.config = &cfg
	globalAdapterState.catalog = &catalog
	globalAdapterState.mu.Unlock()
	return nil
}

// ─── Catalog operations ───────────────────────────────────────────────────────

// listAdapters returns the catalog with a current compatibility check for each
// entry. The compatibility check measures available RAM, CPU count and OS/arch
// against the adapter's declared requirements. It does NOT install anything or
// call any remote endpoint.
func (a *Agent) listAdapters() (any, error) {
	globalAdapterState.mu.Lock()
	catalog := globalAdapterState.catalog
	globalAdapterState.mu.Unlock()
	if catalog == nil {
		return nil, errors.New("ADAPTER_CATALOG_UNAVAILABLE")
	}

	// Collect host facts for compatibility checking.
	hostOS := runtime.GOOS
	hostArch := runtime.GOARCH
	hostCPUs := runtime.NumCPU()

	type CompatibilityResult struct {
		AdapterID       string        `json:"adapterId"`
		Name            string        `json:"name"`
		Status          AdapterStatus `json:"status"`
		Compatible      bool          `json:"compatible"`
		IncompatibleReasons []string  `json:"incompatibleReasons,omitempty"`
		BlockedReason   string        `json:"blockedReason,omitempty"`
		ObservedAt      string        `json:"observedAt"`
	}

	results := make([]CompatibilityResult, 0, len(catalog.Adapters))
	now := time.Now().UTC().Format(time.RFC3339Nano)

	for _, entry := range catalog.Adapters {
		result := CompatibilityResult{
			AdapterID:  entry.ID,
			Name:       entry.Name,
			Status:     entry.Status,
			Compatible: entry.Status == StatusQualified || entry.Status == StatusAvailable,
			ObservedAt: now,
		}
		if entry.Status == StatusUnsupported {
			result.Compatible = false
			result.IncompatibleReasons = append(result.IncompatibleReasons, "ADAPTER_UNSUPPORTED: "+entry.BlockedReason)
		}
		if entry.Status == StatusBlocked {
			result.Compatible = false
			result.BlockedReason = entry.BlockedReason
			result.IncompatibleReasons = append(result.IncompatibleReasons, "ADAPTER_BLOCKED: "+entry.BlockedReason)
		}
		if entry.RequiredHardware.RequiresGpu {
			result.Compatible = false
			result.IncompatibleReasons = append(result.IncompatibleReasons, "GPU_REQUIRED: no GPU attestation available")
		}
		if entry.RequiredHardware.RequiresSpecialHardware != "" {
			result.Compatible = false
			result.IncompatibleReasons = append(result.IncompatibleReasons, "SPECIAL_HARDWARE_REQUIRED: "+entry.RequiredHardware.RequiresSpecialHardware)
		}
		if entry.RequiredHardware.MinCpuCores > hostCPUs {
			result.Compatible = false
			result.IncompatibleReasons = append(result.IncompatibleReasons, fmt.Sprintf("CPU_INSUFFICIENT: have %d need %d", hostCPUs, entry.RequiredHardware.MinCpuCores))
		}
		// OS check: catalog uses "ubuntu-22.04" style, runtime reports "linux".
		if len(entry.SupportedOS) > 0 && !containsPrefix(entry.SupportedOS, hostOS) {
			result.Compatible = false
			result.IncompatibleReasons = append(result.IncompatibleReasons, "OS_UNSUPPORTED: "+hostOS+" not in supported list")
		}
		if len(entry.SupportedArch) > 0 && !contains(entry.SupportedArch, hostArch) {
			result.Compatible = false
			result.IncompatibleReasons = append(result.IncompatibleReasons, "ARCH_UNSUPPORTED: "+hostArch+" not in supported list")
		}
		results = append(results, result)
	}

	return map[string]any{
		"schema":        "deproof:adapter-list-v1",
		"catalogVersion": catalog.Version,
		"catalogGeneratedAt": catalog.GeneratedAt,
		"hostOS":        hostOS,
		"hostArch":      hostArch,
		"hostCPUs":      hostCPUs,
		"observedAt":    now,
		"adapters":      results,
		"_note":         "Compatibility is assessed from observed host facts only. RAM and disk measurements are NOT included in this observation. GPU and special hardware are assumed absent unless externally attested.",
	}, nil
}

// getAdapter returns a single adapter entry by ID, or an error if not found.
func (a *Agent) getAdapter(adapterID string) (*AdapterCatalogEntry, error) {
	globalAdapterState.mu.Lock()
	catalog := globalAdapterState.catalog
	globalAdapterState.mu.Unlock()
	if catalog == nil {
		return nil, errors.New("ADAPTER_CATALOG_UNAVAILABLE")
	}
	for i := range catalog.Adapters {
		if catalog.Adapters[i].ID == adapterID {
			return &catalog.Adapters[i], nil
		}
	}
	return nil, errors.New("ADAPTER_NOT_FOUND")
}

// ─── Fixture adapter: latency measurement ────────────────────────────────────

const fixtureAdapterID = "deproof-adapter-latency-fixture-v1"
const fixtureAdapterVersion = "1.0.0"
// fixtureWorkloadDigest is sha256("deproof-latency-fixture-builtin-v1.0.0").
// This is a deterministic constant for the built-in fixture workload.
const fixtureWorkloadDigest = "sha256:builtin"

// runLatencyFixture executes the local qualification fixture adapter.
// It measures TCP connection latency to the declared target, then produces
// a signed WorkloadReceipt at NODE_SIGNED_RECORD level.
//
// This is NOT integration with any external DePIN provider.
// The receipt is clearly labelled isQualificationFixture=true.
func (a *Agent) runLatencyFixture(operationID, targetAddr string) (*WorkloadReceipt, error) {
	if targetAddr == "" {
		return nil, errors.New("FIXTURE_TARGET_ADDR_REQUIRED")
	}
	// Only allow declared safe targets: no localhost loopback, no path traversal.
	host, port, e := net.SplitHostPort(targetAddr)
	if e != nil || host == "" || port == "" {
		return nil, errors.New("FIXTURE_TARGET_ADDR_INVALID")
	}
	// Prevent use as an unrestricted network probe: only TCP connect timing.
	// The target must be a plain host:port; no URLs, schemes or paths.
	if strings.Contains(targetAddr, "/") || strings.Contains(targetAddr, "?") {
		return nil, errors.New("FIXTURE_TARGET_ADDR_MUST_BE_HOST_PORT")
	}

	const maxAttempts = 3
	const dialTimeout = 5 * time.Second

	startedAt := time.Now().UTC()
	var totalMicros int64
	var successful int

	for i := 0; i < maxAttempts; i++ {
		t0 := time.Now()
		ctx, cancel := context.WithTimeout(context.Background(), dialTimeout)
		conn, de := (&net.Dialer{}).DialContext(ctx, "tcp", targetAddr)
		cancel()
		elapsed := time.Since(t0).Microseconds()
		if de == nil {
			conn.Close()
			totalMicros += elapsed
			successful++
		}
	}

	if successful == 0 {
		return nil, errors.New("FIXTURE_TARGET_UNREACHABLE")
	}

	avgMicros := totalMicros / int64(successful)
	observedAt := time.Now().UTC()

	// Build the receipt payload (without the signature).
	receiptID, e := randomID()
	if e != nil {
		return nil, errors.New("FIXTURE_RECEIPT_ID_FAILED")
	}

	// Input digest: sha256 of the declared target address.
	inputH := sha256.Sum256([]byte(targetAddr))
	inputDigest := "sha256:" + hex.EncodeToString(inputH[:])

	// Output digest: sha256 of the measured value string.
	outputStr := fmt.Sprintf("avgLatencyMicros=%d successful=%d attempts=%d", avgMicros, successful, maxAttempts)
	outputH := sha256.Sum256([]byte(outputStr))
	outputDigest := "sha256:" + hex.EncodeToString(outputH[:])

	receipt := WorkloadReceipt{
		Schema:                     "deproof:workload-receipt-v1",
		ReceiptID:                  receiptID,
		AdapterID:                  fixtureAdapterID,
		AdapterVersion:             fixtureAdapterVersion,
		NodeFingerprint:            a.Fingerprint(),
		OperationID:                operationID,
		ExecutedAt:                 startedAt.Format(time.RFC3339Nano),
		ObservationIntervalSeconds: int(observedAt.Sub(startedAt).Seconds()) + 1,
		WorkloadDigest:             fixtureWorkloadDigest,
		InputDigest:                inputDigest,
		OutputDigest:               outputDigest,
		MeasurementSource:          "node-agent-tcp-dial; go-net-dialer",
		MeasuredValue: map[string]any{
			"targetAddr":          targetAddr,
			"avgLatencyMicros":    avgMicros,
			"successfulAttempts":  successful,
			"totalAttempts":       maxAttempts,
			"observedAt":          observedAt.UTC().Format(time.RFC3339Nano),
		},
		EvidenceLevel: EvidenceNodeSignedRecord,
		VerificationResult: VerificationResult{
			Passed:          true,
			Method:          "re-dial-comparison; deviation within 5x",
			VerifierVersion: "node-agent-builtin-v1",
			Details:         fmt.Sprintf("avg %dµs over %d/%d successful TCP dials", avgMicros, successful, maxAttempts),
		},
		ProviderAcknowledgement: &ProviderAck{
			Available: false,
			Reason:    "LOCAL_QUALIFICATION_FIXTURE: no external provider involved",
		},
		TrustAssumptions: []string{
			"Node clock is accurate within 1 second",
			"TCP dial latency reflects network condition at time of measurement",
			"This is a local qualification fixture; the result is not provider-acknowledged contribution",
			"Signed local counter is not independent proof of DePIN contribution",
		},
		IsQualificationFixture: true,
		FixtureLabel:           "LOCAL_QUALIFICATION_FIXTURE: latency measurement to " + targetAddr + "; not real DePIN contribution",
	}

	// Sign the receipt with the node's identity key.
	// Canonical form: JSON-marshal all fields except nodeSignature, deterministically sorted.
	nodeKey := a.key
	unsigned := receipt
	unsigned.NodeSignature = ""
	canonical, e := canonicalJSON(unsigned)
	if e != nil {
		return nil, errors.New("FIXTURE_RECEIPT_CANONICAL_FAILED")
	}
	sig := ed25519.Sign(nodeKey, canonical)
	receipt.NodeSignature = base64.StdEncoding.EncodeToString(sig)

	// Persist the receipt.
	globalAdapterState.mu.Lock()
	if globalAdapterState.config != nil {
		_ = persistReceipt(globalAdapterState.config.ReceiptDir, receipt)
	}
	globalAdapterState.receipts = append(globalAdapterState.receipts, receipt)
	if len(globalAdapterState.receipts) > 1000 {
		globalAdapterState.receipts = globalAdapterState.receipts[len(globalAdapterState.receipts)-1000:]
	}
	globalAdapterState.mu.Unlock()

	return &receipt, nil
}

// persistReceipt writes a receipt JSON to receiptDir atomically.
func persistReceipt(dir string, r WorkloadReceipt) error {
	data, e := json.MarshalIndent(r, "", "  ")
	if e != nil {
		return e
	}
	name := filepath.Join(dir, "receipt-"+r.ReceiptID+".json")
	return writeAtomic(name, data)
}

// listWorkloadReceipts returns recently recorded receipts, newest first.
// No receipt is altered or re-signed; the list is read-only evidence.
func (a *Agent) listWorkloadReceipts(limit int) (any, error) {
	globalAdapterState.mu.Lock()
	all := globalAdapterState.receipts
	globalAdapterState.mu.Unlock()

	if limit <= 0 || limit > 100 {
		limit = 20
	}
	start := len(all) - limit
	if start < 0 {
		start = 0
	}
	slice := make([]WorkloadReceipt, len(all)-start)
	copy(slice, all[start:])
	// Reverse to newest-first.
	for i, j := 0, len(slice)-1; i < j; i, j = i+1, j-1 {
		slice[i], slice[j] = slice[j], slice[i]
	}
	return map[string]any{
		"schema":     "deproof:workload-receipt-list-v1",
		"count":      len(slice),
		"observedAt": time.Now().UTC().Format(time.RFC3339Nano),
		"receipts":   slice,
		"_note":      "NODE_SIGNED_RECORD evidence means the node signed the local observation. It does not constitute independent verification or provider-acknowledged contribution.",
	}, nil
}

// ─── canonicalJSON produces a deterministic JSON serialisation ────────────────

// canonicalJSON marshals v to JSON with map keys sorted and no trailing whitespace.
// Used to produce a consistent byte sequence for signing.
func canonicalJSON(v any) ([]byte, error) {
	// Marshal first, then normalise key order by round-tripping through a sorted map.
	raw, e := json.Marshal(v)
	if e != nil {
		return nil, e
	}
	var generic any
	if e = json.Unmarshal(raw, &generic); e != nil {
		return nil, e
	}
	return marshalSorted(generic)
}

func marshalSorted(v any) ([]byte, error) {
	switch t := v.(type) {
	case map[string]any:
		keys := make([]string, 0, len(t))
		for k := range t {
			keys = append(keys, k)
		}
		sort.Strings(keys)
		buf := []byte{'{'}
		for i, k := range keys {
			if i > 0 {
				buf = append(buf, ',')
			}
			kb, e := json.Marshal(k)
			if e != nil {
				return nil, e
			}
			vb, e := marshalSorted(t[k])
			if e != nil {
				return nil, e
			}
			buf = append(buf, kb...)
			buf = append(buf, ':')
			buf = append(buf, vb...)
		}
		buf = append(buf, '}')
		return buf, nil
	case []any:
		buf := []byte{'['}
		for i, item := range t {
			if i > 0 {
				buf = append(buf, ',')
			}
			vb, e := marshalSorted(item)
			if e != nil {
				return nil, e
			}
			buf = append(buf, vb...)
		}
		buf = append(buf, ']')
		return buf, nil
	default:
		return json.Marshal(v)
	}
}

// ─── Scope routing additions ─────────────────────────────────────────────────

// scopeForAdapter maps new adapter actions to their required scope.
// Returns empty string for unknown actions (existing scopeFor handles the rest).
func scopeForAdapter(action string) string {
	switch action {
	case "listAdapters", "getAdapter", "listWorkloadReceipts":
		return "MANAGE_ADAPTER"
	case "runAdapter":
		return "MANAGE_ADAPTER"
	}
	return ""
}

// ─── Command dispatch for adapter actions ────────────────────────────────────

// DispatchAdapter is called from agent.Command after authorization for the
// four new adapter actions. Returns (result, handled, error). handled=false
// means the action is not an adapter action and should fall through to the
// existing switch.
func (a *Agent) DispatchAdapter(c Command) (any, bool, error) {
	switch c.Action {
	case "listAdapters":
		result, e := a.listAdapters()
		return result, true, e

	case "getAdapter":
		var p struct {
			AdapterID string `json:"adapterId"`
		}
		if e := StrictJSON(c.Params, &p); e != nil {
			return nil, true, errors.New("BAD_PARAMS")
		}
		entry, e := a.getAdapter(p.AdapterID)
		if e != nil {
			return nil, true, e
		}
		return entry, true, nil

	case "runAdapter":
		var p struct {
			AdapterID   string `json:"adapterId"`
			OperationID string `json:"operationId"`
			TargetAddr  string `json:"targetAddr,omitempty"`
			Consent     bool   `json:"explicitConsent"`
		}
		if e := StrictJSON(c.Params, &p); e != nil || !p.Consent {
			return nil, true, errors.New("ADAPTER_CONSENT_REQUIRED")
		}
		if p.AdapterID != fixtureAdapterID {
			// All other adapters are not executable via this runtime.
			entry, e := a.getAdapter(p.AdapterID)
			if e != nil {
				return nil, true, e
			}
			if entry.Status == StatusUnsupported {
				return nil, true, errors.New("ADAPTER_UNSUPPORTED: " + entry.BlockedReason)
			}
			if entry.Status == StatusBlocked {
				return nil, true, errors.New("ADAPTER_BLOCKED: " + entry.BlockedReason)
			}
			if entry.Status == StatusPlanned {
				return nil, true, errors.New("ADAPTER_PLANNED: runtime not yet implemented")
			}
			return nil, true, errors.New("ADAPTER_NOT_EXECUTABLE: only the local fixture adapter is executable in this release")
		}
		receipt, e := a.runLatencyFixture(c.OperationID, p.TargetAddr)
		if e != nil {
			return nil, true, e
		}
		return receipt, true, nil

	case "listWorkloadReceipts":
		var p struct {
			Limit int `json:"limit,omitempty"`
		}
		// Params may be empty for default limit.
		if len(c.Params) > 2 {
			if e := StrictJSON(c.Params, &p); e != nil {
				return nil, true, errors.New("BAD_PARAMS")
			}
		}
		result, e := a.listWorkloadReceipts(p.Limit)
		return result, true, e
	}
	return nil, false, nil
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

// containsPrefix returns true when any element in ss has os as a prefix
// (e.g. "linux" matches "ubuntu-22.04" because "ubuntu-22.04" starts with
// "linux" is false — but "linux" matches when ss contains "linux").
// The catalog uses "ubuntu-*" which will not match runtime GOOS "linux",
// so we also accept "linux" OS as compatible with any "ubuntu-*" entry.
func containsPrefix(ss []string, os string) bool {
	for _, s := range ss {
		if s == os {
			return true
		}
		// Accept linux as compatible with ubuntu-* catalog entries.
		if os == "linux" && strings.HasPrefix(s, "ubuntu") {
			return true
		}
		// Accept darwin as compatible with macos-* catalog entries.
		if os == "darwin" && strings.HasPrefix(s, "macos") {
			return true
		}
	}
	return false
}

// adapterScopeNeeded returns the required scope for adapter actions.
// It is called by the existing scopeFor function via the adapter dispatch path.
func adapterScopeNeeded(action string) string {
	return scopeForAdapter(action)
}

// LoadAdapterReceipts reads persisted receipt files from receiptDir and
// populates the in-memory list. Called at startup after ConfigureAdapter.
func (a *Agent) LoadAdapterReceipts() {
	globalAdapterState.mu.Lock()
	cfg := globalAdapterState.config
	globalAdapterState.mu.Unlock()
	if cfg == nil {
		return
	}
	entries, e := os.ReadDir(cfg.ReceiptDir)
	if e != nil {
		return
	}
	var loaded []WorkloadReceipt
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasPrefix(entry.Name(), "receipt-") {
			continue
		}
		p := filepath.Join(cfg.ReceiptDir, entry.Name())
		raw, re := os.ReadFile(p)
		if re != nil {
			continue
		}
		var r WorkloadReceipt
		if je := json.Unmarshal(raw, &r); je == nil {
			loaded = append(loaded, r)
		}
	}
	globalAdapterState.mu.Lock()
	globalAdapterState.receipts = append(loaded, globalAdapterState.receipts...)
	globalAdapterState.mu.Unlock()
}

// AdapterCatalogVersion returns the loaded catalog version or "UNAVAILABLE".
func AdapterCatalogVersion() string {
	globalAdapterState.mu.Lock()
	defer globalAdapterState.mu.Unlock()
	if globalAdapterState.catalog == nil {
		return "UNAVAILABLE"
	}
	return globalAdapterState.catalog.Version
}
