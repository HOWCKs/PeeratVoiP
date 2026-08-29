package com.peeratvoip.app.audio

import com.peeratvoip.app.audio.dsp.BiquadFilter
import com.peeratvoip.app.audio.dsp.BiquadType
import com.peeratvoip.app.audio.dsp.BitCrusher
import com.peeratvoip.app.audio.dsp.DelayEffect
import com.peeratvoip.app.audio.dsp.Distortion
import com.peeratvoip.app.audio.dsp.PitchShifter
import com.peeratvoip.app.audio.dsp.RingModulator
import com.peeratvoip.app.audio.dsp.SimpleReverb
import kotlin.math.pow

/**
 * Wires all DSP stages into a single real-time chain and applies a
 * [VoicePreset] on top of it. `process` must be called with the exact same
 * sample rate the stages were constructed with.
 */
class VoiceEffectEngine(private val sampleRate: Float) {

    private val pitchShifter = PitchShifter(sampleRate)
    private val ringModulator = RingModulator(sampleRate)
    private val distortion = Distortion()
    private val bitCrusher = BitCrusher()
    private val echo = DelayEffect(sampleRate, delayMs = 180f)
    private val reverb = SimpleReverb(sampleRate)
    private val lowPass = BiquadFilter(sampleRate, BiquadType.LOW_PASS, 18000f)
    private val highPass = BiquadFilter(sampleRate, BiquadType.HIGH_PASS, 40f)
    private val telephoneBandLow = BiquadFilter(sampleRate, BiquadType.HIGH_PASS, 300f, q = 0.9f)
    private val telephoneBandHigh = BiquadFilter(sampleRate, BiquadType.LOW_PASS, 3400f, q = 0.9f)

    @Volatile
    var preset: VoicePreset = VoicePreset.NORMAL
        set(value) {
            field = value
            applyPreset(value)
        }

    /** Extra global controls layered on top of the active preset (from Live screen sliders). */
    @Volatile var extraPitchSemitones: Float = 0f
    @Volatile var wetDryMix: Float = 1f // 1 = fully processed, 0 = bypass/dry
    @Volatile var outputGain: Float = 1f

    init {
        applyPreset(preset)
    }

    private fun applyPreset(p: VoicePreset) {
        ringModulator.setMix(p.ringModMix)
        ringModulator.setCarrierHz(p.ringModHz)
        distortion.setMix(p.distortionMix)
        bitCrusher.setBits(p.bitCrushBits)
        bitCrusher.setSampleHold(p.bitCrushHold)
        echo.setMix(p.echoMix)
        echo.setDelayMs(sampleRate, p.echoMs)
        reverb.setMix(p.reverbMix)
        lowPass.configure(BiquadType.LOW_PASS, p.lowPassHz.coerceIn(200f, sampleRate / 2f - 100f), 0.707f)
        highPass.configure(BiquadType.HIGH_PASS, p.highPassHz.coerceIn(20f, 2000f), 0.707f)
    }

    fun reset() {
        pitchShifter.reset()
        ringModulator.reset()
        echo.reset()
        reverb.reset()
        lowPass.reset()
        highPass.reset()
        telephoneBandLow.reset()
        telephoneBandHigh.reset()
    }

    private val dryBuffer = FloatArray(8192)

    /** Processes [numSamples] in-place inside [buffer]. */
    fun process(buffer: FloatArray, numSamples: Int) {
        val p = preset
        if (numSamples > dryBuffer.size) return

        System.arraycopy(buffer, 0, dryBuffer, 0, numSamples)

        val totalSemitones = p.pitchSemitones + extraPitchSemitones
        pitchShifter.pitchFactor = 2f.pow(totalSemitones / 12f)
        if (totalSemitones != 0f) {
            pitchShifter.process(buffer, buffer, numSamples)
        }

        if (p.bandTelephone) {
            telephoneBandLow.process(buffer, numSamples)
            telephoneBandHigh.process(buffer, numSamples)
        } else {
            highPass.process(buffer, numSamples)
            lowPass.process(buffer, numSamples)
        }

        if (p.ringModMix > 0f) ringModulator.process(buffer, numSamples)
        if (p.distortionMix > 0f) distortion.process(buffer, numSamples)
        if (p.bitCrushBits < 16) bitCrusher.process(buffer, numSamples)
        if (p.echoMix > 0f) echo.process(buffer, numSamples)
        if (p.reverbMix > 0f) reverb.process(buffer, numSamples)

        val gain = outputGain * 10f.pow(p.gainDb / 20f)
        val mix = wetDryMix.coerceIn(0f, 1f)
        for (i in 0 until numSamples) {
            val wet = buffer[i] * gain
            val dry = dryBuffer[i]
            buffer[i] = (dry * (1 - mix) + wet * mix).coerceIn(-1f, 1f)
        }
    }
}
