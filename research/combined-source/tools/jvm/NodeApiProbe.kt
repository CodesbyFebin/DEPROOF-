package com.example.probe

import com.example.domain.*
import com.example.data.NodeClient
import java.util.Base64

/** Test harness invokes the SAME client/protocol as Android over an actual TLS socket. */
fun main() {
    val request=Json.parse(System.`in`.bufferedReader().readText())
    val client=NodeClient(request["endpoint"].asText(),request["pin"].asText())
    val protocol=NodeProtocol(Base64.getDecoder().decode(request["seed"].asText()))
    val result=if(request["mode"].asText()=="pair") client.post("/pair",protocol.pair(request["challenge"],request["code"].asText(),request["fingerprint"].asText(),request["scopes"].map {it.asText()}))
        else client.post("/command",protocol.command(request["sessionId"].asText(),request["action"].asText(),request["params"],request["operationId"].asText()))
    if(result["schema"]?.asText()=="deproof-node-signed-v1") NodeProtocol.verifyRecord(result,request["fingerprint"].asText(),request["operationId"].asText())
    println(Json.mapper.writeValueAsString(result))
}
