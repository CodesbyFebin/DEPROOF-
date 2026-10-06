package com.example.domain

// EF019 matchProofJob(job, capabilities): compatible backend and resource profile,
// or an explicit list of mismatches. Pure logic over the supplied values. It does
// not discover real backends, measure resources or contact a prover network.
// The Android wiring that would call it is not implemented, so EF019 stays BLOCKED.

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
