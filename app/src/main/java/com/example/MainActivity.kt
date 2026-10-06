package com.example

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.deproof.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.example.data.*
import com.example.domain.*
import com.example.wallet.*
import com.deproof.app.BuildConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.*
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.Base64

class MainActivity : ComponentActivity() {
    private lateinit var wallet: Wallet
    private lateinit var db: DeproofDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wallet = Wallet(ActivityResultSender(this)); db = DeproofDatabase.open(this)
        setContent {
            val preferences by applicationContext.preferences.data.collectAsState(initial=emptyPreferences())
            val language=preferences[stringPreferencesKey("language")] ?: if(resources.configuration.locales[0].language=="es") "es" else "en"
            val localized=remember(language) {languageContext(this,language)}
            CompositionLocalProvider(LocalContext provides localized,LocalConfiguration provides localized.resources.configuration) {Deproof(wallet,db.records())}
        }
    }
    @Composable private fun Deproof(wallet: Wallet, dao: RecordsDao) {
        val scope = rememberCoroutineScope()
        var route by remember { mutableStateOf("Now") }
        var nested by remember { mutableStateOf("Workspace") }
        var address by remember { mutableStateOf("") }
        var account by remember { mutableStateOf<String?>(null) }
        var sol by remember { mutableStateOf<Balance?>(null) }; var skr by remember { mutableStateOf<Balance?>(null) }
        var solError by remember { mutableStateOf<String?>(null) }; var skrError by remember { mutableStateOf<String?>(null) }
        var history by remember { mutableStateOf<JsonNode?>(null) }
        var historyError by remember { mutableStateOf<String?>(null) }
        var historyAddress by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
        var work by remember { mutableStateOf<Job?>(null) }
        var review by remember { mutableStateOf<Review?>(null) }
        var reviewedFee by remember { mutableStateOf(false) }
        var memoRebuilds by remember { mutableStateOf(0) }
        var rawDraft by remember { mutableStateOf("") }
        var textExport by remember { mutableStateOf<String?>(null) }
        var fileMeta by remember { mutableStateOf<List<EvidenceFile>>(emptyList()) }
        var note by remember { mutableStateOf("") }
        var selectedTask by remember { mutableStateOf<Task?>(null) }
        var location by remember(selectedTask?.id) {mutableStateOf<LocationObservation?>(null)}
        var taskTitle by remember { mutableStateOf("") }
        var taskSearch by remember { mutableStateOf("") }
        var taskRequirements by remember { mutableStateOf("") }
        var receiptSearch by remember { mutableStateOf("") }
        val events by dao.events().collectAsState(initial = emptyList())
        val tasks by dao.tasks().collectAsState(initial = emptyList())
        val contributions by dao.contributions().collectAsState(initial=emptyList())
        val operations by dao.operations().collectAsState(initial=emptyList())
        LaunchedEffect(Unit) {withContext(Dispatchers.IO) {
            dao.recoverInterrupted()
            ReminderRepository(applicationContext,dao).recover()
            for(op in dao.readyImports()) {
                try {
                    val meta=Json.parse(op.result ?: throw Failure("IMPORT_METADATA_MISSING"));val file=File(filesDir,"evidence/${op.target}")
                    val h=file.inputStream().use {sha256File(it)};ensure(h.sha256==meta["digest"].asText() && h.byteLength==meta["size"].asText(),"FILE_CHANGED")
                    EvidenceRepository(dao,File(filesDir,"evidence")).attachEvidence(Attachment(op.target,meta["taskId"].takeUnless {it.isNull}?.asText(),h.sha256,meta["mime"].asText(),h.byteLength,meta["provenance"].asText(),meta["createdAt"].asText()),op.id)
                } catch(ex: Exception) {dao.operationState(op.id,"OUTCOME_UNKNOWN",Json.mapper.writeValueAsString(Json.obj("error" to "IMPORT_RECOVERY_NEEDS_INSPECTION")),Instant.now().toString())}
            }
        }}

        val preferences by applicationContext.preferences.data.collectAsState(initial=emptyPreferences())
        var firstPrograms by remember(review?.messageSha256,review?.context?.cluster) {mutableStateOf<Set<String>?>(null)}
        var programHistoryError by remember(review?.messageSha256,review?.context?.cluster) {mutableStateOf(false)}
        LaunchedEffect(review?.messageSha256,review?.context?.cluster) {
            val r=review
            firstPrograms=null
            try {if(r!=null) applicationContext.preferences.edit { prefs ->
                val key=stringPreferencesKey("seen_programs_${r.context.cluster}")
                val known=prefs[key]?.split('\n')?.filter {it.isNotBlank()}?.toSet() ?: emptySet()
                val programs=r.tx.instructions.map {it.program}
                firstPrograms=firstSeenPrograms(programs,known)
                prefs[key]=(known+programs).sorted().joinToString("\n")
            }} catch(e:CancellationException) {throw e} catch(e:Exception) {programHistoryError=true;firstPrograms=null}
        }
        val mainnetEndpoint=preferences[stringPreferencesKey("rpc_mainnet-beta")] ?: "https://api.mainnet-beta.solana.com"
        val devnetEndpoint=preferences[stringPreferencesKey("rpc_devnet")] ?: "https://api.devnet.solana.com"
        val mainnet=remember(mainnetEndpoint) { Rpc(mainnetEndpoint,"mainnet-beta") }
        val devnet=remember(devnetEndpoint) { Rpc(devnetEndpoint,"devnet") }
        var mainnetDraft by remember { mutableStateOf(mainnetEndpoint) }
        var devnetDraft by remember { mutableStateOf(devnetEndpoint) }
        fun run(block: suspend () -> Unit) {
            if(busy) return
            work=scope.launch { busy=true; error=null; try { block() } catch(e: CancellationException) { error="CANCELLED"; throw e } catch(e: Exception) { error=(e as? Failure)?.code ?: "OPERATION_UNAVAILABLE" } finally { busy=false } }
        }
        suspend fun importEvidence(uri: Uri, provenance: String) {
            withContext(Dispatchers.IO) {
                val id=UUID.randomUUID().toString(); val dir=File(filesDir,"evidence").apply { mkdirs() }; val target=File(dir,id)
                dao.operation(Operation(id,"FILE_IMPORT",id,"RUNNING",Json.mapper.writeValueAsString(Json.obj("taskId" to selectedTask?.id)),null,Instant.now().toString()))
                try {
                    contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { output -> val buf=ByteArray(65536); var total=0L; while(true) { currentCoroutineContext().ensureActive(); val n=input.read(buf); if(n<0) break; total+=n; ensure(total<=64L*1024*1024,"FILE_TOO_LARGE"); output.write(buf,0,n) }; output.fd.sync() } } ?: throw Failure("CONTENT_UNAVAILABLE")
                    val h=target.inputStream().use { sha256File(it) }; val mime=contentResolver.getType(uri) ?: "application/octet-stream"
                    val at=Instant.now().toString()
                    val meta=Json.obj("taskId" to selectedTask?.id,"digest" to h.sha256,"mime" to mime,"size" to h.byteLength,"provenance" to provenance,"createdAt" to at)
                    dao.operationState(id,"FILE_READY",Json.mapper.writeValueAsString(meta),at)
                    val repository=EvidenceRepository(dao,dir);val a=repository.metadata(id,selectedTask?.id,mime,provenance,at);ensure(a.digest==h.sha256 && a.size==h.byteLength,"FILE_CHANGED");repository.attachEvidence(a,id)
                    withContext(Dispatchers.Main) { fileMeta=fileMeta+EvidenceFile(id,h.sha256,mime,h.byteLength,provenance) }
                } catch(e: Exception) { withContext(NonCancellable) {if(dao.attachment(id)==null)target.delete()}; throw e }
            }
        }
        val importFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if(uris.isNotEmpty()) run { for(uri in uris) importEvidence(uri,"import") } }
        var cameraUriText by rememberSaveable {mutableStateOf<String?>(null)}

        val capture=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri=cameraUriText?.let {Uri.parse(it)}; cameraUriText=null
            if(ok && uri!=null) run { importEvidence(uri,"capture"); contentResolver.delete(uri,null,null) }
            else { if(uri!=null) runCatching { contentResolver.delete(uri,null,null) }; error="CAPTURE_CANCELLED_OR_UNAVAILABLE; no photo record created" }
        }
        val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val text=textExport; if(uri!=null && text!=null) run { withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: throw Failure("EXPORT_UNAVAILABLE") } }
        }
        suspend fun loadEvidence(t: Task?) {
            location=null
            selectedTask=t; note=t?.note ?: ""
            fileMeta=if(t == null) emptyList() else withContext(Dispatchers.IO) { dao.attachments(t.id).map { EvidenceFile(it.id,it.digest,it.mime,it.size,it.provenance) } }
            nested="Evidence"
        }
        fun copy(text: String) {
            textExport=text
            try { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Deproof receipt",text)); error="Copied JSON" }
            catch(e: Exception) { error="CLIPBOARD_UNAVAILABLE — selectable JSON below" }
        }
        val colors=deproofColors(preferences[stringPreferencesKey("theme")] == "pearl")
        MaterialTheme(colorScheme=colors) {
            Scaffold(bottomBar={ NavigationBar { for(r in listOf("Now","Review","Receipts")) NavigationBarItem(selected=route==r,onClick={route=r},icon={ Text(r.take(1)) },label={Text(routeLabel(r))}) } }) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Image(painterResource(R.drawable.depr),"DEPR",Modifier.size(48.dp)); Column { Text(stringResource(R.string.ui_832c8de7d27e),style=MaterialTheme.typography.headlineSmall); Text(stringResource(R.string.ui_10dac3fc86e1),style=MaterialTheme.typography.bodyMedium) } }
                    Text(stringResource(R.string.ui_de8bd0d0e09c),color=colors.onSurfaceVariant)
                    if(busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Button(onClick={work?.cancel()}) { Text(stringResource(R.string.ui_19766ed6ccb2)) } }
                    error?.let { Text(diagnosticLabel(it),color=colors.error) }
                    when(route) {
                        "Now" -> if(nested=="Workspace") {
                            Text(stringResource(R.string.ui_4d0260e6c3e7),style=MaterialTheme.typography.headlineMedium)
                            Panel(stringResource(R.string.ui_d11de81bbbf8)) {
                                Value(stringResource(R.string.ui_42e9b1466285),account ?: stringResource(R.string.not_connected))
                                Text(stringResource(R.string.ui_9842d107db35 ,(BuildConfig.WALLET_IDENTITY_URI).toString()))
                                if(account==null) Button(enabled=!busy,onClick={run { account=wallet.connect(); review=null }}) { Text(stringResource(R.string.ui_7b1f118169bc)) }
                                else Button(enabled=!busy,onClick={run { try { wallet.disconnect() } finally { account=null; review=null } }}) { Text(stringResource(R.string.ui_acfc5be785a9)) }
                                Text(if(wallet.strict) stringResource(R.string.ui_65afa8deec49) else stringResource(R.string.ui_db8d1ca13b8b))
                                Button(enabled=account!=null && wallet.strict && !busy,onClick={run {
                                    val a=account ?: throw Failure("NO_WALLET_ACCOUNT")
                                    val r=withContext(Dispatchers.IO) {
                                        val genesis=devnet.genesis(); val latest=devnet.latestDevnetBlockhash(); val bytes=buildMemo(a,latest["value"]["blockhash"].asText(),System.currentTimeMillis())
                                        val fee=devnet.estimateFee(parseTransaction(bytes).messageBytes()); devnet.simulateTransaction(bytes)
                                        Review(bytes,ReviewContext(a,"devnet",genesis,Policy.DEVNET_MEMO_V1,reviewedAt=Instant.now().toString(),lastValidBlockHeight=latest["value"]["lastValidBlockHeight"].asText(),feeLamports=fee["value"].asText(),feeSlot=fee["context"]["slot"].asText()))
                                    }
                                    review=r; reviewedFee=false; memoRebuilds=0; route="Review"
                                }}) { Text(stringResource(R.string.ui_48a010281107)) }
                                Text(stringResource(R.string.ui_60cd63aa91fc))
                            }
                            Panel(stringResource(R.string.ui_a0991c3086d4)) {
                                OutlinedTextField(address,{address=it},label={Text(stringResource(R.string.ui_667a0f71f42c))},modifier=Modifier.fillMaxWidth())
                                Button(enabled=!busy,onClick={run {
                                    val queried=address.trim(); Base58.pubkey(queried); historyAddress=queried
                                    coroutineScope {
                                        val jobs=listOf(
                                            launch { try { sol=withContext(Dispatchers.IO) { mainnet.balance(queried) }; solError=null } catch(e:Exception) { solError=(e as? Failure)?.code ?: "RPC_UNAVAILABLE" } },
                                            launch { try { skr=withContext(Dispatchers.IO) { mainnet.skrBalance(queried) }; skrError=null } catch(e:Exception) { skrError=(e as? Failure)?.code ?: "RPC_UNAVAILABLE" } },
                                            launch { try { history=withContext(Dispatchers.IO) { mainnet.history(queried) }; historyError=null } catch(e:Exception) { historyError=(e as? Failure)?.code ?: "RPC_UNAVAILABLE" } })
                                        jobs.joinAll()
                                    }
                                }}) { Text(stringResource(R.string.ui_fe47dedf8786)) }
                                BalanceView(stringResource(R.string.ui_29296c07a5ba),sol,solError); BalanceView(stringResource(R.string.ui_e698a4565469),skr,skrError)
                            }
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                Button(onClick={nested="Tasks"}) {Text(stringResource(R.string.ui_b3a60e61a523))}
                                Button(onClick={run { loadEvidence(null) }}) {Text(stringResource(R.string.ui_03867aea70ac))}
                            }
                            for(page in listOf("Nodes","Bandwidth","Proof jobs")) Button(onClick={nested=page}) {Text(routeLabel(page))}
                            TextButton(onClick={mainnetDraft=mainnetEndpoint;devnetDraft=devnetEndpoint;nested="Settings"}) {Text(stringResource(R.string.ui_48f37e0a293b))}
                            events.firstOrNull()?.let { Panel(stringResource(R.string.ui_682d5b670f85)) { Text(it.kind); Text(it.createdAt); Button(onClick={route="Receipts"}) {Text(stringResource(R.string.ui_bc059d436b21))} } }
                            Panel(stringResource(R.string.ui_a6198526bf12)) {
                                Value(stringResource(R.string.ui_744512146c1b),historyAddress ?: stringResource(R.string.none))
                                if(historyError!=null) Text(stringResource(R.string.ui_1a0ffbf84254 ,(historyError).toString()))
                                if(history==null || history!!.isEmpty) Text(stringResource(R.string.ui_f36b63464ec3))
                                history?.forEach { h ->
                                    val sig=h["signature"].asText(); Value(stringResource(R.string.ui_f1a73e2204a1),sig)
                                    Text(stringResource(R.string.ui_a26c0050f43a ,(h["confirmationStatus"]?.asText() ?: "Unknown").toString(),(h["slot"].asText()).toString(),(h["blockTime"]).toString(),(h["err"]).toString()))
                                    Button(enabled=!busy,onClick={run {
                                        val result=withContext(Dispatchers.IO) { mainnet.transaction(sig) }; val tx=parseTransaction(result.first)
                                        val r=Review(result.first,ReviewContext(tx.keys.first().address,"mainnet-beta",withContext(Dispatchers.IO){mainnet.genesis()},Policy.SKR_TRANSFER_V1,reviewedAt=Instant.now().toString(),lastValidBlockHeight=null,feeLamports=null,feeSlot=null,historical=true))
                                        val record=receipt("OBSERVED",historyAddress,"mainnet-beta","Historical transaction",r,sig) as ObjectNode
                                        val obs=record["chainObservation"] as ObjectNode; obs.put("availability","AVAILABLE"); obs.put("lastKnownStatus",if(!h["err"].isNull) "FAILED" else h["confirmationStatus"].asText("unknown").uppercase()); obs.put("slot",h["slot"].asText()); obs.set<JsonNode>("blockTime",h["blockTime"]?.let { if(it.isNull) Json.mapper.nodeFactory.nullNode() else Json.mapper.valueToTree<JsonNode>(it.asText()) }); obs.put("observedAt",Instant.now().toString()); obs.set<JsonNode>("error",h["err"])
                                        dao.saveHistoricalObservation(record); review=r; reviewedFee=false; memoRebuilds=0; route="Review"
                                    }}) {Text(stringResource(R.string.ui_a292578987d4))}
                                    TextButton(onClick={startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,Uri.parse("https://explorer.solana.com/tx/$sig?cluster=mainnet-beta")))}) {Text(stringResource(R.string.ui_8fe371b8e097))}
                                }
                            }
                        } else if(nested in listOf("Nodes","Bandwidth","Proof jobs")) {
                            TextButton(onClick={nested="Workspace"}) {Text(stringResource(R.string.ui_0f0c0c6e215e))}
                            NodeWorkspace(dao,nested)
                        } else if(nested=="Settings") {
                            BackupWorkspace(db,busy,{busy=it},{review=null;reviewedFee=false;memoRebuilds=0;location=null})
                            TextButton(onClick={nested="Workspace"}) {Text(stringResource(R.string.ui_0f0c0c6e215e))}
                            Text(stringResource(R.string.ui_66962f72a088),style=MaterialTheme.typography.headlineMedium)
                            LanguageControls(applicationContext)
                            Button(onClick={run { applicationContext.preferences.edit { it[stringPreferencesKey("theme")]=if(it[stringPreferencesKey("theme")]=="pearl") "dark" else "pearl" } }}) {Text(stringResource(R.string.ui_a0ce7a4e3d66))}
                            OutlinedTextField(mainnetDraft,{mainnetDraft=it},label={Text(stringResource(R.string.ui_4e103a16cfc9))},modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(devnetDraft,{devnetDraft=it},label={Text(stringResource(R.string.ui_dcd26770187a))},modifier=Modifier.fillMaxWidth())
                            Text(stringResource(R.string.ui_f1f2bb7c30e8))
                            Button(enabled=!busy,onClick={run {
                                Rpc(mainnetDraft,"mainnet-beta");Rpc(devnetDraft,"devnet")
                                Preferences(applicationContext).rpc("mainnet-beta",mainnetDraft);Preferences(applicationContext).rpc("devnet",devnetDraft)
                                review=null;reviewedFee=false;solError="Stale — provider changed";skrError="Stale — provider changed";historyError="Stale — provider changed";nested="Workspace"
                            }}) {Text(stringResource(R.string.ui_da7a8919a179))}
                        } else if(nested=="Tasks") {
                            TextButton(onClick={nested="Workspace"}) {Text(stringResource(R.string.ui_0f0c0c6e215e))}
                            Text(stringResource(R.string.ui_b3a60e61a523),style=MaterialTheme.typography.headlineMedium)
                            OutlinedTextField(taskTitle,{taskTitle=it},label={Text(stringResource(R.string.ui_11622e0f259d))})
                            OutlinedTextField(taskRequirements,{taskRequirements=it},label={Text(stringResource(R.string.ui_75efd228febb))})
                            Button(enabled=!busy,onClick={run { val t=dao.createTask(taskTitle,taskRequirements.lines().filter { it.isNotBlank() }); taskTitle=""; loadEvidence(t) }}) {Text(stringResource(R.string.ui_6f541e1b2531))}
                            OutlinedTextField(taskSearch,{taskSearch=it},label={Text(stringResource(R.string.ui_46c6f1dea7e9))})
                            if(tasks.isEmpty()) Text(stringResource(R.string.ui_cca8d533c8d5))
                            tasks.filter { it.title.contains(taskSearch,true) }.forEach { t -> Button(onClick={run { loadEvidence(t) }}) {Text(t.title)} }
                        } else {
                            TextButton(onClick={nested="Workspace"}) {Text(stringResource(R.string.ui_0f0c0c6e215e))}
                            Text(selectedTask?.title ?: stringResource(R.string.ui_7fb47c90e2b1),style=MaterialTheme.typography.headlineSmall)
                            Text(stringResource(R.string.ui_482c300c5ab9))
                            selectedTask?.let { t ->
                                val checklist=Json.parse(t.checklist)
                                checklist.forEachIndexed { i,item -> Row { Checkbox(item["done"].asBoolean(),onCheckedChange={ checked -> run {
                                    val revised=Json.parse(t.checklist); (revised[i] as ObjectNode).put("done",checked); val updated=t.copy(checklist=Json.mapper.writeValueAsString(revised)); dao.update(updated); selectedTask=updated
                                } }); Text(item["text"].asText(),Modifier.padding(top=12.dp)) } }
                            }
                            OutlinedTextField(note,{note=it},label={Text(stringResource(R.string.ui_f3544034a726))},modifier=Modifier.fillMaxWidth())
                            selectedTask?.let { t -> Button(onClick={run { val updated=t.copy(note=note); dao.update(updated); selectedTask=updated }}) {Text(stringResource(R.string.ui_f9457847a5c9))} }
                            key(selectedTask?.id) {LocationConsent {location=it}}
                            selectedTask?.let {t ->
                                MappingPlanner(dao,t) {textExport=it;export.launch("deproof-mapping-${t.id}.json")}
                                TaskReminder(dao,t)
                            }
                            Button(enabled=!busy,onClick={importFile.launch(arrayOf("*/*"))}) {Text(stringResource(R.string.ui_824ca255db35))}
                            Button(enabled=!busy,onClick={
                                val dir=File(filesDir,"evidence").apply {mkdirs()}; val file=File(dir,"capture-${UUID.randomUUID()}.jpg"); file.createNewFile()
                                val uri=FileProvider.getUriForFile(this@MainActivity,"$packageName.files",file); cameraUriText=uri.toString()
                                try {capture.launch(uri)} catch(e:Exception) {cameraUriText=null; file.delete(); error="NO_CAMERA_APP"}
                            }) {Text(stringResource(R.string.ui_d8622146000f))}
                            Text(stringResource(R.string.ui_12f5cea86252))
                            fileMeta.forEach { f -> Panel(stringResource(R.string.ui_455fc3345560 ,(f.provenance).toString(),(f.mime).toString())) {
                                Text(stringResource(R.string.ui_83189e704c76 ,(f.byteLength).toString())); Value(stringResource(R.string.ui_bbd07c4fc02c),f.sha256)
                                var preview by remember(f.id) {mutableStateOf(false)}
                                Button(onClick={preview=!preview}) {Text(if(preview) stringResource(R.string.ui_7d8ab368210c) else stringResource(R.string.ui_7a8d5ef5fd94))}
                                if(preview) EvidencePreview(File(filesDir,"evidence"),f)
                                Button(onClick={run { val h=withContext(Dispatchers.IO) { File(filesDir,"evidence/${f.id}").inputStream().use {sha256File(it)} }; ensure(h.sha256==f.sha256 && h.byteLength==f.byteLength,"FILE_CHANGED"); error="Raw-file integrity matches" }}) {Text(stringResource(R.string.ui_efd2fd8c45ce))}
                                Button(onClick={run { dao.deleteAttachment(f.id); withContext(Dispatchers.IO) { ensure(File(filesDir,"evidence/${f.id}").delete(),"FILE_DELETE_FAILED") }; fileMeta=fileMeta.filter {it.id!=f.id} }}) {Text(stringResource(R.string.ui_cd00b7556cc9))}
                            } }
                            Button(enabled=!busy,onClick={run {
                                val manifest=canonicalEvidence(fileMeta,note,selectedTask?.id,null,Instant.now().toString(),consentedLocation(location,location!=null,LocationCapture(applicationContext).permissionAvailable()))
                                val r=receipt("OBSERVED",null,null,"Unsigned evidence-only manifest",taskId=selectedTask?.id,manifest=manifest); dao.saveReceipt(r); route="Receipts"
                            }}) {Text(stringResource(R.string.ui_395e596e55db))}
                            Button(enabled=!busy,onClick={run {
                                val manifest=canonicalEvidence(fileMeta,note,selectedTask?.id,null,Instant.now().toString(),consentedLocation(location,location!=null,LocationCapture(applicationContext).permissionAvailable()))
                                val signature=withContext(Dispatchers.IO) {EvidenceSigner().signEvidenceEnvelope(manifest)}
                                dao.saveReceipt(receipt("LOCAL_EVIDENCE_SIGNED",null,null,"Evidence signed locally",taskId=selectedTask?.id,manifest=manifest,localSignature=signature)); route="Receipts"
                            }}) {Text(stringResource(R.string.ui_c6e7b6cfa940))}
                        }
                        "Review" -> {
                            val r=review
                            if(r==null) {
                                Text(stringResource(R.string.ui_fa4e91263d40))
                                OutlinedTextField(rawDraft,{rawDraft=it},label={Text(stringResource(R.string.ui_b5363651b5f3))},modifier=Modifier.fillMaxWidth())
                                Button(enabled=!busy,onClick={run {
                                    val bytes=Base64.getDecoder().decode(rawDraft); val tx=parseTransaction(bytes)
                                    review=Review(bytes,ReviewContext(tx.keys.first().address,"mainnet-beta","unvalidated-import",Policy.SKR_TRANSFER_V1,reviewedAt=Instant.now().toString(),lastValidBlockHeight=null,feeLamports=null,feeSlot=null,historical=true))
                                }}) {Text(stringResource(R.string.ui_447fd6dfcc39))}
                            } else {
                                Text(if(r.context.historical) stringResource(R.string.ui_7b436823cbeb) else stringResource(R.string.ui_2fa6b828e5fd),style=MaterialTheme.typography.headlineSmall)
                                val verdicts=evaluateAllInstructions(r.tx.instructions,r.context.policy)
                                Text(if(verdicts.all {it.allowed}) stringResource(R.string.ui_e2ff06eb2f5d) else stringResource(R.string.ui_26700a6bfc51))
                                r.tx.instructions.forEachIndexed { i,ix -> Panel(stringResource(R.string.ui_3988f525a687 ,(i+1).toString())) {
                                    Text(verdicts[i].summary); Text(verdicts[i].reason); Value(stringResource(R.string.ui_90920d93e2c7),ix.program)
                                    val effects=explainAccountEffects(ix,r.context.policy)
                                    Text(stringResource(R.string.ui_0f765c1d1f18 ,(effects.status).toString(),(effects.summary).toString()))
                                    Text(if(programHistoryError) stringResource(R.string.ui_93c9988a4175) else if(firstPrograms==null) stringResource(R.string.ui_7e6fbce38c27) else if(ix.program in firstPrograms!!) stringResource(R.string.ui_11cdcb68c591) else stringResource(R.string.ui_1c3c3c6cc7e7))
                                    ix.accounts.forEachIndexed { j,a -> Value(stringResource(R.string.ui_4557a8e579d5 ,(j).toString(),(a.signer).toString(),(a.writable).toString()),a.address) }
                                    Value(stringResource(R.string.ui_28740dbb46f8),ix.data.joinToString(""){"%02x".format(it.toInt() and 255)})
                                } }
                                Value(stringResource(R.string.ui_7e1b0d5641f2),r.context.account); Value(stringResource(R.string.ui_c137a7f43e23),r.context.cluster); Value(stringResource(R.string.ui_81ddc8d248b2),r.context.genesis)
                                Value(stringResource(R.string.ui_b5687cf04af3),r.context.requester); Value(stringResource(R.string.ui_ada08643a234),r.context.feeLamports ?: stringResource(R.string.fee_unknown))
                                Value(stringResource(R.string.ui_211a60331d01),r.context.feeSlot ?: stringResource(R.string.unknown))
                                Text(stringResource(R.string.ui_326bdd385465))
                                val explanation=explainPacket(r.unsignedBytes(),false,null)
                                Text(stringResource(R.string.ui_e4af2c5e7d6e ,(explanation.status).toString(),(explanation.reason).toString()))
                                Value(stringResource(R.string.ui_e843398ff8a4),r.messageSha256); Value(stringResource(R.string.ui_ff8182131c67),r.cardHash); Value(stringResource(R.string.ui_07645834b06d),r.contextHash)
                                if(!r.context.historical) {
                                    Row {Checkbox(reviewedFee,{reviewedFee=it}); Text(stringResource(R.string.ui_eeef9d50c514),Modifier.padding(top=12.dp))}
                                    Button(enabled=!busy && reviewedFee && account==r.context.account && wallet.strict && verdicts.all {it.allowed},onClick={run {
                                        val fresh=withContext(Dispatchers.IO) { devnet.blockhashValid(r.tx.blockhash) }
                                        if(!fresh) {
                                            ensure(memoRebuildAllowed(memoRebuilds),"BLOCKHASH_EXPIRED_RETRY_EXHAUSTED")
                                            val rebuilt=withContext(Dispatchers.IO) {
                                                val latest=devnet.latestDevnetBlockhash();val bytes=rebuildExpiredTransaction(r,latest["value"]["blockhash"].asText(),System.currentTimeMillis());val fee=devnet.estimateFee(parseTransaction(bytes).messageBytes());devnet.simulateTransaction(bytes)
                                                Review(bytes,r.context.copy(reviewedAt=Instant.now().toString(),lastValidBlockHeight=latest["value"]["lastValidBlockHeight"].asText(),feeLamports=fee["value"].asText(),feeSlot=fee["context"]["slot"].asText()))
                                            }
                                            review=rebuilt;reviewedFee=false;memoRebuilds++;throw Failure("BLOCKHASH_REBUILT_REVIEW_AGAIN")
                                        }
                                        val allowed=withContext(Dispatchers.IO) { val valid=devnet.blockhashValid(r.tx.blockhash); val liveFee=devnet.estimateFee(r.tx.messageBytes()); ensure(liveFee["value"].asText()==r.context.feeLamports,"FEE_CHANGED_REVIEW_AGAIN"); devnet.simulateTransaction(r.unsignedBytes()); canSign(r,r.unsignedBytes(),r.context,SigningGate(account,"devnet",wallet.strict,valid,reviewedFee,true,false)) }
                                        ensure(allowed,"FRESH_REVIEW_REQUIRED")
                                        val signed=wallet.sign(r); val sig=validateReturnedTransaction(r,signed,::verifyEd25519)
                                        val intent=receipt("WALLET_SIGNED",account,"devnet","Wallet signed; persisted submission intent",r,sig) as ObjectNode
                                        (intent["submission"] as ObjectNode).put("state","SUBMITTING").put("attemptedAt",Instant.now().toString())
                                        dao.saveReceipt(intent) // Durable identity before RPC send. Never auto-retry.
                                        try {
                                            withContext(Dispatchers.IO) {devnet.submitSignedTransaction(signed,sig)}
                                            dao.observe(Observation(UUID.randomUUID().toString(),intent["id"].asText(),Instant.now().toString(),Json.mapper.writeValueAsString(Json.obj("submissionState" to "SUBMITTED","broadcast" to true,"submittedByDeproof" to true,"rpcAcceptedAt" to Instant.now().toString(),"signature" to sig))))
                                            error="RPC accepted exact signed bytes. Confirmation has not been claimed."
                                        } catch(e: Exception) {
                                            dao.observe(Observation(UUID.randomUUID().toString(),intent["id"].asText(),Instant.now().toString(),Json.mapper.writeValueAsString(Json.obj("submissionState" to "SUBMISSION_UNKNOWN","broadcast" to null,"submittedByDeproof" to null,"signature" to sig))))
                                            error="SUBMISSION_UNKNOWN — inspect the known signature; do not resubmit blindly"
                                        }
                                        review=null; reviewedFee=false; route="Receipts"
                                    }}) {Text(stringResource(R.string.ui_8831ba432de4))}
                                    Text(stringResource(R.string.ui_a02b7dcc0922))
                                }
                                Button(enabled=!busy,onClick={run { dao.saveReceipt(receipt("REJECTED",r.context.account,r.context.cluster,"Rejected locally. Nothing submitted.",r)); review=null; reviewedFee=false; route="Receipts" }}) {Text(stringResource(R.string.ui_be6a0b16f878))}
                                TextButton(onClick={review=null; reviewedFee=false}) {Text(stringResource(R.string.ui_d974ac099d5c))}
                            }
                        }
                        "Receipts" -> {
                            Text(stringResource(R.string.ui_60a627df404a),style=MaterialTheme.typography.headlineMedium)
                            operations.filter {it.state=="OUTCOME_UNKNOWN"}.forEach {o -> Text(stringResource(R.string.ui_f149da278e03 ,(o.kind).toString()))}
                            contributions.forEach {c -> Panel(stringResource(R.string.ui_2d9223c2a64c)) {
                                Text(c.createdAt);Text(stringResource(R.string.ui_6f7e112ecce0 ,(c.nodeFingerprint).toString()))
                                Text(stringResource(R.string.ui_ff3a3b5c77d9))
                                SelectionContainer {Text(c.payload)}
                                Button(onClick={textExport=c.payload;export.launch("deproof-contribution-${c.id}.json")}) {Text(stringResource(R.string.ui_a7adb237f93c))}
                            }}
                            OutlinedTextField(receiptSearch,{receiptSearch=it},label={Text(stringResource(R.string.ui_f0348a847987))})
                            if(events.isEmpty()) Text(stringResource(R.string.ui_a5d65e8061cd))
                            events.filter { it.payload.contains(receiptSearch,true) }.forEach { e -> Panel(receiptLabel(e.kind)) {
                                val record=Json.parse(e.payload)
                                Text(record["summary"].asText()); Text(e.createdAt)
                                Text(stringResource(R.string.ui_d7a7630c5ba3 ,(record["submission"]).toString()))
                                var obs by remember(e.id) {mutableStateOf<List<Observation>>(emptyList())}
                                LaunchedEffect(e.id,events) {obs=dao.observations(e.id)}
                                obs.forEach { Text(stringResource(R.string.ui_f050898885e0 ,(it.observedAt).toString(),(it.payload).toString())) }
                                val sig=record["signature"].takeUnless {it.isNull}?.asText()
                                if(sig!=null) Button(enabled=!busy,onClick={run {
                                    val rpc=if(record["network"].asText()=="devnet") devnet else mainnet
                                    val observed=try { withContext(Dispatchers.IO) { rpc.observeSettlement(sig) }.let { Json.obj("availability" to "AVAILABLE","status" to if(it.isNull) "UNKNOWN" else if(!it["err"].isNull) "FAILED" else it["confirmationStatus"].asText("unknown").uppercase(),"raw" to it) } } catch(ex:Exception) {Json.obj("availability" to "UNAVAILABLE","status" to null,"error" to ((ex as? Failure)?.code ?: "RPC_UNAVAILABLE"))}
                                    dao.observe(Observation(UUID.randomUUID().toString(),e.id,Instant.now().toString(),Json.mapper.writeValueAsString(observed))); obs=dao.observations(e.id)
                                }}) {Text(stringResource(R.string.ui_f0073936f785))}
                                Button(onClick={copy(e.payload)}) {Text(stringResource(R.string.ui_b340bffbff30))}
                                Button(onClick={textExport=e.payload; export.launch("deproof-${e.id}.json")}) {Text(stringResource(R.string.ui_e3c86e9e9715))}
                                Button(onClick={run { val observations=dao.observations(e.id); val bundle=Json.obj("schema" to "deproof-portable-event-v1","event" to record,"observations" to observations.map { Json.obj("id" to it.id,"observedAt" to it.observedAt,"payload" to Json.parse(it.payload)) }); textExport=Json.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundle); export.launch("deproof-bundle-${e.id}.json") }}) {Text(stringResource(R.string.ui_6efa387632b5))}
                                Button(enabled=!busy,onClick={run {dao.deleteEvent(e.id)}}) {Text(stringResource(R.string.ui_92d57bed6f28))}
                            } }
                            textExport?.let { SelectionContainer { Text(it,fontFamily=FontFamily.Monospace) } }
                        }
                    }
                    Text(stringResource(R.string.ui_690d2f63efbe),color=colors.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text(title,style=MaterialTheme.typography.titleMedium); content()} } }
@Composable private fun Value(label: String, value: String) { Column {Text(label,style=MaterialTheme.typography.labelLarge); SelectionContainer {Text(value,fontFamily=FontFamily.Monospace)} } }
@Composable private fun BalanceView(label: String, balance: Balance?, error: String?) { Text(label,style=MaterialTheme.typography.titleMedium); if(balance!=null) {Value(stringResource(R.string.ui_55aac025cbb7),balance.address); Text(stringResource(R.string.ui_a0e39ec62364 ,(balance.value).toString(),(balance.slot).toString(),(balance.observedAt).toString()))}; if(error!=null) Text(stringResource(R.string.ui_13326cace603 ,(if(balance!=null) "Stale — last successful read shown" else "Unavailable").toString(),(error).toString())); if(balance==null && error==null) Text(stringResource(R.string.ui_d827812588bb)) }
