<img width="2172" height="724" alt="DEPROOF contribution ecosystem hero banner" src="https://github.com/user-attachments/assets/70b71066-e128-4b2a-a078-8f9aa9a71bba" />
# DEPROOF-

<img width="793" height="1983" alt="image" src="https://github.com/user-attachments/assets/e62dce56-f6ca-450c-99ff-38abfe2376d2" />


<img width="793" height="1983" alt="image" src="https://github.com/user-attachments/assets/7bc0111a-b491-445b-a3b9-04153e375c55" />


## Local implementation

Deproof / DEPR — Built by CodesbyFebin. $DEPR is a brand concept only; no token issuance.

Native Android source, domain tests, schemas, portable verifier, user-owned Go node agent and local documentation website are now present. Android node controls use authenticated real API operations, durable operation recovery and signed contribution receipts. Local qualification covers Android assembly/JVM tests/lint, a fixed isolated hosting profile on the tested Docker VM, educational Groth16 proofs, read-only public RPC observations and automated browser accessibility checks. The project remains incomplete: physical-wallet/hardware, broader host support, production proof setup, external providers, manual accessibility and release gates remain unresolved. Consult the qualification report for exact outcomes and uncovered requirements.

```sh
./scripts/setup-android.sh
./gradlew assembleDebug testDebugUnitTest lintDebug
./scripts/qualify.sh
```

See [setup](docs/setup.md), [qualification report](evidence/qualification/qualification-report.md), [machine-readable status](evidence/qualification/implementation-status.json), [threat model](docs/threat-model.md) and [privacy](docs/privacy.md). Proposed MIT applies to new source; imported asset rights remain unresolved in [licensing](docs/licensing.md). Original README assets above are preserved.
