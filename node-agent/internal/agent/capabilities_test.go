package agent

import (
	"runtime"
	"testing"
)

func TestUnconfiguredProofCapabilitiesAreUnavailable(t *testing.T) {
	caps := proofCapabilities(nil)
	if caps["availability"] != "UNAVAILABLE" || caps["reason"] != "PROOF_BACKEND_UNAVAILABLE" {
		t.Fatalf("unconfigured backend claimed capabilities: %v", caps)
	}
}
func TestResourceObservationDoesNotInventMemoryOnUnsupportedHosts(t *testing.T) {
	memory, available := observedMemoryMiB()
	if runtime.GOOS != "linux" && (memory != 0 || available) {
		t.Fatal("unsupported host fabricated RAM observation")
	}
	if memory < 0 || observedCpuCores() < 0 {
		t.Fatal("negative observed resources")
	}
}
