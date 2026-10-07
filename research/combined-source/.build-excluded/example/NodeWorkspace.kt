package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.deproof.app.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*
import java.time.Instant

@Composable fun NodeWorkspace(dao: RecordsDao, page: String) {
    val repository=remember {NodeRepository(dao)};val scope=rememberCoroutineScope()
    val sessions by dao.nodeSessions().collectAsState(emptyList())
    var selected by remember {mutableStateOf<String?>(null)}
    val session=sessions.firstOrNull {it.id==selected} ?: sessions.lastOrNull()
    var endpoint by remember {mutableStateOf("https://127.0.0.1:9843")}
    var pin by remember {mutableStateOf("")};var fingerprint by remember {mutableStateOf("")}
    var challenge by remember {mutableStateOf("")};var code by remember {mutableStateOf("")}
    var scopes by remember {mutableStateOf(setOf("READ_NODE"))}
    var confirmed by remember {mutableStateOf(false)}
    var status by remember(session?.id) {mutableStateOf<JsonNode?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    var working by remember {mutableStateOf(false)}
    var contributionEndpoint by remember {mutableStateOf("")};var cap by remember {mutableStateOf("1048576")}
    var rate by remember {mutableStateOf("65536")};var bytes by remember {mutableStateOf("4096")}
    var sharingConsent by remember {mutableStateOf(false)};var inputConsent by remember {mutableStateOf(false)}
    var jobs by remember {mutableStateOf<JsonNode?>(null)}
    fun command(action: String,params: JsonNode=Json.obj()) {
        val peer=session ?: return
        scope.launch {
            val exclusive=action !in setOf("stop","cancelProof","revoke","observe")
            if(exclusive && working) return@launch
            if(exclusive) working=true
            error=null
            try {
                val result=if(action=="proof")repository.proof(peer,jobs ?: throw Failure("JOB_UNAVAILABLE"),status ?: throw Failure("CAPABILITIES_UNAVAILABLE"),params["explicitConsent"]?.asBoolean()==true) else repository.command(peer,action,params)
                if(action=="observe") {status=result;inputConsent=false}
                else if(action=="discoverProofJobs") {jobs=result;inputConsent=false}
                else if(action=="revoke") {status=null;error="Session revoked by node; further commands denied"}
                else {error="Node returned an authenticated result. Refresh status for current runtime state."}
            } catch(e: Exception) {error=(e as? Failure)?.code ?: "NODE_UNAVAILABLE — status unknown; refresh, do not replay"}
            finally {if(exclusive) working=false}
        }
    }
    Text(routeLabel(page),style=MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.ui_e40f2f3e4a65))
    if(page=="Nodes") {
        Text(stringResource(R.string.ui_af7c2b9863fd))
        OutlinedTextField(endpoint,{endpoint=it;confirmed=false},label={Text(stringResource(R.string.ui_6c5c7f43f92f))},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(pin,{pin=it.trim();confirmed=false},label={Text(stringResource(R.string.ui_86ac8a3d5af7))},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(fingerprint,{fingerprint=it.trim();confirmed=false},label={Text(stringResource(R.string.ui_92b4d72b24b0))},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(challenge,{challenge=it;confirmed=false},label={Text(stringResource(R.string.ui_5ee9bd60fe92))},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(code,{code=it.trim()},label={Text(stringResource(R.string.ui_7dcf0e4e47e7))},modifier=Modifier.fillMaxWidth())
        NodeProtocol.SCOPES.forEach {s -> Row {Checkbox(s in scopes,{checked -> scopes=if(checked) scopes+s else scopes-s;confirmed=false});Text(s,Modifier.padding(top=12.dp))}}
        Row {Checkbox(confirmed,{confirmed=it});Text(stringResource(R.string.ui_b8cf000395d8),Modifier.padding(top=12.dp))}
        Button(enabled=confirmed && !working,onClick={scope.launch {working=true;error=null;try {repository.pair(endpoint,pin,Json.parse(challenge),code,fingerprint,scopes.sorted());code="";confirmed=false;error="Paired; refresh to observe the node"} catch(e:Exception){error=(e as? Failure)?.code ?: "PAIRING_UNAVAILABLE"}finally{working=false}}}) {Text(stringResource(R.string.ui_b81ce254eba0))}
    }
    sessions.forEach {s -> TextButton(onClick={selected=s.id;status=null;jobs=null;inputConsent=false}) {Text(stringResource(R.string.ui_455fc3345560 ,(s.fingerprint.take(12)).toString(),(s.state).toString()))} }
    session?.let {peer ->
        Text(stringResource(R.string.ui_45ffa1cdb735 ,(peer.fingerprint).toString()))
        Text(stringResource(R.string.ui_cdc4e367f92e ,(peer.state).toString(),(peer.scopes).toString()))
        Button(enabled=peer.state=="PAIRED",onClick={command("observe")}) {Text(stringResource(R.string.ui_4768777917c2))}
        Button(enabled=peer.state=="PAIRED",onClick={command("revoke")}) {Text(stringResource(R.string.ui_987b6b55442d))}
        status?.let {s -> SelectionContainer {Text(Json.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s))};Text(stringResource(R.string.ui_fa69f4cac934 ,(s["observedAt"]).toString()))}
        if(page=="Nodes") {
            Text(stringResource(R.string.ui_b2df30d33fe3))
            Button(enabled=!working,onClick={command("startService",Json.obj("profileDigest" to status?.get("hosting")?.get("profileDigest")?.asText(),"explicitConsent" to true))}) {Text(stringResource(R.string.ui_dae57ce78e42))}
            Button(onClick={command("stopService")}) {Text(stringResource(R.string.ui_b7a562975bae))}
        }
        if(page=="Bandwidth") {
            Text(stringResource(R.string.ui_dd0f63b0996c))
            OutlinedTextField(contributionEndpoint,{contributionEndpoint=it;sharingConsent=false},label={Text(stringResource(R.string.ui_bceb15d665b4))},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(cap,{cap=it;sharingConsent=false},label={Text(stringResource(R.string.ui_1c2ff3922876))})
            OutlinedTextField(rate,{rate=it;sharingConsent=false},label={Text(stringResource(R.string.ui_76d656badda6))})
            Row {Checkbox(sharingConsent,{sharingConsent=it});Text(stringResource(R.string.ui_9146c7bd4d51),Modifier.padding(top=12.dp))}
            Button(enabled=sharingConsent && !working,onClick={command("consent",Json.obj("endpoint" to contributionEndpoint,"capBytes" to cap,"bytesPerSecond" to rate,"deadline" to Instant.now().plusSeconds(600).toString(),"explicitConsent" to true));sharingConsent=false}) {Text(stringResource(R.string.ui_7126a7aee903))}
            OutlinedTextField(bytes,{bytes=it},label={Text(stringResource(R.string.ui_cd805249ed35))})
            Button(enabled=!working,onClick={command("transfer",Json.obj("bytes" to bytes))}) {Text(stringResource(R.string.ui_54a7473a6c5b))}
            Button(onClick={command("stop")}) {Text(stringResource(R.string.ui_96955eb51a57))}
            Text(stringResource(R.string.ui_23e271c3be3f))
        }
        if(page=="Proof jobs") {
            val match=proofDispatchDecision(jobs,status)
            Text("${match.state}: ${match.reasons.joinToString()}")
            Button(enabled=!working,onClick={command("discoverProofJobs")}) {Text(stringResource(R.string.ui_72ef303be147))}
            jobs?.let {j -> SelectionContainer {Text(Json.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(j))}}
            Text(stringResource(R.string.ui_9a9c0cd94b3e))
            Row {Checkbox(inputConsent,{inputConsent=it});Text(stringResource(R.string.ui_5df379fab397),Modifier.padding(top=12.dp))}
            Button(enabled=match.state=="COMPATIBLE_LOCAL_PROFILE" && inputConsent && !working,onClick={command("proof",Json.obj("jobId" to jobs!!["jobId"].asText(),"explicitConsent" to true));inputConsent=false}) {Text(stringResource(R.string.ui_dab4cb8ac57a))}
            Button(onClick={command("cancelProof")}) {Text(stringResource(R.string.ui_dba22c4a4931))}
        }
    } ?: Text(stringResource(R.string.ui_d02f6ffb4e2a))
    if(working) Text(stringResource(R.string.ui_b5bbefef866a))
    error?.let {Text(diagnosticLabel(it))}
}
