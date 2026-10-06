package agent

import (
	"context"
	"crypto/ed25519"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"io"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func openTest(t *testing.T) *Agent {
	t.Helper()
	root := filepath.Join(t.TempDir(), "node")
	a, e := Open(root, "")
	if e != nil {
		t.Fatal(e)
	}
	t.Cleanup(func() { a.Close() })
	return a
}
func pairTest(t *testing.T, a *Agent, scopes []string) (string, ed25519.PrivateKey) {
	t.Helper()
	c, code, e := a.CreateChallenge(scopes)
	if e != nil {
		t.Fatal(e)
	}
	pub, key, _ := ed25519.GenerateKey(rand.Reader)
	p := PairRequest{c.ID, code, base64.StdEncoding.EncodeToString(pub), "", a.Fingerprint(), scopes}
	p.Signature = base64.StdEncoding.EncodeToString(ed25519.Sign(key, PairPayload(c, p.PublicKey, p.Scopes)))
	id, e := a.Pair(p)
	if e != nil {
		t.Fatal(e)
	}
	if _, e = a.Pair(p); e == nil {
		t.Fatal("challenge replay accepted")
	}
	return id, key
}
func command(t *testing.T, id string, key ed25519.PrivateKey, action, op string, params any) Signed {
	t.Helper()
	p, _ := json.Marshal(params)
	c := Command{id, op, time.Now().UTC().Add(30 * time.Second).Format(time.RFC3339Nano), "node-policy-v1", action, p}
	b, _ := json.Marshal(c)
	return Signed{base64.StdEncoding.EncodeToString(b), base64.StdEncoding.EncodeToString(ed25519.Sign(key, b))}
}
func TestIdentityPersistsAndPermissions(t *testing.T) {
	a := openTest(t)
	a.Close()
	b, e := Open(a.root, "")
	if e != nil || a.Fingerprint() != b.Fingerprint() {
		t.Fatal("identity did not persist", e)
	}
	b.Close()
	if e = os.Chmod(filepath.Join(a.root, "identity.key"), 0644); e != nil {
		t.Fatal(e)
	}
	if _, e = Open(a.root, ""); e == nil {
		t.Fatal("insecure key accepted")
	}
}
func TestPairScopeReplayRevocationAndRestart(t *testing.T) {
	a := openTest(t)
	id, key := pairTest(t, a, []string{"READ_NODE"})
	s := command(t, id, key, "observe", "abcdefghijklmnop", map[string]any{})
	if _, e := a.Command(s); e != nil {
		t.Fatal(e)
	}
	if _, e := a.Command(s); e == nil {
		t.Fatal("replay accepted")
	}
	if _, e := a.Command(command(t, id, key, "startService", "abcdefghijklmnop2", map[string]any{})); e == nil {
		t.Fatal("readonly operated service")
	}
	a.Close()
	b, e := Open(a.root, "")
	if e != nil {
		t.Fatal(e)
	}
	if _, e = b.Command(s); e == nil {
		t.Fatal("restart lost replay protection")
	}
	if e = b.Revoke(id); e != nil {
		t.Fatal(e)
	}
	if _, e = b.Command(command(t, id, key, "observe", "abcdefghijklmnop3", map[string]any{})); e == nil {
		t.Fatal("revoked session accepted")
	}
}
func TestForgedAndMalformedDenied(t *testing.T) {
	a := openTest(t)
	id, key := pairTest(t, a, []string{"READ_NODE"})
	s := command(t, id, key, "observe", "abcdefghijklmnop", map[string]any{})
	s.Signature = base64.StdEncoding.EncodeToString(make([]byte, 64))
	if _, e := a.Command(s); e == nil {
		t.Fatal("forged signature accepted")
	}
	var c Command
	for _, raw := range []string{`{"action":"observe","action":"transfer"}`, `{"unknown":true}`, `{} {}`} {
		if e := StrictJSON([]byte(raw), &c); e == nil {
			t.Fatal("bad JSON accepted", raw)
		}
	}
}
func TestDefaultBandwidthOffAndQuotaPersists(t *testing.T) {
	a := openTest(t)
	id, key := pairTest(t, a, []string{"SHARE_BANDWIDTH"})
	if _, e := a.Command(command(t, id, key, "transfer", "abcdefghijklmnop", map[string]string{"bytes": "1"})); e == nil {
		t.Fatal("unconsented transfer")
	}
	a.state.Consent.Used = "123"
	a.state.Consent.Enabled = true
	if e := a.persist(); e != nil {
		t.Fatal(e)
	}
	a.Close()
	b, e := Open(a.root, "")
	if e != nil || b.state.Consent.Used != "123" || b.state.Consent.Enabled {
		t.Fatal("restart resets caps or starts flow", e)
	}
}
func TestHttpHandlerBoundsAndMethod(t *testing.T) {
	a := openTest(t)
	h := a.Handler()
	for _, r := range []*httptest.ResponseRecorder{httptest.NewRecorder()} {
		req := httptest.NewRequest("GET", "https://localhost/command", nil)
		h.ServeHTTP(r, req)
		if r.Code != 405 {
			t.Fatal(r.Code)
		}
	}
	r := httptest.NewRecorder()
	h.ServeHTTP(r, httptest.NewRequest("POST", "https://localhost/command", strings.NewReader(strings.Repeat("x", 65537))))
	if r.Code != 413 {
		t.Fatal(r.Code)
	}
}

func TestReaderKnownBytesCancellationAndLimit(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	r := &pacedReader{ctx: ctx, left: 8193, rate: 1024 * 1024, start: time.Now().Add(-time.Hour)}
	b, e := io.ReadAll(r)
	if e != nil || len(b) != 8193 || r.measured != 8193 {
		t.Fatal("counter mismatch", len(b), e)
	}
	cancel()
	r = &pacedReader{ctx: ctx, left: 10, rate: 1, start: time.Now()}
	if _, e = r.Read(make([]byte, 10)); e == nil {
		t.Fatal("cancel did not stop reader")
	}
}

func TestExclusiveOwnerLock(t *testing.T) {
	a := openTest(t)
	if _, e := Open(a.root, ""); e == nil {
		t.Fatal("second instance acquired state")
	}
	a.Close()
	b, e := Open(a.root, "")
	if e != nil {
		t.Fatal(e)
	}
	b.Close()
}

func TestOperationIdentityCannotTraversePaths(t *testing.T) {
	a := openTest(t)
	id, key := pairTest(t, a, []string{"RUN_PROOF_JOB"})
	for _, op := range []string{"abcdefghijkl/../../outside", "abcdefghijkl\\outside", "abcdefghijklmnop.."} {
		if _, e := a.Command(command(t, id, key, "proof", op, map[string]any{"jobId": "job", "explicitConsent": true})); e == nil || e.Error() != "BAD_OPERATION_ID" {
			t.Fatal("unsafe operation identity was not refused", e)
		}
	}
}
func TestSelfRevocationDoesNotDisableAnotherPeersConsent(t *testing.T) {
	a := openTest(t)
	owner, _ := pairTest(t, a, []string{"SHARE_BANDWIDTH"})
	reader, key := pairTest(t, a, []string{"READ_NODE"})
	a.state.ConsentSession = owner
	a.state.Consent.Enabled = true
	a.state.Consent.Deadline = time.Now().Add(time.Hour).UTC().Format(time.RFC3339Nano)
	if _, e := a.Command(command(t, reader, key, "revoke", "reader-revoke-123456", map[string]any{})); e != nil {
		t.Fatal(e)
	}
	if !a.state.Consent.Enabled {
		t.Fatal("read-only self revocation affected another peer's consent")
	}
}
