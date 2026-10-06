# Ubuntu Bootstrap Report — DeProof

**Timestamp:** 2026-10-06T21:39:00Z
**Machine:** HP ProLiant DL20 Gen9

## OS / Kernel / Architecture
- Ubuntu 25.04 (plucky) — interim release, supported until Jan 2026
- Kernel: 6.14.0-37-generic x86_64
- Note: Not LTS; no blocker but recommend Ubuntu 24.04 LTS for long-term use

## Hardware
- CPU: Intel Xeon E3-1220 v6 @ 3.00GHz, 4 cores (no hyperthreading)
- RAM: 30 GiB total, ~27 GiB available
- Disk: 915 GiB (/dev/sda2), 852 GiB free (2% used) — adequate for all assets
- Swap: 8 GiB
- Virtualization: bare-metal (systemd-detect-virt=none)
- /dev/kvm: present (crw-rw----+) — emulator support available

## Existing Tooling
| Tool    | Version         | Status     |
|---------|-----------------|------------|
| Git     | 2.48.1          | OK         |
| Python  | 3.13.3          | OK         |
| Java    | not installed   | NEEDED     |
| Go      | not installed   | NEEDED     |
| Node.js | not installed   | NEEDED     |
| npm     | not installed   | NEEDED     |
| Docker  | not installed   | NEEDED     |

## Source
- Path: /home/codesbyfebin/DeProof
- Remote: https://github.com/CodesbyFebin/DEPROOF-
- Branch: main
- HEAD: ba6607a (Promote expanded Mac implementation to main while preserving cloud history)
- Status: clean checkout, no dirty files

## Expanded Implementation Present
- app/             YES (Kotlin/Compose Android)
- node-agent/      YES (Go)
- prover-worker/   YES (Go)
- tools/           YES (Python verifier/bundle)
- web/             YES (static website)
- scripts/         YES (qualify/build scripts)
- docs/            YES (blueprint + architecture)
- docker/          YES (Dockerfiles)

## Requirements From Source
- JDK 17 (compileOptions = VERSION_17)
- AGP 9.1.1, Kotlin 2.4.20, KSP 2.3.6
- Android compileSdk=37, minSdk=24, build-tools 36.0.0
- Gradle 9.3.1 (wrapper)
- Go (node-agent, prover-worker)
- Node.js (browser qualification via scripts/browser/)
- Docker (container qualification)

## Next Steps
1. Install JDK 17
2. Install Android SDK cmdline-tools + platform-37 + build-tools-36.0.0
3. Install Go
4. Install Node.js
5. Install Docker
6. Run setup-android.sh, then qualify.sh
