package com.peeratvoip.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.pm.ServiceInfoCompat
import com.peeratvoip.app.PeeratVoipApp
import com.peeratvoip.app.R
import com.peeratvoip.app.audio.VoiceEngineHolder
import com.peeratvoip.app.audio.VoicePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A draggable floating bubble that stays on top of every other app (like the
 * buzmenow / Samsung Sound Assistant style overlay). Tapping it expands a
 * compact neumorphic-styled panel with the preset carousel, a power toggle and
 * quick pitch controls — so the voice can be changed while the user is inside
 * Discord/WhatsApp/etc. without switching back to the app.
 *
 * Requires the SYSTEM_ALERT_WINDOW ("Display over other apps") permission.
 */
class FloatingControlService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var rootView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var bubble: View? = null
    private var panel: View? = null
    private var expanded = false

    private var presetLabel: TextView? = null
    private var powerButton: TextView? = null
    private var pitchLabel: TextView? = null

    private val density get() = resources.displayMetrics.density
    private fun dp(v: Int): Int = (v * density).roundToInt()
    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    // Neumorphic-ish palette (dark surface for good contrast over any app).
    private val surface = Color.parseColor("#2A2F3A")
    private val surfaceLight = Color.parseColor("#333A47")
    private val accent = Color.parseColor("#8E7CFF")
    private val textPrimary = Color.parseColor("#EDEFF5")
    private val textSecondary = Color.parseColor("#9AA2B6")
    private val danger = Color.parseColor("#FF7A7A")
    private val success = Color.parseColor("#5CE0A0")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfoCompat.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        }

        if (rootView == null && canDrawOverlays(this)) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, PeeratVoipApp.CHANNEL_ID)
            .setContentTitle("PeeratVoiP")
            .setContentText("Menu flutuante ativo")
            .setSmallIcon(R.drawable.ic_stat_voice)
            .setOngoing(true)
            .build()

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(160)
        }

        val container = FrameLayout(this)
        val bubbleView = createBubble()
        val panelView = createPanel()
        panelView.visibility = View.GONE

        container.addView(panelView)
        container.addView(bubbleView)

        bubble = bubbleView
        panel = panelView
        rootView = container

        attachDrag(bubbleView)

        windowManager.addView(container, params)

        stateJob = scope.launch {
            launch { VoiceEngineHolder.preset.collect { renderPreset(it) } }
            launch { VoiceEngineHolder.isRunning.collect { renderRunning(it) } }
            launch { VoiceEngineHolder.extraPitchSemitones.collect { renderPitch(it) } }
        }
    }

    private fun createBubble(): View {
        val size = dp(56)
        val tv = TextView(this).apply {
            text = VoiceEngineHolder.preset.value.emoji
            gravity = Gravity.CENTER
            textSize = 24f
            setTextColor(textPrimary)
            background = circle(surface)
            elevation = dp(8).toFloat()
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        tv.setOnClickListener { togglePanel() }
        return tv
    }

    private fun togglePanel() {
        expanded = !expanded
        panel?.visibility = if (expanded) View.VISIBLE else View.GONE
    }

    private fun createPanel(): View {
        val panelRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(surface, dp(22).toFloat())
            elevation = dp(12).toFloat()
            setPadding(dp(16), dp(14), dp(16), dp(16))
            // sit the panel above the bubble
            layoutParams = FrameLayout.LayoutParams(
                dp(268),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(64) }
        }

        panelRoot.addView(TextView(this).apply {
            text = "PeeratVoiP"
            setTextColor(textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(12f))
        })

        presetLabel = TextView(this).apply {
            setTextColor(textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(18f))
            setPadding(0, dp(2), 0, dp(12))
        }
        panelRoot.addView(presetLabel)

        // Preset carousel row: ◀  [name]  ▶
        val carousel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        carousel.addView(pill("◀") { VoiceEngineHolder.previousPreset() })
        carousel.addView(TextView(this).apply {
            text = "trocar voz"
            gravity = Gravity.CENTER
            setTextColor(textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13f))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        carousel.addView(pill("▶") { VoiceEngineHolder.nextPreset() })
        panelRoot.addView(carousel)

        // Pitch row: −  [value]  +
        pitchLabel = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(accent)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(13f))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val pitchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(10) }
        }
        pitchRow.addView(pill("−") { stepPitch(-1f) })
        pitchRow.addView(pitchLabel)
        pitchRow.addView(pill("+") { stepPitch(1f) })
        panelRoot.addView(pitchRow)

        // Power toggle (full width)
        powerButton = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(15f))
            setPadding(0, dp(12), 0, dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(14) }
            setOnClickListener { VoiceEngineHolder.toggle() }
        }
        panelRoot.addView(powerButton)

        renderPreset(VoiceEngineHolder.preset.value)
        renderRunning(VoiceEngineHolder.isRunning.value)
        renderPitch(VoiceEngineHolder.extraPitchSemitones.value)
        return panelRoot
    }

    private fun stepPitch(delta: Float) {
        val next = (VoiceEngineHolder.extraPitchSemitones.value + delta).coerceIn(-12f, 12f)
        VoiceEngineHolder.setExtraPitch(next)
    }

    private fun pill(label: String, onClick: () -> Unit): TextView {
        val s = dp(44)
        return TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sp(18f))
            background = circle(surfaceLight)
            layoutParams = LinearLayout.LayoutParams(s, s)
            setOnClickListener { onClick() }
        }
    }

    private fun renderPreset(preset: VoicePreset) {
        presetLabel?.text = "${preset.emoji}  ${preset.name}"
        bubble?.let { (it as? TextView)?.text = preset.emoji }
    }

    private fun renderRunning(running: Boolean) {
        powerButton?.apply {
            text = if (running) "● Ao vivo — tocar p/ pausar" else "○ Pausado — tocar p/ ativar"
            background = roundedRect(if (running) success else surfaceLight, dp(16).toFloat())
        }
        bubble?.background = circle(if (running) accent else surface)
    }

    private fun renderPitch(semitones: Float) {
        val st = semitones.toInt()
        val sign = if (st > 0) "+" else ""
        pitchLabel?.text = "tom $sign$st st"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag(view: View) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    animateScale(v, 0.92f)
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    rootView?.let { windowManager.updateViewLayout(it, params) }
                    false
                }
                MotionEvent.ACTION_UP -> {
                    animateScale(v, 1f)
                    if (!moved) v.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun animateScale(v: View, target: Float) {
        ValueAnimator.ofFloat(v.scaleX, target).apply {
            duration = 90
            addUpdateListener {
                val s = it.animatedValue as Float
                v.scaleX = s
                v.scaleY = s
            }
            start()
        }
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun roundedRect(color: Int, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(color)
    }

    private fun stopSelfSafely() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        rootView?.let { runCatching { windowManager.removeView(it) } }
        rootView = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 43
        const val ACTION_SHOW = "com.peeratvoip.app.action.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.peeratvoip.app.action.HIDE_OVERLAY"

        fun canDrawOverlays(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        fun show(context: Context) {
            val intent = Intent(context, FloatingControlService::class.java).setAction(ACTION_SHOW)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, FloatingControlService::class.java).setAction(ACTION_HIDE)
            context.startService(intent)
        }
    }
}
