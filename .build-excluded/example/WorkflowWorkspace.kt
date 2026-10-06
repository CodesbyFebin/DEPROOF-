package com.example

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.*
import java.time.Instant
import java.util.UUID

@Composable fun LocationConsent(onObservation: (LocationObservation?) -> Unit) {
    val context=LocalContext.current;val capture=remember {LocationCapture(context)};val scope=rememberCoroutineScope()
    var consent by remember {mutableStateOf(false)};var result by remember {mutableStateOf<LocationObservation?>(null)}
    var error by remember {mutableStateOf<String?>(null)};var job by remember {mutableStateOf<Job?>(null)}
    var generation by remember {mutableStateOf(0L)}
    fun collect() {
        if(!consent)return
        generation++;val ticket=generation;job?.cancel();result=null;onObservation(null);error=null
        job=scope.launch {
            try {val r=capture.once(consent);if(ticket==generation && consent && capture.permissionAvailable()) {result=r;onObservation(r)}}
            catch(e:TimeoutCancellationException) {error="LOCATION_TIMEOUT_OR_OFFLINE"}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {error=(e as? Failure)?.code ?: "LOCATION_UNAVAILABLE"}
            finally {if(ticket==generation)job=null}
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted -> if(granted && consent)collect() else {result=null;onObservation(null);error="LOCATION_PERMISSION_DENIED"}}
    DisposableEffect(Unit) {onDispose {generation++;job?.cancel();onObservation(null)}}
    Text(stringResource(R.string.ui_e4a446e2c795),style=MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.ui_04a27e1e5c8c))
    Row {Checkbox(consent,{consent=it;if(!it){generation++;job?.cancel();job=null;result=null;onObservation(null)}});Text(stringResource(R.string.ui_de9dbdd316e4),Modifier.padding(top=12.dp))}
    Button(enabled=consent && job==null,onClick={if(capture.permissionAvailable())collect() else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)}) {Text(stringResource(R.string.ui_c878737f9f23))}
    if(job!=null)Button(onClick={generation++;job?.cancel();job=null;result=null;onObservation(null);error="LOCATION_CANCELLED"}) {Text(stringResource(R.string.ui_9bda5fc4d160))}
    result?.let {Text(stringResource(R.string.ui_99002b892711 ,(it.permission).toString(),(it.provider).toString(),(it.accuracyMeters).toString(),(it.mock).toString()));SelectionContainer {Text(stringResource(R.string.ui_f807cc045a83 ,(it.latitude).toString(),(it.longitude).toString(),(it.observedAt).toString()))}}
    error?.let {Text(it)}
}

@Composable fun MappingPlanner(dao: RecordsDao,task: Task,onExport: (String) -> Unit) {
    val scope=rememberCoroutineScope();var title by remember(task.id) {mutableStateOf(task.title)}
    var points by remember(task.id) {mutableStateOf("")};var saved by remember(task.id) {mutableStateOf<MappingPlan?>(null)}
    var error by remember(task.id) {mutableStateOf<String?>(null)};var busy by remember(task.id) {mutableStateOf(false)}
    LaunchedEffect(task.id) {try {dao.draft("mapping:"+task.id)?.let {saved=parseMappingPlan(it.payload);title=saved!!.title;points=saved!!.points.joinToString("\n") {p -> "${p.label}|${p.latitude}|${p.longitude}"}}} catch(e:Exception){error="MAPPING_STORED_RECORD_INVALID"}}
    Text(stringResource(R.string.ui_506b681fa9b8),style=MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.ui_ff6bfa85cd68))
    OutlinedTextField(title,{title=it},label={Text(stringResource(R.string.ui_5395a99dcfc6))},modifier=Modifier.fillMaxWidth())
    OutlinedTextField(points,{points=it},label={Text(stringResource(R.string.ui_0d45a9b6852c))},modifier=Modifier.fillMaxWidth())
    Button(enabled=!busy,onClick={scope.launch {busy=true;error=null;try {
        val at=Instant.now().toString();val p=MappingPlan(saved?.id ?: UUID.randomUUID().toString(),task.id,title,parseMappingPoints(points),saved?.createdAt ?: at);p.validate()
        dao.draft(Draft("mapping:"+task.id,p.canonical().toString(Charsets.UTF_8),at));saved=p
    } catch(e:Exception){error=(e as? Failure)?.code ?: "MAPPING_SAVE_FAILED"}finally{busy=false}}}) {Text(stringResource(R.string.ui_a3d0dd4aedcb))}
    saved?.let {p ->
        Text(pluralStringResource(R.plurals.saved_waypoints,p.points.size,p.points.size))
        Button(onClick={onExport(p.canonical().toString(Charsets.UTF_8))}) {Text(stringResource(R.string.ui_199875b36a89))}
    }
    error?.let {Text(it)}
}

@Composable fun TaskReminder(dao: RecordsDao,task: Task) {
    val context=LocalContext.current;val repository=remember {ReminderRepository(context,dao)};val scope=rememberCoroutineScope()
    var due by remember(task.id) {mutableStateOf("")};var consent by remember(task.id) {mutableStateOf(false)}
    var record by remember(task.id) {mutableStateOf<LocalReminder?>(null)};var error by remember(task.id) {mutableStateOf<String?>(null)}
    var busy by remember(task.id) {mutableStateOf(false)}
    val id=UUID.nameUUIDFromBytes(("deproof-reminder-v1:"+task.id).toByteArray(Charsets.UTF_8)).toString()
    LaunchedEffect(task.id) {try {dao.draft("reminder:"+id)?.let {record=parseReminder(it.payload);due=record!!.dueAt}} catch(e:Exception){error="REMINDER_STORED_RECORD_INVALID"}}
    fun schedule() {
        if(!consent || busy)return
        scope.launch {busy=true;error=null;try {record=repository.schedule(task,due,consent);consent=false} catch(e:Exception) {error=(e as? Failure)?.code ?: "REMINDER_SCHEDULE_FAILED"} finally{busy=false}}
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {if(consent)schedule()}
    Text(stringResource(R.string.ui_ce5991839594),style=MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.ui_1295cc77a781))
    OutlinedTextField(due,{due=it;consent=false},label={Text(stringResource(R.string.ui_8bd3fb29b56a))},modifier=Modifier.fillMaxWidth())
    Row {Checkbox(consent,{consent=it});Text(stringResource(R.string.ui_6226def29190),Modifier.padding(top=12.dp))}
    Button(enabled=consent && !busy,onClick={if(Build.VERSION.SDK_INT>=33 && !repository.permissionAvailable())permission.launch(Manifest.permission.POST_NOTIFICATIONS) else schedule()}) {Text(stringResource(R.string.ui_7cfb47e027b0))}
    record?.let {r -> Text(stringResource(R.string.ui_53abd2e69946 ,(r.state).toString(),(r.dueAt).toString()));Button(enabled=!busy,onClick={scope.launch {busy=true;error=null;try {repository.cancel(r);record=r.copy(state="CANCELLED");consent=false} catch(e:CancellationException){throw e} catch(e:Exception){error="REMINDER_CANCEL_FAILED"} finally{busy=false}}}) {Text(stringResource(R.string.ui_1def9b637d1a))} }
    Button(enabled=!busy,onClick={scope.launch {busy=true;error=null;try {repository.disableAll();record=record?.copy(state="CANCELLED");consent=false} catch(e:CancellationException){throw e} catch(e:Exception){error="REMINDER_CANCEL_FAILED"} finally{busy=false}}}) {Text(stringResource(R.string.ui_13d896ddd934))}
    error?.let {Text(it)}
}
