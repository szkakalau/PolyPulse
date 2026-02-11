package com.polypulse.app.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.polypulse.app.presentation.util.NotificationHelper

class PolyPulseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here.
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            NotificationHelper.showNotification(
                this,
                it.title ?: "PolyPulse Alert",
                it.body ?: "New alert received"
            )
        }
        
        // Also check data payload if needed
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Handle data payload
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // TODO: Send this token to your backend server if you want to target specific devices
    }

    companion object {
        private const val TAG = "PolyPulseMessaging"
    }
}
