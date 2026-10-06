package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.domain.*
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// F079: each chain status is tracked as its own observation; availability is separate;
// statuses outside the receipt schema enum are refused. Local tests only.
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ObservationStatusTest {
    private lateinit var db: DeproofDatabase
    @Before fun open() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),DeproofDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() {db.close()}
    private val schemaStatuses=listOf("UNKNOWN","PROCESSED","CONFIRMED","FINALIZED","FAILED")
    private fun record(status:String,at:String,availability:String="AVAILABLE"):ObjectNode {
        val r=receipt("OBSERVED",null,"mainnet-beta","Historical transaction",signature=Base58.encode(ByteArray(64){1})) as ObjectNode
        r.put("messageSha256","a".repeat(64))
        (r["chainObservation"] as ObjectNode).apply {put("availability",availability);put("lastKnownStatus",status);put("observedAt",at)}
        return r
    }
    @Test fun everySchemaStatusIsTrackedAsItsOwnObservation()=runBlocking {
        val dao=db.records()
        val id=dao.saveHistoricalObservation(record("UNKNOWN","2026-10-06T10:00:00Z"))
        schemaStatuses.drop(1).forEachIndexed {i,s -> dao.saveHistoricalObservation(record(s,"2026-10-06T1${i+1}:00:00Z")) }
        val tracked=dao.observations(id).map {Json.parse(it.payload)["lastKnownStatus"].asText()}.toSet()
        assertEquals(schemaStatuses.toSet(),tracked)
        assertEquals(1,dao.eventsSnapshot().size)
    }
    @Test fun statusesOutsideTheSchemaEnumAreRefused() {
        for (bad in listOf("PENDING","CONFIRMING","UNAVAILABLE","")) {
            try { validateReceipt(record(bad,"2026-10-06T12:00:00Z")); fail("accepted $bad") }
            catch(e:Failure){ assertEquals("BAD_CHAIN_STATUS",e.code) }
        }
    }
    @Test fun unavailabilityIsRecordedSeparatelyFromTheLastKnownStatus()=runBlocking {
        val dao=db.records()
        val id=dao.saveHistoricalObservation(record("CONFIRMED","2026-10-06T10:00:00Z"))
        dao.saveHistoricalObservation(record("UNKNOWN","2026-10-06T11:00:00Z",availability="UNAVAILABLE"))
        val rows=dao.observations(id).map {Json.parse(it.payload)}
        assertTrue(rows.any {it["availability"].asText()=="UNAVAILABLE"})
        assertTrue(rows.any {it["availability"].asText()=="AVAILABLE" && it["lastKnownStatus"].asText()=="CONFIRMED"})
    }

    // Contract-level: the qualified schema is the source of truth. Reads it from the repository.
    private fun schemaFile(): java.io.File {
        var dir: java.io.File? = java.io.File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val f = java.io.File(dir, "contracts/receipt-v2.schema.json")
            if (f.isFile) return f
            dir = dir.parentFile
        }
        error("contracts/receipt-v2.schema.json not found")
    }
    private fun schemaEnum(vararg path: String): List<String> {
        var node = com.fasterxml.jackson.databind.ObjectMapper().readTree(schemaFile())
        for (p in path) node = node[p] ?: error("schema path missing: $p")
        return node.map { it.asText() }
    }
    @Test fun schemaStatusAndAvailabilityEnumsAreExactlyWhatTheValidatorAccepts() {
        val statuses = schemaEnum("properties","chainObservation","properties","lastKnownStatus","enum")
        val availability = schemaEnum("properties","chainObservation","properties","availability","enum")
        assertEquals(schemaStatuses.toSet(), statuses.toSet())
        for (s in statuses) validateReceipt(record(s,"2026-10-06T12:00:00Z"))
        for (a in availability) validateReceipt(record("CONFIRMED","2026-10-06T12:00:00Z",availability=a))
        // Availability values are not statuses, and statuses are not availability values.
        for (a in availability) try { validateReceipt(record(a,"2026-10-06T12:00:00Z")); fail("availability accepted as status: $a") } catch(e:Failure){ assertEquals("BAD_CHAIN_STATUS",e.code) }
    }
    // F079 mapping (docs/f079-status-mapping.md): "pending" has no schema status. For a transaction
    // Deproof submitted it is outcome SUBMITTED with submission.state SUBMITTED, and chain status
    // UNKNOWN (not yet observed) or PROCESSED (observed, not final). Historical observations use PROCESSED.
    private fun submittedReceipt(status: String): ObjectNode {
        val r = record(status, "2026-10-06T12:00:00Z")
        r.put("outcome", "SUBMITTED")
        (r["submission"] as ObjectNode).apply {
            put("state", "SUBMITTED"); put("broadcast", true); put("submittedByDeproof", true)
            put("rpcAcceptedAt", "2026-10-06T11:59:00Z")
        }
        return r
    }
    @Test fun pendingForADeproofSubmissionIsSubmittedWithUnknownOrProcessedChainStatus() {
        for (status in listOf("UNKNOWN", "PROCESSED")) validateReceipt(submittedReceipt(status))
        try { validateReceipt(submittedReceipt("PENDING")); fail("PENDING accepted as chain status") }
        catch (e: Failure) { assertEquals("BAD_CHAIN_STATUS", e.code) }
    }
    @Test fun submittingIsNotAPersistedReceiptState() {
        val r = submittedReceipt("UNKNOWN")
        (r["submission"] as ObjectNode).put("state", "SUBMITTING")
        try { validateReceipt(r); fail("SUBMITTING persisted on a SUBMITTED receipt") }
        catch (e: Failure) { assertEquals("MISSING_RPC_ACCEPTANCE", e.code) }
    }
}
