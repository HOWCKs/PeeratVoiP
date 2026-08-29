package com.peeratvoip.app.audio.dsp

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Real-time phase-vocoder pitch shifter, adapted from Stephan M. Bernsee's
 * classic `smbPitchShift` public-domain algorithm.
 *
 * Shifts pitch by [pitchFactor] (1.0 = no change, 2.0 = one octave up,
 * 0.5 = one octave down) without changing the playback rate, which is what
 * makes "chipmunk" / "giant" / "robot base" voice effects possible while
 * keeping normal speech speed.
 *
 * Works on Float samples in [-1, 1] range, processed in a streaming fashion:
 * feed samples via [process] and read the same number of output samples back.
 */
class PitchShifter(
    private val sampleRate: Float,
    private val fftFrameSize: Int = 1024,
    private val oversampling: Int = 32,
) {
    private val stepSize = fftFrameSize / oversampling
    private val freqPerBin = sampleRate / fftFrameSize
    private val expectedPhaseDiff = 2.0 * PI * stepSize / fftFrameSize

    private val inFifo = DoubleArray(fftFrameSize)
    private val outFifo = DoubleArray(fftFrameSize)
    private val fftWorkspace = DoubleArray(2 * fftFrameSize)
    private val lastPhase = DoubleArray(fftFrameSize / 2 + 1)
    private val sumPhase = DoubleArray(fftFrameSize / 2 + 1)
    private val outputAccum = DoubleArray(2 * fftFrameSize)
    private val analyzedFreq = DoubleArray(fftFrameSize)
    private val analyzedMagn = DoubleArray(fftFrameSize)
    private val synthesizedFreq = DoubleArray(fftFrameSize)
    private val synthesizedMagn = DoubleArray(fftFrameSize)
    private val window = DoubleArray(fftFrameSize) { i ->
        -0.5 * cos(2.0 * PI * i / fftFrameSize) + 0.5
    }

    private var inFifoLatency = fftFrameSize - stepSize
    private var rover = inFifoLatency

    var pitchFactor: Float = 1.0f

    /** Processes [input] in place-friendly fashion, writing shifted output to [output]. Same length required. */
    fun process(input: FloatArray, output: FloatArray, numSamples: Int) {
        val pitch = pitchFactor.coerceIn(0.25f, 4.0f).toDouble()

        for (i in 0 until numSamples) {
            inFifo[rover] = input[i].toDouble()
            output[i] = outFifo[rover - inFifoLatency].toFloat()
            rover++

            if (rover >= fftFrameSize) {
                rover = inFifoLatency

                // Analysis window
                for (k in 0 until fftFrameSize) {
                    fftWorkspace[2 * k] = inFifo[k] * window[k]
                    fftWorkspace[2 * k + 1] = 0.0
                }

                FFT.transform(fftWorkspace, fftFrameSize, -1)

                val half = fftFrameSize / 2
                for (k in 0..half) {
                    val real = fftWorkspace[2 * k]
                    val imag = fftWorkspace[2 * k + 1]
                    val magn = 2.0 * sqrt(real * real + imag * imag)
                    val phase = atan2(imag, real)

                    var tmp = phase - lastPhase[k]
                    lastPhase[k] = phase
                    tmp -= k * expectedPhaseDiff
                    var qpd = (tmp / PI).toInt()
                    if (qpd >= 0) qpd += qpd and 1 else qpd -= qpd and 1
                    tmp -= PI * qpd
                    tmp = oversampling * tmp / (2.0 * PI)
                    tmp = k * freqPerBin + tmp * freqPerBin

                    analyzedMagn[k] = magn
                    analyzedFreq[k] = tmp
                }

                synthesizedMagn.fill(0.0)
                synthesizedFreq.fill(0.0)
                for (k in 0..half) {
                    val index = (k * pitch).toInt()
                    if (index <= half) {
                        synthesizedMagn[index] += analyzedMagn[k]
                        synthesizedFreq[index] = analyzedFreq[k] * pitch
                    }
                }

                for (k in 0..half) {
                    val magn = synthesizedMagn[k]
                    var tmp = synthesizedFreq[k]
                    tmp -= k * freqPerBin
                    tmp /= freqPerBin
                    tmp = 2.0 * PI * tmp / oversampling
                    tmp += k * expectedPhaseDiff
                    sumPhase[k] += tmp
                    val phase = sumPhase[k]

                    fftWorkspace[2 * k] = magn * cos(phase)
                    fftWorkspace[2 * k + 1] = magn * kotlin.math.sin(phase)
                }
                for (k in fftFrameSize + 2 until 2 * fftFrameSize) {
                    fftWorkspace[k] = 0.0
                }

                FFT.transform(fftWorkspace, fftFrameSize, 1)

                for (k in 0 until fftFrameSize) {
                    val w = window[k]
                    outputAccum[k] += 2.0 * w * fftWorkspace[2 * k] / (half * oversampling)
                }
                for (k in 0 until stepSize) {
                    outFifo[k] = outputAccum[k]
                }
                System.arraycopy(outputAccum, stepSize, outputAccum, 0, fftFrameSize)
                for (k in fftFrameSize - stepSize until fftFrameSize) {
                    outputAccum[k] = 0.0
                }
                for (k in 0 until fftFrameSize - stepSize) {
                    inFifo[k] = inFifo[k + stepSize]
                }
            }
        }
    }

    fun reset() {
        inFifo.fill(0.0)
        outFifo.fill(0.0)
        outputAccum.fill(0.0)
        lastPhase.fill(0.0)
        sumPhase.fill(0.0)
        rover = inFifoLatency
    }
}
