# Support matrix

| Component/environment | Actual status |
| --- | --- |
| JVM domain on OpenJDK 17/macOS x86_64 | Local test command evidence recorded |
| Android minSdk 24 / target 37 | Source/pins only; compile/APK/device gates blocked/unrun |
| Physical MWA wallet and TEE/StrongBox | NOT_RUN; device unavailable under ADB restrictions |
| Solana mainnet observations/devnet signing | Source only; real RPC/device qualification blocked/unrun |
| Mainnet spending | Disabled; not performed |
| Node agent macOS x86_64 | Binary built; library tests/vet pass |
| Node agent Linux x86_64/ARM64 | NOT_RUN; no Linux runtime qualification |
| Loopback TLS topology/real bandwidth receiver | Source only; live network tests NOT_RUN |
| Rootless container hosting | BLOCKED; runtime socket denied and isolation unqualified |
| Encrypted mesh tunnels | BLOCKED; no adapter/handshake qualified |
| Local gnark cubic proof | BLOCKED; dependency DNS unavailable, no proof emitted |
| External prover/provider/payout/SKR stake | BLOCKED; current interfaces/authorization/evidence incomplete |
| Website static files | Generated local pages/links/JS checks; responsive browser/a11y NOT_RUN |
| Release signing/store distribution | BLOCKED; no credentials/rights/device/release qualification |
