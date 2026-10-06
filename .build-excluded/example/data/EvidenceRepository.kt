package com.example.data

import com.example.domain.*
import java.io.File
import java.time.Instant
import java.util.UUID

/** FN052: trusted metadata comes from the private file bytes, never a caller-supplied digest. */
class EvidenceRepository(private val dao: RecordsDao,private val directory: File) {
    fun metadata(id: String,taskId: String?,mime: String,provenance: String,at: String): Attachment {
        ensure(UUID.fromString(id).toString()==id,"BAD_EVIDENCE_ID")
        if(taskId!=null)ensure(UUID.fromString(taskId).toString()==taskId,"BAD_TASK_ID")
        ensure(mime.isNotBlank() && mime.length<=255 && mime.none {it.isISOControl()},"BAD_MIME")
        ensure(provenance in setOf("import","capture"),"BAD_PROVENANCE");Instant.parse(at)
        val file=File(directory,id);ensure(file.isFile && file.canonicalFile.parentFile==directory.canonicalFile,"EVIDENCE_FILE_UNAVAILABLE")
        val hash=file.inputStream().use {sha256File(it)}
        return Attachment(id,taskId,hash.sha256,mime,hash.byteLength,provenance,at)
    }
    suspend fun attachEvidence(expected: Attachment,operationId: String?=null): Attachment {
        val actual=metadata(expected.id,expected.taskId,expected.mime,expected.provenance,expected.createdAt)
        ensure(actual==expected,"EVIDENCE_METADATA_CHANGED")
        if(operationId==null)dao.attach(actual) else {ensure(operationId==actual.id,"IMPORT_OPERATION_MISMATCH");dao.commitImport(operationId,actual)}
        return actual
    }
}
