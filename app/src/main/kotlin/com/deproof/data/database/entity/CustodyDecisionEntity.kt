package com.deproof.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.deproof.data.database.converters.InstructionListConverter
import com.deproof.domain.model.CustodyDecisionStatus
import com.deproof.domain.model.Instruction

@Entity(tableName = "custody_decisions")
@TypeConverters(InstructionListConverter::class)
data class CustodyDecisionEntity(
    @PrimaryKey
    val id: String,
    val transactionId: String,
    val payer: String,
    val instructions: List<Instruction>,
    val fee: Long,
    val status: CustodyDecisionStatus,

    // Review phase
    val reviewedMessageHash: String,
    val reviewedAt: Long,

    // Policy validation phase
    val policyValidated: Boolean,
    val policyValidatedAt: Long,

    // MWA approval phase
    val walletApprovalTime: Long,
    val walletApprovalStatus: String,

    // Signature verification phase
    val signatureVerified: Boolean,
    val signatureVerifiedAt: Long,
    val signatureHash: String,

    // Submission phase
    val submittedAt: Long,
    val chainConfirmedAt: Long,
    val finalSignature: String,

    // Error tracking
    val lastError: String,
    val createdAt: Long
) {

    companion object {
        fun fromDomain(domain: com.deproof.domain.model.CustodyDecision): CustodyDecisionEntity {
            return CustodyDecisionEntity(
                id = domain.id,
                transactionId = domain.transactionId,
                payer = domain.payer,
                instructions = domain.instructions,
                fee = domain.fee,
                status = domain.status,
                reviewedMessageHash = domain.reviewedMessageHash,
                reviewedAt = domain.reviewedAt,
                policyValidated = domain.policyValidated,
                policyValidatedAt = domain.policyValidatedAt,
                walletApprovalTime = domain.walletApprovalTime,
                walletApprovalStatus = domain.walletApprovalStatus,
                signatureVerified = domain.signatureVerified,
                signatureVerifiedAt = domain.signatureVerifiedAt,
                signatureHash = domain.signatureHash,
                submittedAt = domain.submittedAt,
                chainConfirmedAt = domain.chainConfirmedAt,
                finalSignature = domain.finalSignature,
                lastError = domain.lastError,
                createdAt = domain.createdAt
            )
        }
    }

    fun toDomain(): com.deproof.domain.model.CustodyDecision {
        return com.deproof.domain.model.CustodyDecision(
            id = id,
            transactionId = transactionId,
            payer = payer,
            instructions = instructions,
            fee = fee,
            status = status,
            reviewedMessageHash = reviewedMessageHash,
            reviewedAt = reviewedAt,
            policyValidated = policyValidated,
            policyValidatedAt = policyValidatedAt,
            walletApprovalTime = walletApprovalTime,
            walletApprovalStatus = walletApprovalStatus,
            signatureVerified = signatureVerified,
            signatureVerifiedAt = signatureVerifiedAt,
            signatureHash = signatureHash,
            submittedAt = submittedAt,
            chainConfirmedAt = chainConfirmedAt,
            finalSignature = finalSignature,
            lastError = lastError,
            createdAt = createdAt
        )
    }
}
