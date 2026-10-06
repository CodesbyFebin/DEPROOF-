package com.example.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.languageContext
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import com.example.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class ReminderRepository(private val context: Context,private val dao: RecordsDao) {
    companion object {private val mutex=Mutex()}
    private val prefs=context.getSharedPreferences("local_reminders",Context.MODE_PRIVATE)
    fun enabled()=prefs.getBoolean("enabled",false)
    fun permissionAvailable(): Boolean {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled())return false
        return Build.VERSION.SDK_INT<26 || (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).getNotificationChannel("deproof-reminders")?.importance!=NotificationManager.IMPORTANCE_NONE
    }
    private fun intent(r: LocalReminder)=Intent(context,ReminderReceiver::class.java).setAction("com.aistudio.deproof.sdwk.REMINDER").setData(Uri.parse("deproof-reminder:${r.id}")).putExtra("id",r.id).putExtra("generation",r.generation)
    private fun pending(r: LocalReminder)=PendingIntent.getBroadcast(context,0,intent(r),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private suspend fun save(r: LocalReminder) {r.validate();dao.draft(Draft("reminder:"+r.id,Json.mapper.writeValueAsString(r),r.updatedAt))}
    private fun unschedule(r: LocalReminder) {val p=pending(r);(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(p);p.cancel()}
    suspend fun schedule(task: Task,dueAt: String,consent: Boolean): LocalReminder = mutex.withLock {
        val r=newReminder(task.id,task.title,dueAt,consent,permissionAvailable(),Instant.now())
        // Stable per-task ID; replacing increments generation and invalidates already queued deliveries.
        dao.draft("reminder:"+r.id)?.let {unschedule(parseReminder(it.payload))}
        ensure(prefs.edit().putBoolean("enabled",true).commit(),"REMINDER_PREFERENCE_FAILED")
        save(r)
        if(r.state=="PERMISSION_DENIED")r else arm(r)
    }
    private suspend fun arm(r: LocalReminder): LocalReminder {
        return try {
            // Inexact and OS-controlled. No exact-alarm or background location permission.
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,Instant.parse(r.dueAt).toEpochMilli(),pending(r))
            r.copy(state="SCHEDULED",updatedAt=Instant.now().toString()).also {save(it)}
        } catch(e:Exception) {r.copy(state="SCHEDULE_FAILED",updatedAt=Instant.now().toString()).also {save(it)}}
    }
    private suspend fun cancelUnlocked(r: LocalReminder) {save(r.copy(state="CANCELLED",updatedAt=Instant.now().toString()));unschedule(r);NotificationManagerCompat.from(context).cancel(r.id.hashCode())}
    suspend fun cancel(r: LocalReminder) = mutex.withLock {cancelUnlocked(r)}
    suspend fun disableAll() = mutex.withLock {ensure(prefs.edit().putBoolean("enabled",false).commit(),"REMINDER_PREFERENCE_FAILED");dao.reminderDrafts().forEach {cancelUnlocked(parseReminder(it.payload))}}
    suspend fun recover() = mutex.withLock {
        dao.reminderDrafts().forEach {d ->
            val r=parseReminder(d.payload);val next=reminderRecovery(r,Instant.now(),enabled(),permissionAvailable())
            if(next=="PENDING")arm(r.copy(state=next))
            else {unschedule(r);save(r.copy(state=next,updatedAt=Instant.now().toString()))}
        }
    }
    suspend fun deliver(id: String,generation: String) = mutex.withLock {
        val r=dao.draft("reminder:"+id)?.let {parseReminder(it.payload)} ?: return@withLock
        if(!reminderMayPost(r,generation,Instant.now(),enabled(),permissionAvailable()))return@withLock
        // Persist before notify; recovery never claims actual delivery or silently replays.
        save(r.copy(state="POST_REQUESTED",updatedAt=Instant.now().toString()))
        val preferred=context.preferences.data.first()[stringPreferencesKey("language")] ?: if(context.resources.configuration.locales[0].language=="es") "es" else "en"
        val labels=languageContext(context,preferred)
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("deproof-reminders",labels.getString(R.string.reminder_channel),NotificationManager.IMPORTANCE_DEFAULT))
        val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Lock-screen content is generic; private task labels never go to notifications.
        val notification=NotificationCompat.Builder(context,"deproof-reminders").setSmallIcon(R.drawable.depr).setContentTitle(labels.getString(R.string.reminder_notification_title)).setContentText(labels.getString(R.string.reminder_notification_body)).setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try {NotificationManagerCompat.from(context).notify(r.id.hashCode(),notification)} catch(e:SecurityException) {save(r.copy(state="PERMISSION_DENIED",updatedAt=Instant.now().toString()))}
    }
}
class ReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val accepted=intent.action in setOf("com.aistudio.deproof.sdwk.REMINDER",Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED)
        if(!accepted)return
        val pending=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val db=DeproofDatabase.open(context)
            try {withTimeout(8000) {val repo=ReminderRepository(context,db.records());if(intent.action=="com.aistudio.deproof.sdwk.REMINDER")repo.deliver(intent.getStringExtra("id") ?: return@withTimeout,intent.getStringExtra("generation") ?: return@withTimeout) else repo.recover()}}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {android.util.Log.w("DeproofReminder","REMINDER_RECOVERY_UNAVAILABLE")}
            finally {db.close();pending.finish()}
        }
    }
}
