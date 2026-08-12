package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.Preferences
import com.example.tools.AlarmScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // 1. Re-register scheduled local alarms
            val alarmScheduler = AlarmScheduler(context.applicationContext)
            alarmScheduler.reRegisterAlarms()

            // 2. Restore background assistant service if enabled
            val prefs = Preferences(context)
            if (prefs.isAutoStartEnabled || prefs.isBackgroundAssistantEnabled) {
                JarvisForegroundService.startService(context)
                showBootNotification(context)
            }
        }
    }

    private fun showBootNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jarvis_boot_channel",
                "JARVIS System Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "jarvis_boot_channel")
            .setContentTitle("JARVIS Personal Assistant")
            .setContentText("System restarted — Alarms & background assistant online.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        manager.notify(2001, notification)
    }
}
