package main

import (
	"crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"deproof.local/node-agent/internal/agent"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"flag"
	"fmt"
	"log"
	"math/big"
	"net"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"syscall"
	"time"
)

func certificate(root string) (tls.Certificate, error) {
	certPath := filepath.Join(root, "tls.pem")
	keyPath := filepath.Join(root, "tls.key")
	if _, e := os.Stat(certPath); e == nil {
		return tls.LoadX509KeyPair(certPath, keyPath)
	}
	pub, key, e := ed25519.GenerateKey(rand.Reader)
	if e != nil {
		return tls.Certificate{}, e
	}
	serial, e := rand.Int(rand.Reader, new(big.Int).Lsh(big.NewInt(1), 128))
	if e != nil {
		return tls.Certificate{}, e
	}
	template := &x509.Certificate{SerialNumber: serial, Subject: pkix.Name{CommonName: "Deproof local node"}, NotBefore: time.Now().Add(-time.Minute), NotAfter: time.Now().Add(30 * 24 * time.Hour), KeyUsage: x509.KeyUsageDigitalSignature, ExtKeyUsage: []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth}, IPAddresses: []net.IP{net.ParseIP("127.0.0.1")}, DNSNames: []string{"localhost"}}
	der, e := x509.CreateCertificate(rand.Reader, template, template, pub, key)
	if e != nil {
		return tls.Certificate{}, e
	}
	pk, e := x509.MarshalPKCS8PrivateKey(key)
	if e != nil {
		return tls.Certificate{}, e
	}
	if e = os.WriteFile(keyPath, pem.EncodeToMemory(&pem.Block{Type: "PRIVATE KEY", Bytes: pk}), 0600); e != nil {
		return tls.Certificate{}, e
	}
	if e = os.WriteFile(certPath, pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: der}), 0600); e != nil {
		return tls.Certificate{}, e
	}
	return tls.LoadX509KeyPair(certPath, keyPath)
}
func validateListener(address string, container bool) error {
	host, _, err := net.SplitHostPort(address)
	if err != nil {
		return fmt.Errorf("INVALID_LISTENER")
	}
	ip := net.ParseIP(host)
	if ip == nil || (!ip.IsLoopback() && !(container && ip.IsUnspecified())) {
		return fmt.Errorf("NON_LOOPBACK_LISTENER_DENIED")
	}
	return nil
}
func main() {
	root := flag.String("state", ".deproof-node", "Owner-only state directory")
	endpoint := flag.String("contribution-endpoint", "", "Owner-allowlisted HTTPS known-byte receiver; empty disables sharing")
	scopes := flag.String("pair-scopes", "READ_NODE", "Owner-approved comma-separated pairing scopes")
	proofTool := flag.String("proof-tool", "", "Absolute path to owner-reviewed tools/proof_jobs.py")
	proofJob := flag.String("proof-job", "", "Absolute path to signed owner-local job")
	proofOwner := flag.String("proof-owner", "", "Absolute path to pinned owner public key")
	receiverCA := flag.String("contribution-ca", "", "Optional owner-pinned CA PEM for the fixed contribution receiver")
	listen := flag.String("listen", "127.0.0.1:9843", "Loopback-only TLS address")
	hostingTool := flag.String("hosting-tool", "", "Absolute path to owner-reviewed tools/local_hosting.py")
	hostingEvidence := flag.String("hosting-evidence", "", "Absolute path to actual local isolation qualification report")
	containerListener := flag.Bool("container-listener", false, "Explicit owner opt-in for container wildcard listener; requires localhost-only host publication and no public ingress")
	flag.Parse()
	if err := validateListener(*listen, *containerListener); err != nil {
		log.Fatal(err)
	}
	a, e := agent.Open(*root, *endpoint)
	if e != nil {
		log.Fatal(e)
	}
	defer a.Close()
	if *hostingTool != "" || *hostingEvidence != "" {
		if e = a.ConfigureHosting(agent.HostingConfig{Tool: *hostingTool, Evidence: *hostingEvidence}); e != nil {
			log.Fatal(e)
		}
	}
	if *receiverCA != "" {
		if e = a.ConfigureReceiverCA(*receiverCA); e != nil {
			log.Fatal(e)
		}
	}
	if *proofTool != "" || *proofJob != "" || *proofOwner != "" {
		if e = a.ConfigureProof(agent.ProofConfig{Tool: *proofTool, Job: *proofJob, Owner: *proofOwner}); e != nil {
			log.Fatal(e)
		}
	}
	challenge, code, e := a.CreateChallenge(strings.Split(*scopes, ","))
	if e != nil {
		log.Fatal(e)
	}
	b, _ := json.Marshal(challenge)
	fmt.Printf("Node fingerprint: %s\nPairing challenge (2 minutes): %s\nSingle-use pairing code: %s\nPrivate node key: software, permission-restricted file.\n", a.Fingerprint(), b, code)
	cert, e := certificate(*root)
	if e != nil {
		log.Fatal(e)
	}
	digest := sha256.Sum256(cert.Certificate[0])
	fmt.Printf("TLS certificate SHA-256: %s\n", hex.EncodeToString(digest[:]))
	server := &http.Server{Addr: *listen, Handler: a.Handler(), TLSConfig: &tls.Config{MinVersion: tls.VersionTLS13, Certificates: []tls.Certificate{cert}}, ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 20 * time.Second, WriteTimeout: 90 * time.Second, IdleTimeout: 30 * time.Second, MaxHeaderBytes: 8192}
	// Local owner can terminate every session/flow with SIGINT/SIGTERM; restarting disables consent.
	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM, syscall.SIGUSR1)
	go func() {
		for sig := range sigs {
			if sig == syscall.SIGUSR1 {
				if err := a.RevokeAll(); err != nil {
					log.Print("OWNER_REVOCATION_PERSIST_FAILED")
				}
				continue
			}
			server.Close()
			return
		}
	}()
	listenerLabel := "Loopback-only TLS listener: https://"
	if *containerListener {
		listenerLabel = "Container TLS listener: https://"
	}
	fmt.Println(listenerLabel + *listen + "; certificate in state/tls.pem. Hosting blocked until isolation qualified. Proofs require an owner-pinned local job. Owner revokes all sessions with SIGUSR1.")
	if e = server.ListenAndServeTLS("", ""); e != nil && e != http.ErrServerClosed {
		log.Fatal(e)
	}
}
