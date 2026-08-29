package com.peeratvoip.app.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimal iterative radix-2 Cooley-Tukey FFT operating in-place on an
 * interleaved real/imaginary [Double] array of size 2*fftFrameSize.
 *
 * `sign = -1` performs the forward transform, `sign = 1` the inverse
 * (unnormalized, matching the classic smbFft reference implementation
 * used by many real-time phase-vocoder pitch shifters).
 */
object FFT {
    fun transform(fftBuffer: DoubleArray, fftFrameSize: Int, sign: Int) {
        var i: Int
        var bitm: Int
        var j = 0
        var le: Int
        var le2: Int
        var wr: Double
        var wi: Double
        var arg: Double
        var tr: Double
        var ti: Double
        var ur: Double
        var ui: Double

        val n = fftFrameSize shl 1

        i = 2
        while (i < n - 2) {
            if (i < j) {
                var tmp = fftBuffer[i]
                fftBuffer[i] = fftBuffer[j]
                fftBuffer[j] = tmp
                tmp = fftBuffer[i + 1]
                fftBuffer[i + 1] = fftBuffer[j + 1]
                fftBuffer[j + 1] = tmp
            }
            bitm = fftFrameSize
            while (bitm >= 2 && j >= bitm) {
                j -= bitm
                bitm = bitm shr 1
            }
            j += bitm
            i += 2
        }

        var mMax = 2
        while (n > mMax) {
            le = mMax shl 1
            le2 = le shl 1
            arg = PI / (mMax shr 1)
            wr = cos(arg)
            wi = sign * sin(arg)
            var localUr = 1.0
            var localUi = 0.0
            var mIdx = 0
            while (mIdx < mMax) {
                i = mIdx
                while (i < n) {
                    j = i + mMax
                    tr = fftBuffer[j] * localUr - fftBuffer[j + 1] * localUi
                    ti = fftBuffer[j] * localUi + fftBuffer[j + 1] * localUr
                    fftBuffer[j] = fftBuffer[i] - tr
                    fftBuffer[j + 1] = fftBuffer[i + 1] - ti
                    fftBuffer[i] += tr
                    fftBuffer[i + 1] += ti
                    i += le2
                }
                ur = localUr * wr - localUi * wi
                ui = localUr * wi + localUi * wr
                localUr = ur
                localUi = ui
                mIdx += 2
            }
            mMax = le
        }
    }
}
