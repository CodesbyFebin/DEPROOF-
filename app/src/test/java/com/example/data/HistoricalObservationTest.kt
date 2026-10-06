package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.domain.*
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class HistoricalObservationTest {
    private lateinit var db: DeproofDatabase
    @Before fun open() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),DeproofDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() {db.close()}
    private fun record(status:String="CONFIRMED",network:String="mainnet-beta",digest:String="a".repeat(64)):ObjectNode {
        val r=receipt("OBSERVED",null,network,"Historical transaction",signature=Base58.encode(ByteArray(64){1})) as ObjectNode
        r.put("messageSha256",digest)
        (r["chainObservation"] as ObjectNode).apply {put("availability","AVAILABLE");put("lastKnownStatus",status);put("observedAt","2026-10-06T12:00:00Z")}
        return r
    }
    @Test fun repeatedHistoryRefreshPreservesOriginalEventAndTracksChangedStatus()=runBlocking {
        val dao=db.records();val initial=record();val id=dao.saveHistoricalObservation(initial);val original=dao.event(id)!!
        val next=record("FINALIZED");assertEquals(id,dao.saveHistoricalObservation(next))
        assertEquals(listOf(original),dao.eventsSnapshot())
        assertEquals(setOf("CONFIRMED","FINALIZED"),dao.observations(id).map {Json.parse(it.payload)["lastKnownStatus"].asText()}.toSet())
        val saved=Json.parse(dao.event(id)!!.payload);assertFalse(saved["submission"]["broadcast"].asBoolean());assertFalse(saved["submission"]["submittedByDeproof"].asBoolean())
    }
    @Test fun concurrentOpensCreateOneReceiptWithIndependentObservations()=runBlocking {
        val ids=coroutineScope {List(8){async {db.records().saveHistoricalObservation(record())}}.map {it.await()}}
        assertEquals(1,ids.toSet().size);assertEquals(1,db.records().eventsSnapshot().size);assertEquals(8,db.records().observations(ids.first()).size)
    }
    @Test fun identicalSignatureOnDifferentClustersIsNotConflated()=runBlocking {
        val dao=db.records();assertNotEquals(dao.saveHistoricalObservation(record()),dao.saveHistoricalObservation(record(network="devnet")));assertEquals(2,dao.eventsSnapshot().size)
    }
    @Test fun conflictingMessageIsRefusedWithoutChangingReceiptOrObservations()=runBlocking {
        val dao=db.records();val id=dao.saveHistoricalObservation(record());val before=dao.event(id);val observations=dao.observations(id)
        try {dao.saveHistoricalObservation(record(digest="b".repeat(64)));fail("changed message accepted")}catch(e:Failure){assertEquals("HISTORICAL_MESSAGE_CONFLICT",e.code)}
        assertEquals(before,dao.event(id));assertEquals(observations,dao.observations(id));assertEquals(1,dao.eventsSnapshot().size)
    }
    @Test fun unavailableRefreshRetainsOriginalKnownStatusWithoutInventingSuccess()=runBlocking {
        val dao=db.records();val r=record();val id=dao.saveHistoricalObservation(r);val unavailable=record("UNKNOWN")
        (unavailable["chainObservation"] as ObjectNode).apply {put("availability","UNAVAILABLE");put("error","RPC_UNAVAILABLE")}
        dao.saveHistoricalObservation(unavailable)
        assertEquals("CONFIRMED",Json.parse(dao.event(id)!!.payload)["chainObservation"]["lastKnownStatus"].asText())
        assertTrue(dao.observations(id).any {Json.parse(it.payload)["availability"].asText()=="UNAVAILABLE"});assertEquals(1,dao.eventsSnapshot().size)
    }
}
