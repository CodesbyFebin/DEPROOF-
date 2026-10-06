package agent

import (
	"os"
	"runtime"
	"strconv"
	"strings"
)

// Read-only Linux observations, not reservations or production isolation evidence.
func observedMemoryMiB() (int, bool) {
	if runtime.GOOS != "linux" {
		return 0, false
	}
	raw, err := os.ReadFile("/proc/meminfo")
	if err != nil {
		return 0, false
	}
	var available uint64
	for _, line := range strings.Split(string(raw), "\n") {
		parts := strings.Fields(line)
		if len(parts) == 3 && parts[0] == "MemAvailable:" {
			v, e := strconv.ParseUint(parts[1], 10, 64)
			if e != nil {
				return 0, false
			}
			available = v * 1024
		}
	}
	if available == 0 {
		return 0, false
	}
	limit, le := os.ReadFile("/sys/fs/cgroup/memory.max")
	used, ue := os.ReadFile("/sys/fs/cgroup/memory.current")
	if le != nil || ue != nil {
		return 0, false
	}
	if strings.TrimSpace(string(limit)) != "max" {
		max, e := strconv.ParseUint(strings.TrimSpace(string(limit)), 10, 64)
		cur, c := strconv.ParseUint(strings.TrimSpace(string(used)), 10, 64)
		if e != nil || c != nil {
			return 0, false
		}
		remaining := uint64(0)
		if max > cur {
			remaining = max - cur
		}
		if remaining < available {
			available = remaining
		}
	}
	return int(available / (1024 * 1024)), true
}
func observedCpuCores() int {
	cores := runtime.NumCPU()
	raw, e := os.ReadFile("/sys/fs/cgroup/cpu.max")
	if e == nil {
		parts := strings.Fields(string(raw))
		if len(parts) == 2 && parts[0] != "max" {
			quota, q := strconv.Atoi(parts[0])
			period, p := strconv.Atoi(parts[1])
			if q == nil && p == nil && period > 0 && quota/period < cores {
				cores = quota / period
			}
		}
	}
	return cores
}
func proofCapabilities(config *ProofConfig) map[string]any {
	result := map[string]any{"availability": "UNAVAILABLE", "reason": "PROOF_BACKEND_UNAVAILABLE"}
	if config == nil {
		return result
	}
	job, e := discoverProof(config)
	if e != nil {
		result["reason"] = "PINNED_JOB_UNAVAILABLE"
		return result
	}
	n, ok := job.(map[string]any)
	if !ok {
		return result
	}
	profile, ok := n["resourceProfile"].(map[string]any)
	if !ok {
		result["reason"] = "JOB_PROFILE_UNAVAILABLE"
		return result
	}
	memory, ok := observedMemoryMiB()
	if !ok {
		result["reason"] = "RESOURCE_OBSERVATION_UNAVAILABLE_ON_HOST"
		return result
	}
	return map[string]any{"availability": "AVAILABLE", "backends": []string{"gnark-v0.14.0"}, "circuitVersions": map[string]any{"owner-local-cubic": []any{profile["circuitVersion"]}}, "memoryMiB": memory, "cpuCores": observedCpuCores(), "resourceSource": "LINUX_MEMAVAILABLE_WITH_CGROUP_V2_CAP; NOT_RESERVED", "providerQualification": "LOCAL_EDUCATIONAL_ONLY; EXTERNAL_PROVIDER_NOT_QUALIFIED"}
}
