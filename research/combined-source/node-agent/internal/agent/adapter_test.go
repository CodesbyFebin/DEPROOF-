package agent

import (
	"context"
	"crypto/ed25519"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"net"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

func catalogFixturePath(t *testing.T) string {
	t.Helper()
	// Walk up from the test file to find the repo root fixtures directory.
	// The test runs from node-agent/internal/agent; fixtures is at ../../../fixtures
	// relative to node-agent/internal/agent (three levels up to repo root).
	rel := filepath.Join("..", "..", "..", "fixtures", "project-catalog-v1.json")
	abs, e := filepath.Abs(rel)
	if e != nil {
		t.Skipf("cannot resolve catalog path: %v", e)
	}
	if _, e := os.Stat(abs); e != nil {
		t.Skipf("catalog fixture not found at %s: %v", abs, e)
	}
	return abs
}

func newTestAgent(t *testing.T) *Agent {
	t.Helper()
	root := t.TempDir()
	_, priv, e := ed25519.GenerateKey(rand.Reader)
	if e != nil {
		t.Fatalf("ed25519 keygen: %v", e)
	}
	a := &Agent{
		root:          root,
		key:           priv,
		state:         State{Sessions: map[string]Session{}, Operations: map[string]string{}},
		allowedScopes: []string{"READ_NODE", "MANAGE_ADAPTER", "EXPORT_PUBLIC_RECORDS"},
		active:        map[string]context.CancelFunc{},
		activeOwners:  map[string]string{},
	}
	return a
}

// ─── Catalog loading ──────────────────────────────────────────────────────────

func TestCatalogLoad(t *testing.T) {
	// Reset global state.
	globalAdapterState.mu.Lock()
	globalAdapterState.catalog = nil
	globalAdapterState.config = nil
	globalAdapterState.mu.Unlock()

	a := newTestAgent(t)
	catalogPath := catalogFixturePath(t)
	receiptDir := filepath.Join(t.TempDir(), "receipts")

	if e := a.ConfigureAdapter(AdapterConfig{CatalogPath: catalogPath, ReceiptDir: receiptDir}); e != nil {
		t.Fatalf("ConfigureAdapter: %v", e)
	}

	globalAdapterState.mu.Lock()
	cat := globalAdapterState.catalog
	globalAdapterState.mu.Unlock()

	if cat == nil {
		t.Fatal("catalog not loaded")
	}
	if cat.Schema != "deproof:adapter-catalog-v1" {
		t.Errorf("unexpected schema: %s", cat.Schema)
	}
	if len(cat.Adapters) == 0 {
		t.Error("no adapters in catalog")
	}
	t.Logf("catalog v%s loaded with %d adapters", cat.Version, len(cat.Adapters))
}

func TestCatalogLoadRelativePath(t *testing.T) {
	a := newTestAgent(t)
	e := a.ConfigureAdapter(AdapterConfig{CatalogPath: "relative/path.json", ReceiptDir: t.TempDir()})
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_CONFIG_PATH_DENIED") {
		t.Errorf("expected ADAPTER_CONFIG_PATH_DENIED, got %v", e)
	}
}

func TestCatalogLoadMissing(t *testing.T) {
	a := newTestAgent(t)
	e := a.ConfigureAdapter(AdapterConfig{
		CatalogPath: filepath.Join(t.TempDir(), "nonexistent.json"),
		ReceiptDir:  t.TempDir(),
	})
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_CATALOG_NOT_FOUND") {
		t.Errorf("expected ADAPTER_CATALOG_NOT_FOUND, got %v", e)
	}
}

func TestCatalogLoadInvalidJSON(t *testing.T) {
	dir := t.TempDir()
	badPath := filepath.Join(dir, "bad.json")
	_ = os.WriteFile(badPath, []byte("{not valid json"), 0600)
	a := newTestAgent(t)
	e := a.ConfigureAdapter(AdapterConfig{CatalogPath: badPath, ReceiptDir: t.TempDir()})
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_CATALOG_INVALID") {
		t.Errorf("expected ADAPTER_CATALOG_INVALID, got %v", e)
	}
}

// ─── listAdapters ─────────────────────────────────────────────────────────────

func TestListAdaptersUnconfigured(t *testing.T) {
	globalAdapterState.mu.Lock()
	globalAdapterState.catalog = nil
	globalAdapterState.config = nil
	globalAdapterState.mu.Unlock()

	a := newTestAgent(t)
	_, e := a.listAdapters()
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_CATALOG_UNAVAILABLE") {
		t.Errorf("expected ADAPTER_CATALOG_UNAVAILABLE, got %v", e)
	}
}

func TestListAdaptersCompatibility(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	raw, e := a.listAdapters()
	if e != nil {
		t.Fatalf("listAdapters: %v", e)
	}
	data, _ := json.Marshal(raw)
	var result map[string]any
	_ = json.Unmarshal(data, &result)

	if result["schema"] != "deproof:adapter-list-v1" {
		t.Errorf("unexpected schema: %v", result["schema"])
	}
	adapters, ok := result["adapters"].([]any)
	if !ok || len(adapters) == 0 {
		t.Fatal("no adapters in result")
	}

	// Fixture adapter must be present and compatible.
	var foundFixture bool
	for _, a := range adapters {
		m := a.(map[string]any)
		if m["adapterId"] == fixtureAdapterID {
			foundFixture = true
			if m["compatible"] != true {
				t.Errorf("fixture adapter should be compatible, incompatibleReasons=%v", m["incompatibleReasons"])
			}
		}
		// GPU-required adapter must be incompatible.
		if id, _ := m["adapterId"].(string); strings.Contains(id, "render") {
			if m["compatible"] == true {
				t.Error("render adapter (GPU required) must not be compatible")
			}
		}
	}
	if !foundFixture {
		t.Errorf("fixture adapter %s not found in list", fixtureAdapterID)
	}
}

// ─── getAdapter ───────────────────────────────────────────────────────────────

func TestGetAdapterFound(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	entry, e := a.getAdapter(fixtureAdapterID)
	if e != nil {
		t.Fatalf("getAdapter: %v", e)
	}
	if entry.Status != StatusQualified {
		t.Errorf("fixture adapter expected QUALIFIED, got %s", entry.Status)
	}
}

func TestGetAdapterNotFound(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	_, e := a.getAdapter("deproof-adapter-does-not-exist-v99")
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_NOT_FOUND") {
		t.Errorf("expected ADAPTER_NOT_FOUND, got %v", e)
	}
}

// ─── Fixture adapter: latency measurement ────────────────────────────────────

func TestRunLatencyFixtureSuccess(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	opID := "test-op-" + randomIDMust(t)
	// Use a reliable TCP target that is always reachable on the local machine.
	// We use the node-agent's own TLS port if running, otherwise use a known
	// public address. For CI environments, use a loopback listener.
	listener, e := listenLocalTCP(t)
	if e != nil {
		t.Skipf("cannot create local TCP listener: %v", e)
	}
	defer listener.Close()
	addr := listener.Addr().String()

	receipt, e := a.runLatencyFixture(opID, addr)
	if e != nil {
		t.Fatalf("runLatencyFixture: %v", e)
	}

	if receipt.Schema != "deproof:workload-receipt-v1" {
		t.Errorf("unexpected schema: %s", receipt.Schema)
	}
	if receipt.AdapterID != fixtureAdapterID {
		t.Errorf("unexpected adapterId: %s", receipt.AdapterID)
	}
	if receipt.EvidenceLevel != EvidenceNodeSignedRecord {
		t.Errorf("expected NODE_SIGNED_RECORD, got %s", receipt.EvidenceLevel)
	}
	if !receipt.IsQualificationFixture {
		t.Error("isQualificationFixture must be true")
	}
	if receipt.FixtureLabel == "" {
		t.Error("fixtureLabel must not be empty")
	}
	if receipt.NodeSignature == "" {
		t.Error("nodeSignature must be present")
	}
	if !receipt.VerificationResult.Passed {
		t.Errorf("verification should pass, got: %s", receipt.VerificationResult.Details)
	}
	if len(receipt.TrustAssumptions) == 0 {
		t.Error("trustAssumptions must not be empty")
	}
	if receipt.ProviderAcknowledgement == nil || receipt.ProviderAcknowledgement.Available {
		t.Error("providerAcknowledgement.available must be false for fixture")
	}
	t.Logf("receipt %s: avgLatency=%v", receipt.ReceiptID, receipt.MeasuredValue["avgLatencyMicros"])
}

func TestRunLatencyFixtureEmptyTarget(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	_, e := a.runLatencyFixture("op-empty", "")
	if e == nil || !strings.Contains(e.Error(), "FIXTURE_TARGET_ADDR_REQUIRED") {
		t.Errorf("expected FIXTURE_TARGET_ADDR_REQUIRED, got %v", e)
	}
}

func TestRunLatencyFixtureInvalidAddr(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	_, e := a.runLatencyFixture("op-bad", "not-a-valid-addr")
	if e == nil || !strings.Contains(e.Error(), "FIXTURE_TARGET_ADDR_INVALID") {
		t.Errorf("expected FIXTURE_TARGET_ADDR_INVALID, got %v", e)
	}
}

func TestRunLatencyFixtureURLRejected(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	_, e := a.runLatencyFixture("op-url", "https://example.com/path")
	if e == nil || !strings.Contains(e.Error(), "FIXTURE_TARGET_ADDR_MUST_BE_HOST_PORT") {
		t.Errorf("expected FIXTURE_TARGET_ADDR_MUST_BE_HOST_PORT, got %v", e)
	}
}

func TestRunLatencyFixtureUnreachable(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	// Port 1 on loopback should be refused immediately.
	_, e := a.runLatencyFixture("op-unreachable", "127.0.0.1:1")
	if e == nil || !strings.Contains(e.Error(), "FIXTURE_TARGET_UNREACHABLE") {
		t.Errorf("expected FIXTURE_TARGET_UNREACHABLE, got %v", e)
	}
}

// ─── Receipt signature verification ──────────────────────────────────────────

func TestReceiptSignatureVerifiable(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	listener, e := listenLocalTCP(t)
	if e != nil {
		t.Skipf("cannot create local TCP listener: %v", e)
	}
	defer listener.Close()

	receipt, e := a.runLatencyFixture("op-sig-test", listener.Addr().String())
	if e != nil {
		t.Fatalf("runLatencyFixture: %v", e)
	}

	// Re-derive canonical bytes and verify signature.
	unsigned := *receipt
	unsigned.NodeSignature = ""
	canonical, ce := canonicalJSON(unsigned)
	if ce != nil {
		t.Fatalf("canonicalJSON: %v", ce)
	}

	sigBytes, de := base64Decode(receipt.NodeSignature)
	if de != nil {
		t.Fatalf("decode sig: %v", de)
	}

	pub := a.key.Public().(ed25519.PublicKey)
	if !ed25519.Verify(pub, canonical, sigBytes) {
		t.Error("receipt signature verification failed")
	}
	t.Logf("receipt signature verified for %s", receipt.ReceiptID)
}

func TestReceiptTamperedSignatureFails(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	listener, e := listenLocalTCP(t)
	if e != nil {
		t.Skipf("cannot create local TCP listener: %v", e)
	}
	defer listener.Close()

	receipt, e := a.runLatencyFixture("op-tamper-test", listener.Addr().String())
	if e != nil {
		t.Fatalf("runLatencyFixture: %v", e)
	}

	// Tamper: change a measured value.
	receipt.MeasuredValue["avgLatencyMicros"] = int64(999999999)
	unsigned := *receipt
	unsigned.NodeSignature = ""
	canonical, _ := canonicalJSON(unsigned)
	sigBytes, _ := base64Decode(receipt.NodeSignature)

	pub := a.key.Public().(ed25519.PublicKey)
	if ed25519.Verify(pub, canonical, sigBytes) {
		t.Error("tampered receipt should not verify")
	}
}

// ─── DispatchAdapter ─────────────────────────────────────────────────────────

func TestDispatchAdapterUnsupported(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	p, _ := json.Marshal(map[string]any{
		"adapterId":    "deproof-adapter-render-v1",
		"operationId":  "op-render",
		"explicitConsent": true,
	})
	_, handled, e := a.DispatchAdapter(Command{Action: "runAdapter", Params: p})
	if !handled {
		t.Fatal("expected handled=true")
	}
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_UNSUPPORTED") {
		t.Errorf("expected ADAPTER_UNSUPPORTED, got %v", e)
	}
}

func TestDispatchAdapterConsentRequired(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	p, _ := json.Marshal(map[string]any{
		"adapterId":    fixtureAdapterID,
		"operationId":  "op-noconsent",
		"explicitConsent": false,
	})
	_, handled, e := a.DispatchAdapter(Command{Action: "runAdapter", Params: p})
	if !handled {
		t.Fatal("expected handled=true")
	}
	if e == nil || !strings.Contains(e.Error(), "ADAPTER_CONSENT_REQUIRED") {
		t.Errorf("expected ADAPTER_CONSENT_REQUIRED, got %v", e)
	}
}

func TestDispatchUnknownAction(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)
	_, handled, _ := a.DispatchAdapter(Command{Action: "bogusAction"})
	if handled {
		t.Error("unknown action should not be handled")
	}
}

// ─── listWorkloadReceipts ─────────────────────────────────────────────────────

func TestListWorkloadReceipts(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	listener, e := listenLocalTCP(t)
	if e != nil {
		t.Skipf("cannot create local TCP listener: %v", e)
	}
	defer listener.Close()
	addr := listener.Addr().String()

	// Run two receipts.
	for i := 0; i < 2; i++ {
		opID := "op-list-" + randomIDMust(t)
		if _, e := a.runLatencyFixture(opID, addr); e != nil {
			t.Fatalf("runLatencyFixture %d: %v", i, e)
		}
	}

	raw, e := a.listWorkloadReceipts(10)
	if e != nil {
		t.Fatalf("listWorkloadReceipts: %v", e)
	}
	data, _ := json.Marshal(raw)
	var result map[string]any
	_ = json.Unmarshal(data, &result)

	count, _ := result["count"].(float64)
	if count < 2 {
		t.Errorf("expected at least 2 receipts, got %v", count)
	}
}

// ─── canonicalJSON ────────────────────────────────────────────────────────────

func TestCanonicalJSONDeterministic(t *testing.T) {
	v := map[string]any{
		"z": "last",
		"a": "first",
		"m": map[string]any{"b": 2, "a": 1},
	}
	b1, _ := canonicalJSON(v)
	b2, _ := canonicalJSON(v)
	if string(b1) != string(b2) {
		t.Errorf("canonicalJSON not deterministic: %s vs %s", b1, b2)
	}
	if !strings.HasPrefix(string(b1), `{"a"`) {
		t.Errorf("expected keys sorted, got: %s", b1)
	}
}

// ─── containsPrefix ──────────────────────────────────────────────────────────

func TestContainsPrefixLinuxUbuntu(t *testing.T) {
	if !containsPrefix([]string{"ubuntu-22.04", "ubuntu-24.04"}, "linux") {
		t.Error("linux should match ubuntu-* entries")
	}
	if containsPrefix([]string{"windows-2022"}, "linux") {
		t.Error("linux should not match windows entries")
	}
}

// ─── Receipt persistence ──────────────────────────────────────────────────────

func TestReceiptPersistence(t *testing.T) {
	dir := t.TempDir()
	r := WorkloadReceipt{
		Schema:          "deproof:workload-receipt-v1",
		ReceiptID:       "testreceipt001",
		AdapterID:       fixtureAdapterID,
		AdapterVersion:  "1.0.0",
		EvidenceLevel:   EvidenceNodeSignedRecord,
		TrustAssumptions: []string{"test assumption"},
	}
	if e := persistReceipt(dir, r); e != nil {
		t.Fatalf("persistReceipt: %v", e)
	}
	expected := filepath.Join(dir, "receipt-testreceipt001.json")
	if _, e := os.Stat(expected); e != nil {
		t.Errorf("persisted file not found: %v", e)
	}
}

// ─── Stale observation detection (observedAt freshness) ───────────────────────

func TestReceiptObservedAtIsFresh(t *testing.T) {
	setupCatalog(t)
	a := newTestAgent(t)

	listener, e := listenLocalTCP(t)
	if e != nil {
		t.Skipf("cannot create local TCP listener: %v", e)
	}
	defer listener.Close()

	receipt, e := a.runLatencyFixture("op-freshness", listener.Addr().String())
	if e != nil {
		t.Fatalf("runLatencyFixture: %v", e)
	}
	observed := receipt.MeasuredValue["observedAt"].(string)
	ts, pe := time.Parse(time.RFC3339Nano, observed)
	if pe != nil {
		t.Fatalf("bad observedAt: %v", pe)
	}
	if time.Since(ts) > 30*time.Second {
		t.Errorf("observedAt is stale: %s", observed)
	}
}

// ─── Test utilities ───────────────────────────────────────────────────────────

func setupCatalog(t *testing.T) {
	t.Helper()
	globalAdapterState.mu.Lock()
	globalAdapterState.receipts = nil
	globalAdapterState.mu.Unlock()
	catalogPath := catalogFixturePath(t)
	a := newTestAgent(t)
	if e := a.ConfigureAdapter(AdapterConfig{
		CatalogPath: catalogPath,
		ReceiptDir:  t.TempDir(),
	}); e != nil {
		t.Fatalf("setupCatalog: %v", e)
	}
}

func randomIDMust(t *testing.T) string {
	t.Helper()
	id, e := randomID()
	if e != nil {
		t.Fatalf("randomID: %v", e)
	}
	return id
}

func listenLocalTCP(t *testing.T) (net.Listener, error) {
	t.Helper()
	return net.Listen("tcp", "127.0.0.1:0")
}

func base64Decode(s string) ([]byte, error) {
	return base64.StdEncoding.DecodeString(s)
}
