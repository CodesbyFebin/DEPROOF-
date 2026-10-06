package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.Base64

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class BackupRepositoryTest {
    private val context: android.content.Context get()=ApplicationProvider.getApplicationContext()
    private lateinit var source: DeproofDatabase;private lateinit var target: DeproofDatabase
    private lateinit var sourceFiles: File;private lateinit var targetFiles: File
    private val password="separate portable passphrase"
    @Before fun setup(){source=Room.inMemoryDatabaseBuilder(context,DeproofDatabase::class.java).allowMainThreadQueries().build();target=Room.inMemoryDatabaseBuilder(context,DeproofDatabase::class.java).allowMainThreadQueries().build();sourceFiles=java.nio.file.Files.createTempDirectory("deproof-backup-source").toFile();targetFiles=java.nio.file.Files.createTempDirectory("deproof-backup-target").toFile()}
    @After fun cleanup(){source.close();target.close();sourceFiles.deleteRecursively();targetFiles.deleteRecursively()}
    private suspend fun seed(db: DeproofDatabase,dir: File): Pair<Task,Attachment> {
        val task=db.records().createTask("Retain this task",listOf("photo"));val id=UUID.randomUUID().toString();File(dir,"evidence").mkdir();File(dir,"evidence/$id").writeText("exact private bytes")
        val repo=EvidenceRepository(db.records(),File(dir,"evidence"));val a=repo.metadata(id,task.id,"text/plain","import",Instant.now().toString());repo.attachEvidence(a);return task to a
    }
    private suspend fun blob()=BackupRepository(source,sourceFiles).export(password.toCharArray())
    private suspend fun reject(code: String,bytes: ByteArray,key: String=password){try {BackupRepository(target,targetFiles).restore(bytes,key.toCharArray());fail("Accepted $code")}catch(e:Failure){assertEquals(code,e.code)}}
    private fun changed(blob: ByteArray,edit:(com.fasterxml.jackson.databind.node.ObjectNode)->Unit): ByteArray {
        val plain=decryptBackup(blob,password.toCharArray());val node=Json.parse(plain.toString(Charsets.UTF_8)) as com.fasterxml.jackson.databind.node.ObjectNode;edit(node);plain.fill(0);return encryptBackup(Json.canonical(node),password.toCharArray())
    }
    @Test fun exportRestorePersistsMetadataAssociationsAndExactFileBytes()=runBlocking {
        val (task,a)=seed(source,sourceFiles);val bytes=blob();BackupRepository(target,targetFiles).restore(bytes,password.toCharArray());assertEquals(task,target.records().tasksSnapshot().single());assertEquals(a,target.records().attachments(task.id).single());assertEquals("exact private bytes",File(targetFiles,"evidence/${a.id}").readText())
    }
    @Test fun wrongKeyAndTamperingLeaveExistingRoomRowsAndFilesIntact()=runBlocking {
        seed(source,sourceFiles);val (task,a)=seed(target,targetFiles);val bytes=blob();reject("BACKUP_AUTH_FAILED",bytes,"different valid passphrase");reject("BACKUP_AUTH_FAILED",bytes.copyOf().also {it[it.lastIndex]=(it.last().toInt() xor 1).toByte()});assertEquals(listOf(task),target.records().tasksSnapshot());assertEquals("exact private bytes",File(targetFiles,"evidence/${a.id}").readText());assertEquals(1,target.records().allAttachments().size)
    }
    @Test fun authenticatedMalformedRecordOrDigestIsRefusedBeforeAnyWrite()=runBlocking {
        seed(source,sourceFiles);val bytes=blob();reject("BACKUP_BAD_TASK",changed(bytes){(it["tasks"][0] as com.fasterxml.jackson.databind.node.ObjectNode).put("title","")});reject("BACKUP_FILE_INTEGRITY",changed(bytes){(it["files"] as com.fasterxml.jackson.databind.node.ObjectNode).put(it["attachments"][0]["id"].asText(),Base64.getEncoder().encodeToString("different".toByteArray()))});assertTrue(target.records().tasksSnapshot().isEmpty());assertFalse(File(targetFiles,"evidence").exists())
    }
    @Test fun unknownVersionsAndExtraFieldsAreRejected()=runBlocking {
        seed(source,sourceFiles);val bytes=blob();reject("BACKUP_VERSION_UNSUPPORTED",changed(bytes){it.put("schema","deproof-portable-records-v99")});reject("BACKUP_BAD_RECORD",changed(bytes){it.put("credentials","not portable")});reject("BACKUP_FORMAT_UNSUPPORTED",bytes.copyOf().also {it[4]=99});assertTrue(target.records().tasksSnapshot().isEmpty())
    }
    @Test fun existingIdsAndFilesAreNeverOverwritten()=runBlocking {
        val (task,a)=seed(source,sourceFiles);target.records().create(task);val bytes=blob();reject("BACKUP_ID_CONFLICT",bytes);assertEquals(listOf(task),target.records().tasksSnapshot());assertTrue(target.records().allAttachments().isEmpty());assertFalse(File(targetFiles,"evidence/${a.id}").exists())
    }
    @Test fun laterDatabaseConstraintFailureRollsBackRowsAndRemovesOnlyNewFiles()=runBlocking {
        val (_,a)=seed(source,sourceFiles);val existing=receipt("OBSERVED",null,null,"existing");val incoming=receipt("OBSERVED",null,null,"incoming");target.records().saveReceipt(existing);source.records().saveReceipt(incoming)
        target.records().observe(Observation("same-id",existing["id"].asText(),Instant.now().toString(),"{}"));source.records().observe(Observation("same-id",incoming["id"].asText(),Instant.now().toString(),"{}"))
        try {BackupRepository(target,targetFiles).restore(blob(),password.toCharArray());fail("constraint accepted")}catch(e:android.database.sqlite.SQLiteConstraintException) {assertNotNull(e.message)}
        assertTrue(target.records().tasksSnapshot().isEmpty());assertTrue(target.records().allAttachments().isEmpty());assertNull(target.records().event(incoming["id"].asText()));assertNotNull(target.records().event(existing["id"].asText()));assertFalse(File(targetFiles,"evidence/${a.id}").exists())
    }

    @Test fun restoredPlansRetainTaskLinkButRemindersStayCancelled()=runBlocking {
        val (task,_)=seed(source,sourceFiles);val plan=MappingPlan(UUID.randomUUID().toString(),task.id,"Local plan",listOf(MappingPoint("Home","10","20")),Instant.now().toString())
        val reminder=newReminder(task.id,task.title,Instant.now().plusSeconds(3600).toString(),true,true,Instant.now())
        source.records().draft(Draft("mapping:"+task.id,plan.canonical().toString(Charsets.UTF_8),Instant.now().toString()));source.records().draft(Draft("reminder:"+reminder.id,Json.mapper.writeValueAsString(reminder.copy(state="SCHEDULED")),Instant.now().toString()))
        BackupRepository(target,targetFiles).restore(blob(),password.toCharArray());assertEquals(plan,parseMappingPlan(target.records().draft("mapping:"+task.id)!!.payload));assertEquals("CANCELLED",parseReminder(target.records().draft("reminder:"+reminder.id)!!.payload).state)
    }
    @Test fun exportedScopeExcludesPairingSeedsOperationsAndPermissions()=runBlocking {
        seed(source,sourceFiles);source.records().pair(NodeSession("session","https://localhost:9843","pin","fingerprint","secret seed","[]","PAIRED"));source.records().operation(Operation("op","NODE_proof","session","RUNNING","{}",null,Instant.now().toString()))
        val text=decryptBackup(blob(),password.toCharArray()).toString(Charsets.UTF_8);assertFalse(text.contains("secret seed"));assertFalse(text.contains("node_sessions"));assertFalse(text.contains("operations"));assertEquals(setOf("schema","tasks","attachments","events","observations","drafts","contributions","files"),Json.parse(text).fieldNames().asSequence().toSet())
    }
    @Test fun actualFileMetadataFactoryRefusesChangedBytesAndMissingTaskWithoutRows()=runBlocking {
        val id=UUID.randomUUID().toString();File(sourceFiles,"evidence").mkdir();val file=File(sourceFiles,"evidence/$id").apply {writeText("before")};val repo=EvidenceRepository(source.records(),file.parentFile!!);val a=repo.metadata(id,null,"text/plain","import",Instant.now().toString());file.writeText("after");try {repo.attachEvidence(a);fail("changed bytes attached")}catch(e:Failure){assertEquals("EVIDENCE_METADATA_CHANGED",e.code)};assertNull(source.records().attachment(id))
        val missing=repo.metadata(id,UUID.randomUUID().toString(),"text/plain","import",Instant.now().toString());try {repo.attachEvidence(missing);fail("orphan accepted")}catch(e:android.database.sqlite.SQLiteConstraintException){assertNotNull(e.message)};assertNull(source.records().attachment(id))
    }
}
