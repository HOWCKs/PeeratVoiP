package com.peeratvoip.app.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import com.peeratvoip.app.MainActivity
import com.peeratvoip.app.PeeratVoipApp
import com.peeratvoip.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the shared [VoiceEngineHolder] pipeline alive
 * while the app is backgrounded (e.g. while the user switches to a VoIP app),
 * and exposes quick-controls directly in the ongoing notification so the voice
 * can be changed without reopening the app — similar to Samsung Sound Assistant.
 *
 * Supported [Intent] actions:
 *  - [ACTION_START]  : start the mic pipeline + foreground notification.
 *  - [ACTION_STOP]   : stop pipeline and tear down the service.
 *  - [ACTION_TOGGLE] : toggle the pipeline on/off.
 *  - [ACTION_NEXT]   : switch to the next voice preset.
 *  - [ACTION_PREV]   : switch to the previous voice preset.
 */
class LiveVoiceFxService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Re-render the notification whenever running state or preset changes.
        observerJob = scope.launch {
            combine(
                VoiceEngineHolder.isRunning,
                VoiceEngineHolder.preset,
            ) { running, preset -> running to preset }
                .collect { (running, _) ->
                    if (started) {
                        if (!running) {
                            stopSelfSafely()
                        } else {
                            NotificationManagerCompat.from(this@LiveVoiceFxService)
                                .notify(NOTIFICATION_ID, buildNotification())
                        }
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                VoiceEngineHolder.stop()
                stopSelfSafely()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> VoiceEngineHolder.toggle()
            ACTION_NEXT -> VoiceEngineHolder.nextPreset()
            ACTION_PREV -> VoiceEngineHolder.previousPreset()
            else -> {
                // ACTION_START or a plain start: make sure the engine is running.
                if (!VoiceEngineHolder.isRunning.value) VoiceEngineHolder.start()
            }
        }

        started = true
        runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        }
        return START_STICKY
    }

    private fun stopSelfSafely() {
        started = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun action(name: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, LiveVoiceFxService::class.java).setAction(name)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun buildNotification(): Notification {
        val running = VoiceEngineHolder.isRunning.value
        val preset = VoiceEngineHolder.preset.value

        val openIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(this, PeeratVoipApp.CHANNEL_ID)
            .setContentTitle("PeeratVoiP · ${preset.emoji} ${preset.name}")
            .setContentText(
                if (running) "Voz ao vivo ativa — toque nas ações para trocar o efeito"
                else "Voz pausada",
            )
            .setSmallIcon(R.drawable.ic_stat_voice)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(running)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.addAction(
            android.R.drawable.ic_media_previous,
            "Anterior",
            action(ACTION_PREV, 1),
        )
        builder.addAction(
            if (running) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (running) "Pausar" else "Ativar",
            action(ACTION_TOGGLE, 2),
        )
        builder.addAction(
            android.R.drawable.ic_media_next,
            "Próxima",
            action(ACTION_NEXT, 3),
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Encerrar",
            action(ACTION_STOP, 4),
        )

        return builder.build()
    }

    override fun onDestroy() {
        observerJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42

        const val ACTION_START = "com.peeratvoip.app.action.START"
        const val ACTION_STOP = "com.peeratvoip.app.action.STOP"
        const val ACTION_TOGGLE = "com.peeratvoip.app.action.TOGGLE"
        const val ACTION_NEXT = "com.peeratvoip.app.action.NEXT"
        const val ACTION_PREV = "com.peeratvoip.app.action.PREV"

        fun start(context: android.content.Context) {
            val intent = Intent(context, LiveVoiceFxService::class.java).setAction(ACTION_START)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, LiveVoiceFxService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
