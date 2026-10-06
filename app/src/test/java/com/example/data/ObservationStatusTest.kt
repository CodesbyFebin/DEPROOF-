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
}
