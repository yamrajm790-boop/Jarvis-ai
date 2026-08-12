package com.example.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.example.data.AlarmEntity
import com.example.data.JarvisDatabase
import com.example.service.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val database = JarvisDatabase.getDatabase(context)

    fun scheduleAlarm(hour: Int, minute: Int, label: String? = null, onScheduled: ((Boolean, String) -> Unit)? = null): ToolExecutionResult {
        val safeLabel = if (!label.isNullOrBlank()) label else "JARVIS Alarm"

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerAtMillis = calendar.timeInMillis
        val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)

        CoroutineScope(Dispatchers.IO).launch {
            val alarmEntity = AlarmEntity(
                hour = hour,
                minute = minute,
                label = safeLabel,
                isEnabled = true,
                triggerAtMillis = triggerAtMillis
            )
            val alarmId = database.jarvisDao().insertAlarm(alarmEntity)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, safeLabel)
                putExtra(AlarmReceiver.EXTRA_ALARM_TIME, formattedTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                }
            } catch (e: Exception) {
                // Fallback to system Clock set alarm intent
                try {
                    val systemIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, safeLabel)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(systemIntent)
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }

        val msg = "Done sir, $formattedTime alarm set."
        onScheduled?.invoke(true, msg)
        return ToolExecutionResult(true, msg)
    }

    fun cancelAlarm(alarmId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            database.jarvisDao().updateAlarmEnabled(alarmId, false)
        }
    }

    fun reRegisterAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            val activeAlarms = database.jarvisDao().getActiveAlarms()
            val now = System.currentTimeMillis()

            for (alarm in activeAlarms) {
                if (alarm.triggerAtMillis > now) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_ALARM_TRIGGER
                        putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
                        putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
                        putExtra(
                            AlarmReceiver.EXTRA_ALARM_TIME,
                            String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
                        )
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.id.toInt(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        val alarmClockInfo = AlarmManager.AlarmClockInfo(alarm.triggerAtMillis, pendingIntent)
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    } catch (e: Exception) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pendingIntent)
                    }
                }
            }
        }
    }
}
