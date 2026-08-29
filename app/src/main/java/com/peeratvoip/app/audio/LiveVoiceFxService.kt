package com.peeratvoip.app.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.pm.ServiceInfoCompat
import com.peeratvoip.app.MainActivity
import com.peeratvoip.app.PeeratVoipApp

/**
 * Foreground service that keeps [LiveVoiceEngine] alive while the user is on
 * the Live screen with the effect active, so Android doesn't kill the mic
 * pipeline when the app is backgrounded (e.g. while switching to a VoIP app).
 */
class LiveVoiceFxService : Service() {

    private val binder = LocalBinder()
    val engine by lazy { LiveVoiceEngine() }

    inner class LocalBinder : Binder() {
        fun getService(): LiveVoiceFxService = this@LiveVoiceFxService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfoCompat.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, PeeratVoipApp.CHANNEL_ID)
            .setContentTitle("PeeratVoiP")
            .setContentText("Transformando sua voz em tempo real")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
