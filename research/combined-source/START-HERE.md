# DEPROOF combined source handoff

Solana × SKR · Built by CodesbyFebin

Own Your Infrastructure. Carry Your Proof.

## What this ZIP contains

The selected source snapshot of CodesbyFebin/DeProof--Solana-Depin-Dapp main at 59b5785d780387725ff34288024e2b534cbc182d, plus the supplied upstream research pack under references/upstream-research/. Existing source paths and bytes are preserved. This is a combined source/reference handoff, not a code-level merger or repaired application. There is no Git history or initialized repository in the ZIP.

326 repository files were retrieved and matched to Git blob identities, including the Gradle wrapper JAR. Full upstream path/omission lists are in handoff/source-manifest.json. Historical APKs, archived source bundles, images/videos/PDFs, bulky evidence histories, generated web delivery copies and credential-shaped paths are omitted. The source selection is not a byte-for-byte clone. Consult omissions before relying on image links or historical evidence links.

## Read before building

1. handoff/CURRENT-REVIEW.md — active source issues and qualification boundaries.
2. docs/blueprint/ — existing authoritative contracts and exact registry IDs.
3. references/upstream-research/REPOSITORY-REVIEW.md and INTEGRATION-PLAN.md.
4. docs/setup.md and the repository's pinned build configuration.

Preserve any existing DEPROOF checkout. Extract this into a new directory, not over uncommitted work. Do not import .build-excluded examples into active source automatically. Reference code remains unqualified; GPL examples stay separate. Preserve all upstream license/copyright notices.

## Ubuntu continuation

Check disk space and active writers first. Confirm JDK/SDK/tool versions against the pinned project, not an assumed Ubuntu version. From this root:

```bash
python3 handoff/verify-handoff.py
bash ./gradlew :app:testDebugUnitTest --stacktrace
bash ./gradlew :app:lintDebug --stacktrace
bash ./gradlew :app:assembleDebug --stacktrace
```

These commands were not executed in this handoff. Archive integrity PASS is not BUILD_QUALIFIED. Correct lint enforcement and inspect actual lint findings; exit zero alone is insufficient with the current configuration. Run the broader qualification only after inspecting its dependencies and recording fresh source-bound results. Hash only new successfully produced artifacts. Do not reuse historical acceptance for this combined package.

Device/wallet/hardware, provider, live RPC, Linux isolation, container runtime and release acceptance are NOT_RUN here. No production readiness, universal provider support, payment or store submission is claimed. SKR is distinct from $DEPR; $DEPR is a brand concept, not token issuance. Nothing has been pushed or deployed.
