package com.deproof.domain.model

import java.util.*

// ========== Custody Decision Timeline ==========

enum class CustodyDecisionStatus {
    PENDING_REVIEW,
    POLICY_VALIDATED,
    MWA_APPROVED,
    SIGNATURE_VERIFIED,
    READY_FOR_CHAIN,
    SUBMITTED,
    CONFIRMED,
    FAILED,
    REJECTED
}

// ========== Custody Decision Entity ==========

data class CustodyDecision(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val payer: String,
    val instructions: List<Instruction>,
    val fee: Long,
    val status: CustodyDecisionStatus = CustodyDecisionStatus.PENDING_REVIEW,

    // Review phase
    val reviewedMessageHash: String = "",
    val reviewedAt: Long = 0,

    // Policy validation phase
    val policyValidated: Boolean = false,
    val policyValidatedAt: Long = 0,

    // MWA approval phase
    val walletApprovalTime: Long = 0,
    val walletApprovalStatus: String = "",

    // Signature verification phase
    val signatureVerified: Boolean = false,
    val signatureVerifiedAt: Long = 0,
    val signatureHash: String = "",

    // Submission phase
    val submittedAt: Long = 0,
    val chainConfirmedAt: Long = 0,
    val finalSignature: String = "",

    // Error tracking
    val lastError: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {

    fun isReadyForWallet(): Boolean {
        return status == CustodyDecisionStatus.POLICY_VALIDATED
    }

    fun isReadyForChain(): Boolean {
        return status == CustodyDecisionStatus.READY_FOR_CHAIN
    }

    fun isPolicyValidated(): Boolean {
        return policyValidated && status.ordinal >= CustodyDecisionStatus.POLICY_VALIDATED.ordinal
    }

    fun isSignatureVerified(): Boolean {
        return signatureVerified && status.ordinal >= CustodyDecisionStatus.SIGNATURE_VERIFIED.ordinal
    }

    fun getTimeline(): String {
        return buildString {
            appendLine("Timeline for $transactionId:")
            if (reviewedAt > 0) appendLine("  - Reviewed: ${Date(reviewedAt)}")
            if (policyValidatedAt > 0) appendLine("  - Policy Validated: ${Date(policyValidatedAt)}")
            if (walletApprovalTime > 0) appendLine("  - MWA Approved: ${Date(walletApprovalTime)}")
            if (signatureVerifiedAt > 0) appendLine("  - Signature Verified: ${Date(signatureVerifiedAt)}")
            if (submittedAt > 0) appendLine("  - Submitted: ${Date(submittedAt)}")
            if (chainConfirmedAt > 0) appendLine("  - Confirmed on Chain: ${Date(chainConfirmedAt)}")
            if (lastError.isNotEmpty()) appendLine("  - Error: $lastError")
        }
    }

    fun copy(
        status: CustodyDecisionStatus = this.status,
        policyValidated: Boolean = this.policyValidated,
        policyValidatedAt: Long = this.policyValidatedAt,
        walletApprovalTime: Long = this.walletApprovalTime,
        signatureVerified: Boolean = this.signatureVerified,
        signatureVerifiedAt: Long = this.signatureVerifiedAt,
        signatureHash: String = this.signatureHash,
        lastError: String = this.lastError
    ): CustodyDecision {
        return CustodyDecision(
            id = this.id,
            transactionId = this.transactionId,
            payer = this.payer,
            instructions = this.instructions,
            fee = this.fee,
            status = status,
            reviewedMessageHash = this.reviewedMessageHash,
            reviewedAt = this.reviewedAt,
            policyValidated = policyValidated,
            policyValidatedAt = policyValidatedAt,
            walletApprovalTime = walletApprovalTime,
            walletApprovalStatus = this.walletApprovalStatus,
            signatureVerified = signatureVerified,
            signatureVerifiedAt = signatureVerifiedAt,
            signatureHash = signatureHash,
            submittedAt = this.submittedAt,
            chainConfirmedAt = this.chainConfirmedAt,
            finalSignature = this.finalSignature,
            lastError = lastError,
            createdAt = this.createdAt
        )
    }
}
