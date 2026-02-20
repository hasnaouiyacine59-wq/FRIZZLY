package com.example.f_rizzly

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FrizzlyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token to Firestore for this user
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        message.notification?.let {
            val title = it.title ?: "FRIZZLY"
            val body = it.body ?: ""
            val type = message.data["type"] ?: "general"
            val orderId = message.data["orderId"]
            
            // Save notification to storage
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val notifications = DataStore.loadNotifications(this, userId).toMutableList()
            notifications.add(0, AppNotification(
                id = System.currentTimeMillis().toString(),
                title = title,
                body = body,
                type = type,
                timestamp = System.currentTimeMillis(),
                orderId = orderId,
                isRead = false
            ))
            DataStore.saveNotifications(this, userId, notifications)
            
            showNotification(title, body, type, orderId)
        }
    }

    private fun showNotification(title: String, body: String, type: String = "general", orderId: String? = null) {
        val channelId = "frizzly_orders_v7"  // New channel for custom beep
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Use custom beep sound from raw folder
        val soundUri = android.net.Uri.parse("android.resource://$packageName/${R.raw.notification_beep}")

        android.util.Log.d("FrizzlyFCM", "Notification sound URI: $soundUri")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channels
            notificationManager.deleteNotificationChannel("frizzly_orders")
            notificationManager.deleteNotificationChannel("frizzly_orders_v2")
            notificationManager.deleteNotificationChannel("frizzly_orders_v3")
            notificationManager.deleteNotificationChannel("frizzly_orders_v4")
            notificationManager.deleteNotificationChannel("frizzly_orders_v5")
            notificationManager.deleteNotificationChannel("frizzly_orders_v6")
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            
            val channel = NotificationChannel(
                channelId,
                "FRIZZLY Order Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important order status updates"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("order_id", orderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        
        // Play custom beep manually as backup
        try {
            val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.notification_beep)
            mediaPlayer?.start()
            mediaPlayer?.setOnCompletionListener { it.release() }
            android.util.Log.d("FrizzlyFCM", "Playing custom beep")
        } catch (e: Exception) {
            android.util.Log.e("FrizzlyFCM", "Failed to play beep: ${e.message}")
        }
    }
}
