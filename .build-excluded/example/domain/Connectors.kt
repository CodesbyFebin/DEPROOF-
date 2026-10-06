package com.example.domain

// FN059 runConnector(connectorId, request): PARTIAL implementation.
// The contract requires an attributable adapter result with freshness from a
// real, qualified adapter. No adapter is qualified in this build, and this
// dispatcher performs no network request. Every outcome is therefore BLOCKED,
// with the reason taken from connectors/support.json. No observation, freshness
// or provider support is invented.

data class ConnectorEntry(val id: String, val status: String, val blocker: String?, val source: String?)
data class ConnectorRequest(val operation: String)
data class ConnectorResult(
    val connectorId: String,
    val outcome: String,
    val reason: String,
    val source: String?,
    val observedAt: String?,
    val freshness: String
)

fun parseConnectorSupport(raw: String): List<ConnectorEntry> {
    val root = Json.parse(raw)
    ensure(root["schema"]?.asText() == "deproof-connector-support-v1", "CONNECTOR_SCHEMA_MISMATCH")
    return root["adapters"].map { a ->
        ConnectorEntry(a["id"].asText(), a["status"].asText(), a["blocker"]?.asText(), a["source"]?.asText())
    }
}

fun runConnector(registry: List<ConnectorEntry>, connectorId: String, request: ConnectorRequest): ConnectorResult {
    ensure(request.operation.isNotBlank() && request.operation.length <= 64, "BAD_OPERATION")
    val entry = registry.firstOrNull { it.id == connectorId }
        ?: return blockedResult(connectorId, "UNKNOWN_CONNECTOR: no adapter with this id", null)
    val reason = when (entry.status) {
        "BLOCKED" -> "BLOCKED: ${entry.blocker ?: "no reason recorded"}"
        "IMPLEMENTED_UNVERIFIED" -> "UNQUALIFIED: ${entry.blocker ?: "no reason recorded"}. This dispatcher performs no observation."
        "QUALIFIED" -> "NO_DISPATCH_IMPLEMENTED: no qualified adapter is wired into this dispatcher"
        else -> "UNRECOGNIZED_STATUS: ${entry.status}"
    }
    return blockedResult(connectorId, reason, entry.source)
}

private fun blockedResult(connectorId: String, reason: String, source: String?) =
    ConnectorResult(connectorId, "BLOCKED", reason, source, observedAt = null, freshness = "NOT_OBSERVED")
