package com.peeratvoip.app.audio.dsp

/** Simple feedback delay line — powers Echo and lightweight Reverb-ish effects. */
class DelayEffect(
    sampleRate: Float,
    delayMs: Float,
    private var feedback: Float = 0.35f,
    private var mix: Float = 0.3f,
    maxDelayMs: Float = 1000f,
) : AudioEffect {
    private val buffer = FloatArray((sampleRate * maxDelayMs / 1000f).toInt().coerceAtLeast(1))
    private var writeIndex = 0
    private var delaySamples = (sampleRate * delayMs / 1000f).toInt().coerceIn(1, buffer.size - 1)

    fun setDelayMs(sampleRate: Float, delayMs: Float) {
        delaySamples = (sampleRate * delayMs / 1000f).toInt().coerceIn(1, buffer.size - 1)
    }

    fun setFeedback(value: Float) { feedback = value.coerceIn(0f, 0.95f) }
    fun setMix(value: Float) { mix = value.coerceIn(0f, 1f) }

    override fun process(buffer_: FloatArray, numSamples: Int) {
        for (i in 0 until numSamples) {
            val readIndex = (writeIndex - delaySamples + buffer.size) % buffer.size
            val delayed = buffer[readIndex]
            val input = buffer_[i]
            val toStore = input + delayed * feedback
            buffer[writeIndex] = toStore
            buffer_[i] = input * (1 - mix) + delayed * mix
            writeIndex = (writeIndex + 1) % buffer.size
        }
    }

    override fun reset() {
        buffer.fill(0f)
        writeIndex = 0
    }
}

/** Short multi-tap delay used to approximate a small-room reverb ("hall" character). */
class SimpleReverb(
    sampleRate: Float,
    private var mix: Float = 0.25f,
) : AudioEffect {
    private val taps = listOf(29.7f, 37.1f, 41.3f, 53.9f).map { ms ->
        DelayEffect(sampleRate, ms, feedback = 0.5f, mix = 1f, maxDelayMs = 120f)
    }

    fun setMix(value: Float) { mix = value.coerceIn(0f, 1f) }

    override fun process(buffer: FloatArray, numSamples: Int) {
        val wet = FloatArray(numSamples)
        for (i in 0 until numSamples) wet[i] = buffer[i]
        val accum = FloatArray(numSamples)
        for (tap in taps) {
            val tapBuf = wet.copyOf()
            tap.process(tapBuf, numSamples)
            for (i in 0 until numSamples) accum[i] += tapBuf[i] / taps.size
        }
        for (i in 0 until numSamples) {
            buffer[i] = buffer[i] * (1 - mix) + accum[i] * mix
        }
    }

    override fun reset() {
        taps.forEach { it.reset() }
    }
}
