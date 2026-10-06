package com.example.data

import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import java.security.cert.X509Certificate
import javax.net.ssl.*

/** Owner pins the exact TLS certificate separately from the node Ed25519 fingerprint. */
class NodeClient(val endpoint: String, private val certificateSha256: String) {
    init {
        val uri=URI(endpoint)
        ensure(uri.scheme=="https" && uri.host in listOf("localhost","127.0.0.1") && uri.userInfo==null && uri.query==null && uri.fragment==null && uri.path in listOf("","/"),"NODE_ENDPOINT_DENIED")
        ensure(certificateSha256.matches(Regex("[0-9a-f]{64}")),"BAD_TLS_PIN")
    }
    fun post(path: String, body: JsonNode): JsonNode {
        ensure(path in setOf("/pair","/command"),"NODE_ROUTE_DENIED")
        val trust=object:X509TrustManager {
            override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain: Array<X509Certificate>, auth: String)=throw java.security.cert.CertificateException("CLIENT_CERT_UNSUPPORTED")
            override fun checkServerTrusted(chain: Array<X509Certificate>, auth: String) {
                if(chain.isEmpty() || sha256Hex(chain[0].encoded)!=certificateSha256) throw java.security.cert.CertificateException("TLS_PIN_MISMATCH")
                chain[0].checkValidity()
            }
        }
        val tls=SSLContext.getInstance("TLS").apply {init(null,arrayOf(trust),null)}
        val connection=URI(endpoint.trimEnd('/')+path).toURL().openConnection() as HttpsURLConnection
        connection.sslSocketFactory=tls.socketFactory // Default hostname verifier remains enabled.
        try {
            connection.requestMethod="POST";connection.doOutput=true;connection.instanceFollowRedirects=false;connection.connectTimeout=5000;connection.readTimeout=65000
            connection.setRequestProperty("Content-Type","application/json")
            val bytes=Json.mapper.writeValueAsBytes(body);ensure(bytes.size<=65536,"NODE_REQUEST_TOO_LARGE")
            connection.outputStream.use {it.write(bytes)}
            val status=connection.responseCode
            val input=if(status in 200..299) connection.inputStream else connection.errorStream
            val raw=input?.use {stream -> val out=java.io.ByteArrayOutputStream();val buf=ByteArray(8192);while(true){val n=stream.read(buf);if(n<0)break;ensure(out.size()+n<=2*1024*1024,"NODE_RESPONSE_TOO_LARGE");out.write(buf,0,n)};out.toString("UTF-8")} ?: throw Failure("NODE_UNAVAILABLE")
            val result=Json.parse(raw)
            if(status !in 200..299) throw Failure(result["error"]?.asText()?.takeIf { it.matches(Regex("[A-Z_0-9]+")) } ?: "NODE_COMMAND_FAILED")
            return result
        } finally {connection.disconnect()}
    }
}
