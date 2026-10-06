package com.example.domain

/** Read-only effects. These observations never extend the supported signing policy. */
data class AccountEffects(val status: String,val summary: String,val fundingLamports: String?=null,val spaceBytes: String?=null,val owner: String?=null)
fun explainAccountEffects(ix: Instruction, policy: Policy, token: TokenContext?=null): AccountEffects {
    val verdict=decodeInstruction(ix,policy,token)
    if(verdict.allowed && ix.program==Programs.MEMO) return AccountEffects("KNOWN_MEMO","This memo instruction creates no account and deposits no rent. The transaction fee is separate.")
    if(verdict.allowed && ix.program==Programs.TOKEN) return AccountEffects("KNOWN_TRANSFER_CHECKED","This validated TransferChecked moves tokens between existing initialized accounts. It creates no account; transaction fees are separate.")
    if(ix.program=="11111111111111111111111111111111" && ix.data.size==52 && readU32LE(ix.data,0)==0L && ix.accounts.size==2 && ix.accounts.all {it.signer && it.writable && isPubkey(it.address)} && ix.accounts[0].address!=ix.accounts[1].address) {
        val lamports=readU64LE(ix.data,4).toString();val space=readU64LE(ix.data,12).toString();val owner=Base58.encode(ix.data.copyOfRange(20,52))
        return AccountEffects("READ_ONLY_CREATE_ACCOUNT","CreateAccount funds the new account with $lamports lamports, allocates $space bytes and assigns owner $owner. Funding is separate from the transaction fee. Rent-exemption minimum has not been queried; sufficient funding and execution are not established. This instruction remains unsupported for signing.",lamports,space,owner)
    }
    return AccountEffects("UNKNOWN","Account creation and rent effects are unknown for this unsupported instruction/layout. No zero cost or rent-exemption claim; do not sign.")
}

/** No external explanation adapter is qualified or registered in this build. */
data class ExplanationProviderConfig(val providerId: String)
data class ExplanationResult(val status: String,val reason: String,val authorizationEffect: String="NONE")
fun explainPacket(packet: ByteArray, consent: Boolean, providerConfig: ExplanationProviderConfig?): ExplanationResult {
    if(providerConfig==null) return ExplanationResult("NOT_CONFIGURED","No explanation provider configured; no packet transmitted.")
    if(!consent) return ExplanationResult("GROK_NOT_CALLED","Explicit transmission consent absent; no packet transmitted.")
    if(packet.isEmpty() || packet.size>128*1024) return ExplanationResult("PROVIDER_ERROR","Invalid or oversized packet; no packet transmitted.")
    return ExplanationResult("PROVIDER_ERROR","External adapter is blocked pending official interface, authorization and qualification. No packet transmitted.")
}
