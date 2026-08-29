package com.peeratvoip.app.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class BiquadType { LOW_PASS, HIGH_PASS, BAND_PASS, NOTCH, PEAK, LOW_SHELF, HIGH_SHELF }

/**
 * Standard RBJ biquad filter (direct form 1). Used to build the tonal
 * character of effects like "Telephone" (band-pass), "Radio" (high/low shelf),
 * "Deep voice" (low-pass warmth) and "Robot" (notch/peak combs).
 */
class BiquadFilter(
    private val sampleRate: Float,
    type: BiquadType,
    frequency: Float,
    q: Float = 0.707f,
    gainDb: Float = 0f,
) : AudioEffect {

    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a0 = 1.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        configure(type, frequency, q, gainDb)
    }

    fun configure(type: BiquadType, frequency: Float, q: Float, gainDb: Float = 0f) {
        val w0 = 2.0 * PI * frequency / sampleRate
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * q)
        val a = Math.pow(10.0, gainDb / 40.0)

        when (type) {
            BiquadType.LOW_PASS -> {
                b0 = (1 - cosw0) / 2
                b1 = 1 - cosw0
                b2 = (1 - cosw0) / 2
                a0 = 1 + alpha
                a1 = -2 * cosw0
                a2 = 1 - alpha
            }
            BiquadType.HIGH_PASS -> {
                b0 = (1 + cosw0) / 2
                b1 = -(1 + cosw0)
                b2 = (1 + cosw0) / 2
                a0 = 1 + alpha
                a1 = -2 * cosw0
                a2 = 1 - alpha
            }
            BiquadType.BAND_PASS -> {
                b0 = alpha
                b1 = 0.0
                b2 = -alpha
                a0 = 1 + alpha
                a1 = -2 * cosw0
                a2 = 1 - alpha
            }
            BiquadType.NOTCH -> {
                b0 = 1.0
                b1 = -2 * cosw0
                b2 = 1.0
                a0 = 1 + alpha
                a1 = -2 * cosw0
                a2 = 1 - alpha
            }
            BiquadType.PEAK -> {
                b0 = 1 + alpha * a
                b1 = -2 * cosw0
                b2 = 1 - alpha * a
                a0 = 1 + alpha / a
                a1 = -2 * cosw0
                a2 = 1 - alpha / a
            }
            BiquadType.LOW_SHELF -> {
                val sqrtA = sqrt(a)
                b0 = a * ((a + 1) - (a - 1) * cosw0 + 2 * sqrtA * alpha)
                b1 = 2 * a * ((a - 1) - (a + 1) * cosw0)
                b2 = a * ((a + 1) - (a - 1) * cosw0 - 2 * sqrtA * alpha)
                a0 = (a + 1) + (a - 1) * cosw0 + 2 * sqrtA * alpha
                a1 = -2 * ((a - 1) + (a + 1) * cosw0)
                a2 = (a + 1) + (a - 1) * cosw0 - 2 * sqrtA * alpha
            }
            BiquadType.HIGH_SHELF -> {
                val sqrtA = sqrt(a)
                b0 = a * ((a + 1) + (a - 1) * cosw0 + 2 * sqrtA * alpha)
                b1 = -2 * a * ((a - 1) + (a + 1) * cosw0)
                b2 = a * ((a + 1) + (a - 1) * cosw0 - 2 * sqrtA * alpha)
                a0 = (a + 1) - (a - 1) * cosw0 + 2 * sqrtA * alpha
                a1 = 2 * ((a - 1) - (a + 1) * cosw0)
                a2 = (a + 1) - (a - 1) * cosw0 - 2 * sqrtA * alpha
            }
        }
        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0
        a0 = 1.0
    }

    override fun process(buffer: FloatArray, numSamples: Int) {
        for (i in 0 until numSamples) {
            val x0 = buffer[i].toDouble()
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            buffer[i] = y0.toFloat()
        }
    }

    override fun reset() {
        x1 = 0.0; x2 = 0.0; y1 = 0.0; y2 = 0.0
    }
}
