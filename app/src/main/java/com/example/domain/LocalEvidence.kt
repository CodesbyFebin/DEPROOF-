package com.example.domain

import java.io.File
import java.nio.ByteBuffer

enum class PreviewKind { TEXT, IMAGE, UNSUPPORTED }
fun previewKind(mime: String): PreviewKind = when(mime.lowercase()) {
    "text/plain", "text/markdown", "application/json" -> PreviewKind.TEXT
    "image/jpeg", "image/png", "image/webp" -> PreviewKind.IMAGE
    else -> PreviewKind.UNSUPPORTED
}
fun verifiedEvidenceFile(directory: File, id: String, digest: String, length: String): File {
    ensure(Regex("[A-Za-z0-9_-]{1,128}").matches(id),"BAD_EVIDENCE_ID")
    val file=File(directory,id)
    ensure(file.canonicalFile.parentFile==directory.canonicalFile && file.isFile,"CONTENT_UNAVAILABLE")
    val hash=file.inputStream().use {sha256File(it)}
    ensure(hash.sha256==digest && hash.byteLength==length,"FILE_CHANGED")
    return file
}
fun previewText(bytes: ByteArray): String {
    ensure(bytes.size<=65536,"PREVIEW_TOO_LARGE")
    return try {Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()}
    catch(e:Exception) {throw Failure("PREVIEW_INVALID_UTF8")}
}
fun legacyDigestBytes(digest: ByteArray): ByteArray {
    ensure(digest.size==32,"LEGACY_DIGEST_SIZE")
    return digest.copyOf() // SHA256withECDSA signs these raw bytes; never hex/base64 text.
}
fun firstSeenPrograms(programs: List<String>, known: Set<String>): Set<String> {
    programs.forEach {Base58.pubkey(it)}
    return programs.filterNot {it in known}.toSet()
}
