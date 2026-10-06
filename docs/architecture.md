# Architecture

A monorepo separates the native Android app, dependency-free Go node agent, separately pinned proof worker, independent Python verifier and static website. Node/prover dependencies do not enter the Android build. No optional service server exists without a concrete credentialed integration need.

Android domain code has no Android imports: base58, u64, strict Solana parser/policies, immutable review hashes, constrained canonical manifests and receipt invariants. Android adapters add MWA, RPC, Room, DataStore, content streams and Keystore. UI projects real state through Now, Review and Receipts. Tasks/evidence are nested under Now. Node/Proof/Bandwidth app routes are not exposed as working controls because API wiring is incomplete.

Room stores immutable event payloads separately from append-only chain/submission observations, tasks and attachments. Durable WALLET_SIGNED intent precedes RPC send. Successful acceptance adds an observation; timeout remains SUBMISSION_UNKNOWN. Automatic crash recovery/reconciliation is unfinished and is a release blocker. Original event export preserves its provenance; bundle export includes separately labeled unsigned local observations.

The Go agent exposes loopback TLS signed commands and scoped pairing. Hosting/proof commands fail closed. Its bandwidth flow is a fixed-endpoint qualification transfer with persisted quota reservation and signed local measurement receipt. Independent counter, Linux isolation and external provider gates remain separate.

Portable verification reconstructs exact JCS envelope bytes, checks P-256/Ed25519 signatures and supplied raw files, and reports missing files/chain/physical truth separately. The website generates status from registries. Concept reference imagery is labeled. Its JSON inspector displays provenance only; it cannot enable app approval.
