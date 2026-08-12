package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class JarvisNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Notification received locally
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification dismissed
    }

    fun getNotificationSummary(): NotificationSummary {
        val notifications = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        val nonAppList = notifications.filter { sbn ->
            val pkg = sbn.packageName ?: ""
            !pkg.contains("android") && !pkg.contains("systemui") && !pkg.contains("example")
        }

        val count = nonAppList.size
        val titles = nonAppList.take(3).mapNotNull { sbn ->
            val extras = sbn.notification?.extras
            val title = extras?.getCharSequence("android.title")?.toString()
            val text = extras?.getCharSequence("android.text")?.toString()
            if (!title.isNullOrBlank()) "$title: ${text ?: ""}" else text
        }

        return NotificationSummary(
            totalCount = count,
            recentTitles = titles
        )
    }

    companion object {
        var instance: JarvisNotificationListenerService? = null
            private set

        fun isServiceAvailable(): Boolean = instance != null
    }
}

data class NotificationSummary(
    val totalCount: Int,
    val recentTitles: List<String>
)
