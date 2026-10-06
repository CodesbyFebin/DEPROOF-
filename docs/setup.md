# Build and verification setup

Work from the repository root. Do not replace existing local.properties. Run:

```sh
./scripts/setup-android.sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./scripts/qualify.sh
```

Requires JDK 17, Android SDK platform 37.0/build-tools 36.0.0, and access to Google Maven/Maven Central/Gradle distributions. Gradle needs local lock/daemon socket permission. The workspace sandbox prevents these sockets, but an approved run outside the sandbox succeeded. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`; assembly, JVM tests and lint pass. Run these commands with local cache/socket and dependency access. Device qualification remains separate.

The local cache-only JVM runner works independently:

```sh
python3 scripts/qualify-core.py
```

It locates exact pinned jars in ~/.gradle/caches/modules-2/files-2.1 (override DEPROOF_MAVEN_CACHE). It runs the same CoreTest class, not Android integration tests. No stub org.json or fake production wallet is used.

Go agent:

```sh
./scripts/qualify-node.sh
node-agent/build/deproof-node --state /private/owner/deproof-node
```

The binary produced here is darwin/amd64. Linux deployment/isolation is NOT_RUN. Loopback-only TLS, owner-controlled certificate and pairing challenge are required. See ecosystem/node.md. Do not expose it publicly.

Prover:

```sh
./scripts/qualify-prover.sh
```

Network access is required to resolve pinned gnark modules. The script builds separate producer/verifier executables, generates an educational cubic proof and rejects a wrong public input. Current execution stops at dependency fetch; no proof exists. Jobs, execution isolation, cancellation, signed external job protocol and pinned verification-key approval are unfinished; do not route arbitrary jobs to this local sample CLI.

Independent verifier:

```sh
python3 -m pip install -r tools/requirements.txt
python3 tools/verify.py receipt.json --files /private/manifest-files
python3 -m unittest discover -s tools -p 'test_*.py'
```

Use stable file IDs as names in the supplied directory. Private files are not uploaded. Missing files are NOT_RUN, not valid. Local signature integrity does not remotely attest hardware or establish physical truth/chain settlement.

Website:

```sh
python3 scripts/build-web.py
python3 -m http.server 8080 --bind 127.0.0.1
# open http://127.0.0.1:8080/web/index.html in a normal local browser
```

Serve the repository root so documentation links resolve. Starting a local listener is blocked in the present sandbox; static link/JS checks still run.

## Manual gates

1. Restore normal ADB access; run adb devices -l. Install only when a real device is listed: adb install -r app/build/outputs/apk/debug/app-debug.apk.
2. Test Now/Review/Receipts at 320dp, tablet width, 200% text, TalkBack and dark/light theme. Light/adaptive UI work remains incomplete.
3. Configure owned identity URL; connect a real MWA wallet; check authorized address/account selection, missing wallet, rejection, cancellation and disconnect. Strict capability must return signed bytes.
4. Prepare/review fresh devnet memo; read live fee/simulation/blockhash. Approve explicitly on device; validate returned message/signatures, derive actual signature, match RPC acceptance and independently observe confirmed/finalized. Record sanitized device/cluster/capability evidence. Mainnet remains disabled until the qualified gate is implemented and recorded.
5. Import/capture files; deny/cancel access; test size/cancellation, file mutation, process death, duplicate IDs and task association. System-camera permission is controlled by the camera app; capture cancellation never creates a photo record. Camera pending URI recovery and video preview are unfinished.
6. Test real qualified TEE/StrongBox key generation, report KeyInfo level and independently verify exported envelope. Older generic hardware-only reports refuse as UNQUALIFIED_KEY.
7. Crash during submission and reconcile known signature; duplicate taps must not resubmit. Submission intent is durable but automatic recovery UI/reconciliation remains incomplete.
8. Qualify actual Linux container isolation, host-resource failures, TLS pairing topology, rate/cap/stop/restart behavior, independent receiver counters and real proof execution before expanding dependent features.

No release/store qualification, mainnet spend, deployment or token issuance is performed.

## Portable encrypted backups

```sh
python3 tools/bundle.py export public-bundle.json receipt.json
python3 tools/bundle.py verify public-bundle.json
python3 tools/bundle.py create-key backup.key
python3 tools/bundle.py encrypt public-bundle.json backup.enc --key backup.key
python3 tools/bundle.py decrypt backup.enc restored-bundle.json --key backup.key
```

Store the owner-controlled key separately offline. Loss prevents recovery. Private files are omitted unless --include-private-files DIRECTORY is explicitly supplied to export. Exact original receipt bytes are preserved; no hardware/wallet private keys are included. Output files use exclusive creation to avoid overwriting existing records. Android backup UI is not implemented.

## Connected local node workflows

See [local workflows](ecosystem/local-workflows.md) for actual pinned-TLS pairing, owner revocation, quota consent, the qualified fixed isolation profile, signed proof-job discovery and recovery behavior. Run `npm ci --prefix scripts/browser --ignore-scripts --no-audit --no-fund` for pinned browser tooling and use an installed local Chrome. `bash scripts/qualify.sh` now includes real local TLS/Docker/browser checks and permitted read-only public RPC. Device-only gates remain separate.
