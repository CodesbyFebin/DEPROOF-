package com.example

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.deproof.app.R
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import com.example.data.preferences
import kotlinx.coroutines.launch

fun languageContext(context: Context,tag: String): Context {
    require(tag in setOf("en","es"))
    return context.createConfigurationContext(Configuration(context.resources.configuration).apply {setLocale(Locale.forLanguageTag(tag))})
}
@Composable fun routeLabel(route: String): String=stringResource(when(route) {
    "Now"->R.string.route_now;"Review"->R.string.route_review;"Receipts"->R.string.ui_60a627df404a
    "Nodes"->R.string.route_nodes;"Bandwidth"->R.string.route_bandwidth;"Proof jobs"->R.string.route_proofs
    else->R.string.ui_b3a60e61a523
})
@Composable fun LanguageControls(context: Context) {
    val scope=rememberCoroutineScope()
    Text(stringResource(R.string.language_heading))
    Text(stringResource(R.string.language_scope))
    for(tag in listOf("en","es"))Button(onClick={scope.launch {context.preferences.edit {it[stringPreferencesKey("language")]=tag}}}) {Text(if(tag=="en") "English" else "Español")}
}

@Composable fun receiptLabel(kind: String): String {
    val id=when(kind) {"REJECTED"->R.string.rejected;"OBSERVED"->R.string.observed;"LOCAL_EVIDENCE_SIGNED"->R.string.local_signed;"WALLET_SIGNED"->R.string.wallet_signed;"SUBMITTED"->R.string.submitted;"SUBMISSION_UNKNOWN"->R.string.submission_unknown;"UNAVAILABLE"->R.string.observation_unavailable;else->null}
    return if(id==null)kind else stringResource(id)
}
@Composable fun diagnosticLabel(value: String): String {
    val id=when(value) {
        "Copied JSON"->R.string.copied
        "Raw-file integrity matches"->R.string.integrity_matches
        "RPC accepted exact signed bytes. Confirmation has not been claimed."->R.string.rpc_accepted
        "SUBMISSION_UNKNOWN — inspect the known signature; do not resubmit blindly"->R.string.submit_unknown
        "CAPTURE_CANCELLED_OR_UNAVAILABLE; no photo record created"->R.string.capture_cancelled
        "CLIPBOARD_UNAVAILABLE — selectable JSON below"->R.string.clip_unavailable
        "Session revoked by node; further commands denied"->R.string.revoked
        "Node returned an authenticated result. Refresh status for current runtime state."->R.string.node_result
        "Paired; refresh to observe the node"->R.string.paired
        "NODE_UNAVAILABLE — status unknown; refresh, do not replay"->R.string.node_unavailable
        else->null
    }
    return if(id==null)value else stringResource(id)
}
