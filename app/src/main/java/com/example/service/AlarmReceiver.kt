package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.ui.screens.AlarmActivity
import com.example.voice.TTSManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_DISMISS) {
            stopAlarmSoundAndVibration()
            cancelNotification(context)
            return
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val alarmLabel = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "JARVIS Alarm"
        val alarmTime = intent.getStringExtra(EXTRA_ALARM_TIME) ?: "07:00 AM"

        // Wake screen
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "JARVIS:AlarmWakeLock"
        )
        wakeLock.acquire(10000)

        // Play alarm ringtone & vibration
        startAlarmSoundAndVibration(context)

        // Trigger TTS
        try {
            val tts = TTSManager(context.applicationContext)
            tts.speak("Sir, your $alarmTime alarm $alarmLabel is ringing now.")
        } catch (e: Exception) {
            // Ignore TTS errors during receiver
        }

        // Create high-priority full-screen intent notification
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(EXTRA_ALARM_TIME, alarmTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt() + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("JARVIS ALARM")
            .setContentText("Alarm for $alarmTime ($alarmLabel)")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISMISS", dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Try launching AlarmActivity directly as well
        try {
            context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
            // Screen locking policy may handle fullScreenIntent via Notification
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "JARVIS Full Screen Alarm Alerts"
                setBypassDnd(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.example.action.ALARM_TRIGGER"
        const val ACTION_DISMISS = "com.example.action.ALARM_DISMISS"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_TIME = "extra_alarm_time"

        const val CHANNEL_ID = "jarvis_alarm_channel"
        const val NOTIFICATION_ID = 2002

        private var ringtonePlayer: android.media.Ringtone? = null

        fun startAlarmSoundAndVibration(context: Context) {
            stopAlarmSoundAndVibration()
            try {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ringtonePlayer = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
                ringtonePlayer?.play()
            } catch (e: Exception) {
                // Ignore
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val vibrator = vibratorManager.defaultVibrator
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500, 500), 0))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(longArrayOf(0, 500, 500, 500), 0)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        fun stopAlarmSoundAndVibration() {
            try {
                ringtonePlayer?.stop()
                ringtonePlayer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
