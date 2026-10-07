<img width="2172" height="724" alt="DEPROOF contribution ecosystem hero banner" src="https://github.com/user-attachments/assets/70b71066-e128-4b2a-a078-8f9aa9a71bba" />
# DEPROOF-
<div align="center">

# DEPROOF

### The work. The proof. The payment.

**A contribution workspace for Solana Mobile × SKR.**

Run supported jobs on infrastructure you own.
Verify the result. Review the payment. Keep the receipt.

![Deproof contribution and SKR payment concept](assets/deproof-pitch-banner.png)

[Get started](docs/setup.md) ·
[Qualification](evidence/qualification/qualification-report.md) ·
[Threat model](docs/threat-model.md) ·
[Source](https://github.com/CodesbyFebin/DEPROOF-)

**Built by CodesbyFebin**

</div>

---<img width="1672" height="941" alt="image" src="https://github.com/user-attachments/assets/e4b5ed37-beae-481e-828f-b96922b927ae" />


## Why Deproof?

DePIN participation creates scattered records: node logs, job results,
bandwidth measurements, wallet transactions and provider dashboards.

A payment alone does not explain the contribution.
A signed record alone does not prove the work was independently verified.

Deproof connects those records so contributors can inspect:

- **What ran** — the job, runtime and inputs.
- **What was verified** — the result and verification method.
- **What was approved** — the exact transaction message.
- **What was paid** — observed settlement, when available.
- **What can be exported** — portable contribution evidence.

## One contribution journey

| Stage | Action | Record |
|---|---|---|
| **01 · Contribute** | Run a supported job on an owned node | Job identity, input digest and operation history |
| **02 · Verify** | Check the result using a separate verifier | Verification outcome and stated assumptions |
| **03 · Review & pay** | Inspect and authorize a supported SKR payment | Exact-message review and transaction reference |
| **04 · Keep the receipt** | Link evidence to observed settlement | Exportable contribution receipt |

**The SKR payment journey is proposed functionality until its end-to-end
implementation and qualification are recorded.**

## Your infrastructure, your controls

Deproof’s Android workspace connects to a user-owned node agent.

Compatible workloads run on the paired host. The phone provides the
control and evidence interface; it does not automatically run every
DePIN project.

### Node workspace

- Scoped pairing and authorization
- Supported workload discovery
- Hardware and capability observations
- Operation controls and health information
- Resource limits and revocation
- Contribution history and receipt export

### Project adapters

Each adapter defines its own:

- Supported operating systems and hardware
- Installation artifacts and pinned versions
- Required permissions and credentials
- Available runtime operations
- Measurement sources and verification rules
- Qualification status

**A catalog entry is not proof of compatibility.**
External project support must be qualified individually.

## Proof of contribution, with clear boundaries

Deproof distinguishes four evidence levels:

| Evidence level | What it establishes |
|---|---|
| **Local observation** | A particular measurement source reported a value |
| **Node-signed record** | A node identity signed the recorded data |
| **Independently verified result** | A separate verifier checked a defined result |
| **Provider-acknowledged contribution** | An external provider acknowledged the contribution |

These levels are not interchangeable.

A file hash checks integrity, not physical truth.
A computation proof covers its defined computation and assumptions.
A node signature does not independently establish bandwidth delivery.
A contribution receipt does not automatically establish payment.

## Solana Mobile × SKR

The proposed SKR workflow connects a useful service to a reviewable payment:

**Select a job → execute → verify → review payment → authorize → observe settlement.**

The initial design uses explicit requester-approved payment.
It does not imply escrow, automatic rewards or guaranteed earnings.

Implementation requirements include:

- Official mint and token-program validation
- Exact integer amount handling
- Network and recipient checks
- Complete serialized-message review binding
- Validation of wallet-returned signed transactions
- Separate verification and payment states
- Duplicate-payment protection and recovery
- Unavailable observations shown explicitly

Wallet private keys remain in the wallet.

**SKR is separate from DEPR.**
DEPR is Deproof’s ecosystem brand; `$DEPR` is a brand concept only.
This project does not define a DEPR token issuance, supply or launch.

## Android experience

| Surface | Purpose |
|---|---|
| **Now** | Account context, recent activity and contribution controls |
| **Review** | Inspect transaction details and enforce signing gates |
| **Receipts** | Inspect records, observations and exports |
| **Nodes** | Pair and monitor owned infrastructure |
| **Projects** | Explore supported adapters and compatibility |
| **Proof jobs** | Discover jobs, match capabilities and inspect verification |

## Implementation status

Deproof is under active development.

The repository includes Android, node-agent, proof and portable-verification
source, documentation and qualification tooling.

Historical local qualification reports cover their recorded source and
environment. They do not automatically qualify another branch, operating
system, device or external provider.

**Production readiness is not claimed.**

Consult:

- [Qualification report](evidence/qualification/qualification-report.md)
- [Implementation status](evidence/qualification/implementation-status.json)
- [Remaining work](docs/remaining-work.md)
- [Privacy](docs/privacy.md)
- [Threat model](docs/threat-model.md)

Planned integrations are not partnerships.
Mock fixtures must remain separate from production behavior.

## Build locally

Follow the [setup guide](docs/setup.md) for the project’s pinned toolchain.

```bash
git clone https://github.com/CodesbyFebin/DEPROOF-.git
cd DEPROOF-

bash scripts/setup-android.sh

bash ./gradlew :app:testDebugUnitTest --stacktrace
bash ./gradlew :app:lintDebug --stacktrace
bash ./gradlew :app:assembleDebug --stacktrace
```

Run broader qualification after installing its prerequisites:

```bash
bash scripts/qualify.sh
```

Record actual outcomes. These commands are not a promise that every
revision or environment passes.

## Architecture

| Component | Responsibility |
|---|---|
| **Android app** | Wallet interaction, node controls, review and receipts |
| **Node agent** | Scoped operations and observed runtime state |
| **Project adapters** | Project-specific compatibility and interfaces |
| **Prover / verifier** | Supported computation execution and verification |
| **Evidence tools** | Manifest validation and portable inspection |
| **Website / docs** | Setup, project scope and qualification boundaries |

## Roadmap

1. **Reliable mobile core** — exact-message review and persistent receipts.
2. **Evidence workflows** — tasks, import, backup and recovery.
3. **Owned-node operations** — supported hosting, bandwidth and proof jobs.
4. **Multi-project adapters** — independently qualified workloads.
5. **SKR contribution payments** — explicit authorization and settlement linkage.
6. **Broader qualification** — devices, providers, accessibility and release.

The authoritative blueprint contains **336 overlapping registry records**:
120 features, 100 checks, 30 ecosystem requirements and 86 function contracts.

These describe scope—not a count of completed capabilities.

## Hackathon demonstration

Our target demonstration is one complete contribution journey:

1. Pair an owned node from Android.
2. Select and execute a supported bounded job.
3. Independently verify the result.
4. Inspect the contribution evidence.
5. Demonstrate the qualified payment flow, if available.
6. Export the receipt and verify its supported contents separately.

Show failures too: changed messages, tampered evidence, revoked permissions
and unavailable providers.

## Contribute

We welcome reproducible bug reports, contract-specific tests,
device qualification, accessibility improvements and adapter reviews.

Include your source revision, environment, commands and observed results.
Do not replace missing integration with fabricated signatures,
balances, measurements or rewards.

## License

See [LICENSE](LICENSE) and [licensing notes](docs/licensing.md)
for source licensing and imported-asset boundaries.

---

<div align="center">

### Inspect what you did. Verify what you were paid.

**DEPROOF / DEPR — Built by CodesbyFebin**

[Explore the repository](https://github.com/CodesbyFebin/DEPROOF-) ·
[Meet the builder](https://github.com/CodesbyFebin)

</div>

## Local implementation

Deproof / DEPR — Built by CodesbyFebin. $DEPR is a brand concept only; no token issuance.

Native Android source, domain tests, schemas, portable verifier, user-owned Go node agent and local documentation website are now present. Android node controls use authenticated real API operations, durable operation recovery and signed contribution receipts. Local qualification covers Android assembly/JVM tests/lint, a fixed isolated hosting profile on the tested Docker VM, educational Groth16 proofs, read-only public RPC observations and automated browser accessibility checks. The project remains incomplete: physical-wallet/hardware, broader host support, production proof setup, external providers, manual accessibility and release gates remain unresolved. Consult the qualification report for exact outcomes and uncovered requirements.

```sh
./scripts/setup-android.sh
./gradlew assembleDebug testDebugUnitTest lintDebug
./scripts/qualify.sh
```

See [setup](docs/setup.md), [qualification report](evidence/qualification/qualification-report.md), [machine-readable status](evidence/qualification/implementation-status.json), [threat model](docs/threat-model.md) and [privacy](docs/privacy.md). Proposed MIT applies to new source; imported asset rights remain unresolved in [licensing](docs/licensing.md). Original README assets above are preserved.
