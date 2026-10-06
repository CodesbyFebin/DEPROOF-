# Dependency decisions — 2026-10-06

Installed: macOS x86_64; OpenJDK 17.0.20.1; Go 1.26.4; Node 26.4.0; Android platforms 36.1/37.0 and build-tools 36.0.0; Gradle distributions 9.3.1/9.5.1. About 9.8 GiB disk remained at inspection. Shell DNS is unavailable. ADB cannot install its socket listener. Docker 29.7.2 cannot access the configured Colima socket. No physical-device availability was established.

| Component | Pin / decision | Qualification |
| --- | --- | --- |
| AGP / Gradle / JDK | 9.1.1 / 9.3.1 / 17 | Official AGP compatibility supports API 37.0, build-tools 36.0.0. Gradle fails on sandbox lock sockets; wrapper additionally cannot download through DNS. |
| Kotlin / Compose compiler | 2.4.20 | Cached compiler runs JVM tests. Root KGP dependency overrides AGP built-in Kotlin. Android compatibility still requires actual build. |
| Compose UI/foundation | 1.12.1 | Official stable UI release; Android assembly NOT_RUN past bootstrap. |
| Material 3 | 1.3.1 | Conservative cached stable pin; no alpha dependency. |
| Activity | 1.13.0 | Cached artifact pin; assemble gate unresolved. |
| Coroutines | 1.9.0 | Explicit Android runtime pin. Local compiler runner additionally uses compiler's cached 1.11.0 dependency. |
| Room / KSP | 2.8.4 / 2.3.6 | Room official release; KSP artifact resolution and generated schema remain unverified. |
| DataStore | 1.2.1 | Current stable official release read during implementation; persisted endpoint/theme preferences have UI source; device qualification remains unrun. |
| MWA KTX | 2.0.7 | Official repository version; API inspected from upstream source. Artifact unavailable locally; exact tagged API compilation is BLOCKED. No atomic sign-and-send fallback. |
| Jackson | databind/core 2.21.1; annotations 2.21 | Cached JVM-capable JSON library; strict duplicate detection; canonical restricted string/bool/null profile tested. Numeric manifest values refused. |
| Bouncy Castle | 1.79 | Cached Ed25519 verifier pin; upstream security review/update remains a release gate. |
| desugar_jdk_libs | 2.1.5 | Required for java.time/Base64 compatibility on minSdk 24; unavailable assembly gate. |
| Python cryptography | 50.0.0 | Installed, independently verifies P-256 and Ed25519 test signatures. |
| Go agent | 1.26.4; standard library only | Built for darwin/amd64; Linux host gates not run. |
| gnark / gnark-crypto | 0.14.0 / 0.19.0 | Official versioned example and go.mod inspected; fetch fails DNS; no proof emitted. Transitive go.sum and artifact/VK hashes cannot exist until dependency download/setup. |
| Website | HTML/CSS/vanilla JS; no npm dependency | Seven generated pages, local JSON inspection and search; browser/device accessibility qualification still unrun. |

The MWA identity is `http://localhost`, a local development identity. It is not a claim to own deproof.app or a public deployment. Before release, configure a developer-controlled HTTPS identity and actual icon. Account publicKey bytes are encoded as base58. Tokens stay in the adapter's memory and are not backed up/exported. A process restart requires reconnection and fresh review.

Official sources actually consulted:

- [AGP 9.1.1 compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Kotlin Gradle configuration](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Compose UI releases](https://developer.android.com/jetpack/androidx/releases/compose-ui)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
- [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore)
- [KSP releases](https://github.com/google/ksp/releases)
- [MWA repository](https://github.com/solana-mobile/mobile-wallet-adapter)
- [MWA Android guide](https://docs.solanamobile.com/android-native/using_mobile_wallet_adapter)
- [MWA adapter source](https://raw.githubusercontent.com/solana-mobile/mobile-wallet-adapter/main/android/clientlib-ktx/src/main/java/com/solana/mobilewalletadapter/clientlib/AdapterOperations.kt)
- [gnark versioned module](https://raw.githubusercontent.com/Consensys/gnark/v0.14.0/go.mod)
- [gnark versioned cubic example](https://raw.githubusercontent.com/Consensys/gnark/v0.14.0/examples/cubic/cubic.go)
- [Docker rootless boundary](https://docs.docker.com/engine/security/rootless/)

Pins describe the requested stack, not proof that its Android dependency graph assembled. Dependency lockfiles, Gradle verification metadata, resolved SBOM and security advisory review remain required after network/tool access is restored. No release target/store readiness claim is made.

CI action release pins were read from their official GitHub latest-release pages on 2026-10-06: checkout v7.0.1, setup-go v7.0.0, setup-java v6.0.1, setup-python v7.0.0. CI was not run remotely. Sources: https://github.com/actions/checkout/releases/latest, https://github.com/actions/setup-go/releases/latest, https://github.com/actions/setup-java/releases/latest, https://github.com/actions/setup-python/releases/latest.

Android SDK CI setup is pinned to the officially inspected release android-actions/setup-android v4.0.4: https://github.com/android-actions/setup-android/releases/tag/v4.0.4. Portable backup uses installed cryptography 50.0.0 AESGCM (AES-256-GCM) with random 96-bit nonces, a separate 256-bit owner key and domain-separated authenticated data. This is local portable backup tooling; Android integration remains unqualified.

## Approved build retry

The initial sandbox restrictions were resolved by an approved execution outside the sandbox. `scripts/qualify-android.sh` passes assembly, JVM tests and lint. MWA 2.0.7 shares namespaces across clientlib and clientlib-ktx; `android.uniquePackageNames=false` preserves compatibility with AGP 9 (https://developer.android.com/build/releases/agp-9-0-0-release-notes). StrongBox generation and exception handling are guarded at API 28. Lint is enabled without a baseline or suppressed errors.

The gnark graph is resolved and pinned in `prover-worker/go.mod` and `go.sum`. Producer and independent verifier pass a generated local cubic proof and reject a wrong public input. This uses an educational local trusted setup and does not qualify external jobs or production proof trust. Historical environment observations above describe the initial sandbox attempts.

## Continuation qualification

Pinned Playwright 1.63.0 and @axe-core/playwright 4.13.0 are local browser-test dependencies in `scripts/browser/package-lock.json`; the installed Chrome version is recorded in browser evidence. No browser or test tooling enters the APK. The local service image is immutable BusyBox digest `73aaf090f3d85aa34ee199857f03fa3a95c8ede2ffd4cc2cdb5b94e566b11662`, inspected from the owner's cache. Docker engine 29.5.2 is inside the local Ubuntu 24.04.4 Linux VM; rootful engine trust is explicit. A differently configured/runtime host requires requalification.
