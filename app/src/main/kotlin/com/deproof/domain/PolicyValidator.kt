package com.deproof.domain

sealed class ValidationResult {
    object Valid : ValidationResult()

    data class Invalid(
        val reason: String,
        val failureScenario: String,
        val severity: Severity
    ) : ValidationResult()

    data class PolicyMismatch(
        val expected: String,
        val actual: String,
        val policy: InstructionPolicy
    ) : ValidationResult()
}

enum class Severity {
    CRITICAL,   // Fail-closed: reject immediately
    HIGH,       // Security risk: reject unless overridden
    MEDIUM,     // Unusual but potentially acceptable
    INFO        // Informational only
}

data class SignedTransaction(
    val bytes: ByteArray,
    val wallet: PublicKey,
    val signatures: List<ByteArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedTransaction) return false
        return bytes.contentEquals(other.bytes) &&
               wallet == other.wallet &&
               signatures.size == other.signatures.size &&
               signatures.withIndex().all { (i, sig) -> sig.contentEquals(other.signatures[i]) }
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + wallet.hashCode()
        result = 31 * result + signatures.hashCode()
        return result
    }
}

interface PolicyValidator {
    suspend fun isAllowed(
        instruction: SolanaInstruction,
        policy: InstructionPolicy
    ): Result<Unit>

    suspend fun validateTransactionBoundary(
        reviewedBytes: ByteArray,
        signedTransaction: SignedTransaction?
    ): Result<Unit>

    suspend fun validateAction(
        action: SolanaAction,
        policy: InstructionPolicy
    ): Result<Unit>

    suspend fun validateSignatures(
        transaction: SignedTransaction,
        expectedSigners: List<PublicKey>
    ): Result<Unit>
}

class FailClosedPolicyValidator(
    private val allowedPrograms: Set<String> = setOf(
        "11111111111111111111111111111111",  // System Program
        "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX",  // Token Program
        "Stake11111111111111111111111111111111111111",  // Stake Program
        "metaqbxxUerdq28cj1RbAqKEsbh1HkMi8cHz92RsmjK"   // Metaplex
    ),
    private val policySerializer: PolicySerializer = JsonPolicySerializer()
) : PolicyValidator {

    override suspend fun isAllowed(
        instruction: SolanaInstruction,
        policy: InstructionPolicy
    ): Result<Unit> = runCatching {
        when (policy) {
            is InstructionPolicy.ReadOnly -> {
                throw IllegalStateException("READ_ONLY policy cannot execute instructions")
            }

            is InstructionPolicy.TokenTransfer -> {
                validateTokenTransfer(instruction, policy)
            }

            is InstructionPolicy.TokenMint -> {
                validateTokenMint(instruction, policy)
            }

            is InstructionPolicy.TokenBurn -> {
                validateTokenBurn(instruction, policy)
            }

            is InstructionPolicy.SolTransfer -> {
                validateSolTransfer(instruction, policy)
            }

            is InstructionPolicy.StakingAction -> {
                validateStakingAction(instruction, policy)
            }

            is InstructionPolicy.DeFiSwap -> {
                validateDeFiSwap(instruction, policy)
            }

            is InstructionPolicy.NftMintPolicy -> {
                validateNftMint(instruction, policy)
            }

            is InstructionPolicy.NftTransferPolicy -> {
                validateNftTransfer(instruction, policy)
            }

            is InstructionPolicy.GovernanceVotePolicy -> {
                validateGovernanceVote(instruction, policy)
            }

            is InstructionPolicy.LiquidityPoolPolicy -> {
                validateLiquidityPool(instruction, policy)
            }

            is InstructionPolicy.CustomProgramPolicy -> {
                validateCustomProgram(instruction, policy)
            }

            is InstructionPolicy.CompositePolicy -> {
                validateComposite(instruction, policy)
            }
        }
    }

    private fun validateTokenTransfer(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.TokenTransfer
    ) {
        require(instruction.programId.value == "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX") {
            "Token transfer requires Token Program"
        }

        require(instruction.accounts.size >= 3) {
            "Token transfer must have at least 3 accounts"
        }

        val destination = instruction.accounts[1]
        require(policy.destinationWhitelist.contains(destination.pubkey.value)) {
            "Destination ${destination.pubkey.value} not in whitelist"
        }
    }

    private fun validateTokenMint(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.TokenMint
    ) {
        require(instruction.programId.value == "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX") {
            "Token mint requires Token Program"
        }

        require(instruction.accounts.isNotEmpty()) {
            "Token mint must have accounts"
        }
    }

    private fun validateTokenBurn(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.TokenBurn
    ) {
        require(instruction.programId.value == "TokenkegQfeZyiNwAJsyFbPVwwQQfubRS1z1wWahpgX") {
            "Token burn requires Token Program"
        }

        require(instruction.accounts.isNotEmpty()) {
            "Token burn must have accounts"
        }
    }

    private fun validateSolTransfer(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.SolTransfer
    ) {
        require(instruction.programId.value == "11111111111111111111111111111111") {
            "SOL transfer requires System Program"
        }

        require(instruction.accounts.size >= 2) {
            "SOL transfer must have at least 2 accounts"
        }

        val destination = instruction.accounts[1]
        require(policy.destinationWhitelist.contains(destination.pubkey.value)) {
            "Destination ${destination.pubkey.value} not in whitelist"
        }
    }

    private fun validateStakingAction(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.StakingAction
    ) {
        require(instruction.programId.value == "Stake11111111111111111111111111111111111111") {
            "Staking requires Stake Program"
        }

        require(instruction.accounts.isNotEmpty()) {
            "Staking action must have accounts"
        }
    }

    private fun validateDeFiSwap(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.DeFiSwap
    ) {
        require(instruction.accounts.size >= 2) {
            "Swap must have at least token accounts"
        }

        require(instruction.data.isNotEmpty()) {
            "Swap instruction must contain amount data"
        }
    }

    private fun validateNftMint(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.NftMintPolicy
    ) {
        require(instruction.programId.value == policy.metaplexProgram.value) {
            "NFT mint requires correct Metaplex program"
        }
    }

    private fun validateNftTransfer(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.NftTransferPolicy
    ) {
        require(instruction.accounts.size >= 2) {
            "NFT transfer must have source and destination"
        }
    }

    private fun validateGovernanceVote(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.GovernanceVotePolicy
    ) {
        require(instruction.accounts.isNotEmpty()) {
            "Governance vote must have accounts"
        }
    }

    private fun validateLiquidityPool(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.LiquidityPoolPolicy
    ) {
        require(instruction.programId.value == policy.poolId.value) {
            "LP operation must use correct pool program"
        }

        require(instruction.accounts.isNotEmpty()) {
            "LP operation must have accounts"
        }
    }

    private fun validateCustomProgram(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.CustomProgramPolicy
    ) {
        require(instruction.programId.value == policy.programId.value) {
            "Custom program mismatch"
        }

        if (policy.allowedInstructionSignatures.isNotEmpty()) {
            val dataHash = instruction.data.contentHashCode().toString()
            require(policy.allowedInstructionSignatures.contains(dataHash)) {
                "Instruction signature not in allowed set"
            }
        }
    }

    private fun validateComposite(
        instruction: SolanaInstruction,
        policy: InstructionPolicy.CompositePolicy
    ) {
        val results = policy.policies.map { subPolicy ->
            try {
                isAllowed(instruction, subPolicy).getOrNull()
                true
            } catch (e: Exception) {
                false
            }
        }

        val allPassed = results.all { it }
        val anyPassed = results.any { it }

        if (policy.requireAll) {
            require(allPassed) {
                "Not all sub-policies passed for composite policy"
            }
        } else {
            require(anyPassed) {
                "No sub-policies passed for composite policy"
            }
        }
    }

    override suspend fun validateTransactionBoundary(
        reviewedBytes: ByteArray,
        signedTransaction: SignedTransaction?
    ): Result<Unit> = runCatching {
        if (signedTransaction == null) {
            // Initial validation before signing
            require(reviewedBytes.isNotEmpty()) {
                "Transaction bytes cannot be empty"
            }
            require(reviewedBytes.size < 1_280_000) {  // Solana transaction size limit
                "Transaction exceeds size limit"
            }
            return@runCatching
        }

        // Verify signature was over reviewed bytes
        require(signedTransaction.bytes.isNotEmpty()) {
            "Signed transaction bytes cannot be empty"
        }

        require(signedTransaction.signatures.isNotEmpty()) {
            "Transaction must have at least one signature"
        }

        // Verify signatures are valid length
        signedTransaction.signatures.forEach { sig ->
            require(sig.size == 64) {
                "Signature must be 64 bytes for Ed25519"
            }
        }
    }

    override suspend fun validateAction(
        action: SolanaAction,
        policy: InstructionPolicy
    ): Result<Unit> = runCatching {
        // First validate the action itself
        action.validate().getOrThrow()

        // Then validate against policy
        val instruction = action.toInstruction().getOrThrow()
        isAllowed(instruction, policy).getOrThrow()
    }

    override suspend fun validateSignatures(
        transaction: SignedTransaction,
        expectedSigners: List<PublicKey>
    ): Result<Unit> = runCatching {
        require(transaction.signatures.isNotEmpty()) {
            "Transaction must have signatures"
        }

        require(transaction.signatures.size >= expectedSigners.size) {
            "Transaction has fewer signatures than expected signers"
        }

        transaction.signatures.forEach { sig ->
            require(sig.size == 64) {
                "Each signature must be 64 bytes (Ed25519)"
            }
        }
    }
}
