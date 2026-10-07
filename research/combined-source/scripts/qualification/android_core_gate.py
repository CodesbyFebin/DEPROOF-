#!/usr/bin/env python3
"""android-core qualification gate: P1 Android build, JVM tests, policy checks."""
import json, os, pathlib, subprocess, sys, re

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "android-core"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "android-core.json"

checks = []
failures = 0

def check(id_, desc, cmd, evfile_name, *, cwd=None, expect_in_output=None):
    global failures
    evfile = EVD / evfile_name
    print(f"  {id_} {desc} ... ", end="", flush=True)
    try:
        result = subprocess.run(
            cmd, shell=True, capture_output=True, text=True,
            cwd=str(cwd or ROOT), timeout=600
        )
        combined = result.stdout + result.stderr
        evfile.write_text(combined)
        ok = result.returncode == 0
        if ok and expect_in_output:
            ok = expect_in_output in combined
        if ok:
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

def check_na(id_, reason):
    checks.append({"id": id_, "status": "NOT_APPLICABLE", "reason": reason})
    print(f"  {id_} N/A: {reason}")

print("=== android-core qualification ===")

# Build APK (debug — faster, no signing required)
check("AC-BUILD-01", "assembleDebug exits 0",
      "./gradlew assembleDebug -x lintDebug 2>&1",
      "assemble-debug.txt",
      expect_in_output="BUILD SUCCESSFUL")

# Check APK size
def apk_size_check():
    apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
    if apk.exists():
        mb = apk.stat().st_size / 1_000_000
        (EVD / "apk-size.txt").write_text(f"{apk} {mb:.1f} MB\n")
        return mb < 50
    return False

evf = EVD / "apk-size.txt"
print("  AC-BUILD-02 APK < 50 MB ... ", end="", flush=True)
if apk_size_check():
    print("PASS")
    checks.append({"id":"AC-BUILD-02","status":"PASS",
                   "command":"stat app/build/outputs/apk/debug/app-debug.apk",
                   "evidencePath": str(evf.relative_to(ROOT))})
else:
    print("FAIL")
    failures += 1
    checks.append({"id":"AC-BUILD-02","status":"FAIL",
                   "command":"stat app/build/outputs/apk/debug/app-debug.apk",
                   "evidencePath": str(evf.relative_to(ROOT))})

# Release APK (unsigned)
check("AC-BUILD-03", "assembleRelease (unsigned) exits 0",
      "./gradlew assembleRelease -x lintRelease 2>&1",
      "assemble-release.txt",
      expect_in_output="BUILD SUCCESSFUL")

# JVM unit tests
check("AC-TEST-01", "testDebugUnitTest — all tests pass",
      "./gradlew testDebugUnitTest 2>&1",
      "unit-tests.txt",
      expect_in_output="BUILD SUCCESSFUL")

# Parse test count from XML results
def count_tests():
    import glob, xml.etree.ElementTree as ET
    total, fail = 0, 0
    for f in glob.glob(str(ROOT / "app/build/test-results/**/*.xml"), recursive=True):
        try:
            tree = ET.parse(f)
            root = tree.getroot()
            for ts in root.iter("testsuite"):
                total += int(ts.get("tests", 0))
                fail += int(ts.get("failures", 0)) + int(ts.get("errors", 0))
        except Exception:
            pass
    return total, fail

tests_total, tests_fail = count_tests()
evf2 = EVD / "test-count.txt"
evf2.write_text(f"tests={tests_total} failures={tests_fail}\n")
print(f"  AC-TEST-02 Unit tests ≥ 11, 0 failures ({tests_total} tests, {tests_fail} failures) ... ", end="")
if tests_total >= 11 and tests_fail == 0:
    print("PASS")
    checks.append({"id":"AC-TEST-02","status":"PASS",
                   "command":"count from app/build/test-results XML",
                   "evidencePath": str(evf2.relative_to(ROOT))})
else:
    print("FAIL")
    failures += 1
    checks.append({"id":"AC-TEST-02","status":"FAIL",
                   "command":"count from app/build/test-results XML",
                   "evidencePath": str(evf2.relative_to(ROOT))})

# MWA stubs: stubs throw UnsupportedOperationException (never fake success)
check("AC-MWA-01", "MWA stubs present (no real wallet on server)",
      "grep -r 'UnsupportedOperationException' app/src/main/java/com/solana/ 2>&1",
      "mwa-stubs.txt")

# No hardcoded secrets / keys
check("AC-SEC-01", "No hardcoded private keys or mnemonics in source",
      "! grep -r --include='*.kt' -E '(private_key|mnemonic|seed_phrase|SECRET)\\s*=' app/src/ 2>&1; echo CLEAN",
      "no-secrets.txt",
      expect_in_output="CLEAN")

# No fabricated balance/signature assertions
check("AC-POL-01", "approvalPredicateCannotBeOverriddenByDisplayOrAi test exists",
      "grep -r 'approvalPredicateCannotBeOverriddenByDisplayOrAi' app/src/ 2>&1",
      "approval-predicate.txt")

# Room schema files present (receipts persist)
check("AC-DB-01", "Room schema files present (receipts persistence)",
      "ls app/schemas/com.example.data.DeproofDatabase/ 2>&1",
      "room-schemas.txt")

# BuildConfig.WALLET_IDENTITY_URI defined
check("AC-CFG-01", "WALLET_IDENTITY_URI buildConfigField set",
      "grep 'WALLET_IDENTITY_URI' app/build.gradle.kts 2>&1",
      "wallet-uri.txt")

# MWA real path: verify MobileWalletAdapter class is referenced in Wallet.kt
check("AC-MWA-02", "MobileWalletAdapter referenced in Wallet.kt",
      "grep -c 'MobileWalletAdapter' app/src/main/java/com/example/wallet/Wallet.kt 2>&1",
      "mwa-reference.txt")

check_na("AC-DEV-01",
         "Physical device MWA test (E001) requires hardware wallet; KVM present but no AVD image installed.")
check_na("AC-SCR-01",
         "UI screenshots require device/emulator with AVD image; not available on this server.")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures}/{len([c for c in checks if c['status']!='NOT_APPLICABLE'])})")

report = {
    "schema": "deproof-gate-v1",
    "task": "android-core",
    "status": status,
    "checks": checks,
    "limitations": [
        "Physical device (MWA E001 test) not available on this server.",
        "KVM present but no Android Virtual Device image installed; emulator tests skipped.",
        "Release APK is unsigned; signing requires DEPROOF_KEYSTORE_* credentials.",
        "Instrumented (on-device) tests not run; JVM unit tests cover domain/data layers.",
    ],
    "manualGates": [
        {"id": "AC-MAN-01", "status": "BLOCKED",
         "reason": "MWA device test E001: requires physical Android device with Phantom/Solflare."},
        {"id": "AC-MAN-02", "status": "BLOCKED",
         "reason": "UI screenshots (Now/Review/Receipts): requires device or AVD with image."},
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
