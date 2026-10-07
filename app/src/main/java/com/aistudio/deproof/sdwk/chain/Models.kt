package com.aistudio.deproof.sdwk.chain

import com.google.gson.annotations.SerializedName

data class AccountSummary(
    val solBalance: Long,
    val skrBalance: Long,
    val slot: Long
)

data class RpcResponse<T>(
    @SerializedName("jsonrpc")
    val jsonRpc: String,
    @SerializedName("result")
    val result: T?,
    @SerializedName("error")
    val error: RpcError?
)

data class RpcError(
    val code: Int,
    val message: String
)

data class TransactionResponse(
    @SerializedName("slot")
    val slot: Long,
    @SerializedName("transaction")
    val transaction: Transaction,
    @SerializedName("meta")
    val meta: Meta?
)

data class Transaction(
    @SerializedName("signatures")
    val signatures: List<String>,
    @SerializedName("message")
    val message: Message
)

data class Message(
    @SerializedName("accountKeys")
    val accountKeys: List<String>,
    @SerializedName("instructions")
    val instructions: List<Instruction>,
    @SerializedName("recentBlockhash")
    val recentBlockhash: String
)

data class Instruction(
    @SerializedName("programIdIndex")
    val programIdIndex: Int,
    @SerializedName("accounts")
    val accounts: List<Int>,
    @SerializedName("data")
    val data: String
)

data class Meta(
    @SerializedName("err")
    val err: Any?,
    @SerializedName("status")
    val status: TransactionStatus?,
    @SerializedName("blockTime")
    val blockTime: Long?
)

data class TransactionStatus(
    @SerializedName("Ok")
    val ok: Any? = null
)

data class SignatureStatusResponse(
    @SerializedName("context")
    val context: Context,
    @SerializedName("value")
    val value: List<SignatureStatus?>
)

data class Context(
    @SerializedName("slot")
    val slot: Long
)

data class SignatureStatus(
    @SerializedName("slot")
    val slot: Long,
    @SerializedName("err")
    val err: Any?,
    @SerializedName("confirmations")
    val confirmations: Long?,
    @SerializedName("status")
    val status: Map<String, Any>?
)

data class ConfirmationStatus(
    val status: String
) {
    companion object {
        fun from(confirmations: Long?, err: Any?): String {
            return when {
                err != null -> "Failed"
                confirmations == null -> "Processed"
                confirmations < 1 -> "Confirmed"
                else -> "Finalized"
            }
        }
    }
}

data class SignaturesResponse(
    @SerializedName("jsonrpc")
    val jsonRpc: String,
    @SerializedName("result")
    val result: List<SignatureInfo>?,
    @SerializedName("error")
    val error: RpcError?
)

data class SignatureInfo(
    @SerializedName("signature")
    val signature: String,
    @SerializedName("slot")
    val slot: Long,
    @SerializedName("err")
    val err: Any?,
    @SerializedName("memo")
    val memo: String?,
    @SerializedName("blockTime")
    val blockTime: Long?
)

data class TokenAccountInfo(
    @SerializedName("mint")
    val mint: String,
    @SerializedName("owner")
    val owner: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("decimals")
    val decimals: Int
)

data class ProgramAccountResponse(
    @SerializedName("pubkey")
    val pubkey: String,
    @SerializedName("account")
    val account: AccountData
)

data class AccountData(
    @SerializedName("lamports")
    val lamports: Long,
    @SerializedName("data")
    val data: List<Any>,
    @SerializedName("owner")
    val owner: String,
    @SerializedName("executable")
    val executable: Boolean,
    @SerializedName("rentEpoch")
    val rentEpoch: Long
)

data class Verdict(
    val title: String,
    val reason: String,
    val isPayable: Boolean,
    val summary: String
)
