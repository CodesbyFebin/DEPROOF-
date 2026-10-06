# Docker development

Deproof / DEPR — Built by CodesbyFebin. $DEPR is a brand concept only.

These containers wrap the existing implementation. They do not replace Android Studio or qualify physical wallet/hardware behavior. Tested target: Docker Linux/amd64 VM on an Intel Mac (Colima), Docker Engine 29.5.2, Compose 5.5.0, Buildx 0.37.2. Linux/arm64 Android builds are not qualified: use the explicit linux/amd64 platform. Source, prior APKs and host qualification remain intact.

## Separate commands

```sh
# 1: Android image, JVM tests, lint and debug APK (no Mac SDK or native Gradle cache)
bash scripts/docker-build.sh android

# 2: Build and start real services locally
bash scripts/docker-build.sh services
bash scripts/docker-up.sh

# 3: Existing portable tests + actual local proof verification, then container API integration
bash scripts/docker-qualify.sh portable
bash scripts/docker-qualify.sh integration

# 4: Independently qualify the existing hosting profile on the owner-controlled runtime
bash scripts/docker-qualify.sh hosting

# 5: Stop development services without deleting state or caches
bash scripts/docker-down.sh
```

The website is `http://127.0.0.1:8088`; the authenticated TLS node is `https://127.0.0.1:9843`. No registry push or deployment is performed. The prover is the real bounded CLI producer/verifier, idle until owner-invoked work, not an invented external proof network. The integration script uses its own `deproof-qualification` project and localhost ports 19843/18088; fixture containers are stopped afterward and volumes retained. Do not reuse its test pairing session as an owner session.

## Android build and device installation

`docker/android-build.Dockerfile` installs a digest-pinned Temurin JDK17 image, command-line tools archive 15859902 verified against Google's published SHA-256, SDK package platforms;android-37.0 and build-tools36.0.0. It uses the existing Gradle9.3.1 wrapper and application dependency pins. The Mac `local.properties`, `.gradle`, build directories, node_modules, credentials and state are excluded from the context. Source is copied to a private Linux workspace; only there is `sdk.dir=/opt/android-sdk` written. The build script uses the invoking owner UID/GID (nonroot) for host export access; default Compose-only build UID is10001. Keep the same UID/GID when reusing volumes. Named `android_gradle` and `android_home` volumes retain Linux caches and the local debug signing identity. Nothing mounts `$HOME/.gradle` or the Mac SDK.

Build outputs go to timestamped `evidence/docker/android/<run>/`: task logs, exact commands/results, test/lint reports and, only after all tasks pass, `app-debug.apk` and SHA-256. `latest-successful-run.txt` refers to a previous successful run if the latest attempt fails; inspect the current command exit/results rather than assuming that file proves the newest attempt. Failed builds retain their logs and reports but export no new APK. Existing host APK paths are not overwritten.

Keep Android Studio, accelerated emulator and USB ADB on the Mac. Select an authorized dedicated physical device and the actual successful container output:

```sh
adb devices -l
adb -s SERIAL install -r evidence/docker/android/RUN/app-debug.apk
adb -s SERIAL shell am start -W -n com.aistudio.deproof.sdwk/com.example.MainActivity
# For the loopback node endpoint on a physical device, owner explicitly forwards its port:
adb -s SERIAL reverse tcp:9843 tcp:9843
```

The container's debug key is separate from the Mac debug key. An existing differently signed installation cannot be upgraded with `-r`; do not uninstall or clear user data to conceal this. Use a dedicated test device/profile, or plan an explicitly authorized signing-compatible migration fixture. Build both fixture and upgrade with the same container signing volume for migration qualification. Never export the debug key into the source bundle. Emulator observations cannot qualify hardware-backed KeyInfo or physical wallet handoff. Follow `docs/device-qualification.md`; unavailable checks stay NOT_RUN.

## Pairing, consent and persistent state

`node_state` stores the software node identity, TLS certificate/key, operation/replay state and persistent quota reservations. `prover_state` holds owner-local jobs and proof artifacts. General services run as nonroot with read-only root, dropped capabilities, no-new-privileges and enforced CPU/memory/PID limits. Container tmpfs is bounded; ordinary state volumes are not disk-quota qualified. No general service has Docker socket/control access.

The node normally permits loopback only. The Compose entrypoint deliberately supplies `--container-listener --listen 0.0.0.0:9843` for Docker port forwarding; this explicit opt-in does not itself enforce a host firewall. Compose publishes only on 127.0.0.1. Do not publish it on public interfaces or attach untrusted containers. Pairing and every command remain authenticated. Read owner information privately with `docker compose logs node-agent`; never copy pairing codes to public evidence. Export the public certificate with `docker compose cp node-agent:/state/tls.pem ./OWNER_CERT.pem`, compare the owner-console SHA-256 and node fingerprint through an independent owner-approved channel, and use the existing strict TLS client. No insecure-TLS mode is provided. Certificates expire after 30 days; preserve state and arrange owner-controlled renewal rather than clearing identity blindly.

Default pairing grants READ_NODE only. `NODE_CONTRIBUTION_ENDPOINT` is empty, so sharing cannot start. An owner must explicitly configure a trusted fixed HTTPS receiver, authorize SHARE_BANDWIDTH scope and send consent with byte/rate/deadline caps before transfer. Restart revokes consent without resetting charged reservations. No endpoint, signature, traffic, reward or provider acceptance is fabricated.

Owner-local proof jobs can be created explicitly:

```sh
docker compose run --rm prover-worker python3 tools/proof_jobs.py create /state/jobs/OWNER_CHOSEN_NEW_DIRECTORY
```

The directory must be new. Set `NODE_PROOF_JOB=/proof-jobs/jobs/OWNER_CHOSEN_NEW_DIRECTORY/job.json` and `NODE_PROOF_OWNER=/proof-jobs/jobs/OWNER_CHOSEN_NEW_DIRECTORY/owner-public.key`, grant RUN_PROOF_JOB explicitly and recreate only the node service. Job envelopes and circuit/key commitments remain verified; expired jobs fail. No private issuer key is persisted by this educational sample. Containers do not turn its local trusted setup or same-owner verifier into production proof qualification or external integration.

## Explicit owner-managed hosting control

The existing backend is `tools/local_hosting.py`: fixed immutable BusyBox profile, no mounts/host ports/network, nonroot, read-only root and tested resource limits. It needs Docker API authority to create/inspect/stop that constrained profile. Keep the general node backend unconfigured. The `owner-hosting` profile is a separate manual owner-control tool, not an unauthenticated app API or a proxy exposed to the node.

```sh
docker compose --profile owner-hosting build hosting-control
docker compose --profile owner-hosting run --rm hosting-control status \
  --evidence /owner/hosting-isolation.json --state /state --node OWNER_NODE_FINGERPRINT
# start additionally requires the actual pinned profile digest and --explicit-consent.
# stop uses the same fingerprint; do not delete persistent state.
```

This profile alone mounts the Docker socket. Docker socket authority is host-level/root-equivalent even with read-only filesystem, dropped capabilities and network disabled; those restrictions do not contain API authority. Only the trusted local owner enables it. It runs as root to access the owner runtime's socket, unlike the general services. No unattended remote app/proof command is allowed through it. The owner-control image copies the public isolation report; rebuild it after fresh hosting qualification. Its separate hosting_state volume never mounts the node identity volume. The isolation report must be actual PASS evidence for the same runtime/version and younger than one day; start refuses stale/different evidence. Host-level qualification commands require the available supported runtime. Unsupported hosts stay BLOCKED. Rootless engines, native ARM64 and arbitrary images/services are not qualified.

## Pins and evidence

Current qualification is recorded in `evidence/docker/qualification-summary.json` and `evidence/docker/qualification-report.md`. Service image builds and portable proof/verifier tests passed. Actual service startup is BLOCKED by `no space left on device` in the 20 GiB Docker VM. The Android run exposed a cache-owner mismatch; cache volume names now include the build UID, preserving existing volumes. This correction requires testing after storage is available. No container APK exists, and the revised separate owner-control target still needs rebuilding and runtime verification. Do not prune existing volumes or restart the shared Colima VM without owner approval. Supply sufficient free host and VM storage before rebuilding; disk exhaustion is distinct from source, SDK or network failures.

The completion-kit preflight passed. Its requested bounded build stopped in round 1 because the inner sandbox could not inspect the launcher-held lock; it made no source changes or completion plan. The launcher separately ran all 14 host automated gates successfully before exiting 1 for the missing plan. Rounds 2 and 3 did not run. Logs are under `evidence/completion/20261006T071823Z-66439`.

Base images are fixed by registry SHA-256, not just mutable tags: Go1.26.4/bookworm, Python3.13.7/bookworm, Temurin17.0.20+8/noble, Node24/bookworm build helper, BusyBox1.36 and Docker29.5.2 CLI for owner control only. Actual versions/digests and commands are recorded in `evidence/docker`. Python cryptography stays pinned to50.0.0 and Go modules retain their existing go.sum. Apt-installed supporting OS utilities come from the image distribution repositories; a frozen apt snapshot and full SBOM/advisory review remain incomplete.

Primary sources: [Android command-line download/checksum](https://developer.android.com/studio), [sdkmanager](https://developer.android.com/tools/sdkmanager), [official Go images](https://hub.docker.com/_/golang), [Temurin images](https://hub.docker.com/_/eclipse-temurin), [Python images](https://hub.docker.com/_/python), [Compose profiles](https://docs.docker.com/compose/how-tos/profiles/), [Docker daemon authority](https://docs.docker.com/engine/security/).

All 336 registry IDs and unresolved acceptance gates remain authoritative. Container build/integration passes are scoped development evidence, not full production qualification. No publishing, deployment, token issuance or mainnet spending is authorized.
