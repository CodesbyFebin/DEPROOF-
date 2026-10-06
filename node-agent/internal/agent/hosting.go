package agent

import (
	"context"
	"encoding/json"
	"errors"
	"os"
	"path/filepath"
	"time"
)

type HostingConfig struct{ Tool, Evidence string }

func (a *Agent) ConfigureHosting(config HostingConfig) error {
	for _, p := range []string{config.Tool, config.Evidence} {
		if !filepath.IsAbs(p) {
			return errors.New("HOST_CONFIG_DENIED")
		}
		info, e := os.Lstat(p)
		if e != nil || !info.Mode().IsRegular() {
			return errors.New("HOST_CONFIG_DENIED")
		}
	}
	a.hostingConfig = &config
	if a.state.ServiceSession != "" {
		if session, ok := a.state.Sessions[a.state.ServiceSession]; !ok || session.Revoked {
			if _, e := a.service("stop", nil); e != nil {
				return e
			}
		}
	}
	return nil
}
func (a *Agent) service(action string, params json.RawMessage) (any, error) {
	a.serviceMu.Lock()
	defer a.serviceMu.Unlock()
	if action == "start" {
		a.mu.Lock()
		s, ok := a.state.Sessions[a.state.ServiceSession]
		a.mu.Unlock()
		if !ok || s.Revoked {
			return nil, errors.New("SESSION_REVOKED_OR_UNKNOWN")
		}
	}
	if a.hostingConfig == nil {
		return nil, errors.New("RUNTIME_ISOLATION_UNQUALIFIED")
	}
	var p struct {
		ProfileDigest string `json:"profileDigest"`
		Consent       bool   `json:"explicitConsent"`
	}
	if action == "start" {
		if e := StrictJSON(params, &p); e != nil || !p.Consent {
			return nil, errors.New("SERVICE_PROFILE_CONSENT_REQUIRED")
		}
	}
	ctx, cancel := context.WithTimeout(context.Background(), 25*time.Second)
	defer cancel()
	args := []string{action, "--evidence", a.hostingConfig.Evidence, "--state", a.root, "--node", a.Fingerprint()}
	if action == "start" {
		args = append(args, "--profile-digest", p.ProfileDigest, "--explicit-consent")
	}
	result, e := proofCommand(ctx, &ProofConfig{Tool: a.hostingConfig.Tool}, args...)
	if e != nil {
		return nil, errors.New("SERVICE_RUNTIME_OR_PROFILE_FAILED")
	}
	return result, nil
}
