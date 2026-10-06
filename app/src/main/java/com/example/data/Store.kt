package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit

@Entity(tableName = "events")
data class Event(@PrimaryKey val id: String, val createdAt: String, val kind: String, val payload: String)
@Entity(tableName = "observations", foreignKeys = [ForeignKey(entity = Event::class,parentColumns = ["id"],childColumns = ["eventId"],onDelete = ForeignKey.CASCADE)], indices = [Index("eventId")])
data class Observation(@PrimaryKey val id: String, val eventId: String, val observedAt: String, val payload: String)
@Entity(tableName = "tasks")
data class Task(@PrimaryKey val id: String, val title: String, val note: String, val checklist: String, val createdAt: String)
@Entity(tableName = "attachments", foreignKeys = [ForeignKey(entity = Task::class,parentColumns = ["id"],childColumns = ["taskId"],onDelete = ForeignKey.CASCADE)], indices = [Index("taskId")])
data class Attachment(@PrimaryKey val id: String, val taskId: String?, val digest: String, val mime: String, val size: String, val provenance: String, val createdAt: String)
@Entity(tableName = "node_sessions")
data class NodeSession(@PrimaryKey val id: String, val endpoint: String, val certificateSha256: String, val fingerprint: String, val encryptedSeed: String, val scopes: String, val state: String)
@Entity(tableName = "operations")
data class Operation(@PrimaryKey val id: String, val kind: String, val target: String, val state: String, val payload: String, val result: String?, val updatedAt: String)
@Entity(tableName = "contributions")
data class Contribution(@PrimaryKey val id: String, val nodeFingerprint: String, val createdAt: String, val payload: String)
@Entity(tableName = "drafts")
data class Draft(@PrimaryKey val id: String, val payload: String, val updatedAt: String)
@Dao interface RecordsDao {
    @Query("SELECT * FROM node_sessions ORDER BY id") fun nodeSessions(): Flow<List<NodeSession>>
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun pair(s: NodeSession)
    @Query("UPDATE node_sessions SET state=:state WHERE id=:id") suspend fun sessionState(id: String,state: String)
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun operation(o: Operation)
    @Query("UPDATE operations SET state=:state,result=:result,updatedAt=:at WHERE id=:id") suspend fun operationState(id: String,state: String,result: String?,at: String)
    @Query("SELECT * FROM operations WHERE kind='FILE_IMPORT' AND state='FILE_READY'") suspend fun readyImports(): List<Operation>
    @Transaction suspend fun commitImport(id: String,a: Attachment) {attach(a);operationState(id,"COMPLETED",null,Instant.now().toString())}
    @Query("SELECT * FROM operations ORDER BY updatedAt DESC") fun operations(): Flow<List<Operation>>
    @Query("SELECT * FROM operations WHERE state IN ('PREPARED','RUNNING','SUBMITTING')") suspend fun pendingOperations(): List<Operation>
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun contribution(c: Contribution)
    @Query("SELECT * FROM contributions ORDER BY createdAt DESC") fun contributions(): Flow<List<Contribution>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun draft(d: Draft)
    @Query("SELECT * FROM drafts ORDER BY id") suspend fun draftsSnapshot(): List<Draft>
    @Query("SELECT * FROM contributions ORDER BY id") suspend fun contributionsSnapshot(): List<Contribution>
    @Query("SELECT * FROM drafts WHERE id=:id") suspend fun draft(id: String): Draft?
    @Query("SELECT * FROM drafts WHERE id LIKE 'reminder:%'") suspend fun reminderDrafts(): List<Draft>
    @Query("SELECT * FROM events WHERE kind='WALLET_SIGNED'") suspend fun signedIntents(): List<Event>
    @Transaction suspend fun recoverInterrupted() {
        val at=Instant.now().toString()
        pendingOperations().forEach {operationState(it.id,"OUTCOME_UNKNOWN",Json.mapper.writeValueAsString(Json.obj("reason" to "PROCESS_INTERRUPTED","retry" to "OBSERVE_ONLY_NO_AUTOMATIC_REPLAY")),at)}
        signedIntents().forEach {e ->
            if(observations(e.id).isEmpty()) observe(Observation("recovery-"+e.id,e.id,at,Json.mapper.writeValueAsString(Json.obj("submissionState" to "SUBMISSION_UNKNOWN","broadcast" to null,"submittedByDeproof" to null,"signature" to Json.parse(e.payload)["signature"],"reason" to "PROCESS_INTERRUPTED_OBSERVE_KNOWN_SIGNATURE"))))
        }
    }
    @Transaction suspend fun completeNode(o: Operation, result: JsonNode, fingerprint: String) {
        if(result["schema"]?.asText()=="deproof-node-signed-v1") {
            NodeProtocol.verifyRecord(result,fingerprint,o.id)
            contribution(Contribution(o.id,fingerprint,Instant.now().toString(),Json.mapper.writeValueAsString(result)))
        }
        operationState(o.id,"COMPLETED",Json.mapper.writeValueAsString(result),Instant.now().toString())
    }

    @Query("SELECT * FROM attachments ORDER BY id") suspend fun allAttachments(): List<Attachment>
    @Query("SELECT * FROM events ORDER BY createdAt DESC") suspend fun eventsSnapshot(): List<Event>
    @Query("SELECT * FROM observations ORDER BY id") suspend fun allObservations(): List<Observation>
    @Query("SELECT * FROM events ORDER BY createdAt DESC") fun events(): Flow<List<Event>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(e: Event)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun observe(o: Observation)
    @Query("SELECT * FROM observations WHERE eventId=:id ORDER BY observedAt DESC") suspend fun observations(id: String): List<Observation>
    @Query("SELECT * FROM events WHERE id=:id") suspend fun event(id: String): Event?
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC") fun tasks(): Flow<List<Task>>
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC") suspend fun tasksSnapshot(): List<Task>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun create(t: Task)
    @Update suspend fun update(t: Task)
    @Query("SELECT * FROM attachments WHERE taskId=:id ORDER BY id") suspend fun attachments(id: String): List<Attachment>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun attach(a: Attachment)
    @Query("DELETE FROM events WHERE id=:id") suspend fun deleteEvent(id: String)
    @Query("DELETE FROM attachments WHERE id=:id") suspend fun deleteAttachment(id: String)
    @Query("SELECT * FROM attachments WHERE id=:id") suspend fun attachment(id: String): Attachment?
    @Transaction suspend fun saveReceipt(r: JsonNode) { validateReceipt(r); insert(Event(r["id"].asText(),r["createdAt"].asText(),r["outcome"].asText(),exportReceipt(r))) }
    suspend fun createTask(title: String, requirements: List<String>): Task {
        ensure(title.isNotBlank() && title.length <= 200 && requirements.size <= 100,"BAD_TASK")
        val t = Task(UUID.randomUUID().toString(),title,"",Json.mapper.writeValueAsString(requirements.map { mapOf("text" to it,"done" to false) }),Instant.now().toString()); create(t); return t
    }
}
@Database(entities = [Event::class,Observation::class,Task::class,Attachment::class,NodeSession::class,Operation::class,Contribution::class,Draft::class], version = 2, exportSchema = true)
abstract class DeproofDatabase : RoomDatabase() {
    abstract fun records(): RecordsDao
    companion object {
        val MIGRATION_1_2=object: androidx.room.migration.Migration(1,2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                val sql=DeproofDatabase::class.java.getResourceAsStream("/db/migration_1_2.sql")?.bufferedReader()?.use {it.readText()} ?: throw Failure("MIGRATION_RESOURCE_MISSING")
                sql.split(';').map {it.trim()}.filter {it.isNotEmpty()}.forEach {db.execSQL(it)}
            }
        }
        fun open(context: Context) = Room.databaseBuilder(context, DeproofDatabase::class.java,"deproof.db").addMigrations(MIGRATION_1_2).build()
    }
}
val Context.preferences by preferencesDataStore(name = "deproof_preferences")
class Preferences(private val context: Context) {
    suspend fun rpc(cluster: String, endpoint: String) { Rpc(endpoint,cluster); context.preferences.edit { it[stringPreferencesKey("rpc_$cluster")] = endpoint } }
}
