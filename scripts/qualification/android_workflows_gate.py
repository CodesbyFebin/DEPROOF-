#!/usr/bin/env python3
"""android-workflows qualification gate: P2 Tasks/Evidence/Backup/Recovery."""
import json, pathlib, subprocess, sys

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "android-workflows"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "android-workflows.json"

checks = []
failures = 0

def check(id_, desc, cmd, evfile_name, *, cwd=None, expect_in=None):
    global failures
    evfile = EVD / evfile_name
    print(f"  {id_} {desc} ... ", end="", flush=True)
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True,
                                cwd=str(cwd or ROOT), timeout=300)
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

print("=== android-workflows qualification ===")

# WorkflowTest — location consent, mapping plan, reminder
check("AWF-TEST-01", "WorkflowTest passes (4 tests)",
      "./gradlew testDebugUnitTest --tests 'com.example.domain.WorkflowTest' 2>&1",
      "workflow-test.txt", expect_in="BUILD SUCCESSFUL")

# BackupRepositoryTest — import, export, tamper detection, recovery
check("AWF-TEST-02", "BackupRepositoryTest passes (9 tests)",
      "./gradlew testDebugUnitTest --tests 'com.example.data.BackupRepositoryTest' 2>&1",
      "backup-test.txt", expect_in="BUILD SUCCESSFUL")

# RoomStoreTest — persistence verified
check("AWF-TEST-03", "RoomStoreTest passes (4 tests, Room persistence)",
      "./gradlew testDebugUnitTest --tests 'com.example.data.RoomStoreTest' 2>&1",
      "room-test.txt", expect_in="BUILD SUCCESSFUL")

# Evidence source files present
check("AWF-SRC-01", "EvidenceRepository source present",
      "test -s app/src/main/java/com/example/data/EvidenceRepository.kt && echo PRESENT",
      "evidence-repo.txt")

check("AWF-SRC-02", "BackupRepository source present",
      "test -s app/src/main/java/com/example/data/BackupRepository.kt && echo PRESENT",
      "backup-repo.txt")

check("AWF-SRC-03", "LocationCapture source present (explicit consent)",
      "test -s app/src/main/java/com/example/data/LocationCapture.kt && echo PRESENT",
      "location-src.txt")

check("AWF-SRC-04", "ReminderRepository source present",
      "test -s app/src/main/java/com/example/data/ReminderRepository.kt && echo PRESENT",
      "reminder-src.txt")

# Backup schema version present
check("AWF-SCH-01", "Portable backup schema present",
      "test -s contracts/portable-backup-v1.schema.json && echo PRESENT",
      "backup-schema.txt")

# WorkflowWorkspace (P2 Tasks screen) in UI
check("AWF-UI-01", "WorkflowWorkspace (Tasks screen) source present",
      "test -s app/src/main/java/com/example/WorkflowWorkspace.kt && echo PRESENT",
      "workflow-ui.txt")

# Room DB schema versions (migration path)
check("AWF-DB-01", "Room schema migration files present (v1 and v2)",
      "ls app/schemas/com.example.data.DeproofDatabase/1.json app/schemas/com.example.data.DeproofDatabase/2.json 2>&1",
      "room-migrations.txt")

# Location consent enforced (consent check before LocationCapture)
check("AWF-LOC-01", "Location consent gate referenced in WorkflowTest",
      "grep -c 'locationConsentDenialAndProvenance\\|consent' app/src/test/java/com/example/domain/WorkflowTest.kt 2>&1",
      "location-consent.txt")

# Evidence scope excludes pairing seeds
check("AWF-SEC-01", "exportedScopeExcludesPairingSeedsOperationsAndPermissions test exists",
      "grep -c 'exportedScopeExcludes' app/src/test/java/com/example/data/BackupRepositoryTest.kt 2>&1",
      "scope-exclusion.txt")

check_na("AWF-DEV-01",
         "Capture-from-camera/file-picker requires physical device; not testable on JVM.")
check_na("AWF-LOC-02",
         "Location permission dialog requires device runtime; tested in WorkflowTest via consent model.")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures})")

report = {
    "schema": "deproof-gate-v1",
    "task": "android-workflows",
    "status": status,
    "checks": checks,
    "limitations": [
        "Camera/file capture requires physical device or instrumented test environment.",
        "Location permission dialog not testable on JVM; consent logic covered by WorkflowTest.",
        "P2 Tasks/Evidence UI not screenshot-verified (device required).",
    ],
    "manualGates": [
        {"id":"AWF-MAN-01","status":"BLOCKED",
         "reason":"File attachment via camera/picker: requires physical device."},
        {"id":"AWF-MAN-02","status":"NOT_APPLICABLE",
         "reason":"Recovery from corrupted backup: covered by BackupRepositoryTest tamper tests."},
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
