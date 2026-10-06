package com.example.domain
import java.math.BigInteger
import java.time.Instant
import java.time.Duration

// Presentation only: the all-instruction policy always evaluates original order.
fun rankInstruction(ix: Instruction): Int = if(ix.program==Programs.TOKEN && ix.data.firstOrNull()==12.toByte()) 0 else 1
fun statusLabel(value: String): String = when(value) {
    "REJECTED" -> "Rejected locally"
    "OBSERVED" -> "Observed record"
    "LOCAL_EVIDENCE_SIGNED" -> "Evidence signed locally"
    "WALLET_SIGNED" -> "Wallet signed — original event was not submitted"
    "SUBMITTED" -> "Submitted by Deproof — inspect chain observation"
    "SUBMISSION_UNKNOWN" -> "Submission status unknown"
    "UNAVAILABLE" -> "Observation unavailable; retain last known status"
    else -> value.lowercase().replace('_',' ')
}
data class IllustrativeTimer(val startUtc: String, val durationSeconds: String)
fun cooldownRemaining(timer: IllustrativeTimer, clock: Instant): Duration {
    val start=Instant.parse(timer.startUtc); val seconds=timer.durationSeconds.toBigInteger()
    ensure(seconds>BigInteger.ZERO && seconds<=BigInteger.valueOf(365L*24*60*60),"BAD_TIMER_DURATION")
    ensure(!start.isAfter(clock),"FUTURE_TIMER_START")
    val end=start.plusSeconds(seconds.toLong()); return if(end.isAfter(clock)) Duration.between(clock,end) else Duration.ZERO
}
fun selectCluster(action: String): String = when(action) {
    "observeMainnet","historicalMainnet" -> "mainnet-beta"
    "devnetMemo" -> "devnet"
    else -> throw Failure("UNQUALIFIED_ACTION_CLUSTER")
}
data class DevnetQualification(val account: String, val genesis: String, val signature: String, val confirmation: String, val strictCapability: Boolean, val deviceEvidenceReference: String)
data class SkrTransferParameters(val authority: String,val source: String,val destination: String,val blockhash: String,val amount: BigInteger,val context: TokenContext,val qualification: DevnetQualification)
fun buildSkrTransfer(p: SkrTransferParameters): ByteArray {
    val q=p.qualification;ensure(q.account==p.authority && q.genesis=="EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG" && q.confirmation in listOf("CONFIRMED","FINALIZED") && q.strictCapability && q.deviceEvidenceReference.isNotBlank(),"DEVNET_GATE_REQUIRED")
    ensure(Base58.decode(q.signature).size==64,"DEVNET_GATE_REQUIRED")
    val roles=listOf(Role(p.source,false,true),Role(Programs.SKR,false,false),Role(p.destination,false,true),Role(p.authority,true,true))
    val data=transferCheckedData(p.amount,6);val ix=Instruction(Programs.TOKEN,roles,data)
    ensure(decodeInstruction(ix,Policy.SKR_TRANSFER_V1,p.context).allowed,"TOKEN_ACCOUNT_MISMATCH")
    val keys=listOf(p.authority,p.source,p.destination,Programs.SKR,Programs.TOKEN);ensure(keys.distinct().size==5,"DUPLICATE_ACCOUNT_KEY")
    // Draft only. There is deliberately no mainnet submit path in this application.
    return byteArrayOf(1)+ByteArray(64)+byteArrayOf(1,0,2,5)+keys.fold(byteArrayOf()){a,k->a+Base58.pubkey(k)}+Base58.pubkey(p.blockhash)+byteArrayOf(1,4,4,1,3,2,0,10)+data
}
