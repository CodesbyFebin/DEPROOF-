package com.example.domain

// EF019 matchProofJob(job, capabilities): compatible backend and resource profile,
// or an explicit list of mismatches. Pure logic over the supplied values. It does
// not discover real backends, measure resources or contact a prover network.
// Android dispatch calls this matcher through a fresh, attributed capability gate.
// Local compatibility is not external-provider qualification or a resource reservation.

data class ProofJobRequirement(val backend: String, val circuit: String, val circuitVersion: String, val minMemoryMiB: Int, val minCpuCores: Int)
data class ProverCapabilities(val backends: Set<String>, val circuitVersions: Map<String, Set<String>>, val memoryMiB: Int, val cpuCores: Int)
data class ProofJobMatch(val compatible: Boolean, val mismatches: List<String>)

fun matchProofJob(job: ProofJobRequirement, capabilities: ProverCapabilities): ProofJobMatch {
    ensure(job.minMemoryMiB >= 0 && job.minCpuCores >= 0, "BAD_REQUIREMENT")
    val mismatches = mutableListOf<String>()
    if (job.backend !in capabilities.backends) mismatches += "BACKEND_NOT_SUPPORTED"
    val versions = capabilities.circuitVersions[job.circuit]
    when {
        versions == null -> mismatches += "CIRCUIT_NOT_SUPPORTED"
        job.circuitVersion !in versions -> mismatches += "CIRCUIT_VERSION_NOT_SUPPORTED"
    }
    if (capabilities.memoryMiB < job.minMemoryMiB) mismatches += "INSUFFICIENT_MEMORY"
    if (capabilities.cpuCores < job.minCpuCores) mismatches += "INSUFFICIENT_CPU"
    return ProofJobMatch(compatible = mismatches.isEmpty(), mismatches = mismatches)
}

/** Android gate over attributable, fresh node observations. Missing data is unavailable, never zero. */
data class ProofDispatchDecision(val state: String,val reasons: List<String>)
fun proofDispatchDecision(job: com.fasterxml.jackson.databind.JsonNode?,observation: com.fasterxml.jackson.databind.JsonNode?,now: java.time.Instant=java.time.Instant.now()): ProofDispatchDecision {
    fun unavailable(reason: String)=ProofDispatchDecision("UNAVAILABLE",listOf(reason))
    if(job==null || observation==null)return unavailable("JOB_OR_CAPABILITIES_NOT_OBSERVED")
    return try {
        val at=java.time.Instant.parse(observation["observedAt"]?.asText());val age=java.time.Duration.between(at,now).seconds
        if(age !in -5..120)return unavailable("CAPABILITIES_STALE")
        if(job["source"]?.asText()!="OWNER_LOCAL_CUBIC_SAMPLE" || job["backend"]?.asText()!="gnark-v0.14.0" || job["scheme"]?.asText()!="Groth16/BN254")return ProofDispatchDecision("BLOCKED",listOf("EXTERNAL_PROVIDER_UNQUALIFIED"))
        val id=job["jobId"]?.asText();ensure(id==job["id"]?.asText() && java.util.UUID.fromString(id).toString()==id,"BAD_JOB_ID")
        if(!java.time.Instant.parse(job["deadline"]?.asText()).isAfter(now))return ProofDispatchDecision("MISMATCH",listOf("JOB_EXPIRED"))
        val p=job["resourceProfile"] ?: return unavailable("JOB_PROFILE_UNAVAILABLE")
        val caps=observation["proofCapabilities"] ?: return unavailable("CAPABILITIES_UNAVAILABLE")
        if(caps["availability"]?.asText()!="AVAILABLE")return unavailable(caps["reason"]?.asText() ?: "CAPABILITIES_UNAVAILABLE")
        ensure(p["circuit"]?.asText()=="owner-local-cubic" && p["circuitVersion"]?.asText()==job["circuitDigest"]?.asText() && job["circuitDigest"].asText().matches(Regex("[0-9a-f]{64}")),"JOB_PROFILE_MISMATCH")
        for(k in listOf("minMemoryMiB","minCpuCores"))ensure(p[k]?.isTextual==true && p[k].asText().matches(Regex("0|[1-9][0-9]{0,8}")),"BAD_REQUIREMENT")
        for(k in listOf("memoryMiB","cpuCores"))ensure(caps[k]?.isInt==true && caps[k].intValue()>=0,"BAD_CAPABILITIES")
        ensure(caps["backends"]?.isArray==true && caps["backends"].all {it.isTextual} && caps["circuitVersions"]?.isObject==true,"BAD_CAPABILITIES")
        val versions=caps["circuitVersions"].fields().asSequence().associate {ensure(it.value.isArray && it.value.all {v->v.isTextual},"BAD_CAPABILITIES");it.key to it.value.map {v->v.asText()}.toSet()}
        val match=matchProofJob(ProofJobRequirement(job["backend"].asText(),p["circuit"].asText(),p["circuitVersion"].asText(),p["minMemoryMiB"].asText().toInt(),p["minCpuCores"].asText().toInt()),ProverCapabilities(caps["backends"].map {it.asText()}.toSet(),versions,caps["memoryMiB"].intValue(),caps["cpuCores"].intValue()))
        ProofDispatchDecision(if(match.compatible)"COMPATIBLE_LOCAL_PROFILE" else "MISMATCH",match.mismatches)
    } catch(e:Exception) {unavailable((e as? Failure)?.code ?: "JOB_OR_CAPABILITIES_MALFORMED")}
}
