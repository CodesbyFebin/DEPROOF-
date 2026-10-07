# Protocol qualification — 2026-10-06

[Solana Mobile's SKR page](https://solanamobile.com/skr) identifies candidate mint `SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`. That official page was retrieved during implementation. On-chain deployment/owner/decimals observation is NOT_RUN: shell RPC DNS fails. Android's SKR observation validates owner, initialized mint, decimals 6 and each token-account linkage on each query; a failed gate cannot produce a balance. Mainnet signing remains disabled entirely. No staking IDL/account layout has been qualified.

The local codec implements the specified ten-byte TransferChecked layout, checked u64 little-endian values and role/account ownership checks. It does not imply mint deployment, mainnet transaction qualification or token-account creation support. Constants other than the officially sourced mint remain candidate values pending versioned protocol and cluster evidence. SOL stake operations are named and refused; SKR stake is explicitly refused.

[MWA 2.0 specification](https://solana-mobile.github.io/mobile-wallet-adapter/spec/spec.html) and [official Android source](https://github.com/solana-mobile/mobile-wallet-adapter) were consulted. Account bytes are base58 encoded. Signed-byte return is mandatory; missing optional signing capability blocks approval. Upstream main source inspection is not compilation or device evidence for pinned 2.0.7. Wallet source uses signTransactions only and verifies Ed25519 before sendTransaction. Developer identity is local localhost, not a public owned domain.

[RPC getGenesisHash](https://solana.com/docs/rpc/http/getgenesishash) documents cluster identity observation. RPC endpoints are HTTPS candidates. Genesis values in source are qualification candidates; actual responses must match before reads/signing. No network observations, balances or signatures were manufactured. History retrieval requests raw base64 with maxSupportedTransactionVersion=0. v0 without address lookups parses; any lookups/indexes outside static keys refuse with UNRESOLVED_LOOKUP_TABLE. General lookup-table resolution remains incomplete.

[RFC 8785](https://www.rfc-editor.org/rfc/rfc8785) governs canonical manifests. This implementation uses a restricted I-JSON subset: strings, booleans, explicit null, arrays and sorted objects. Numeric values are refused and exact lengths/counters are decimal strings. Kotlin/Python golden vectors test UTF-16 key order, Unicode, newline preservation and digest equality. Legacy signed receipt migration remains incomplete and must preserve original raw bytes when added.

[gnark's pinned official cubic circuit](https://raw.githubusercontent.com/Consensys/gnark/v0.14.0/examples/cubic/cubic.go) motivates the source relation `x^3+x+5=y`. The worker/verifier are separate executables. Dependency access is blocked; there is no generated proof, circuit binary, verification key, verified result or external membership. Local setup, if run later, is educational and not a production trusted ceremony.

## Full RPC genesis identity correction

Read-only public RPC observations on 2026-10-06 match the full genesis constants in the Solana SDK primary source: https://github.com/solana-labs/solana/blob/master/sdk/src/genesis_config.rs. The prior app values were truncated CAIP-style identifiers and incorrectly refused real RPC replies. The app now compares full hashes; wrong clusters and truncated hashes still refuse. Actual observations are recorded in `evidence/qualification/live-rpc.json`. No signing or spending occurred.

Read-only CreateAccount explanation: Solana system interface 3.3.0 documents the funding/new-account signer/writable roles and lamports/space/owner fields:
https://docs.rs/solana-system-interface/3.3.0/solana_system_interface/instruction/enum.SystemInstruction.html
https://docs.rs/solana-system-interface/3.3.0/src/solana_system_interface/instruction.rs.html
Only the exact 52-byte legacy System CreateAccount layout (u32 tag0, u64 lamports, u64 space, 32-byte owner) is displayed. It remains unsupported for signing. Funding does not establish the rent-exemption threshold, successful execution or transaction fee. CreateAccountWithSeed, allow-prefund and other unqualified layouts remain unknown. This read-only display does not expand the transaction policy.
