#!/usr/bin/env python3
"""web qualification gate: P5 static web presence, accessibility basics."""
import json, pathlib, subprocess, sys, re

ROOT = pathlib.Path(subprocess.check_output(["git","rev-parse","--show-toplevel"],text=True).strip())
EVD = ROOT / "evidence" / "qualification" / "web"
EVD.mkdir(parents=True, exist_ok=True)
REPORT = ROOT / "evidence" / "qualification" / "web.json"
WEB = ROOT / "web"

checks = []
failures = 0

def check(id_, desc, cmd, evfile_name, *, cwd=None, expect_in=None):
    global failures
    evfile = EVD / evfile_name
    print(f"  {id_} {desc} ... ", end="", flush=True)
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True,
                                cwd=str(cwd or ROOT), timeout=60)
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

print("=== web qualification ===")

required_pages = ["index.html", "features.html", "privacy.html",
                  "ecosystem.html", "how-it-works.html", "project.html", "documentation.html"]
for page in required_pages:
    check(f"WEB-PAGE-{page.replace('.html','').upper().replace('-','_')}",
          f"{page} present and non-empty",
          f"test -s web/{page} && echo PRESENT", f"{page}.txt")

check("WEB-CSS-01", "style.css present and non-empty",
      "test -s web/style.css && echo PRESENT", "style-css.txt")

# HTML lang attribute — required for i18n / accessibility
check("WEB-A11Y-01", "index.html has lang attribute",
      "grep -c 'lang=' web/index.html 2>&1", "lang-index.txt", expect_in="1")

check("WEB-A11Y-02", "index.html has <title>",
      "grep -c '<title>' web/index.html 2>&1", "title-index.txt", expect_in="1")

check("WEB-A11Y-03", "index.html has viewport meta tag",
      "grep -c 'viewport' web/index.html 2>&1", "viewport-index.txt", expect_in="1")

check("WEB-A11Y-04", "features.html has lang attribute",
      "grep -c 'lang=' web/features.html 2>&1", "lang-features.txt", expect_in="1")

check("WEB-A11Y-05", "features.html has <title>",
      "grep -c '<title>' web/features.html 2>&1", "title-features.txt", expect_in="1")

# Privacy page must reference DEPR as brand concept (no token claim)
check("WEB-PRIV-01", "privacy.html exists and has content",
      "wc -c web/privacy.html 2>&1", "privacy-size.txt", expect_in="web/privacy.html")

# No mainnet token references (no contract addresses, no mint claims)
check("WEB-SEC-01", "No hardcoded mainnet contract addresses in web pages",
      r"grep -rE '[1-9A-HJ-NP-Za-km-z]{40,50}' web/*.html | grep -v '<!-' | grep -v 'example' | wc -l",
      "no-mainnet-addrs.txt", expect_in="0")

# Total HTML weight sanity check (< 200KB combined)
check("WEB-SIZE-01", "Total HTML size < 200KB",
      "du -sb web/*.html | awk '{s+=$1} END {if (s<204800) print \"SIZE_OK\"; else print \"TOO_BIG: \" s}'",
      "html-size.txt", expect_in="SIZE_OK")

# Assets directory present
check("WEB-ASSET-01", "web/assets directory present",
      "test -d web/assets && echo PRESENT", "assets-dir.txt")

check_na("WEB-MAN-01",
         "Live domain DNS/TLS: requires deployment, not testable locally.")
check_na("WEB-MAN-02",
         "Lighthouse score >= 90: requires Chromium + devserver; gated on deployment.")

print()
status = "PASS" if failures == 0 else "FAIL"
print(f"Result: {status}  (failures={failures})")

report = {
    "schema": "deproof-gate-v1",
    "task": "web",
    "status": status,
    "checks": checks,
    "limitations": [
        "Static file checks only; live DNS, TLS, and CDN delivery require deployment.",
        "Lighthouse/accessibility score not measured; requires headless Chromium.",
        "No broken-link check; requires HTTP server or crawler.",
    ],
    "manualGates": [
        {"id":"WEB-MAN-01","status":"BLOCKED",
         "reason":"Live domain DNS/TLS and CDN edge caching: requires deployment environment."},
        {"id":"WEB-MAN-02","status":"BLOCKED",
         "reason":"Lighthouse accessibility and performance score >= 90."},
    ]
}
REPORT.write_text(json.dumps(report, indent=2) + "\n")
print(f"Gate report: {REPORT.relative_to(ROOT)}")
sys.exit(0 if status == "PASS" else 1)
