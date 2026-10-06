# Container qualification

Generated: 2026-10-06T08:44:23.659307+00:00. Deproof / DEPR — Built by CodesbyFebin.

**BLOCKED**: container images and portable checks succeeded; end-to-end container qualification is incomplete.

| Gate | Status | Scope / reason |
|---|---|---|
| compose-and-shell-validation | PASS | Recorded commands and evidence in qualification-summary.json. |
| service-images-build | PASS | Node/prover/web images built. Final source changes require rebuild; not a runtime integration pass. |
| portable-container-qualification | PASS | Prior image: 18 Python tests; actual local Groth16 verification and tampered public input/proof rejection; 336 registry IDs. Newly added workflow tests are not container-qualified. |
| android-container-build | BLOCKED | Observed Gradle cache lock directory failure from prior UID cache. UID-specific volumes now configured but untested; Docker VM disk is full. No container APK produced. |
| container-service-integration | BLOCKED | Docker state storage exhausted |
| fixed-hosting-profile | PASS | Existing fixed profile on this runtime only. Revised separate owner-control image/profile not qualified. |
| revised-owner-control-profile | BLOCKED | Earlier owner target built, but final root-owned separate state/report copy revisions require rebuild and exercise; storage unavailable. |
| shutdown-state-retention | PASS | Only Deproof development containers stopped, named volumes retained; fixture integration also stops its own project without volume deletion. |
| completion-kit | FAIL | Inner sandbox denied process inspection of launcher-held lock; no source changes or completion plan. Launcher ran all 14 host gates successfully then exited 1. Rounds 2/3 not run. |

No container APK was produced. The host-built APK and all prior checkpoints are preserved.

Next: provide host/VM storage, rerun Android/services builds, portable/integration checks and owner-profile qualification. Final source changes are not yet container-qualified. Keep all 336 registry IDs and unresolved device/external acceptance gates.

Commands and usage: [docker-development.md](../../docs/docker-development.md). No publishing, deployment, token issuance or mainnet spending.
