package com.deproof.data.database.converters

import androidx.room.TypeConverter
import com.deproof.domain.model.Instruction
import com.deproof.domain.model.CustodyDecisionStatus
import com.deproof.domain.model.SignatureStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
data class SerializableInstruction(
    val programId: String,
    val discriminator: Byte,
    val accounts: List<String>,
    val data: String // Base64 encoded
)

class InstructionListConverter {

    @TypeConverter
    fun fromInstructionList(instructions: List<Instruction>): String {
        val serializable = instructions.map { instr ->
            SerializableInstruction(
                programId = instr.programId,
                discriminator = instr.discriminator,
                accounts = instr.accounts,
                data = android.util.Base64.encodeToString(instr.data, android.util.Base64.NO_WRAP)
            )
        }
        return Json.encodeToString(serializable)
    }

    @TypeConverter
    fun toInstructionList(json: String): List<Instruction> {
        return try {
            val serializable: List<SerializableInstruction> = Json.decodeFromString(json)
            serializable.map { instr ->
                Instruction(
                    programId = instr.programId,
                    discriminator = instr.discriminator,
                    accounts = instr.accounts,
                    data = android.util.Base64.decode(instr.data, android.util.Base64.NO_WRAP)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class CustodyDecisionStatusConverter {

    @TypeConverter
    fun fromCustodyDecisionStatus(status: CustodyDecisionStatus): String {
        return status.name
    }

    @TypeConverter
    fun toCustodyDecisionStatus(value: String): CustodyDecisionStatus {
        return try {
            CustodyDecisionStatus.valueOf(value)
        } catch (e: Exception) {
            CustodyDecisionStatus.PENDING_REVIEW
        }
    }
}

class SignatureStatusConverter {

    @TypeConverter
    fun fromSignatureStatus(status: SignatureStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSignatureStatus(value: String): SignatureStatus {
        return try {
            SignatureStatus.valueOf(value)
        } catch (e: Exception) {
            SignatureStatus.PENDING
        }
    }
}
