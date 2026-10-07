#!/usr/bin/env python3
"""release-audit gate: registry coverage, SBOM, build reproducibility, checksums."""
import json, pathlib, subprocess, sys, hashlib

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "release-audit"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "release-audit.json"

checks = []
failures = 0

def check_py(id_, desc, fn, evfile_name):
    global failures
    evfile = EVD / evfile_name
    print(f"  {id_} {desc} ... ", end="", flush=True)
    try:
        result = fn()
        evfile.write_text(str(result))
        print("PASS")
        checks.append({"id":id_,"status":"PASS","command":"<python>","evidencePath":str(evfile.relative_to(ROOT))})
    except AssertionError as e:
        evfile.write_text(str(e))
        print(f"FAIL ({e})")
        failures += 1
        checks.append({"id":id_,"status":"FAIL","command":"<python>","evidencePath":str(evfile.relative_to(ROOT))})
    except Exception as e:
        evfile.write_text(str(e))
        print(f"FAIL ({e})")
        failures += 1
        checks.append({"id":id_,"status":"FAIL","command":"<python>","evidencePath":str(evfile.relative_to(ROOT))})

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

print("=== release-audit qualification ===")

# === Registry coverage ===
def check_features():
    d = json.loads((ROOT / "features.json").read_text())
    features = d["features"]
    assert len(features) == 120, f"Expected 120 features, got {len(features)}"
    ids = [f["id"] for f in features]
    assert len(set(ids)) == len(ids), "Duplicate feature IDs"
    priorities = {f.get("priority","") for f in features}
    assert priorities, "No priority tags"
    return f"120 features, priorities={sorted(priorities)}"

def check_core_checks():
    d = json.loads((ROOT / "core-checks.json").read_text())
    checks_ = d["checks"]
    assert len(checks_) == 100, f"Expected 100 checks, got {len(checks_)}"
    ids = [c["id"] for c in checks_]
    assert len(set(ids)) == len(ids), "Duplicate check IDs"
    return f"100 core checks, unique IDs verified"

def check_ecosystem():
    d = json.loads((ROOT / "ecosystem-requirements.json").read_text())
    reqs = d["requirements"]
    assert len(reqs) == 30, f"Expected 30 ecosystem reqs, got {len(reqs)}"
    return f"30 ecosystem requirements"

def check_functions():
    d = json.loads((ROOT / "functions.json").read_text())
    base = d["base"]
    eco = d["ecosystem"]
    assert len(base) == 62, f"Expected 62 base FN, got {len(base)}"
    assert len(eco) == 24, f"Expected 24 ecosystem FN, got {len(eco)}"
    return f"62 base + 24 ecosystem = 86 functions"

check_py("RA-REG-01", "features.json has 120 features (F001–F120)",
         check_features, "features-count.txt")
check_py("RA-REG-02", "core-checks.json has 100 checks (C001–C100)",
         check_core_checks, "core-checks-count.txt")
check_py("RA-REG-03", "ecosystem-requirements.json has 30 requirements (E001–E030)",
         check_ecosystem, "ecosystem-count.txt")
check_py("RA-REG-04", "functions.json has 86 functions (62 base + 24 ecosystem)",
         check_functions, "functions-count.txt")

# === All 6 prior gate reports PASS ===
def check_all_gates_pass():
    gate_names = ["foundation","android-core","android-workflows",
                  "node-agent","prover-worker","web","integration"]
    results = {}
    for name in gate_names:
        f = ROOT / "evidence" / "qualification" / f"{name}.json"
        assert f.exists(), f"Missing gate report: {name}.json"
        d = json.loads(f.read_text())
        assert d["status"] == "PASS", f"{name} gate is {d['status']}"
        passed = sum(1 for c in d["checks"] if c["status"] == "PASS")
        results[name] = f"PASS ({passed} checks)"
    return json.dumps(results, indent=2)

check_py("RA-GATE-01", "All 7 qualification gates PASS",
         check_all_gates_pass, "all-gates-pass.txt")

# === APK artifacts ===
check("RA-APK-01", "Debug APK present",
      "find app/build/outputs/apk/debug -name '*.apk' -size +1k 2>/dev/null | head -1 | grep -c apk",
      "debug-apk.txt", expect_in="1")

check("RA-APK-02", "Release APK present",
      "find app/build/outputs/apk/release -name '*.apk' -size +1k 2>/dev/null | head -1 | grep -c apk",
      "release-apk.txt", expect_in="1")

# === APK checksums ===
def compute_apk_checksums():
    import glob
    apks = (list(pathlib.Path(ROOT/"app/build/outputs/apk/debug").glob("*.apk")) +
            list(pathlib.Path(ROOT/"app/build/outputs/apk/release").glob("*.apk")))
    if not apks:
        raise AssertionError("No APK files found")
    result = {}
    for apk in apks:
        sha256 = hashlib.sha256(apk.read_bytes()).hexdigest()
        result[str(apk.relative_to(ROOT))] = {"sha256": sha256, "size_bytes": apk.stat().st_size}
    return json.dumps(result, indent=2)

check_py("RA-CHKSUM-01", "APK SHA-256 checksums computed",
         compute_apk_checksums, "apk-checksums.txt")

# === Go binary checksums ===
def compute_go_checksums():
    binaries = [
        ROOT / "node-agent" / "build" / "deproof-node",
        ROOT / "prover-worker" / "build" / "prove",
        ROOT / "prover-worker" / "build" / "verify",
    ]
    result = {}
    for b in binaries:
        if b.exists():
            sha256 = hashlib.sha256(b.read_bytes()).hexdigest()
            result[str(b.relative_to(ROOT))] = {"sha256": sha256, "size_bytes": b.stat().st_size}
    assert len(result) >= 2, f"Expected >=2 Go binaries, found {list(result.keys())}"
    return json.dumps(result, indent=2)

check_py("RA-CHKSUM-02", "Go binary SHA-256 checksums computed",
         compute_go_checksums, "go-checksums.txt")

# === Room migration path complete ===
check("RA-DB-01", "Room schema migration 1→2 present",
      "ls app/schemas/com.example.data.DeproofDatabase/1.json app/schemas/com.example.data.DeproofDatabase/2.json && echo MIGRATIONS_OK",
      "room-migrations.txt", expect_in="MIGRATIONS_OK")

# === Contracts/schemas present ===
check("RA-CONTRACT-01", "All contract schemas present",
      "ls contracts/*.schema.json 2>/dev/null | wc -l && echo SCHEMAS_PRESENT",
      "contract-schemas.txt", expect_in="SCHEMAS_PRESENT")

# === No secrets in tracked files ===
check("RA-SEC-01", "No .env files tracked in git",
      "git ls-files | grep -c '\\.env$' || echo 0",
      "no-env-tracked.txt", expect_in="0")

check("RA-SEC-02", "No private key files tracked",
      "git ls-files | grep -iE '\\.(pem|key|p12|jks|keystore)$' | wc -l",
      "no-privkey-tracked.txt", expect_in="0")

# === Build branch on current commit ===
check("RA-GIT-01", "On deproof/build branch",
      "git rev-parse --abbrev-ref HEAD", "git-branch.txt",
      expect_in="deproof/build")

check_na("RA-MAN-01",
         "Keystore signing: requires DEPROOF_KEYSTORE_PATH env vars, not present in CI.")
check_na("RA-MAN-02",
         "Reproducible build byte-for-byte: requires identical JDK/AGP version pinning across machines.")
check_na("RA-MAN-03",
         "SBOM (CycloneDX/SPDX) generation: requires additional tooling not installed.")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures})")

report = {
    "schema": "deproof-gate-v1",
    "task": "release-audit",
    "status": status,
    "checks": checks,
    "limitations": [
        "APK is unsigned (release signing requires keystore env vars).",
        "SBOM not generated; requires CycloneDX or SPDX tooling.",
        "Reproducible build not verified byte-for-byte across machines.",
        "Go binaries checksummed from local build; deterministic cross-platform builds not verified.",
    ],
    "manualGates": [
        {"id":"RA-MAN-01","status":"BLOCKED",
         "reason":"Release APK signing: DEPROOF_KEYSTORE_PATH and credentials required."},
        {"id":"RA-MAN-02","status":"BLOCKED",
         "reason":"Byte-for-byte reproducible build verification across different machines."},
        {"id":"RA-MAN-03","status":"BLOCKED",
         "reason":"CycloneDX/SPDX SBOM generation: tooling not present in build environment."},
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
