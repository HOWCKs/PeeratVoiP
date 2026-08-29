package com.peeratvoip.app.audio.dsp

/** A single stage in the real-time voice effect chain. */
interface AudioEffect {
    /** Processes [numSamples] float samples in [-1,1], in place. */
    fun process(buffer: FloatArray, numSamples: Int)
    fun reset() {}
}
