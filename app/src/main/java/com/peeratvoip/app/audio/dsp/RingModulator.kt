package com.peeratvoip.app.audio.dsp

import kotlin.math.PI
import kotlin.math.sin

/**
 * Ring modulator: multiplies the signal by a low-frequency sine carrier,
 * producing the classic metallic "robot voice" sideband effect.
 */
class RingModulator(
    private val sampleRate: Float,
    private var carrierHz: Float = 30f,
    private var mix: Float = 0.5f,
) : AudioEffect {
    private var phase = 0.0

    fun setCarrierHz(value: Float) { carrierHz = value }
    fun setMix(value: Float) { mix = value.coerceIn(0f, 1f) }

    override fun process(buffer: FloatArray, numSamples: Int) {
        val phaseInc = 2.0 * PI * carrierHz / sampleRate
        for (i in 0 until numSamples) {
            val carrier = sin(phase).toFloat()
            val modulated = buffer[i] * carrier
            buffer[i] = buffer[i] * (1 - mix) + modulated * mix
            phase += phaseInc
            if (phase > 2 * PI) phase -= 2 * PI
        }
    }

    override fun reset() { phase = 0.0 }
}

/** Soft-clip / bitcrush style distortion used for "Monster" and "Radio static" character. */
class Distortion(
    private var drive: Float = 2.5f,
    private var mix: Float = 0.4f,
) : AudioEffect {
    fun setDrive(value: Float) { drive = value.coerceIn(1f, 12f) }
    fun setMix(value: Float) { mix = value.coerceIn(0f, 1f) }

    override fun process(buffer: FloatArray, numSamples: Int) {
        for (i in 0 until numSamples) {
            val x = buffer[i] * drive
            val shaped = (x / (1f + kotlin.math.abs(x)))
            buffer[i] = buffer[i] * (1 - mix) + shaped * mix
        }
    }
}

/** Bit-depth / sample-rate reducer for lo-fi "radio"/"robot" texture. */
class BitCrusher(
    private var bits: Int = 8,
    private var sampleHold: Int = 2,
) : AudioEffect {
    private var counter = 0
    private var heldValue = 0f

    fun setBits(value: Int) { bits = value.coerceIn(2, 16) }
    fun setSampleHold(value: Int) { sampleHold = value.coerceIn(1, 8) }

    override fun process(buffer: FloatArray, numSamples: Int) {
        val levels = (1 shl bits).toFloat()
        for (i in 0 until numSamples) {
            if (counter % sampleHold == 0) {
                heldValue = kotlin.math.round(buffer[i] * levels) / levels
            }
            buffer[i] = heldValue
            counter++
        }
    }

    override fun reset() {
        counter = 0
        heldValue = 0f
    }
}
