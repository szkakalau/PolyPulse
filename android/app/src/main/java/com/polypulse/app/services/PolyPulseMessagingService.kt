package com.polypulse.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.polypulse.app.MainActivity
import com.polypulse.app.R
import com.polypulse.app.data.auth.TokenManager
import com.polypulse.app.data.notifications.NotificationPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class PolyPulseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Save token to DataStore
        val tokenManager = TokenManager(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleNow()
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            val signalId = remoteMessage.data["signalId"]
            val template = resolveTemplate(remoteMessage)
            val prefs = runBlocking {
                NotificationPreferencesStore(applicationContext).getSnapshot()
            }
            if (isTemplateEnabled(template, prefs) && !shouldSuppressNotification(prefs) && !shouldThrottlePush(template)) {
                sendNotification(it.title, it.body, signalId)
            }
        }
    }

    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    private fun sendNotification(title: String?, messageBody: String?, signalId: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (!signalId.isNullOrBlank()) {
            intent.putExtra(MainActivity.EXTRA_SIGNAL_ID, signalId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "polypulse_alerts_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_whale) // Ensure this resource exists
            .setContentTitle(title ?: "PolyPulse Alert")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Market Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "PolyPulseMsgService"
    }

    private fun shouldSuppressNotification(prefs: com.polypulse.app.data.notifications.NotificationPreferences): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (prefs.quietHoursStart < prefs.quietHoursEnd) {
            hour in prefs.quietHoursStart until prefs.quietHoursEnd
        } else {
            hour >= prefs.quietHoursStart || hour < prefs.quietHoursEnd
        }
    }

    private fun shouldThrottlePush(template: String): Boolean {
        return runBlocking {
            val store = NotificationPreferencesStore(applicationContext)
            val config = store.getThrottleConfig()
            val intervalMs = when (template) {
                "whale" -> config.whalePushIntervalMinutes * 60000L
                "daily_pulse" -> config.dailyPushIntervalMinutes * 60000L
                else -> config.generalPushIntervalMinutes * 60000L
            }
            store.shouldThrottlePushTemplate(template, intervalMs)
        }
    }

    private fun resolveTemplate(remoteMessage: RemoteMessage): String {
        val raw = remoteMessage.data["template"] ?: remoteMessage.data["type"] ?: "general"
        return when (raw.lowercase()) {
            "whale", "whale_radar" -> "whale"
            "daily", "daily_pulse" -> "daily_pulse"
            else -> "general"
        }
    }

    private fun isTemplateEnabled(template: String, prefs: com.polypulse.app.data.notifications.NotificationPreferences): Boolean {
        return when (template) {
            "whale" -> prefs.whaleRadarEnabled
            "daily_pulse" -> prefs.dailyPulseEnabled
            else -> true
        }
    }
}
