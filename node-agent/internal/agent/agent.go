// Package agent implements local, scoped node control. It never accepts shell commands.
package agent

import (
	"bytes"
	"context"
	"crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"syscall"
	"time"
)

type Session struct {
	PublicKey string   `json:"publicKey"`
	Scopes    []string `json:"scopes"`
	Revoked   bool     `json:"revoked"`
	ExpiresAt string   `json:"expiresAt"`
}
type Consent struct {
	Enabled  bool   `json:"enabled"`
	Endpoint string `json:"endpoint"`
	Cap      string `json:"capBytes"`
	Used     string `json:"reservedBytes"`
	Measured string `json:"measuredBytes"`
	Rate     string `json:"bytesPerSecond"`
	Deadline string `json:"deadline"`
}
type State struct {
	Sessions       map[string]Session `json:"sessions"`
	Operations     map[string]string  `json:"operations"`
	Consent        Consent            `json:"consent"`
	ConsentSession string             `json:"consentSession"`
	ServiceSession string             `json:"serviceSession"`
	Sequence       uint64             `json:"sequence"`
}
type Command struct {
	SessionID   string          `json:"sessionId"`
	OperationID string          `json:"operationId"`
	Deadline    string          `json:"deadline"`
	Policy      string          `json:"policy"`
	Action      string          `json:"action"`
	Params      json.RawMessage `json:"params"`
}
type Signed struct {
	Payload   string `json:"payloadBase64"`
	Signature string `json:"signatureBase64"`
}
type Challenge struct {
	ID          string `json:"id"`
	Nonce       string `json:"nonce"`
	Fingerprint string `json:"fingerprint"`
	ExpiresAt   string `json:"expiresAt"`
}
type PairRequest struct {
	ChallengeID         string   `json:"challengeId"`
	Code                string   `json:"code"`
	PublicKey           string   `json:"publicKey"`
	Signature           string   `json:"signatureBase64"`
	ApprovedFingerprint string   `json:"approvedFingerprint"`
	Scopes              []string `json:"scopes"`
}
type Agent struct {
	lockFile       *os.File
	mu             sync.Mutex
	serviceMu      sync.Mutex
	root           string
	key            ed25519.PrivateKey
	state          State
	challenge      Challenge
	codeHash       [32]byte
	challengeUsed  bool
	allowedScopes  []string
	endpoint       string
	client         *http.Client
	active         map[string]context.CancelFunc
	activeSession  string
	activeOwners   map[string]string
	proofConfig    *ProofConfig
	hostingConfig  *HostingConfig
	proofCancel    context.CancelFunc
	proofOperation string
	proofSession   string
}

func StrictJSON(raw []byte, out any) error {
	if len(raw) > 65536 {
		return errors.New("PAYLOAD_TOO_LARGE")
	}
	// Reject duplicate keys at every object level, in addition to unknown typed fields.
	var scan func(*json.Decoder) error
	scan = func(d *json.Decoder) error {
		t, e := d.Token()
		if e != nil {
			return e
		}
		if delim, ok := t.(json.Delim); ok {
			switch delim {
			case '{':
				seen := map[string]bool{}
				for d.More() {
					k, e := d.Token()
					if e != nil {
						return e
					}
					s := k.(string)
					if seen[s] {
						return errors.New("DUPLICATE_KEY")
					}
					seen[s] = true
					if e = scan(d); e != nil {
						return e
					}
				}
				_, e = d.Token()
				return e
			case '[':
				for d.More() {
					if e = scan(d); e != nil {
						return e
					}
				}
				_, e = d.Token()
				return e
			}
		}
		return nil
	}
	d := json.NewDecoder(bytes.NewReader(raw))
	if e := scan(d); e != nil {
		return e
	}
	if _, e := d.Token(); e != io.EOF {
		return errors.New("TRAILING_JSON")
	}
	d = json.NewDecoder(bytes.NewReader(raw))
	d.DisallowUnknownFields()
	return d.Decode(out)
}
func randomID() (string, error) {
	b := make([]byte, 24)
	_, e := rand.Read(b)
	return hex.EncodeToString(b), e
}
func writeAtomic(path string, data []byte) error {
	f, e := os.CreateTemp(filepath.Dir(path), ".state-")
	if e != nil {
		return e
	}
	tmp := f.Name()
	defer os.Remove(tmp)
	if e = f.Chmod(0600); e != nil {
		f.Close()
		return e
	}
	if _, e = f.Write(data); e != nil {
		f.Close()
		return e
	}
	if e = f.Sync(); e != nil {
		f.Close()
		return e
	}
	if e = f.Close(); e != nil {
		return e
	}
	if e = os.Rename(tmp, path); e != nil {
		return e
	}
	dir, e := os.Open(filepath.Dir(path))
	if e != nil {
		return e
	}
	defer dir.Close()
	return dir.Sync()
}
func Open(root, endpoint string) (*Agent, error) {
	if e := os.MkdirAll(root, 0700); e != nil {
		return nil, e
	}
	st, e := os.Lstat(root)
	if e != nil {
		return nil, e
	}
	if !st.IsDir() || st.Mode()&os.ModeSymlink != 0 || st.Mode().Perm()&0077 != 0 {
		return nil, errors.New("INSECURE_STATE_DIRECTORY")
	}
	lock, e := os.OpenFile(filepath.Join(root, ".owner-lock"), os.O_CREATE|os.O_RDWR, 0600)
	if e != nil {
		return nil, e
	}
	if e = syscall.Flock(int(lock.Fd()), syscall.LOCK_EX|syscall.LOCK_NB); e != nil {
		lock.Close()
		return nil, errors.New("NODE_ALREADY_RUNNING")
	}
	opened := false
	defer func() {
		if !opened {
			syscall.Flock(int(lock.Fd()), syscall.LOCK_UN)
			lock.Close()
		}
	}()
	a := &Agent{lockFile: lock, root: root, endpoint: endpoint, client: &http.Client{Timeout: 15 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return errors.New("REDIRECT_DENIED") }}, active: map[string]context.CancelFunc{}, activeOwners: map[string]string{}}
	path := filepath.Join(root, "identity.key")
	b, e := os.ReadFile(path)
	if errors.Is(e, os.ErrNotExist) {
		_, key, e := ed25519.GenerateKey(rand.Reader)
		if e != nil {
			return nil, e
		}
		b = key
		if e = writeAtomic(path, b); e != nil {
			return nil, e
		}
	} else if e != nil {
		return nil, e
	}
	st, e = os.Lstat(path)
	if e != nil {
		return nil, e
	}
	if st.Mode().Perm()&0077 != 0 || st.Mode()&os.ModeSymlink != 0 || len(b) != ed25519.PrivateKeySize {
		return nil, errors.New("INSECURE_OR_INVALID_NODE_KEY")
	}
	a.key = ed25519.PrivateKey(b)
	a.state = State{Sessions: map[string]Session{}, Operations: map[string]string{}, Consent: Consent{Cap: "0", Used: "0", Measured: "0", Rate: "0"}}
	b, e = os.ReadFile(filepath.Join(root, "state.json"))
	if e == nil {
		if e = StrictJSON(b, &a.state); e != nil {
			return nil, fmt.Errorf("CORRUPT_STATE: %w", e)
		}
	} else if !errors.Is(e, os.ErrNotExist) {
		return nil, e
	}
	if a.state.Sessions == nil || a.state.Operations == nil {
		return nil, errors.New("CORRUPT_STATE")
	}
	// Restart requires renewed consent but does not reset quota reservations.
	a.state.Consent.Enabled = false
	for id, session := range a.state.Sessions {
		session.Revoked = true
		a.state.Sessions[id] = session
	}
	if e = a.persist(); e != nil {
		return nil, e
	}
	opened = true
	return a, nil
}
func (a *Agent) Close() error {
	a.mu.Lock()
	defer a.mu.Unlock()
	if a.lockFile == nil {
		return nil
	}
	for _, cancel := range a.active {
		cancel()
	}
	if a.proofCancel != nil {
		a.proofCancel()
	}
	syscall.Flock(int(a.lockFile.Fd()), syscall.LOCK_UN)
	e := a.lockFile.Close()
	a.lockFile = nil
	return e
}
func (a *Agent) persist() error {
	b, e := json.Marshal(a.state)
	if e != nil {
		return e
	}
	return writeAtomic(filepath.Join(a.root, "state.json"), b)
}
func (a *Agent) PublicKey() ed25519.PublicKey {
	return append(ed25519.PublicKey(nil), a.key.Public().(ed25519.PublicKey)...)
}
func (a *Agent) Fingerprint() string {
	h := sha256.Sum256(a.PublicKey())
	return hex.EncodeToString(h[:])
}
func validScope(scope string) bool {
	switch scope {
	case "READ_NODE", "MANAGE_SERVICE", "MANAGE_TUNNEL", "SHARE_BANDWIDTH", "RUN_PROOF_JOB", "EXPORT_PUBLIC_RECORDS":
		return true
	}
	return false
}
func contains(v []string, s string) bool {
	for _, x := range v {
		if x == s {
			return true
		}
	}
	return false
}
func (a *Agent) CreateChallenge(scopes []string) (Challenge, string, error) {
	a.mu.Lock()
	defer a.mu.Unlock()
	if len(scopes) == 0 {
		return Challenge{}, "", errors.New("NO_SCOPES")
	}
	for _, s := range scopes {
		if !validScope(s) {
			return Challenge{}, "", errors.New("UNKNOWN_SCOPE")
		}
	}
	id, e := randomID()
	if e != nil {
		return Challenge{}, "", e
	}
	nonce, e := randomID()
	if e != nil {
		return Challenge{}, "", e
	}
	code, e := randomID()
	if e != nil {
		return Challenge{}, "", e
	}
	a.challenge = Challenge{id, nonce, a.Fingerprint(), time.Now().UTC().Add(2 * time.Minute).Format(time.RFC3339Nano)}
	a.codeHash = sha256.Sum256([]byte(code))
	a.challengeUsed = false
	a.allowedScopes = append([]string(nil), scopes...)
	return a.challenge, code, nil
}
func PairPayload(c Challenge, public string, scopes []string) []byte {
	b, _ := json.Marshal(struct {
		Domain    string    `json:"domain"`
		Challenge Challenge `json:"challenge"`
		Public    string    `json:"publicKey"`
		Scopes    []string  `json:"scopes"`
	}{"deproof-pair-v1", c, public, scopes})
	return b
}
func (a *Agent) Pair(p PairRequest) (string, error) {
	a.mu.Lock()
	defer a.mu.Unlock()
	expiry, e := time.Parse(time.RFC3339Nano, a.challenge.ExpiresAt)
	if e != nil || time.Now().After(expiry) || a.challengeUsed || p.ChallengeID != a.challenge.ID {
		return "", errors.New("EXPIRED_OR_REPLAYED_CHALLENGE")
	}
	h := sha256.Sum256([]byte(p.Code))
	if subtle.ConstantTimeCompare(h[:], a.codeHash[:]) != 1 || p.ApprovedFingerprint != a.Fingerprint() {
		return "", errors.New("PAIRING_DENIED")
	}
	if len(p.Scopes) == 0 {
		return "", errors.New("NO_SCOPES")
	}
	for _, s := range p.Scopes {
		if !contains(a.allowedScopes, s) {
			return "", errors.New("SCOPE_DENIED")
		}
	}
	pub, e := base64.StdEncoding.DecodeString(p.PublicKey)
	if e != nil || len(pub) != 32 {
		return "", errors.New("BAD_PEER_KEY")
	}
	sig, e := base64.StdEncoding.DecodeString(p.Signature)
	if e != nil || !ed25519.Verify(pub, PairPayload(a.challenge, p.PublicKey, p.Scopes), sig) {
		return "", errors.New("BAD_PAIRING_SIGNATURE")
	}
	id, e := randomID()
	if e != nil {
		return "", e
	}
	a.state.Sessions[id] = Session{p.PublicKey, append([]string(nil), p.Scopes...), false, time.Now().UTC().Add(24 * time.Hour).Format(time.RFC3339Nano)}
	a.challengeUsed = true
	if e = a.persist(); e != nil {
		delete(a.state.Sessions, id)
		return "", e
	}
	return id, nil
}
func (a *Agent) Revoke(id string) error {
	a.mu.Lock()
	defer a.mu.Unlock()
	s, ok := a.state.Sessions[id]
	if !ok {
		return errors.New("UNKNOWN_SESSION")
	}
	s.Revoked = true
	a.state.Sessions[id] = s
	for _, cancel := range a.active {
		cancel()
	}
	if a.proofCancel != nil {
		a.proofCancel()
	}
	a.state.Consent.Enabled = false
	return a.persist()
}
func scopeFor(action string) string {
	switch action {
	case "observe", "revoke":
		return "READ_NODE"
	case "consent", "transfer", "stop":
		return "SHARE_BANDWIDTH"
	case "startService", "stopService":
		return "MANAGE_SERVICE"
	case "proof", "cancelProof", "discoverProofJobs":
		return "RUN_PROOF_JOB"
	case "export":
		return "EXPORT_PUBLIC_RECORDS"
	}
	return ""
}
func (a *Agent) authorize(s Signed) (Command, error) {
	payload, e := base64.StdEncoding.DecodeString(s.Payload)
	if e != nil {
		return Command{}, errors.New("BAD_PAYLOAD")
	}
	var c Command
	if e = StrictJSON(payload, &c); e != nil {
		return c, e
	}
	session, ok := a.state.Sessions[c.SessionID]
	if !ok || session.Revoked {
		return c, errors.New("SESSION_REVOKED_OR_UNKNOWN")
	}
	expires, e := time.Parse(time.RFC3339Nano, session.ExpiresAt)
	if e != nil || time.Now().After(expires) {
		return c, errors.New("SESSION_EXPIRED")
	}
	pub, e := base64.StdEncoding.DecodeString(session.PublicKey)
	if e != nil {
		return c, e
	}
	sig, e := base64.StdEncoding.DecodeString(s.Signature)
	if e != nil || !ed25519.Verify(pub, payload, sig) {
		return c, errors.New("BAD_COMMAND_SIGNATURE")
	}
	deadline, e := time.Parse(time.RFC3339Nano, c.Deadline)
	if e != nil || time.Now().After(deadline) || deadline.After(time.Now().Add(time.Minute)) {
		return c, errors.New("INVALID_DEADLINE")
	}
	if c.Policy != "node-policy-v1" || scopeFor(c.Action) == "" || (c.Action != "revoke" && !contains(session.Scopes, scopeFor(c.Action))) {
		return c, errors.New("SCOPE_DENIED")
	}
	if len(c.OperationID) < 16 || len(c.OperationID) > 128 || strings.IndexFunc(c.OperationID, func(ch rune) bool {
		return !(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '_' || ch == '-')
	}) >= 0 {
		return c, errors.New("BAD_OPERATION_ID")
	}
	if _, ok := a.state.Operations[c.OperationID]; ok {
		return c, errors.New("REPLAYED_OPERATION")
	}
	if len(a.state.Operations) >= 10000 {
		return c, errors.New("OPERATION_LOG_FULL")
	}
	h := sha256.Sum256(payload)
	a.state.Operations[c.OperationID] = hex.EncodeToString(h[:])
	if e = a.persist(); e != nil {
		return c, e
	}
	return c, nil
}
func (a *Agent) Command(s Signed) (any, error) {
	a.mu.Lock()
	if a.lockFile == nil {
		a.mu.Unlock()
		return nil, errors.New("NODE_CLOSED")
	}
	c, e := a.authorize(s)
	if e != nil {
		a.mu.Unlock()
		return nil, e
	}
	switch c.Action {
	case "revoke":
		session := a.state.Sessions[c.SessionID]
		session.Revoked = true
		a.state.Sessions[c.SessionID] = session
		for op, cancel := range a.active {
			if a.activeOwners[op] == c.SessionID {
				cancel()
			}
		}
		if a.proofCancel != nil && a.proofSession == c.SessionID {
			a.proofCancel()
		}
		if a.state.ConsentSession == c.SessionID {
			a.state.Consent.Enabled = false
		}
		stopService := a.state.ServiceSession == c.SessionID
		e = a.persist()
		a.mu.Unlock()
		if stopService && a.hostingConfig != nil {
			if _, se := a.service("stop", nil); se != nil {
				return nil, se
			}
		}
		return map[string]string{"state": "REVOKED; flows cancellation requested"}, e
	case "observe":
		if deadline, de := time.Parse(time.RFC3339Nano, a.state.Consent.Deadline); a.state.Consent.Enabled && (de != nil || time.Now().After(deadline)) {
			a.state.Consent.Enabled = false
			if pe := a.persist(); pe != nil {
				a.mu.Unlock()
				return nil, pe
			}
		}
		active := make([]string, 0, len(a.active))
		for id := range a.active {
			active = append(active, id)
		}
		proofState := "IDLE"
		if a.proofOperation != "" {
			proofState = "RUNNING"
		}
		result := map[string]any{"fingerprint": a.Fingerprint(), "hostOS": runtime.GOOS, "architecture": runtime.GOARCH, "logicalCPUs": runtime.NumCPU(), "observedAt": time.Now().UTC().Format(time.RFC3339Nano), "keyProtection": "PERMISSION_RESTRICTED_FILE; SOFTWARE", "hosting": "BLOCKED_RUNTIME_ISOLATION_UNQUALIFIED", "serviceCount": 0, "bandwidth": a.state.Consent, "proofBackend": a.proofCapability(), "proofState": proofState, "proofOperation": a.proofOperation, "activeOperations": active}
		a.mu.Unlock()
		if a.hostingConfig != nil {
			service, se := a.service("status", nil)
			if se != nil {
				result["hosting"] = "UNAVAILABLE_RUNTIME_OR_PROFILE"
			} else {
				result["hosting"] = service
			}
		}
		return result, nil
	case "consent":
		var p struct {
			Endpoint string `json:"endpoint"`
			Cap      string `json:"capBytes"`
			Rate     string `json:"bytesPerSecond"`
			Deadline string `json:"deadline"`
			Explicit bool   `json:"explicitConsent"`
		}
		e = StrictJSON(c.Params, &p)
		if e != nil {
			a.mu.Unlock()
			return nil, e
		}
		u, e := url.Parse(p.Endpoint)
		cap, ce := strconv.ParseUint(p.Cap, 10, 64)
		rate, re := strconv.ParseUint(p.Rate, 10, 64)
		used, _ := strconv.ParseUint(a.state.Consent.Used, 10, 64)
		deadline, de := time.Parse(time.RFC3339Nano, p.Deadline)
		if e != nil || !p.Explicit || a.endpoint == "" || p.Endpoint != a.endpoint || u.Scheme != "https" || u.User != nil || ce != nil || re != nil || de != nil || cap < used || cap > 64*1024*1024 || rate == 0 || rate > 1024*1024 || deadline.Before(time.Now()) || deadline.After(time.Now().Add(time.Hour)) {
			a.mu.Unlock()
			return nil, errors.New("CONSENT_PROFILE_DENIED")
		}
		a.state.ConsentSession = c.SessionID
		a.state.Consent = Consent{true, p.Endpoint, p.Cap, a.state.Consent.Used, a.state.Consent.Measured, p.Rate, p.Deadline}
		e = a.persist()
		result := a.state.Consent
		a.mu.Unlock()
		return result, e
	case "stop":
		for _, cancel := range a.active {
			cancel()
		}
		a.state.Consent.Enabled = false
		e = a.persist()
		a.mu.Unlock()
		return map[string]string{"state": "STOP_REQUESTED; inspect completion receipts"}, e
	case "transfer":
		if len(a.active) > 0 {
			a.mu.Unlock()
			return nil, errors.New("FLOW_ALREADY_RUNNING")
		}
		var p struct {
			Bytes string `json:"bytes"`
		}
		if e = StrictJSON(c.Params, &p); e != nil {
			a.mu.Unlock()
			return nil, e
		}
		n, e := strconv.ParseUint(p.Bytes, 10, 64)
		used, ue := strconv.ParseUint(a.state.Consent.Used, 10, 64)
		cap, ce := strconv.ParseUint(a.state.Consent.Cap, 10, 64)
		rate, re := strconv.ParseUint(a.state.Consent.Rate, 10, 64)
		deadline, de := time.Parse(time.RFC3339Nano, a.state.Consent.Deadline)
		if !a.state.Consent.Enabled || e != nil || ue != nil || ce != nil || re != nil || de != nil || n == 0 || n > 1024*1024 || used > cap || n > cap-used || time.Now().After(deadline) {
			a.mu.Unlock()
			return nil, errors.New("QUOTA_OR_CONSENT_DENIED")
		}
		// Reserve before network I/O so crashes cannot reset caps. Uncertain sends remain charged.
		a.state.Consent.Used = strconv.FormatUint(used+n, 10)
		if used+n == cap {
			a.state.Consent.Enabled = false
		}
		if e = a.persist(); e != nil {
			a.mu.Unlock()
			return nil, e
		}
		ctx, cancel := context.WithDeadline(context.Background(), deadline)
		a.active[c.OperationID] = cancel
		a.activeOwners[c.OperationID] = c.SessionID
		a.activeSession = c.SessionID
		endpoint := a.state.Consent.Endpoint
		a.mu.Unlock()
		return a.transfer(ctx, cancel, c.OperationID, endpoint, n, rate)
	case "startService", "stopService":
		if c.Action == "startService" {
			a.state.ServiceSession = c.SessionID
			if pe := a.persist(); pe != nil {
				a.mu.Unlock()
				return nil, pe
			}
		}
		a.mu.Unlock()
		action := "start"
		if c.Action == "stopService" {
			action = "stop"
		}
		return a.service(action, c.Params)
	case "discoverProofJobs":
		config := a.proofConfig
		a.mu.Unlock()
		return discoverProof(config)
	case "cancelProof":
		if a.proofCancel != nil {
			a.proofCancel()
		}
		a.mu.Unlock()
		return map[string]string{"state": "CANCEL_REQUESTED; refresh observed state"}, nil
	case "proof":
		if a.proofConfig == nil {
			a.mu.Unlock()
			return nil, errors.New("PROOF_BACKEND_UNAVAILABLE")
		}
		if a.proofCancel != nil {
			a.mu.Unlock()
			return nil, errors.New("PROOF_ALREADY_RUNNING")
		}
		var params struct {
			JobID   string `json:"jobId"`
			Consent bool   `json:"explicitConsent"`
		}
		if e = StrictJSON(c.Params, &params); e != nil || !params.Consent {
			a.mu.Unlock()
			return nil, errors.New("PROOF_CONSENT_REQUIRED")
		}
		ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
		a.proofCancel = cancel
		a.proofOperation = c.OperationID
		a.proofSession = c.SessionID
		a.activeSession = c.SessionID
		config := a.proofConfig
		a.mu.Unlock()
		return a.executeProof(ctx, cancel, config, params.JobID, c.OperationID)
	case "export":
		a.mu.Unlock()
		return map[string]string{"fingerprint": a.Fingerprint(), "publicKey": base64.StdEncoding.EncodeToString(a.PublicKey()), "privateKey": "NOT_EXPORTED"}, nil
	}
	a.mu.Unlock()
	return nil, errors.New("UNKNOWN_OPERATION")
}

type pacedReader struct {
	ctx      context.Context
	left     uint64
	rate     uint64
	measured uint64
	start    time.Time
}

func (r *pacedReader) Read(p []byte) (int, error) {
	if r.left == 0 {
		return 0, io.EOF
	}
	if len(p) > 4096 {
		p = p[:4096]
	}
	if uint64(len(p)) > r.left {
		p = p[:int(r.left)]
	}
	wait := r.start.Add(time.Duration((r.measured + uint64(len(p))) * uint64(time.Second) / r.rate))
	timer := time.NewTimer(time.Until(wait))
	defer timer.Stop()
	select {
	case <-r.ctx.Done():
		return 0, r.ctx.Err()
	case <-timer.C:
	}
	clear(p)
	r.left -= uint64(len(p))
	r.measured += uint64(len(p))
	return len(p), nil
}
func (a *Agent) transfer(ctx context.Context, cancel context.CancelFunc, id, endpoint string, n, rate uint64) (any, error) {
	defer cancel()
	start := time.Now()
	reader := &pacedReader{ctx: ctx, left: n, rate: rate, start: start}
	req, e := http.NewRequestWithContext(ctx, "POST", endpoint, reader)
	if e == nil {
		req.ContentLength = int64(n)
		req.Header.Set("Content-Type", "application/octet-stream")
		var res *http.Response
		res, e = a.client.Do(req)
		if e == nil {
			_, readErr := io.Copy(io.Discard, io.LimitReader(res.Body, 65537))
			res.Body.Close()
			if readErr != nil {
				e = readErr
			}
			if res.StatusCode < 200 || res.StatusCode >= 300 {
				e = fmt.Errorf("PEER_HTTP_%d", res.StatusCode)
			}
		}
	}
	a.mu.Lock()
	defer a.mu.Unlock()
	delete(a.active, id)
	delete(a.activeOwners, id)
	measured, _ := strconv.ParseUint(a.state.Consent.Measured, 10, 64)
	a.state.Consent.Measured = strconv.FormatUint(measured+reader.measured, 10)
	a.state.Sequence++
	if pe := a.persist(); pe != nil {
		return nil, pe
	}
	outcome := "HTTP_ACCEPTED_LOCAL_OBSERVATION"
	if e != nil {
		outcome = "FAILED_OR_PARTIAL"
	}
	envelope := map[string]any{"domain": "deproof-metering-v1", "nodeFingerprint": a.Fingerprint(), "operationId": id, "sequence": strconv.FormatUint(a.state.Sequence, 10), "endpoint": endpoint, "reservedBytes": strconv.FormatUint(n, 10), "measuredBytes": strconv.FormatUint(reader.measured, 10), "unit": "application_payload_bytes_read_by_http_transport; excludes_headers_tls_retransmissions", "durationNs": strconv.FormatInt(time.Since(start).Nanoseconds(), 10), "startedAt": start.UTC().Format(time.RFC3339Nano), "endedAt": time.Now().UTC().Format(time.RFC3339Nano), "outcome": outcome, "peerAcknowledgment": nil, "reward": nil, "assurance": "LOCAL_MEASUREMENT_AND_NODE_SIGNATURE_ONLY"}
	payload, pe := json.Marshal(envelope)
	if pe != nil {
		return nil, pe
	}
	signed := map[string]any{"schema": "deproof-node-signed-v1", "payloadBase64": base64.StdEncoding.EncodeToString(payload), "signatureBase64": base64.StdEncoding.EncodeToString(ed25519.Sign(a.key, payload)), "publicKeyBase64": base64.StdEncoding.EncodeToString(a.PublicKey()), "keyProtection": "PERMISSION_RESTRICTED_FILE; SOFTWARE"}
	b, _ := json.Marshal(signed)
	if pe = writeAtomic(filepath.Join(a.root, "usage-"+strconv.FormatUint(a.state.Sequence, 10)+".json"), b); pe != nil {
		return nil, pe
	}
	return signed, e
}
func (a *Agent) Handler() http.Handler {
	mux := http.NewServeMux()
	reply := func(w http.ResponseWriter, r *http.Request, handler func([]byte) (any, error)) {
		if r.Method != "POST" {
			http.Error(w, "METHOD_NOT_ALLOWED", 405)
			return
		}
		b, e := io.ReadAll(http.MaxBytesReader(w, r.Body, 65536))
		if e != nil {
			http.Error(w, "PAYLOAD_TOO_LARGE", 413)
			return
		}
		result, e := handler(b)
		w.Header().Set("Content-Type", "application/json")
		if e != nil {
			w.WriteHeader(403)
			json.NewEncoder(w).Encode(map[string]string{"error": e.Error()})
			return
		}
		json.NewEncoder(w).Encode(result)
	}
	mux.HandleFunc("/pair", func(w http.ResponseWriter, r *http.Request) {
		reply(w, r, func(b []byte) (any, error) {
			var p PairRequest
			if e := StrictJSON(b, &p); e != nil {
				return nil, e
			}
			id, e := a.Pair(p)
			return map[string]string{"sessionId": id}, e
		})
	})
	mux.HandleFunc("/command", func(w http.ResponseWriter, r *http.Request) {
		reply(w, r, func(b []byte) (any, error) {
			var s Signed
			if e := StrictJSON(b, &s); e != nil {
				return nil, e
			}
			return a.Command(s)
		})
	})
	return mux
}
