package agent

import (
	"bytes"
	"context"
	"crypto/ed25519"
	"encoding/base64"
	"encoding/json"
	"errors"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
	"time"
)

type ProofConfig struct{ Tool, Job, Owner string }

func (a *Agent) ConfigureProof(config ProofConfig) error {
	for _, p := range []string{config.Tool, config.Job, config.Owner} {
		if !filepath.IsAbs(p) {
			return errors.New("PROOF_CONFIG_PATH_DENIED")
		}
		info, e := os.Lstat(p)
		if e != nil || !info.Mode().IsRegular() {
			return errors.New("PROOF_CONFIG_PATH_DENIED")
		}
	}
	a.mu.Lock()
	defer a.mu.Unlock()
	a.proofConfig = &config
	return nil
}
func (a *Agent) proofCapability() string {
	if a.proofConfig == nil {
		return "UNCONFIGURED"
	}
	return "OWNER_PINNED_LOCAL_CUBIC; HOST_ISOLATION_NOT_QUALIFIED"
}

// Bounded capture; job/profile text is never executed as a shell command.
type boundedBuffer struct{ bytes.Buffer }

func (b *boundedBuffer) Write(p []byte) (int, error) {
	if b.Len()+len(p) > 2*1024*1024 {
		return 0, errors.New("PROOF_OUTPUT_TOO_LARGE")
	}
	return b.Buffer.Write(p)
}
func proofCommand(ctx context.Context, config *ProofConfig, args ...string) (map[string]any, error) {
	if config == nil {
		return nil, errors.New("PROOF_BACKEND_UNAVAILABLE")
	}
	all := append([]string{config.Tool}, args...)
	cmd := exec.CommandContext(ctx, "python3", all...)
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
	cmd.Cancel = func() error {
		if cmd.Process == nil {
			return nil
		}
		return syscall.Kill(-cmd.Process.Pid, syscall.SIGKILL)
	}
	cmd.WaitDelay = 2 * time.Second
	var out, stderr boundedBuffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	if e := cmd.Run(); e != nil {
		if ctx.Err() != nil {
			return nil, errors.New("PROOF_CANCELLED_OR_DEADLINE")
		}
		return nil, errors.New("PROOF_JOB_VALIDATION_OR_EXECUTION_FAILED")
	}
	var result map[string]any
	if e := json.Unmarshal(out.Bytes(), &result); e != nil {
		return nil, errors.New("PROOF_OUTPUT_INVALID")
	}
	return result, nil
}
func discoverProof(config *ProofConfig) (any, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if config == nil {
		return nil, errors.New("PROOF_BACKEND_UNAVAILABLE")
	}
	job, e := proofCommand(ctx, config, "discover", "--job", config.Job, "--owner", config.Owner)
	if e != nil {
		return nil, e
	}
	job["jobId"] = job["id"]
	job["runtimeIsolation"] = "NOT_QUALIFIED_LOCAL_EDUCATIONAL_PROFILE"
	return job, nil
}
func (a *Agent) executeProof(ctx context.Context, cancel context.CancelFunc, config *ProofConfig, jobID, operationID string) (any, error) {
	defer cancel()
	result, e := proofCommand(ctx, config, "run", "--job", config.Job, "--owner", config.Owner, "--job-id", jobID, "--output", filepath.Join(a.root, "proof-"+operationID))
	a.mu.Lock()
	defer a.mu.Unlock()
	a.proofCancel = nil
	a.proofOperation = ""
	if e != nil {
		return nil, e
	}
	result["operationId"] = operationID
	result["domain"] = "deproof-contribution-v1"
	result["producerFingerprint"] = a.Fingerprint()
	result["nodeFingerprint"] = a.Fingerprint()
	delete(result, "artifactDirectory")
	payload, e := json.Marshal(result)
	if e != nil {
		return nil, e
	}
	signed := map[string]any{"schema": "deproof-node-signed-v1", "payloadBase64": base64.StdEncoding.EncodeToString(payload), "signatureBase64": base64.StdEncoding.EncodeToString(ed25519.Sign(a.key, payload)), "publicKeyBase64": base64.StdEncoding.EncodeToString(a.PublicKey()), "keyProtection": "PERMISSION_RESTRICTED_FILE; SOFTWARE"}
	raw, e := json.Marshal(signed)
	if e != nil {
		return nil, e
	}
	if e = writeAtomic(filepath.Join(a.root, "contribution-"+operationID+".json"), raw); e != nil {
		return nil, e
	}
	return signed, nil
}
func (a *Agent) RevokeAll() error {
	a.mu.Lock()
	for id, s := range a.state.Sessions {
		s.Revoked = true
		a.state.Sessions[id] = s
	}
	for _, cancel := range a.active {
		cancel()
	}
	if a.proofCancel != nil {
		a.proofCancel()
	}
	a.state.Consent.Enabled = false
	stopService := a.state.ServiceSession != ""
	e := a.persist()
	a.mu.Unlock()
	if stopService && a.hostingConfig != nil {
		if _, se := a.service("stop", nil); se != nil {
			return se
		}
	}
	return e
}
