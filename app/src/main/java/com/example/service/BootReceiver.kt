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

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val action =
            intent.action

        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val appContext =
            context.applicationContext

        // Restore alarms
        try {
            AlarmScheduler(
                appContext
            ).reRegisterAlarms()
        } catch (_: Exception) {
        }

        val prefs =
            Preferences(appContext)

        if (
            prefs.isAutoStartEnabled &&
            prefs.isBackgroundAssistantEnabled
        ) {

            // Do NOT silently start the microphone service from boot.
            // Android restricts microphone foreground services started
            // from background/boot.

            showBootNotification(
                appContext
            )
        }
    }

    private fun showBootNotification(
        context: Context
    ) {

        val channelId =
            "jarvis_boot"

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "JARVIS System",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val launchIntent =
            context.packageManager
                .getLaunchIntentForPackage(
                    context.packageName
                )

        val pendingIntent =
            if (launchIntent != null) {

                androidx.core.app.PendingIntentCompat
                    .getActivity(
                        context,
                        900,
                        launchIntent,
                        0,
                        false
                    )

            } else {
                null
            }

        val notification =
            NotificationCompat.Builder(
                context,
                channelId
            )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setContentTitle(
                    "JARVIS"
                )
                .setContentText(
                    "Phone restarted. Open JARVIS to resume background listening."
                )
                .setAutoCancel(true)
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .apply {
                    if (pendingIntent != null) {
                        setContentIntent(
                            pendingIntent
                        )
                    }
                }
                .build()

        manager.notify(
            2001,
            notification
        )
    }
}
