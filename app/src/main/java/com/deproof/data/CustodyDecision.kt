package com.deproof.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custody_decisions",
    indices = [
        Index("transactionHash"),
        Index("decisionStatus"),
        Index("timestamp"),
        Index("walletAddress")
    ]
)
data class CustodyDecision(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val walletAddress: String,
    val transactionHash: String?,

    val decisionStatus: DecisionStatus,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val instructionBytes: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val reviewHash: ByteArray,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val walletSignature: ByteArray?,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val nodeAgentSignature: ByteArray?,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val policyUsed: ByteArray,

    val policyName: String,
    val policyVersion: Int,

    val timestamp: Long,
    val chainObservationTime: Long?,

    val slot: Long?,
    val blockTime: Long?,
    val confirmed: Boolean,

    val rpcEndpoint: String,
    val commitment: String,

    val errorMessage: String?,

    val nodeAgentId: String?,
    val sessionId: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustodyDecision) return false
        return id == other.id &&
               walletAddress == other.walletAddress &&
               transactionHash == other.transactionHash &&
               decisionStatus == other.decisionStatus &&
               instructionBytes.contentEquals(other.instructionBytes) &&
               reviewHash.contentEquals(other.reviewHash) &&
               (walletSignature?.contentEquals(other.walletSignature) ?: (other.walletSignature == null)) &&
               (nodeAgentSignature?.contentEquals(other.nodeAgentSignature) ?: (other.nodeAgentSignature == null)) &&
               policyUsed.contentEquals(other.policyUsed) &&
               policyName == other.policyName &&
               policyVersion == other.policyVersion &&
               timestamp == other.timestamp &&
               chainObservationTime == other.chainObservationTime &&
               slot == other.slot &&
               blockTime == other.blockTime &&
               confirmed == other.confirmed &&
               rpcEndpoint == other.rpcEndpoint &&
               commitment == other.commitment &&
               errorMessage == other.errorMessage &&
               nodeAgentId == other.nodeAgentId &&
               sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + walletAddress.hashCode()
        result = 31 * result + (transactionHash?.hashCode() ?: 0)
        result = 31 * result + decisionStatus.hashCode()
        result = 31 * result + instructionBytes.contentHashCode()
        result = 31 * result + reviewHash.contentHashCode()
        result = 31 * result + (walletSignature?.contentHashCode() ?: 0)
        result = 31 * result + (nodeAgentSignature?.contentHashCode() ?: 0)
        result = 31 * result + policyUsed.contentHashCode()
        result = 31 * result + policyName.hashCode()
        result = 31 * result + policyVersion
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (chainObservationTime?.hashCode() ?: 0)
        result = 31 * result + (slot?.hashCode() ?: 0)
        result = 31 * result + (blockTime?.hashCode() ?: 0)
        result = 31 * result + confirmed.hashCode()
        result = 31 * result + rpcEndpoint.hashCode()
        result = 31 * result + commitment.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (nodeAgentId?.hashCode() ?: 0)
        result = 31 * result + (sessionId?.hashCode() ?: 0)
        return result
    }
}

enum class DecisionStatus {
    INITIATED,                  // Decision started, not yet reviewed
    USER_APPROVED,              // User reviewed and approved via MWA
    WALLET_SIGNED,              // Signature received from wallet
    SUBMISSION_PENDING,         // Submitted to chain, awaiting confirmation
    SUBMISSION_UNKNOWN,         // Submitted but status unknown (crashed before confirmation)
    CONFIRMED,                  // Chain observation confirms success
    REVOKED,                    // Transaction failed or was revoked
    EXPIRED,                    // Policy or approval expired
    REJECTED                    // User or system rejected
}

data class CustodyDecisionBuilder(
    var walletAddress: String = "",
    var instructionBytes: ByteArray = byteArrayOf(),
    var reviewHash: ByteArray = byteArrayOf(),
    var policyUsed: ByteArray = byteArrayOf(),
    var policyName: String = "",
    var policyVersion: Int = 1,
    var rpcEndpoint: String = "",
    var commitment: String = "confirmed"
) {
    fun build(): CustodyDecision {
        require(walletAddress.isNotEmpty()) { "Wallet address required" }
        require(instructionBytes.isNotEmpty()) { "Instruction bytes required" }
        require(reviewHash.isNotEmpty()) { "Review hash required" }
        require(policyUsed.isNotEmpty()) { "Policy required" }

        return CustodyDecision(
            walletAddress = walletAddress,
            transactionHash = null,
            decisionStatus = DecisionStatus.INITIATED,
            instructionBytes = instructionBytes,
            reviewHash = reviewHash,
            walletSignature = null,
            nodeAgentSignature = null,
            policyUsed = policyUsed,
            policyName = policyName,
            policyVersion = policyVersion,
            timestamp = System.currentTimeMillis(),
            chainObservationTime = null,
            slot = null,
            blockTime = null,
            confirmed = false,
            rpcEndpoint = rpcEndpoint,
            commitment = commitment,
            errorMessage = null,
            nodeAgentId = null,
            sessionId = null
        )
    }
}
