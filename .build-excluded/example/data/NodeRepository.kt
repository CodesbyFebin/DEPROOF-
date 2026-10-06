package com.example.data

import com.example.domain.*
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import java.time.Instant
import java.util.UUID

class NodeRepository(private val dao: RecordsDao) {
    suspend fun pair(endpoint: String,pin: String,challenge: JsonNode,code: String,fingerprint: String,scopes: List<String>)=withContext(Dispatchers.IO) {
        val keys=NodeKeyStore();val encrypted=keys.create();val protocol=keys.protocol(encrypted)
        val response=NodeClient(endpoint,pin).post("/pair",protocol.pair(challenge,code,fingerprint,scopes))
        val id=response["sessionId"]?.asText() ?: throw Failure("BAD_PAIR_RESPONSE")
        ensure(id.matches(Regex("[0-9a-f]{48}")),"BAD_PAIR_RESPONSE")
        dao.pair(NodeSession(id,endpoint,pin,fingerprint,encrypted,Json.mapper.writeValueAsString(scopes),"PAIRED"))
    }
    suspend fun proof(session: NodeSession,job: JsonNode,observation: JsonNode,consent: Boolean): JsonNode {
        ensure(consent,"PROOF_CONSENT_REQUIRED")
        ensure(observation["fingerprint"]?.asText()==session.fingerprint,"NODE_FINGERPRINT_MISMATCH")
        val match=proofDispatchDecision(job,observation);ensure(match.state=="COMPATIBLE_LOCAL_PROFILE","PROOF_"+match.state+":"+match.reasons.joinToString(","))
        return command(session,"proof",Json.obj("jobId" to job["jobId"].asText(),"explicitConsent" to true))
    }
    suspend fun command(session: NodeSession,action: String,params: JsonNode): JsonNode=withContext(Dispatchers.IO) {
        ensure(session.state=="PAIRED","NODE_REPAIR_REQUIRED")
        val id=UUID.randomUUID().toString()
        val signed=NodeKeyStore().protocol(session.encryptedSeed).command(session.id,action,params,id)
        val op=Operation(id,"NODE_"+action,session.id,"RUNNING",Json.mapper.writeValueAsString(signed),null,Instant.now().toString())
        dao.operation(op) // Durable dispatch intent BEFORE network I/O. Never auto replay.
        try {
            val result=NodeClient(session.endpoint,session.certificateSha256).post("/command",signed)
            if(action=="observe") ensure(result["fingerprint"]?.asText()==session.fingerprint,"NODE_FINGERPRINT_MISMATCH")
            dao.completeNode(op,result,session.fingerprint)
            if(action=="revoke") dao.sessionState(session.id,"REVOKED")
            result
        } catch(e: Exception) {
            withContext(NonCancellable) {dao.operationState(id,"OUTCOME_UNKNOWN",Json.mapper.writeValueAsString(Json.obj("error" to ((e as? Failure)?.code ?: "NODE_UNAVAILABLE"),"recovery" to "OBSERVE_STATUS_DO_NOT_REPLAY")),Instant.now().toString())}
            if(e is Failure && e.code=="SESSION_REVOKED_OR_UNKNOWN") dao.sessionState(session.id,"REPAIR_REQUIRED")
            throw e
        }
    }
}
