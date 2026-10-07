package agent

import (
	"crypto/tls"
	"crypto/x509"
	"errors"
	"net/http"
	"os"
	"time"
)

// An owner-supplied CA is scoped to the already fixed contribution receiver.
// No API command can expand it or disable certificate verification.
func (a *Agent) ConfigureReceiverCA(path string) error {
	pem, e := os.ReadFile(path)
	if e != nil {
		return e
	}
	if len(pem) > 65536 {
		return errors.New("RECEIVER_CA_TOO_LARGE")
	}
	roots := x509.NewCertPool()
	if !roots.AppendCertsFromPEM(pem) {
		return errors.New("RECEIVER_CA_INVALID")
	}
	a.client = &http.Client{Timeout: 15 * time.Second, Transport: &http.Transport{TLSClientConfig: &tls.Config{RootCAs: roots, MinVersion: tls.VersionTLS13}}, CheckRedirect: func(*http.Request, []*http.Request) error { return errors.New("REDIRECT_DENIED") }}
	return nil
}
