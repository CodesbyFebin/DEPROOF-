package com.example.domain

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.util.UUID

class ProofDispatchTest {
 private val now=Instant.parse("2026-10-06T00:00:00Z")
 private fun job()=(Json.obj("id" to UUID.randomUUID().toString(),"source" to "OWNER_LOCAL_CUBIC_SAMPLE","backend" to "gnark-v0.14.0","scheme" to "Groth16/BN254","circuitDigest" to "a".repeat(64),"deadline" to now.plusSeconds(60).toString(),"resourceProfile" to Json.obj("circuit" to "owner-local-cubic","circuitVersion" to "a".repeat(64),"minMemoryMiB" to "256","minCpuCores" to "1")) as com.fasterxml.jackson.databind.node.ObjectNode).also {it.put("jobId",it["id"].asText())}
 private fun status()=Json.obj("observedAt" to now.toString(),"proofCapabilities" to Json.obj("availability" to "AVAILABLE","backends" to listOf("gnark-v0.14.0"),"circuitVersions" to mapOf("owner-local-cubic" to listOf("a".repeat(64))),"memoryMiB" to 512,"cpuCores" to 2))
 @Test fun freshObservedLocalProfileMatchesAndResourceMismatchRefuses(){assertEquals("COMPATIBLE_LOCAL_PROFILE",proofDispatchDecision(job(),status(),now).state);val s=status();(s["proofCapabilities"] as com.fasterxml.jackson.databind.node.ObjectNode).put("memoryMiB",0);val result=proofDispatchDecision(job(),s,now);assertEquals("MISMATCH",result.state);assertTrue("INSUFFICIENT_MEMORY" in result.reasons)}
 @Test fun missingStaleOrMalformedCapabilitiesNeverAuthorize(){assertEquals("UNAVAILABLE",proofDispatchDecision(job(),null,now).state);assertEquals(listOf("CAPABILITIES_STALE"),proofDispatchDecision(job(),status(),now.plusSeconds(121)).reasons);val s=status();(s["proofCapabilities"] as com.fasterxml.jackson.databind.node.ObjectNode).put("memoryMiB","512");assertEquals("UNAVAILABLE",proofDispatchDecision(job(),s,now).state)}
 @Test fun unqualifiedProviderAndExpiredJobHaveExplicitStates(){val j=job();j.put("source","EXTERNAL_PROVIDER");assertEquals("BLOCKED",proofDispatchDecision(j,status(),now).state);val expired=job();expired.put("deadline",now.toString());assertEquals(listOf("JOB_EXPIRED"),proofDispatchDecision(expired,status(),now).reasons)}
 @Test fun changedPinnedCircuitOrAbsentJobProfileRefuses(){val j=job();j.put("circuitDigest","b".repeat(64));assertEquals(listOf("JOB_PROFILE_MISMATCH"),proofDispatchDecision(j,status(),now).reasons);j.remove("resourceProfile");assertEquals("UNAVAILABLE",proofDispatchDecision(j,status(),now).state)}
}
