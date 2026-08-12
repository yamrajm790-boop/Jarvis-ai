package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class JarvisForegroundService : Service() {

    private var paused = false

    private val serviceJob =
        SupervisorJob()

    private val scope =
        CoroutineScope(
            Dispatchers.Main.immediate +
                    serviceJob
        )

    private var controller:
        BackgroundAssistantController? = null

    private var statusJob:
        Job? = null

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_STOP -> {

                stopAssistant()

                return START_NOT_STICKY
            }

            ACTION_PAUSE -> {

                paused = true

                controller?.pause()

                updateNotification(
                    "JARVIS ● Paused"
                )
            }

            ACTION_RESUME -> {

                paused = false

                controller?.resume()

                updateNotification(
                    "JARVIS ● Listening"
                )
            }

            else -> {

                isServiceRunning = true

                startForegroundSafely()

                if (controller == null) {

                    val newController =
                        BackgroundAssistantController(
                            applicationContext,
                            scope
                        )

                    controller =
                        newController

                    statusJob =
                        scope.launch {

                            newController
                                .statusText
                                .collect { text ->

                                    updateNotification(
                                        text
                                    )
                                }
                        }

                    newController.start()

                } else {

                    controller?.resume()
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundSafely() {

        val notification =
            buildNotification(
                "JARVIS ● Listening"
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun updateNotification(
        text: String
    ) {

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            buildNotification(text)
        )
    }

    private fun buildNotification(
        text: String
    ): Notification {

        val openIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val openPendingIntent =
            PendingIntent.getActivity(
                this,
                100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val toggleIntent =
            Intent(
                this,
                JarvisForegroundService::class.java
            ).apply {

                action =
                    if (paused)
                        ACTION_RESUME
                    else
                        ACTION_PAUSE
            }

        val togglePendingIntent =
            PendingIntent.getService(
                this,
                101,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val stopIntent =
            Intent(
                this,
                JarvisForegroundService::class.java
            ).apply {

                action = ACTION_STOP
            }

        val stopPendingIntent =
            PendingIntent.getService(
                this,
                102,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setContentTitle(
                "JARVIS Personal Assistant"
            )
            .setContentText(text)
            .setContentIntent(
                openPendingIntent
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .addAction(
                if (paused)
                    android.R.drawable.ic_media_play
                else
                    android.R.drawable.ic_media_pause,
                if (paused)
                    "Resume"
                else
                    "Pause",
                togglePendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "JARVIS Background Assistant",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {

                    description =
                        "JARVIS background microphone service"
                }

            val manager =
                getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun stopAssistant() {

        isServiceRunning = false

        controller?.destroy()
        controller = null

        statusJob?.cancel()
        statusJob = null

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.N
        ) {
            stopForeground(
                STOP_FOREGROUND_REMOVE
            )
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    override fun onDestroy() {

        isServiceRunning = false

        controller?.destroy()
        controller = null

        statusJob?.cancel()

        scope.cancel()

        super.onDestroy()
    }

    companion object {

        const val CHANNEL_ID =
            "jarvis_background_service"

        const val NOTIFICATION_ID =
            1001

        const val ACTION_START =
            "com.example.service.START"

        const val ACTION_STOP =
            "com.example.service.STOP"

        const val ACTION_PAUSE =
            "com.example.service.PAUSE"

        const val ACTION_RESUME =
            "com.example.service.RESUME"

        var isServiceRunning =
            false
            private set

        fun startService(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    JarvisForegroundService::class.java
                ).apply {

                    action = ACTION_START
                }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                context.startForegroundService(
                    intent
                )

            } else {

                context.startService(
                    intent
                )
            }
        }

        fun stopService(
            context: Context
        ) {

            val intent =
                Intent(
                    context,
                    JarvisForegroundService::class.java
                ).apply {

                    action = ACTION_STOP
                }

            context.startService(intent)
        }

        fun pauseForForegroundMic(
            context: Context
        ) {

            if (!isServiceRunning) return

            val intent =
                Intent(
                    context,
                    JarvisForegroundService::class.java
                ).apply {

                    action = ACTION_PAUSE
                }

            context.startService(intent)
        }

        fun resumeAfterForegroundMic(
            context: Context
        ) {

            if (!isServiceRunning) return

            val intent =
                Intent(
                    context,
                    JarvisForegroundService::class.java
                ).apply {

                    action = ACTION_RESUME
                }

            context.startService(intent)
        }
    }
}
