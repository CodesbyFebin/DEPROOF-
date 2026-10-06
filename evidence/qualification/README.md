# Qualification Evidence

This directory contains qualification reports and metadata from build environments.

## Storage Policy

### ✓ What belongs in git
- `qualification-report.md` — Human-readable qualification summary
- `implementation-status.json` — Machine-readable registry coverage and test results
- Test logs and verification records
- Documentation of test commands and gates

### ✗ What should NOT be in git
- `*-source-snapshot.zip` — Large source bundles (typically 10-20 MB)
- Build artifacts (APK, binaries, proof files)
- Large binary archives

Large artifacts should be stored as:
- **GitHub Release artifacts** — Attach to tagged releases for distribution
- **Build server artifacts** — Store in CI/CD system (GitHub Actions, etc.)
- **External storage** — Separate archive servers for historical evidence

## Handoff Process

### Mac → Ubuntu Server

1. **Export qualification evidence:**
   ```bash
   # On Mac build environment
   zip -r deproof-qualification-evidence.zip \
     evidence/qualification/qualification-report.md \
     evidence/qualification/implementation-status.json \
     evidence/qualification/*.log
   ```

2. **Upload as GitHub Release artifact:**
   - Create or update release tag on target branch
   - Attach qualification evidence ZIP
   - Do NOT commit large source snapshots to git

3. **Server receives handoff:**
   - Extract qualification evidence from Release
   - Run independent server-side qualification
   - Preserve historical reports as evidence

## Current State

⚠️ **Note:** Commit 417f8a2 contains a 17 MB source snapshot in git history. This was added as a one-time handoff but should not be repeated. Future handoffs should use GitHub Releases instead.

## Next Steps

1. Workflow fixes applied — CI jobs now unblocked
2. Ubuntu server can now clone and run independent qualification
3. Establish GitHub Release process for future qualification evidence transfers
