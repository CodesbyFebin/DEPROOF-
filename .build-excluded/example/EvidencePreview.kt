package com.example

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.deproof.app.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun EvidencePreview(directory: File, evidence: EvidenceFile) {
    var text by remember(evidence) {mutableStateOf<String?>(null)}
    var bitmap by remember(evidence) {mutableStateOf<android.graphics.Bitmap?>(null)}
    var error by remember(evidence) {mutableStateOf<String?>(null)}
    LaunchedEffect(evidence) {
        try {
            withContext(Dispatchers.IO) {
                val file=verifiedEvidenceFile(directory,evidence.id,evidence.sha256,evidence.byteLength)
                when(previewKind(evidence.mime)) {
                    PreviewKind.TEXT -> {ensure(file.length()<=65536,"PREVIEW_TOO_LARGE");text=previewText(file.readBytes())}
                    PreviewKind.IMAGE -> {
                        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeFile(file.path,bounds)
                        ensure(bounds.outWidth in 1..32768 && bounds.outHeight in 1..32768,"PREVIEW_INVALID_IMAGE")
                        var sample=1;while(bounds.outWidth/sample>1024 || bounds.outHeight/sample>1024)sample*=2
                        bitmap=BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply {inSampleSize=sample}) ?: throw Failure("PREVIEW_INVALID_IMAGE")
                    }
                    PreviewKind.UNSUPPORTED -> throw Failure("PREVIEW_UNSUPPORTED_TYPE")
                }
            }
        } catch(e:CancellationException) {throw e} catch(e:Exception) {error=(e as? Failure)?.code ?: "PREVIEW_UNAVAILABLE"}
    }
    text?.let {SelectionContainer {Text(it)}}
    bitmap?.let {Image(it.asImageBitmap(),stringResource(R.string.preview_description),Modifier.fillMaxWidth().heightIn(max=400.dp))}
    error?.let {Text(stringResource(R.string.ui_2d985bcd1633 ,(it).toString()))}
    if(text==null && bitmap==null && error==null)Text(stringResource(R.string.ui_b2a4d83d5921))
}
