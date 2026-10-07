# Ubuntu Qualification Final Report
**Project**: Deproof — Solana × SKR DePIN DApp  
**Built by**: CodesbyFebin  
**Host**: HP ProLiant DL20 Gen9  
**OS**: Ubuntu 25.04 (plucky) — Linux 6.14.0-37-generic — x86_64  
**Date**: 2026-10-06  
**Final Commit**: `2980061`  
**Verdict**: **BUILD_QUALIFIED**

---

## Environment

| Component | Version |
|-----------|---------|
| OS | Ubuntu 25.04 plucky (Linux 6.14.0-37-generic) |
| JDK | 17 (OpenJDK) |
| Android SDK | platform android-37.0, build-tools 36.0.0 |
| Gradle | 9.3.1 (wrapper, cache pre-populated) |
| Go | 1.26.4 (via GOTOOLCHAIN=auto) |
| Node.js | 20.18.1 |
| Docker | 28.2.2 (cgroup v2, apparmor+seccomp+cgroupns) |
| Python | 3.13.3 |
| KVM | `/dev/kvm` present |

---

## Gate Results (16/16 Automated PASS)

| # | Gate | Result | Detail |
|---|------|--------|--------|
| 1 | qualify-core.py | **PASS** | 66 JVM domain tests |
| 2 | assembleDebug | **PASS** | 18 MB APK, SHA-256 `6ca1f419036b40228f2d99fc753469e1322d2c10c5a6c27bed48db97ae023def` |
| 3 | testDebugUnitTest | **PASS** | 91 Android unit tests |
| 4 | lintDebug | **PASS** | Zero lint errors |
| 5 | qualify-portable.sh | **PASS** | 24 portable tests |
| 6 | verifier-tests | **PASS** | 24/24 Python verifier tests |
| 7 | localization-catalogs | **PASS** | 212 strings |
| 8 | rpc-read-only | **PASS** | devnet slot 508159173; mainnet-beta slot 453960550 |
| 9 | web-build | **PASS** | 7 pages built |
| 10 | qualify-web.py | **PASS** | Static link/JS checks |
| 11 | registry-checks | **PASS** | 120F + 100C + 62FN + 24EF + 30E = 336 exact |
| 12 | qualify-node | **PASS** | Go 1.26.4; 2 packages (cmd/deproof-node, internal/agent) |
| 13 | qualify-prover | **PASS** | Groth16 BN254 circuit (3 constraints); proof verified input 35; input 36 rejected |
| 14 | browser-checks | **PASS** | 110 checks: axe, keyboard nav, overflow, images, links (320px + 1280px) |
| 15 | qualify-hosting | **PASS** | 13/13 Docker isolation fixtures (nonroot, readonly, caps, network, mem/cpu/pid/tmpfs/oom) |
| 16 | qualify-integration | **PASS** | 15/15: TLS pin, revocation, replay, quota, signing, proof chain, service lifecycle |

### Manual / Device Gates (NOT_RUN by design)

| Gate | Status | Reason |
|------|--------|--------|
| Emulator instrumented tests | NOT_RUN | Requires Android emulator image + adb |
| Hardware wallet (MWA/Solana Saga) | NOT_RUN | Requires physical device |
| Production mainnet-beta transactions | NOT_RUN | Read-only probes only; no mainnet spend |
| TEE/StrongBox key qualification | NOT_RUN | Requires qualified hardware |
| File capture/camera permission flows | NOT_RUN | Requires physical device |

---

## Artifacts

| Artifact | Path | Detail |
|----------|------|--------|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` | 18 MB |
| APK SHA-256 | `6ca1f419036b40228f2d99fc753469e1322d2c10c5a6c27bed48db97ae023def` | |
| Source manifest (start) | `evidence/qualification/source-ubuntu-20261006.json` | 236 hashes |
| Source manifest (end) | `evidence/qualification/source-complete-20261006.json` | 236 hashes |
| Hosting isolation JSON | `evidence/qualification/hosting-isolation.json` | 13 checks |
| Browser screenshots | `evidence/qualification/browser/` | 16 PNGs (8 pages × 2 viewports) |
| node-agent binary | `node-agent/build/deproof-node` | linux/amd64 |
| prover binary | `prover-worker/build/prove` | linux/amd64 |
| verifier binary | `prover-worker/build/verify` | linux/amd64 |
| Qualified proof result | `prover-worker/build/qualified-result-*/` | BN254 Groth16 |

---

## Coverage

| Metric | Value |
|--------|-------|
| Total registry entries | 336 |
| Accepted | 55 |
| Unresolved | 281 |
| F (function contracts) | 120 |
| C (constraints) | 100 |
| FN (functions) | 62 |
| EF (entry functions) | 24 |
| E (events) | 30 |

All 281 unresolved items require device, emulator, wallet, or external/mainnet qualification and are documented in `evidence/qualification/prioritized-backlog.json`.

---

## Fixes Applied This Session

| Fix | File | Detail |
|-----|------|--------|
| kotlinx-coroutines version | `scripts/qualify-core.py:12` | `1.11.0` → `1.9.0` |
| local.properties created | `local.properties` | `sdk.dir=/home/codesbyfebin/android-sdk` |
| Gradle 9.3.1 cache | `.gradle/wrapper/dists/` | Manual pre-population (10s timeout workaround) |
| GOTOOLCHAIN=auto | inline | Go 1.24.2 system → 1.26.4 downloaded for node-agent + prover |

---

## Scope Constraints (Maintained Throughout)

- No git push, no PR, no release tags, no deployment
- No token issuance, no mainnet spend ($DEPR brand concept only)
- No credentials printed, no wallet keys disclosed
- No Docker volumes pruned, no caches deleted
- No uncommitted work discarded
- Solana: devnet + mainnet-beta read-only probes only
- One implementation writer per checkout — no nested agents or build orchestrators

---

## Commit History (This Session)

| Commit | Description |
|--------|-------------|
| `c85862e` | All local gates (Android, Python, RPC, web, registry) + qualify-core.py fix |
| `eca2203` | Go, prover, browser gates completed |
| `2980061` | Docker hosting (13 checks) + integration (15 checks) complete |
| `(this)` | Final report, evidence archive, handoff |

---

## Next Steps

1. Manual device gates when hardware is available (see `docs/setup.md` manual gates section)
2. `./gradlew assembleDebugAndroidTest` + emulator when API 37 image is set up
3. Push to release branch only after manual gates complete (if required)
4. No mainnet spend until qualified gate is implemented and recorded per `docs/setup.md`
