package com.deproof.domain.usecase

import com.deproof.domain.model.Instruction
import com.deproof.domain.model.Verdict
import com.deproof.crypto.InstructionDecoder

class DecodeInstructionUseCase {

    fun decodeInstruction(instruction: Instruction): Verdict {
        return try {
            InstructionDecoder.decodeInstruction(instruction)
        } catch (e: Exception) {
            Verdict.Unknown("Failed to decode instruction: ${e.message}")
        }
    }

    fun decodeInstructions(instructions: List<Instruction>): List<Verdict> {
        return instructions.map { decodeInstruction(it) }
    }

    fun determineFinalVerdict(verdicts: List<Verdict>): Verdict {
        return when {
            verdicts.isEmpty() -> Verdict.Unknown("No instructions to decode")
            verdicts.any { it is Verdict.DoNotSign } -> Verdict.DoNotSign
            verdicts.all { it is Verdict.Payable } -> Verdict.Payable
            else -> Verdict.Unknown("Mixed verdicts in transaction")
        }
    }

    fun isSuspiciousTransaction(instruction: Instruction): Boolean {
        return try {
            val verdict = InstructionDecoder.decodeInstruction(instruction)
            verdict is Verdict.DoNotSign
        } catch (e: Exception) {
            true
        }
    }

    fun validateInstructionStructure(instruction: Instruction): Boolean {
        return try {
            instruction.programId.isNotEmpty() &&
                instruction.accounts.isNotEmpty() &&
                instruction.data.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
