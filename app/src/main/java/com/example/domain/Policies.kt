package com.example.domain

// C095: one memo rebuild after a blockhash expires. A rebuilt draft is a new
// Review and must be reviewed and authorized again; the prior approval is not reused.
const val MAX_MEMO_REBUILDS = 1
fun memoRebuildAllowed(rebuildsUsed: Int): Boolean = rebuildsUsed < MAX_MEMO_REBUILDS

// FN062: evidence signing key qualification. Only hardware-backed levels pass.
// The observed level comes from Android Keystore KeyInfo; the observation is device-only.
fun requireQualifiedKeyLevel(level: String) {
    ensure(level != "SOFTWARE", "SOFTWARE_KEY")
    ensure(level == "STRONGBOX" || level == "TRUSTED_ENVIRONMENT", "UNQUALIFIED_KEY")
}

// FN048: a new draft for an expired blockhash. The result is a different message,
// so an approval of the previous message cannot be applied to it (see canSign).
// Only the devnet memo policy can be rebuilt; mainnet and historical reviews cannot.
fun rebuildExpiredTransaction(previous: Review, latestBlockhash: String, unixMillis: Long): ByteArray {
    ensure(!previous.context.historical, "HISTORICAL_READ_ONLY")
    ensure(previous.context.policy == Policy.DEVNET_MEMO_V1 && previous.context.cluster == "devnet", "REBUILD_UNSUPPORTED_POLICY")
    ensure(latestBlockhash != previous.tx.blockhash, "BLOCKHASH_UNCHANGED")
    return buildMemo(previous.context.account, latestBlockhash, unixMillis)
}
