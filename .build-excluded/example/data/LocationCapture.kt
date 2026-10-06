package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.domain.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationCapture(private val context: Context) {
    fun permissionAvailable()=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    suspend fun once(consent: Boolean): LocationObservation {
        ensure(consent,"LOCATION_CONSENT_REQUIRED");ensure(permissionAvailable(),"LOCATION_PERMISSION_DENIED")
        val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider=listOf(LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER).firstOrNull {manager.isProviderEnabled(it)} ?: throw Failure("LOCATION_PROVIDER_DISABLED_OR_OFFLINE")
        return withTimeout(30000) {suspendCancellableCoroutine {cont ->
            val cancel=CancellationSignal();cont.invokeOnCancellation {cancel.cancel()}
            try {
                LocationManagerCompat.getCurrentLocation(manager,provider,cancel,ContextCompat.getMainExecutor(context)) {loc ->
                    if(!cont.isActive)return@getCurrentLocation
                    try {
                        ensure(permissionAvailable(),"LOCATION_PERMISSION_REVOKED")
                        ensure(loc!=null,"LOCATION_UNAVAILABLE");val l=loc!!
                        ensure(l.hasAccuracy() && l.accuracy.isFinite() && l.latitude.isFinite() && l.longitude.isFinite(),"LOCATION_INVALID")
                        val age=SystemClock.elapsedRealtimeNanos()-l.elapsedRealtimeNanos
                        ensure(age in 0..120_000_000_000L,"LOCATION_STALE")
                        fun decimal(value: Double)=BigDecimal.valueOf(value).setScale(7,java.math.RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()
                        val result=LocationObservation(decimal(l.latitude),decimal(l.longitude),decimal(l.accuracy.toDouble()),provider,Instant.ofEpochMilli(l.time).toString(),Instant.now().toString(),if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) "PRECISE" else "APPROXIMATE",l.isFromMockProvider)
                        result.validate();cont.resume(result)
                    } catch(e:Exception) {cont.resumeWithException(e)}
                }
            } catch(e:SecurityException) {cont.resumeWithException(Failure("LOCATION_PERMISSION_DENIED"))}
        }}
    }
}
