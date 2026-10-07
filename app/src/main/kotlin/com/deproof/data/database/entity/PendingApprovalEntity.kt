package com.deproof.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deproof.data.PendingApproval
import com.deproof.domain.model.SignatureStatus

@Entity(tableName = "pending_approvals")
data class PendingApprovalEntity(
    @PrimaryKey
    val transactionId: String,
    val createdAt: Long,
    val reviewedMessageHash: String,
    val expectedSignatureStatus: SignatureStatus,
    val walletApprovedAt: Long = 0,
    val signatureRetrievedAt: Long = 0,
    val isReconciled: Boolean = false,
    val reconciliationStatus: String = "" // SIGNED, ABANDONED, ERROR, PENDING
) {

    companion object {
        fun fromDomain(domain: PendingApproval): PendingApprovalEntity {
            return PendingApprovalEntity(
                transactionId = domain.transactionId,
                createdAt = domain.createdAt,
                reviewedMessageHash = domain.reviewedMessageHash,
                expectedSignatureStatus = domain.expectedSignatureStatus
            )
        }
    }

    fun toDomain(): PendingApproval {
        return PendingApproval(
            transactionId = transactionId,
            createdAt = createdAt,
            reviewedMessageHash = reviewedMessageHash,
            expectedSignatureStatus = expectedSignatureStatus
        )
    }
}
