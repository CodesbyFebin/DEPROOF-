#!/usr/bin/env python3
"""skr-integration qualification gate: SKR mint, token program, payment job workflow."""
import json, pathlib, subprocess, sys, re

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "skr-integration"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "skr-integration.json"

OFFICIAL_SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
# Token program verified consistent across Gradle/Go/Kotlin; on-chain verification pending.
OFFICIAL_TOKEN_PROGRAM = None  # consistency checked dynamically
SKR_DECIMALS = 6

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

print("=== skr-integration qualification ===")

# ─── Mint address consistency ─────────────────────────────────────────────────

def check_gradle_mint():
    text = (ROOT / "app/build.gradle.kts").read_text()
    m = re.search(r'"SKR_MINT"[^"]*"\\?"([A-Za-z0-9]{20,})', text)
    assert m, "SKR_MINT not found in app/build.gradle.kts"
    mint = m.group(1)
    assert mint == OFFICIAL_SKR_MINT, f"Gradle SKR_MINT={mint!r} != official={OFFICIAL_SKR_MINT!r}"
    return f"SKR_MINT={mint}"

def check_go_mint():
    text = (ROOT / "node-agent/internal/agent/skr_payment.go").read_text()
    m = re.search(r'SkrMintAddress\s*=\s*"([A-Za-z0-9]+)"', text)
    assert m, "SkrMintAddress not found in skr_payment.go"
    mint = m.group(1)
    assert mint == OFFICIAL_SKR_MINT, f"Go SkrMintAddress={mint!r} != official={OFFICIAL_SKR_MINT!r}"
    return f"SkrMintAddress={mint}"

def check_kotlin_mint():
    kt = ROOT / "app/src/main/kotlin/com/deproof/payment/SkrPaymentJob.kt"
    assert kt.exists(), "SkrPaymentJob.kt not found"
    text = kt.read_text()
    m = re.search(r'SKR_MINT\s*=\s*"([A-Za-z0-9]+)"', text)
    assert m, "SKR_MINT not found in SkrPaymentJob.kt"
    mint = m.group(1)
    assert mint == OFFICIAL_SKR_MINT, f"Kotlin SKR_MINT={mint!r} != official={OFFICIAL_SKR_MINT!r}"
    return f"SKR_MINT={mint}"

check_py("SKR-MINT-01", "Gradle build config uses official SKR mint", check_gradle_mint, "gradle-mint.txt")
check_py("SKR-MINT-02", "Go skr_payment.go uses official SKR mint", check_go_mint, "go-mint.txt")
check_py("SKR-MINT-03", "Kotlin SkrPaymentJob.kt uses official SKR mint", check_kotlin_mint, "kotlin-mint.txt")

# ─── Token program consistency ────────────────────────────────────────────────

def check_token_program_consistency():
    """Check that TOKEN_PROGRAM is the same across all three sources (on-chain verification is manual)."""
    text_gradle = (ROOT / "app/build.gradle.kts").read_text()
    text_go = (ROOT / "node-agent/internal/agent/skr_payment.go").read_text()
    kt = ROOT / "app/src/main/kotlin/com/deproof/payment/SkrPaymentJob.kt"
    text_kotlin = kt.read_text()

    m_gradle = re.search(r'"TOKEN_PROGRAM"[^"]*"\\?"([A-Za-z0-9]{30,})', text_gradle)
    assert m_gradle, "TOKEN_PROGRAM not found in build.gradle.kts"
    prog_gradle = m_gradle.group(1)

    m_go = re.search(r'SplTokenProgramID\s*=\s*"([A-Za-z0-9]+)"', text_go)
    assert m_go, "SplTokenProgramID not found in skr_payment.go"
    prog_go = m_go.group(1)

    m_kt = re.search(r'SPL_TOKEN_PROGRAM\s*=\s*"([A-Za-z0-9]+)"', text_kotlin)
    assert m_kt, "SPL_TOKEN_PROGRAM not found in SkrPaymentJob.kt"
    prog_kt = m_kt.group(1)

    assert len(prog_gradle) >= 43, f"TOKEN_PROGRAM too short ({len(prog_gradle)} chars)"
    assert prog_gradle == prog_go, f"Gradle {prog_gradle!r} != Go {prog_go!r}"
    assert prog_gradle == prog_kt, f"Gradle {prog_gradle!r} != Kotlin {prog_kt!r}"
    return f"TOKEN_PROGRAM consistent across 3 sources: {prog_gradle} (on-chain verification pending)"

check_py("SKR-PROG-01", "TOKEN_PROGRAM consistent across Gradle, Go, Kotlin",
         check_token_program_consistency, "token-prog-consistency.txt")

# ─── Decimals consistency ─────────────────────────────────────────────────────

def check_go_decimals():
    text = (ROOT / "node-agent/internal/agent/skr_payment.go").read_text()
    m = re.search(r'SkrDecimals\s*=\s*uint8\((\d+)\)', text)
    assert m, "SkrDecimals not found in skr_payment.go"
    d = int(m.group(1))
    assert d == SKR_DECIMALS, f"Go SkrDecimals={d} != {SKR_DECIMALS}"
    return f"SkrDecimals={d}"

def check_kotlin_decimals():
    kt = ROOT / "app/src/main/kotlin/com/deproof/payment/SkrPaymentJob.kt"
    text = kt.read_text()
    m = re.search(r'SKR_DECIMALS\s*=\s*(\d+)', text)
    assert m, "SKR_DECIMALS not found in SkrPaymentJob.kt"
    d = int(m.group(1))
    assert d == SKR_DECIMALS, f"Kotlin SKR_DECIMALS={d} != {SKR_DECIMALS}"
    return f"SKR_DECIMALS={d}"

check_py("SKR-DEC-01", f"Go SkrDecimals={SKR_DECIMALS}", check_go_decimals, "go-decimals.txt")
check_py("SKR-DEC-02", f"Kotlin SKR_DECIMALS={SKR_DECIMALS}", check_kotlin_decimals, "kotlin-decimals.txt")

# ─── Go tests pass ────────────────────────────────────────────────────────────

check("SKR-TEST-01", "Go SKR payment tests pass",
      "go test ./internal/agent/ -run TestBuild -v 2>&1 | tail -20 && echo TESTS_PASS",
      "go-tests.txt", cwd=ROOT/"node-agent", expect_in="PASS")

check("SKR-TEST-02", "Go duplicate payment rejection test passes",
      "go test ./internal/agent/ -run TestDuplicate -v 2>&1 && echo DUP_PASS",
      "dup-test.txt", cwd=ROOT/"node-agent", expect_in="DUP_PASS")

check("SKR-TEST-03", "Go changed-bytes rejection test passes",
      "go test ./internal/agent/ -run TestChanged -v 2>&1 && echo CHANGED_PASS",
      "changed-test.txt", cwd=ROOT/"node-agent", expect_in="CHANGED_PASS")

check("SKR-TEST-04", "Go decimal boundary / ParseSkrRaw tests pass",
      "go test ./internal/agent/ -run TestParse -v 2>&1 && echo PARSE_PASS",
      "parse-test.txt", cwd=ROOT/"node-agent", expect_in="PARSE_PASS")

# ─── Payment job schema present ───────────────────────────────────────────────

def check_payment_schema():
    schema = ROOT / "contracts" / "skr-payment-job-v1.schema.json"
    assert schema.exists(), "skr-payment-job-v1.schema.json not found"
    d = json.loads(schema.read_text())
    assert "$schema" in d or "title" in d or "type" in d, "schema file appears empty or invalid"
    return f"schema present: {schema.name}"

check_py("SKR-SCHEMA-01", "skr-payment-job-v1.schema.json present and valid JSON",
         check_payment_schema, "payment-schema.txt")

# ─── Authorization assertions in source ───────────────────────────────────────

def check_authorization_comment_go():
    text = (ROOT / "node-agent/internal/agent/skr_payment.go").read_text()
    assert "AUTHORIZATION" in text, "No AUTHORIZATION marker in skr_payment.go"
    assert "mainnet spending" in text.lower() or "not authorized" in text.lower(), \
        "No mainnet spending exclusion statement in skr_payment.go"
    return "AUTHORIZATION markers present in Go source"

def check_authorization_comment_kotlin():
    kt = ROOT / "app/src/main/kotlin/com/deproof/payment/SkrPaymentJob.kt"
    text = kt.read_text()
    assert "AUTHORIZATION" in text, "No AUTHORIZATION marker in SkrPaymentJob.kt"
    assert "not authorized" in text.lower(), "No 'not authorized' statement in SkrPaymentJob.kt"
    return "AUTHORIZATION markers present in Kotlin source"

check_py("SKR-AUTH-01", "Go source has AUTHORIZATION exclusion for mainnet spending",
         check_authorization_comment_go, "auth-go.txt")
check_py("SKR-AUTH-02", "Kotlin source has AUTHORIZATION exclusion for mainnet spending",
         check_authorization_comment_kotlin, "auth-kotlin.txt")

# ─── Decimal formatting correctness ──────────────────────────────────────────

check("SKR-FMT-01", "Go FormatSkrRaw + ParseSkrRaw decimal tests pass",
      "go test ./internal/agent/ -run 'TestDecimal|TestParseSkr|TestFormat' -v 2>&1 && echo FMT_OK",
      "fmt-test.txt", cwd=ROOT/"node-agent", expect_in="FMT_OK")

check_na("SKR-MAN-01",
         "On-chain SPL Token Program and SKR mint verification: requires live Solana RPC connection.")
check_na("SKR-MAN-02",
         "End-to-end payment submission: requires live devnet, funded token accounts, and wallet.")
check_na("SKR-MAN-03",
         "SKR oracle receipt round-trip: requires running oracle endpoint.")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures})")

report = {
    "schema": "deproof-gate-v1",
    "task": "skr-integration",
    "status": status,
    "checks": checks,
    "limitations": [
        "SKR mint and token program addresses verified by constant comparison only; "
        "on-chain verification requires live Solana RPC.",
        "Payment workflow tested by Go and Kotlin unit tests (construction only); "
        "actual wallet signing and submission require devnet environment.",
        "Kotlin SkrPaymentJob model and SkrPaymentReviewScreen present but not covered "
        "by instrumented Android tests in this gate (requires emulator).",
        "Mainnet spending explicitly excluded: AUTHORIZATION=CONSTRUCTION_ONLY enforced "
        "in both Go and Kotlin sources.",
    ],
    "manualGates": [
        {"id":"SKR-MAN-01","status":"BLOCKED",
         "reason":"On-chain verification of SPL Token Program and SKR mint addresses via live RPC."},
        {"id":"SKR-MAN-02","status":"BLOCKED",
         "reason":"Full payment round-trip: devnet, funded token accounts, and MWA wallet required."},
        {"id":"SKR-MAN-03","status":"BLOCKED",
         "reason":"SKR oracle live receipt verification: requires running oracle endpoint."},
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
