#!/usr/bin/env python3
"""Foundation qualification gate for Deproof build pipeline."""
import json, os, pathlib, subprocess, sys

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "foundation"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "foundation.json"

checks = []
failures = 0

def check(id_, desc, cmd, evfile_name, *, cwd=None):
    global failures
    evfile = EVD / evfile_name
    print(f"  {id_} {desc} ... ", end="", flush=True)
    try:
        result = subprocess.run(
            cmd, shell=True, capture_output=True, text=True,
            cwd=str(cwd or ROOT), timeout=120
        )
        combined = result.stdout + result.stderr
        evfile.write_text(combined)
        if result.returncode == 0 and combined.strip():
            print("PASS")
            checks.append({"id": id_, "status": "PASS", "command": cmd,
                            "evidencePath": str(evfile.relative_to(ROOT))})
        else:
            print(f"FAIL (rc={result.returncode})")
            failures += 1
            checks.append({"id": id_, "status": "FAIL", "command": cmd,
                            "evidencePath": str(evfile.relative_to(ROOT))})
    except Exception as e:
        evfile.write_text(str(e))
        print(f"FAIL ({e})")
        failures += 1
        checks.append({"id": id_, "status": "FAIL", "command": cmd,
                        "evidencePath": str(evfile.relative_to(ROOT))})

print("=== Foundation qualification ===")

check("FND-TC-01", "Java 17+",
      "java -version 2>&1 | grep -E 'version .1[7-9]|version .2[0-9]'",
      "java-version.txt")

check("FND-TC-02", "Gradle 8/9 wrapper",
      "./gradlew --version 2>&1 | grep -E 'Gradle [89]'",
      "gradle-version.txt")

check("FND-TC-03", "Go 1.21+",
      "go version | grep -E 'go1\\.(2[1-9]|[3-9][0-9])'",
      "go-version.txt")

check("FND-TC-04", "Python 3.10+",
      "python3 --version | grep -E 'Python 3\\.(1[0-9]|[2-9][0-9])'",
      "python-version.txt")

check("FND-TC-05", "Android SDK platforms dir",
      "SDK=$(grep 'sdk.dir' local.properties | cut -d= -f2) && ls \"$SDK/platforms\"",
      "android-sdk.txt")

check("FND-REG-01", "Registry counts 120F/100C/30E/62+24FN",
      "python3 scripts/check-registries.py",
      "registries.txt")

check("FND-SCH-01", "Contract schemas present",
      "ls contracts/evidence-v2.schema.json contracts/receipt-v2.schema.json "
      "contracts/portable-backup-v1.schema.json contracts/evidence-envelope-v2.schema.json",
      "schemas.txt")

check("FND-VEC-01", "Golden test vectors present",
      "ls fixtures/jcs-vectors.json fixtures/location-golden.json "
      "fixtures/transfer-checked-vectors.json",
      "vectors.txt")

check("FND-BP-01", "Blueprint master prompt present",
      "test -s docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/DEPROOF_FINAL_MASTER_PROMPT.md "
      "&& echo PRESENT",
      "blueprint.txt")

check("FND-GO-01", "node-agent builds cleanly",
      "go build ./... && echo BUILD_OK",
      "node-agent-build.txt", cwd=ROOT/"node-agent")

check("FND-GO-02", "prover-worker builds cleanly",
      "go build ./... && echo BUILD_OK",
      "prover-worker-build.txt", cwd=ROOT/"prover-worker")

check("FND-GO-03", "node-agent tests pass",
      "go test ./... 2>&1",
      "node-agent-test.txt", cwd=ROOT/"node-agent")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures}/{len(checks)})")

report = {
    "schema": "deproof-gate-v1",
    "task": "foundation",
    "status": status,
    "checks": checks,
    "limitations": [
        "Device/emulator not tested; KVM present but no AVD image installed.",
        "Docker services not started; qualify-docker.py covers that path separately.",
        "Mainnet/devnet RPC not called; all checks are local.",
    ],
    "manualGates": [
        {"id": "FND-MAN-01", "status": "NOT_APPLICABLE",
         "reason": "Physical measurement not applicable to foundation toolchain task."}
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
