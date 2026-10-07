package com.example

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.deproof.app.R
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.net.Uri
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.*

@Composable fun BackupWorkspace(db: DeproofDatabase, busy: Boolean, onBusy: (Boolean)->Unit,onRestored: ()->Unit) {
    if(android.os.Build.VERSION.SDK_INT<26) {Text("BACKUP_CRYPTO_UNAVAILABLE_ON_ANDROID_BELOW_26");return}
    val context=LocalContext.current;val scope=rememberCoroutineScope();val repository=remember {BackupRepository(db,context.filesDir)}
    var passphrase by remember {mutableStateOf("")};var consent by remember {mutableStateOf(false)}
    var selected by remember {mutableStateOf<Uri?>(null)};var encrypted by remember {mutableStateOf<ByteArray?>(null)}
    var outcome by remember {mutableStateOf<String?>(null)}
    fun work(block: suspend ()->Unit) {if(busy)return;onBusy(true);scope.launch {outcome=null;try {block()} catch(e:CancellationException){throw e} catch(e:Exception){outcome=(e as? Failure)?.code ?: "BACKUP_UNAVAILABLE"} finally {passphrase="";consent=false;onBusy(false)}}}
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {uri ->
        val bytes=encrypted;encrypted=null
        if(uri!=null && bytes!=null)work {try {withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri,"w")?.use {it.write(bytes);it.flush()} ?: throw Failure("BACKUP_EXPORT_UNAVAILABLE")};outcome="BACKUP_EXPORTED"}finally{bytes.fill(0)}} else bytes?.fill(0)
    }
    val select=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {selected=it;consent=false}
    DisposableEffect(Unit){onDispose{encrypted?.fill(0)}}
    Text(stringResource(R.string.backup_title),style=MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.backup_scope))
    OutlinedTextField(passphrase,{passphrase=it;consent=false},label={Text(stringResource(R.string.backup_passphrase))},visualTransformation=PasswordVisualTransformation(),singleLine=true)
    Row {Checkbox(consent,{consent=it},enabled=!busy);Text(stringResource(R.string.backup_consent))}
    Button(enabled=consent && passphrase.length>=12 && !busy,onClick={work {encrypted=repository.export(passphrase.toCharArray());save.launch("deproof-private-backup.dpbk")}}) {Text(stringResource(R.string.backup_export))}
    Button(enabled=!busy,onClick={select.launch(arrayOf("application/octet-stream","*/*"))}) {Text(stringResource(R.string.backup_select))}
    if(selected!=null)Text(stringResource(R.string.backup_selected))
    Button(enabled=selected!=null && consent && passphrase.length>=12 && !busy,onClick={work {
        onRestored() // Invalidate signing approval even if provider I/O or restore is interrupted.
        val uri=selected ?: throw Failure("BACKUP_SOURCE_UNAVAILABLE")
        val bytes=withContext(Dispatchers.IO) {context.contentResolver.openInputStream(uri)?.use {input -> val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(65536);while(true){currentCoroutineContext().ensureActive();val n=input.read(buffer);if(n<0)break;ensure(output.size().toLong()+n<=BackupRepository.MAX_BYTES+64L,"BACKUP_TOO_LARGE");output.write(buffer,0,n)};output.toByteArray()} ?: throw Failure("BACKUP_SOURCE_UNAVAILABLE")}
        try {repository.restore(bytes,passphrase.toCharArray());selected=null;onRestored();outcome="BACKUP_RESTORED_NO_NETWORK_REPLAY"}finally{bytes.fill(0)}
    }}) {Text(stringResource(R.string.backup_restore))}
    outcome?.let {Text(it)}
}
