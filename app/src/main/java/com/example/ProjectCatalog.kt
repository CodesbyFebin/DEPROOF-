package com.example

// ProjectCatalog — Mobile node workspace for DePIN project adapters.
//
// Displays the versioned adapter catalog, per-adapter compatibility results from
// the paired Ubuntu node, workload health, and contribution receipts.
//
// Evidence constraints enforced here:
//   - All displayed values include their measurement source and observation time.
//   - No uptime, bandwidth, earnings or contribution scores are invented.
//   - Qualification fixture receipts are clearly labelled as local-only.
//   - SKR integration status is shown as BLOCKED unless explicitly changed.
//   - Provider acknowledgement is displayed only when the receipt contains it.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deproof.app.R
import com.example.data.*
import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*

@Composable private fun SectionSpacer() = Spacer(Modifier.height(8.dp))

/** Adapter compatibility result received from the node. */
private data class AdapterResult(
    val adapterId: String,
    val name: String,
    val status: String,
    val compatible: Boolean,
    val incompatibleReasons: List<String>,
    val blockedReason: String?
)

/**
 * ProjectCatalog shows the DePIN adapter catalog as received from the paired node,
 * allows running the local qualification fixture, and displays contribution receipts.
 *
 * It never invents hardware capabilities, rewards, or contribution scores.
 * All data is derived from the node's signed observations.
 */
@Composable
fun ProjectCatalog(dao: RecordsDao) {
    val repository = remember { NodeRepository(dao) }
    val scope = rememberCoroutineScope()
    val sessions by dao.nodeSessions().collectAsState(emptyList())

    var selected by remember { mutableStateOf<String?>(null) }
    val session = sessions.firstOrNull { it.id == selected } ?: sessions.lastOrNull()

    // Adapter list result from node.
    var catalogResult by remember { mutableStateOf<JsonNode?>(null) }
    var adapters by remember { mutableStateOf<List<AdapterResult>>(emptyList()) }

    // Workload receipt list from node.
    var receipts by remember { mutableStateOf<JsonNode?>(null) }

    // Fixture execution state.
    var targetAddr by remember { mutableStateOf("") }
    var fixtureConsent by remember { mutableStateOf(false) }
    var lastReceipt by remember { mutableStateOf<JsonNode?>(null) }

    // UI state.
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun parseAdapters(node: JsonNode): List<AdapterResult> {
        val arr = node["adapters"] ?: return emptyList()
        return arr.map { a ->
            AdapterResult(
                adapterId = a["adapterId"]?.asText() ?: "",
                name = a["name"]?.asText() ?: "",
                status = a["status"]?.asText() ?: "UNKNOWN",
                compatible = a["compatible"]?.asBoolean() == true,
                incompatibleReasons = a["incompatibleReasons"]?.map { it.asText() } ?: emptyList(),
                blockedReason = a["blockedReason"]?.asText()
            )
        }
    }

    fun nodeCommand(action: String, params: JsonNode = Json.obj()) {
        val peer = session ?: return
        if (working) return
        working = true
        error = null
        scope.launch {
            try {
                val result = repository.command(peer, action, params)
                when (action) {
                    "listAdapters" -> {
                        catalogResult = result
                        adapters = parseAdapters(result)
                    }
                    "runAdapter" -> {
                        lastReceipt = result
                        fixtureConsent = false
                    }
                    "listWorkloadReceipts" -> {
                        receipts = result
                    }
                }
            } catch (e: Exception) {
                error = (e as? Failure)?.code ?: "NODE_UNAVAILABLE — status unknown"
            } finally {
                working = false
            }
        }
    }

    Text(stringResource(R.string.projects_heading), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.projects_note))

    // Session selector — reuses the same pairing sessions as NodeWorkspace.
    if (sessions.isEmpty()) {
        Text("No paired nodes. Pair a node in the Nodes workspace first.")
        return
    }
    sessions.forEach { s ->
        TextButton(onClick = {
            selected = s.id
            catalogResult = null
            adapters = emptyList()
            receipts = null
            lastReceipt = null
            error = null
        }) {
            Text("Node ${s.fingerprint.take(12)} (${s.state})")
        }
    }

    session?.let { peer ->
        Text("Connected node: ${peer.fingerprint.take(16)}…")

        // ── Catalog section ────────────────────────────────────────────────
        SectionSpacer()
        Text("Adapter catalog", style = MaterialTheme.typography.titleMedium)
        catalogResult?.get("catalogVersion")?.asText()?.let { v ->
            Text("${stringResource(R.string.projects_catalog_version)}: $v")
        }
        catalogResult?.get("observedAt")?.asText()?.let { t ->
            Text("Catalog observed at: $t (source: node-agent listAdapters)")
        }
        Button(
            enabled = peer.state == "PAIRED" && !working,
            onClick = { nodeCommand("listAdapters") }
        ) {
            Text(stringResource(R.string.projects_observe_adapters))
        }

        if (adapters.isNotEmpty()) {
            adapters.forEach { a ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(a.name, style = MaterialTheme.typography.titleSmall)
                        Text("${stringResource(R.string.projects_adapter_id)}: ${a.adapterId}")
                        Text("${stringResource(R.string.projects_status)}: ${a.status}")
                        if (a.compatible) {
                            Text(stringResource(R.string.projects_compatible))
                        } else {
                            Text(stringResource(R.string.projects_incompatible))
                            a.blockedReason?.let { Text("Blocked: $it") }
                            a.incompatibleReasons.forEach { Text("  • $it") }
                        }
                    }
                }
            }
            Text(
                "NOTE: Compatibility is assessed from node-observed host facts only. " +
                "RAM and disk are not measured in this observation. " +
                "GPU and special hardware are assumed absent unless attested externally.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // ── SKR status notice ──────────────────────────────────────────────
        SectionSpacer()
        Text(stringResource(R.string.projects_skr_status), style = MaterialTheme.typography.bodySmall)

        // ── Local qualification fixture ────────────────────────────────────
        SectionSpacer()
        Text("Local qualification fixture", style = MaterialTheme.typography.titleMedium)
        Text(
            "The latency-measurement fixture exercises the full adapter pipeline locally. " +
            "Receipts are labelled isQualificationFixture=true and must not be presented " +
            "as provider-acknowledged DePIN contribution.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = targetAddr,
            onValueChange = { targetAddr = it; fixtureConsent = false },
            label = { Text(stringResource(R.string.projects_target_addr)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row {
            Checkbox(
                checked = fixtureConsent,
                onCheckedChange = { fixtureConsent = it }
            )
            Text(
                stringResource(R.string.projects_consent_label),
                Modifier.padding(top = 12.dp)
            )
        }
        Button(
            enabled = fixtureConsent && targetAddr.isNotBlank() && peer.state == "PAIRED" && !working,
            onClick = {
                nodeCommand(
                    "runAdapter",
                    Json.obj(
                        "adapterId" to "deproof-adapter-latency-fixture-v1",
                        "operationId" to java.util.UUID.randomUUID().toString(),
                        "targetAddr" to targetAddr.trim(),
                        "explicitConsent" to true
                    )
                )
            }
        ) {
            Text(stringResource(R.string.projects_run_fixture))
        }

        // Display last fixture receipt.
        lastReceipt?.let { r ->
            SectionSpacer()
            Text("Latest fixture receipt", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Receipt ID: ${r["receiptId"]?.asText() ?: "—"}")
                Text("${stringResource(R.string.projects_receipt_evidence_level)}: ${r["evidenceLevel"]?.asText() ?: "—"}")
                r["measuredValue"]?.let { mv ->
                    Text("Avg latency (µs): ${mv["avgLatencyMicros"]?.asText() ?: "—"}")
                    Text("Target: ${mv["targetAddr"]?.asText() ?: "—"}")
                    Text("${stringResource(R.string.projects_receipt_observed_at)}: ${mv["observedAt"]?.asText() ?: "—"}")
                }
                r["isQualificationFixture"]?.asBoolean()?.let { fixture ->
                    if (fixture) {
                        Text(
                            "⚑ ${r["fixtureLabel"]?.asText() ?: "LOCAL_QUALIFICATION_FIXTURE"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                r["providerAcknowledgement"]?.let { ack ->
                    if (ack["available"]?.asBoolean() == false) {
                        Text("Provider acknowledgement: NOT AVAILABLE — ${ack["reason"]?.asText() ?: ""}")
                    }
                }
                r["nodeSignature"]?.asText()?.let { sig ->
                    if (sig.isNotBlank()) {
                        Text("${stringResource(R.string.projects_receipt_node_signed)}")
                        Text("Signature: ${sig.take(24)}…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                r["trustAssumptions"]?.let { ta ->
                    Text("Trust assumptions (${ta.size()} total):", style = MaterialTheme.typography.bodySmall)
                    for (i in 0 until minOf(ta.size(), 3)) {
                        Text("  • ${ta[i].asText()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            SelectionContainer {
                Text(
                    Json.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(r),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Contribution history ───────────────────────────────────────────
        SectionSpacer()
        Text("Contribution history", style = MaterialTheme.typography.titleMedium)
        Text(
            "NODE_SIGNED_RECORD: the node signed its local observation. " +
            "This is not independent verification or provider-acknowledged contribution.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            enabled = peer.state == "PAIRED" && !working,
            onClick = { nodeCommand("listWorkloadReceipts", Json.obj("limit" to 20)) }
        ) {
            Text(stringResource(R.string.projects_list_receipts))
        }

        receipts?.let { r ->
            val count = r["count"]?.asInt() ?: 0
            if (count == 0) {
                Text(stringResource(R.string.projects_no_receipts))
            } else {
                Text("$count receipts (observed at: ${r["observedAt"]?.asText() ?: "—"})")
                r["receipts"]?.take(10)?.forEach { rec ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ID: ${rec["receiptId"]?.asText()?.take(20) ?: "—"}…")
                            Text("Adapter: ${rec["adapterId"]?.asText() ?: "—"}")
                            Text("Evidence: ${rec["evidenceLevel"]?.asText() ?: "—"}")
                            Text("Executed: ${rec["executedAt"]?.asText() ?: "—"}")
                            if (rec["isQualificationFixture"]?.asBoolean() == true) {
                                Text("LOCAL FIXTURE — not real contribution", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // ── Offline/failure states ─────────────────────────────────────────
        if (peer.state != "PAIRED") {
            Text("Node state: ${peer.state} — some operations unavailable.")
        }
    } ?: Text("Select a paired node above to view the project catalog.")

    if (working) Text("Working…")
    error?.let { Text(diagnosticLabel(it)) }
}
