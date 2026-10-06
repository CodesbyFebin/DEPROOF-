package com.example.data

import androidx.room.withTransaction
import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant
import java.util.Base64
import java.util.UUID

// Portable public records and private evidence, never wallet/node keys or active permissions.
// Restore is additive: conflicts refuse rather than overwrite user work.
class BackupRepository(private val db: DeproofDatabase, private val filesDir: File) {
    companion object { private val mutex=Mutex(); const val MAX_BYTES=16*1024*1024; private const val MAX_ROWS=10000 }
    private val dao=db.records()
    private fun id(s: String): String {ensure(UUID.fromString(s).toString()==s,"BACKUP_BAD_ID");return s}
    private fun time(s: String): String {Instant.parse(s);return s}
    private fun fields(n: JsonNode, names: Set<String>) {ensure(n.isObject && n.fieldNames().asSequence().toSet()==names,"BACKUP_BAD_RECORD")}
    private fun text(n: JsonNode,key: String,max: Int=1024*1024): String {ensure(n[key]?.isTextual==true,"BACKUP_BAD_RECORD");return n[key].asText().also {ensure(it.length<=max,"BACKUP_BAD_RECORD")}}
    private fun rows(n: JsonNode,key: String): List<JsonNode> {val a=n[key];ensure(a?.isArray==true && a.size()<=MAX_ROWS,"BACKUP_BAD_RECORD");return a.toList()}
    private data class Payload(val tasks: List<Task>,val attachments: List<Attachment>,val events: List<Event>,val observations: List<Observation>,val drafts: List<Draft>,val contributions: List<Contribution>,val files: Map<String,ByteArray>)
    private fun decode(bytes: ByteArray): Payload {
        ensure(bytes.size<=MAX_BYTES,"BACKUP_TOO_LARGE");val n=Json.mapper.readTree(bytes) ?: throw Failure("BACKUP_BAD_RECORD")
        fields(n,setOf("schema","tasks","attachments","events","observations","drafts","contributions","files"));ensure(text(n,"schema")=="deproof-portable-records-v1","BACKUP_VERSION_UNSUPPORTED")
        val tasks=rows(n,"tasks").map {t -> fields(t,setOf("id","title","note","checklist","createdAt"));val list=Json.parse(text(t,"checklist"));ensure(list.isArray && list.size()<=100,"BACKUP_BAD_TASK");list.forEach {c -> fields(c,setOf("text","done"));text(c,"text",2000);ensure(c["done"].isBoolean,"BACKUP_BAD_TASK")};Task(id(text(t,"id")),text(t,"title",200).also {ensure(it.isNotBlank(),"BACKUP_BAD_TASK")},text(t,"note"),text(t,"checklist"),time(text(t,"createdAt"))) }
        val taskIds=tasks.map {it.id}.toSet();ensure(taskIds.size==tasks.size,"BACKUP_DUPLICATE_ID")
        val attachments=rows(n,"attachments").map {a -> fields(a,setOf("id","taskId","digest","mime","size","provenance","createdAt"));val task=if(a["taskId"].isNull)null else id(text(a,"taskId"));ensure(task==null || task in taskIds,"BACKUP_BAD_ASSOCIATION");Attachment(id(text(a,"id")),task,text(a,"digest",64).also {ensure(it.matches(Regex("[0-9a-f]{64}")),"BACKUP_BAD_DIGEST")},text(a,"mime",255).also {ensure(it.isNotBlank(),"BACKUP_BAD_METADATA")},text(a,"size",20).also {ensure(it.matches(Regex("0|[1-9][0-9]*")) && it.toLong()<=MAX_BYTES,"BACKUP_BAD_SIZE")},text(a,"provenance",64),time(text(a,"createdAt"))) }
        val attachmentIds=attachments.map {it.id}.toSet();ensure(attachmentIds.size==attachments.size,"BACKUP_DUPLICATE_ID")
        val events=rows(n,"events").map {e -> fields(e,setOf("id","createdAt","kind","payload"));val raw=text(e,"payload");val receipt=Json.parse(raw);validateReceipt(receipt);val event=Event(id(text(e,"id")),time(text(e,"createdAt")),text(e,"kind",64),raw);ensure(receipt["id"].asText()==event.id && receipt["createdAt"].asText()==event.createdAt && receipt["outcome"].asText()==event.kind,"BACKUP_BAD_RECEIPT");if(!receipt["localSignature"].isNull) {
            val sig=receipt["localSignature"];ensure(sig["domain"]?.asText()=="deproof-evidence-sig-v2" && sig["algorithm"]?.asText()=="SHA256withECDSA","BACKUP_BAD_SIGNATURE")
            val envelope=evidenceEnvelope(Json.canonical(receipt["manifest"]));ensure(Base64.getDecoder().decode(sig["envelopeBase64"].asText()).contentEquals(envelope) && verifyLocalSignature(envelope,Base64.getDecoder().decode(sig["signatureDerBase64"].asText()),Base64.getDecoder().decode(sig["spkiBase64"].asText())),"BACKUP_BAD_SIGNATURE")
        };val task=receipt["taskId"];ensure(task==null || task.isNull || task.asText() in taskIds,"BACKUP_BAD_ASSOCIATION");event }
        val eventIds=events.map {it.id}.toSet();ensure(eventIds.size==events.size,"BACKUP_DUPLICATE_ID")
        val observations=rows(n,"observations").map {o -> fields(o,setOf("id","eventId","observedAt","payload"));val event=id(text(o,"eventId"));ensure(event in eventIds,"BACKUP_BAD_ASSOCIATION");val oid=text(o,"id",100);ensure(oid.matches(Regex("[a-zA-Z0-9-]{1,100}")),"BACKUP_BAD_ID");val raw=text(o,"payload");ensure(Json.parse(raw).isObject,"BACKUP_BAD_OBSERVATION");Observation(oid,event,time(text(o,"observedAt")),raw) }
        ensure(observations.map {it.id}.toSet().size==observations.size,"BACKUP_DUPLICATE_ID")
        val drafts=rows(n,"drafts").map {d -> fields(d,setOf("id","payload","updatedAt"));val key=text(d,"id",100);val raw=text(d,"payload");val safe=when {
            key.startsWith("mapping:") -> {val plan=parseMappingPlan(raw);ensure(key=="mapping:"+plan.taskId && plan.taskId in taskIds,"BACKUP_BAD_ASSOCIATION");raw}
            key.startsWith("reminder:") -> {val reminder=parseReminder(raw);ensure(key=="reminder:"+reminder.id && reminder.taskId in taskIds,"BACKUP_BAD_ASSOCIATION");Json.mapper.writeValueAsString(reminder.copy(state="CANCELLED"))}
            else -> throw Failure("BACKUP_DRAFT_UNSUPPORTED")
        };Draft(key,safe,time(text(d,"updatedAt"))) }
        ensure(drafts.map {it.id}.toSet().size==drafts.size,"BACKUP_DUPLICATE_ID")
        val contributions=rows(n,"contributions").map {c -> fields(c,setOf("id","nodeFingerprint","createdAt","payload"));val row=Contribution(id(text(c,"id")),text(c,"nodeFingerprint",64),time(text(c,"createdAt")),text(c,"payload"));NodeProtocol.verifyRecord(Json.parse(row.payload),row.nodeFingerprint,row.id);row}
        ensure(contributions.map {it.id}.toSet().size==contributions.size,"BACKUP_DUPLICATE_ID")
        val files=n["files"];ensure(files.isObject && files.fieldNames().asSequence().toSet()==attachmentIds,"BACKUP_FILE_SET_MISMATCH")
        var total=0L;val contents=attachments.associate {a -> ensure(files[a.id].isTextual,"BACKUP_BAD_FILE");val raw=Base64.getDecoder().decode(files[a.id].asText());total+=raw.size;ensure(total<=MAX_BYTES && raw.size.toString()==a.size && sha256Hex(raw)==a.digest,"BACKUP_FILE_INTEGRITY");a.id to raw}
        return Payload(tasks,attachments,events,observations,drafts,contributions,contents)
    }
    suspend fun export(passphrase: CharArray): ByteArray=withContext(Dispatchers.IO) {mutex.withLock {
        val plaintext=db.withTransaction {
            val attachments=dao.allAttachments();var total=0L
            val files=attachments.associate {a -> id(a.id);val file=File(filesDir,"evidence/${a.id}");ensure(file.isFile && file.length()<=MAX_BYTES,"BACKUP_FILE_UNAVAILABLE");total+=file.length();ensure(total<=8L*1024*1024,"BACKUP_TOO_LARGE");val raw=file.readBytes();ensure(raw.size.toString()==a.size && sha256Hex(raw)==a.digest,"BACKUP_FILE_INTEGRITY");a.id to Base64.getEncoder().encodeToString(raw)}
            Json.canonical(Json.obj("schema" to "deproof-portable-records-v1","tasks" to dao.tasksSnapshot(),"attachments" to attachments,"events" to dao.eventsSnapshot(),"observations" to dao.allObservations(),"drafts" to dao.draftsSnapshot(),"contributions" to dao.contributionsSnapshot(),"files" to files))
        }
        try {decode(plaintext);encryptBackup(plaintext,passphrase)} finally {plaintext.fill(0);passphrase.fill('\u0000')}
    }}
    suspend fun restore(blob: ByteArray,passphrase: CharArray)=withContext(Dispatchers.IO) {mutex.withLock {
        ensure(blob.size<=MAX_BYTES+64,"BACKUP_TOO_LARGE")
        val plain=try {decryptBackup(blob,passphrase)} finally {passphrase.fill('\u0000')}
        val parsed=try {decode(plain)} finally {plain.fill(0)} // Authenticate/validate every record before touching persistent storage.
        val evidence=File(filesDir,"evidence").apply {ensure(isDirectory || mkdirs(),"BACKUP_STORAGE_UNAVAILABLE")}
        val stage=File(filesDir,"restore-${UUID.randomUUID()}").apply {ensure(mkdir(),"BACKUP_STORAGE_UNAVAILABLE")};val published=mutableListOf<File>()
        try {
            parsed.files.forEach {(id,bytes) -> File(stage,id).outputStream().use {it.write(bytes);it.fd.sync()}}
            db.withTransaction {
                ensure(parsed.tasks.none {t -> dao.tasksSnapshot().any {it.id==t.id}} && parsed.attachments.none {dao.attachment(it.id)!=null} && parsed.events.none {dao.event(it.id)!=null} && parsed.drafts.none {dao.draft(it.id)!=null} && parsed.contributions.none {c -> dao.contributionsSnapshot().any {it.id==c.id}},"BACKUP_ID_CONFLICT")
                // A failed insert rolls back every Room row. Existing files are never replaced.
                parsed.files.keys.forEach {id -> val target=File(evidence,id);ensure(!target.exists(),"BACKUP_FILE_CONFLICT");ensure(File(stage,id).renameTo(target),"BACKUP_STORAGE_UNAVAILABLE");published+=target}
                parsed.tasks.forEach {dao.create(it)};parsed.attachments.forEach {dao.attach(it)};parsed.events.forEach {dao.insert(it)};parsed.observations.forEach {dao.observe(it)};parsed.drafts.forEach {dao.draft(it)};parsed.contributions.forEach {dao.contribution(it)}
            }
        } catch(e:Exception) {published.forEach {it.delete()};throw e}
        finally {stage.deleteRecursively();parsed.files.values.forEach {it.fill(0)}}
    }}
}
