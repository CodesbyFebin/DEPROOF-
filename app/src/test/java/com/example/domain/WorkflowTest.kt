package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.util.UUID

class WorkflowTest {
    private val now=Instant.parse("2026-10-06T10:00:00Z")
    private val task="6f5f22f3-d0c7-4f3e-918c-cee511613ac6"
    private fun refuses(code: String,block: () -> Unit) {try {block();fail("expected refusal")} catch(e:Failure) {assertEquals(code,e.code)}}
    private fun loc()=LocationObservation("10.5","76.25","80","network",now.minusSeconds(2).toString(),now.toString(),"APPROXIMATE",false)
    @Test fun locationConsentDenialAndProvenance() {
        assertNull(consentedLocation(loc(),false,true));refuses("LOCATION_PERMISSION_DENIED") {consentedLocation(loc(),true,false)}
        assertEquals(loc(),consentedLocation(loc(),true,true))
        refuses("LOCATION_STALE") {loc().copy(observedAt=now.minusSeconds(121).toString()).validate()}
        refuses("LOCATION_STALE") {loc().copy(observedAt=now.plusSeconds(6).toString()).validate()}
        refuses("BAD_LOCATION_RANGE") {loc().copy(latitude="91").validate()}
        refuses("BAD_LOCATION_DECIMAL") {loc().copy(latitude="NaN").validate()}
        val bytes=canonicalEvidence(emptyList(),"exact\nनोट",task,null,now.toString(),loc().copy(mock=true))
        assertTrue(Json.parse(bytes.toString(Charsets.UTF_8))["location"]["mock"].booleanValue())
        assertFalse(Json.parse(canonicalEvidence(emptyList(),"",task,null,now.toString()).toString(Charsets.UTF_8)).has("location"))
    }
    @Test fun offlineMappingPlanRoundTripAndBounds() {
        val points=parseMappingPoints("Home|10.5|76.25\nFinish|-90|180")
        val p=MappingPlan(UUID.randomUUID().toString(),task,"Private plan",points,now.toString());val raw=p.canonical().toString(Charsets.UTF_8)
        assertEquals(p,parseMappingPlan(raw));assertEquals("Home",p.points.first().label)
        refuses("BAD_LOCATION_RANGE") {p.copy(points=listOf(MappingPoint("outside","0","181"))).validate()}
        refuses("BAD_MAPPING_PLAN") {p.copy(points=emptyList()).validate()}
        refuses("BAD_MAPPING_PLAN") {p.copy(points=List(101) {points.first()}).validate()}
        refuses("MAPPING_POINT_FORMAT") {parseMappingPoints("unfinished|5")}
        refuses("BAD_MAPPING_FIELDS") {parseMappingPlan(raw.dropLast(1)+",\"providerRewards\":\"999\"}")}
    }
    @Test fun crossLanguageOptionalLocationVectorAndMutation() {
        val file=java.io.File("fixtures/location-golden.json").takeIf {it.isFile} ?: java.io.File("../fixtures/location-golden.json")
        val vector=Json.parse(file.readText())
        val location=parseLocationObservation(vector["manifest"]["location"])
        val bytes=canonicalEvidence(emptyList(),"exact\nनोट",null,null,now.toString(),location)
        assertEquals(vector["canonicalUtf8"].textValue(),bytes.toString(Charsets.UTF_8));assertEquals(vector["sha256"].textValue(),sha256Hex(bytes))
        assertNotEquals(sha256Hex(bytes),sha256Hex(canonicalEvidence(emptyList(),"exact\nनोट",null,null,now.toString(),location.copy(mock=true))))
        val numeric=Json.parse(vector["manifest"]["location"].toString()) as com.fasterxml.jackson.databind.node.ObjectNode
        numeric.put("latitude",10.5);refuses("BAD_LOCATION_FIELD") {parseLocationObservation(numeric)}
    }
    @Test fun reminderConsentPermissionClockAndReplacement() {
        val due=now.plusSeconds(600).toString()
        refuses("REMINDER_CONSENT_REQUIRED") {newReminder(task,"Private task",due,false,true,now)}
        refuses("BAD_REMINDER_TIME") {newReminder(task,"Private task",now.toString(),true,true,now)}
        refuses("REMINDER_TIME_MUST_BE_UTC") {newReminder(task,"Private task","2026-10-06T11:00:00+01:00",true,true,now)}
        val denied=newReminder(task,"Private task",due,true,false,now);assertEquals("PERMISSION_DENIED",denied.state)
        val a=newReminder(task,"Private task",due,true,true,now);val b=newReminder(task,"Private task",due,true,true,now)
        assertEquals(a.id,b.id);assertNotEquals(a.generation,b.generation)
        assertEquals(a,parseReminder(Json.mapper.writeValueAsString(a)))
        assertFalse(reminderMayPost(a,a.generation,now,true,true))
        assertFalse(reminderMayPost(b,a.generation,now.plusSeconds(600),true,true))
        assertTrue(reminderMayPost(b,b.generation,now.plusSeconds(600),true,true))
        assertFalse(reminderMayPost(b,b.generation,now.plusSeconds(600),false,true))
        assertFalse(reminderMayPost(b,b.generation,now.plusSeconds(600),true,false))
        assertFalse(reminderMayPost(b,b.generation,now.plusSeconds(600+86400),true,true))
        assertEquals("PENDING",reminderRecovery(a,now,true,true))
        assertEquals("CANCELLED",reminderRecovery(a,now,false,true))
        assertEquals("EXPIRED",reminderRecovery(a,now.plusSeconds(601),true,true))
        assertEquals("POST_REQUESTED",reminderRecovery(a.copy(state="POST_REQUESTED"),now,true,true))
    }
}
