package com.example.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// Decimal strings keep manifests portable; a reported coordinate is not proof of presence.
data class LocationObservation(val latitude: String,val longitude: String,val accuracyMeters: String,val provider: String,val observedAt: String,val collectedAt: String,val permission: String,val mock: Boolean) {
    fun validate() {
        fun decimal(s: String,min: String,max: String) {ensure(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]{1,12})?").matches(s),"BAD_LOCATION_DECIMAL");ensure(BigDecimal(s)>=BigDecimal(min) && BigDecimal(s)<=BigDecimal(max),"BAD_LOCATION_RANGE")}
        decimal(latitude,"-90","90");decimal(longitude,"-180","180");decimal(accuracyMeters,"0","100000")
        ensure(provider in setOf("gps","network","fused"),"BAD_LOCATION_PROVIDER")
        ensure(permission in setOf("APPROXIMATE","PRECISE"),"BAD_LOCATION_PERMISSION")
        val observed=Instant.parse(observedAt);val collected=Instant.parse(collectedAt)
        ensure(!observed.isAfter(collected.plusSeconds(5)) && !observed.isBefore(collected.minusSeconds(120)),"LOCATION_STALE")
    }
}
fun consentedLocation(observation: LocationObservation?, consent: Boolean, permissionAvailable: Boolean): LocationObservation? {
    if(!consent)return null
    ensure(permissionAvailable,"LOCATION_PERMISSION_DENIED")
    observation?.validate();return observation
}
fun parseLocationObservation(n: com.fasterxml.jackson.databind.JsonNode): LocationObservation {
    val fields=setOf("latitude","longitude","accuracyMeters","provider","observedAt","collectedAt","permission","mock")
    ensure(n.isObject && n.fieldNames().asSequence().toSet()==fields,"BAD_LOCATION_FIELDS")
    fields.filter {it!="mock"}.forEach {ensure(n[it].isTextual,"BAD_LOCATION_FIELD")};ensure(n["mock"].isBoolean,"BAD_LOCATION_FIELD")
    return LocationObservation(n["latitude"].textValue(),n["longitude"].textValue(),n["accuracyMeters"].textValue(),n["provider"].textValue(),n["observedAt"].textValue(),n["collectedAt"].textValue(),n["permission"].textValue(),n["mock"].booleanValue()).also {it.validate()}
}

data class MappingPoint(val label: String,val latitude: String,val longitude: String)
data class MappingPlan(val id: String,val taskId: String,val title: String,val points: List<MappingPoint>,val createdAt: String,val schema: String="deproof-mapping-plan-v1") {
    fun validate() {
        ensure(schema=="deproof-mapping-plan-v1","BAD_MAPPING_SCHEMA");UUID.fromString(id);UUID.fromString(taskId);Instant.parse(createdAt)
        ensure(title.isNotBlank() && title.length<=200 && points.size in 1..100,"BAD_MAPPING_PLAN")
        points.forEach {
            ensure(it.label.isNotBlank() && it.label.length<=200,"BAD_MAPPING_LABEL")
            LocationObservation(it.latitude,it.longitude,"0","gps",createdAt,createdAt,"APPROXIMATE",false).validate()
        }
    }
    fun canonical(): ByteArray {validate();return Json.canonical(Json.mapper.valueToTree(this))}
}
fun parseMappingPlan(raw: String): MappingPlan {
    val n=Json.parse(raw);ensure(n.isObject && n.fieldNames().asSequence().toSet()==setOf("id","taskId","title","points","createdAt","schema"),"BAD_MAPPING_FIELDS")
    ensure(n["points"].isArray,"BAD_MAPPING_POINTS")
    val points=n["points"].map {p -> ensure(p.isObject && p.fieldNames().asSequence().toSet()==setOf("label","latitude","longitude") && p.all {it.isTextual},"BAD_MAPPING_POINT");MappingPoint(p["label"].textValue(),p["latitude"].textValue(),p["longitude"].textValue())}
    for(f in listOf("id","taskId","title","createdAt","schema"))ensure(n[f].isTextual,"BAD_MAPPING_FIELD")
    return MappingPlan(n["id"].textValue(),n["taskId"].textValue(),n["title"].textValue(),points,n["createdAt"].textValue(),n["schema"].textValue()).also {it.validate()}
}
fun parseMappingPoints(raw: String): List<MappingPoint> {
    ensure(raw.length<=65536,"MAPPING_TOO_LARGE")
    return raw.lines().filter {it.isNotBlank()}.map {line -> val p=line.split('|');ensure(p.size==3,"MAPPING_POINT_FORMAT");MappingPoint(p[0].trim(),p[1].trim(),p[2].trim())}
}

data class LocalReminder(val id: String,val taskId: String,val dueAt: String,val state: String,val title: String,val updatedAt: String,val generation: String,val schema: String="deproof-local-reminder-v1") {
    fun validate() {
        ensure(schema=="deproof-local-reminder-v1","BAD_REMINDER_SCHEMA");UUID.fromString(id);UUID.fromString(taskId);UUID.fromString(generation)
        ensure(state in setOf("PENDING","SCHEDULED","POST_REQUESTED","PERMISSION_DENIED","CANCELLED","EXPIRED","SCHEDULE_FAILED"),"BAD_REMINDER_STATE")
        ensure(title.isNotBlank() && title.length<=200,"BAD_REMINDER_TITLE");Instant.parse(dueAt);Instant.parse(updatedAt)
    }
}
fun newReminder(taskId: String,title: String,dueAt: String,consent: Boolean,notificationsAvailable: Boolean,now: Instant): LocalReminder {
    ensure(consent,"REMINDER_CONSENT_REQUIRED");ensure(dueAt.endsWith("Z"),"REMINDER_TIME_MUST_BE_UTC");val due=Instant.parse(dueAt)
    ensure(due.isAfter(now) && !due.isAfter(now.plusSeconds(365L*86400)),"BAD_REMINDER_TIME")
    return LocalReminder(UUID.nameUUIDFromBytes(("deproof-reminder-v1:"+taskId).toByteArray(Charsets.UTF_8)).toString(),taskId,dueAt,if(notificationsAvailable) "PENDING" else "PERMISSION_DENIED",title,now.toString(),UUID.randomUUID().toString()).also {it.validate()}
}
fun reminderRecovery(r: LocalReminder,now: Instant,enabled: Boolean,permission: Boolean): String {
    r.validate()
    if(!enabled || r.state=="CANCELLED")return "CANCELLED"
    if(r.state=="POST_REQUESTED")return "POST_REQUESTED" // Never silently replay an uncertain notification.
    if(!permission)return "PERMISSION_DENIED"
    return if(!Instant.parse(r.dueAt).isAfter(now)) "EXPIRED" else "PENDING"
}
fun parseReminder(raw: String): LocalReminder {
    val n=Json.parse(raw);ensure(n.isObject && n.fieldNames().asSequence().toSet()==setOf("id","taskId","dueAt","state","title","updatedAt","generation","schema") && n.all {it.isTextual},"BAD_REMINDER_FIELDS")
    return LocalReminder(n["id"].textValue(),n["taskId"].textValue(),n["dueAt"].textValue(),n["state"].textValue(),n["title"].textValue(),n["updatedAt"].textValue(),n["generation"].textValue(),n["schema"].textValue()).also {it.validate()}
}
fun reminderMayPost(r: LocalReminder,generation: String,now: Instant,enabled: Boolean,permission: Boolean): Boolean {
    r.validate();return enabled && permission && generation==r.generation && r.state in setOf("PENDING","SCHEDULED") && !now.isBefore(Instant.parse(r.dueAt)) && now.isBefore(Instant.parse(r.dueAt).plusSeconds(86400))
}
