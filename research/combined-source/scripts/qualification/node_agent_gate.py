#!/usr/bin/env python3
"""node-agent qualification gate: P3 edge node, hosting, bandwidth, pairing."""
import json, pathlib, subprocess, sys

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "node-agent"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "node-agent.json"
NODE = ROOT / "node-agent"

checks = []
failures = 0

def check(id_, desc, cmd, evfile_name, *, cwd=None, expect_in=None):
    global failures
    evfile = EVD / evfile_name
    print(f"  {id_} {desc} ... ", end="", flush=True)
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True,
                                cwd=str(cwd or ROOT), timeout=120)
        combined = result.stdout + result.stderr
        evfile.write_text(combined)
        ok = result.returncode == 0
        if ok and expect_in:
            ok = expect_in in combined
        if ok:
            print("PASS")
            checks.append({"id":id_,"status":"PASS","command":cmd,"evidencePath":str(evfile.relative_to(ROOT))})
        else:
            print(f"FAIL (rc={result.returncode})")
            failures += 1
            checks.append({"id":id_,"status":"FAIL","command":cmd,"evidencePath":str(evfile.relative_to(ROOT))})
    except Exception as e:
        (EVD / evfile_name).write_text(str(e))
        print(f"FAIL ({e})")
        failures += 1
        checks.append({"id":id_,"status":"FAIL","command":cmd,"evidencePath":str(evfile.relative_to(ROOT))})

def check_na(id_, reason):
    checks.append({"id":id_,"status":"NOT_APPLICABLE","reason":reason})
    print(f"  {id_} N/A: {reason}")

print("=== node-agent qualification ===")

check("NA-BUILD-01", "node-agent builds",
      "go build ./... && echo BUILD_OK", "build.txt", cwd=NODE)

check("NA-BIN-01", "node-agent binary produced",
      "go build -o /tmp/deproof-node-qa ./cmd/deproof-node/ && ls -lh /tmp/deproof-node-qa && echo OK",
      "binary.txt", cwd=NODE)

check("NA-TEST-01", "node-agent cmd tests pass",
      "go test ./cmd/... -v 2>&1", "cmd-tests.txt", cwd=NODE,
      expect_in="ok  	deproof.local/node-agent/cmd/deproof-node")

check("NA-TEST-02", "node-agent internal/agent tests pass",
      "go test ./internal/... -v 2>&1", "agent-tests.txt", cwd=NODE,
      expect_in="ok  	deproof.local/node-agent/internal/agent")

check("NA-TEST-03", "Identity persists and permissions enforced",
      "go test ./internal/... -run TestIdentityPersistsAndPermissions -v 2>&1",
      "identity-test.txt", cwd=NODE, expect_in="PASS")

check("NA-TEST-04", "Bandwidth off by default, quota persists",
      "go test ./internal/... -run TestDefaultBandwidthOffAndQuotaPersists -v 2>&1",
      "bandwidth-test.txt", cwd=NODE, expect_in="PASS")

check("NA-TEST-05", "Pairing scope replay revocation and restart",
      "go test ./internal/... -run TestPairScopeReplayRevocationAndRestart -v 2>&1",
      "pairing-test.txt", cwd=NODE, expect_in="PASS")

check("NA-TEST-06", "Forged and malformed requests denied",
      "go test ./internal/... -run TestForgedAndMalformedDenied -v 2>&1",
      "forged-test.txt", cwd=NODE, expect_in="PASS")

check("NA-TEST-07", "Container listener requires explicit opt-in",
      "go test ./cmd/... -run TestContainerListenerRequiresExplicitOptIn -v 2>&1",
      "container-test.txt", cwd=NODE, expect_in="PASS")

check("NA-SRC-01", "agent.go source present",
      "test -s node-agent/internal/agent/agent.go && echo PRESENT", "agent-src.txt")

check("NA-SRC-02", "capabilities.go source present",
      "test -s node-agent/internal/agent/capabilities.go && echo PRESENT", "capabilities-src.txt")

check_na("NA-MAN-01",
         "Physical node hosting on real hardware: requires deployment, not automatable here.")
check_na("NA-MAN-02",
         "Tunnel/public endpoint connectivity: requires network environment, not tested locally.")
check_na("NA-MAN-03",
         "Docker container isolation: docker-compose up required; qualify-docker.py covers this.")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures})")

report = {
    "schema": "deproof-gate-v1",
    "task": "node-agent",
    "status": status,
    "checks": checks,
    "limitations": [
        "Physical hosting, tunnels, and container isolation not tested; require deployment environment.",
        "Binary runs locally but network connectivity to devnet/peers not exercised here.",
        "Go tests are unit-level; no integration with Android client tested here.",
    ],
    "manualGates": [
        {"id":"NA-MAN-01","status":"BLOCKED",
         "reason":"Physical node hosting on server hardware with uptime measurement."},
        {"id":"NA-MAN-02","status":"BLOCKED",
         "reason":"Public endpoint tunnel connectivity requires live network environment."},
        {"id":"NA-MAN-03","status":"NOT_APPLICABLE",
         "reason":"Docker isolation tested via qualify-docker.py separately."},
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
