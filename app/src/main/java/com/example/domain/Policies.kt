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
