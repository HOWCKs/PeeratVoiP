package com.peeratvoip.app.audio.file

import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal streaming 16-bit PCM mono/stereo WAV writer. The RIFF/data sizes are
 * written as placeholders and patched at [close] via a random-access rewrite by
 * the caller, or — when the total sample count is known up-front — written
 * directly. Here we buffer to a growable output and finalize sizes at the end.
 */
class WavWriter(
    private val out: OutputStream,
    private val sampleRate: Int,
    private val channels: Int = 1,
) {
    private val buffered = BufferedOutputStream(out)
    private var dataBytes = 0
    private val header = ByteArray(44)
    private var headerWritten = false

    /** Writes a chunk of float samples in [-1,1]. */
    fun writeFloats(samples: FloatArray, count: Int) {
        if (!headerWritten) {
            // reserve header space with a placeholder; final sizes patched by finalizeTo()
            headerWritten = true
        }
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            val v = (samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            bytes.putShort(v)
        }
        buffered.write(bytes.array(), 0, count * 2)
        dataBytes += count * 2
    }

    /** Writes 16-bit PCM shorts directly. */
    fun writeShorts(samples: ShortArray, count: Int) {
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) bytes.putShort(samples[i])
        buffered.write(bytes.array(), 0, count * 2)
        dataBytes += count * 2
    }

    fun dataByteCount(): Int = dataBytes

    fun flush() = buffered.flush()

    fun close() = buffered.close()

    companion object {
        /** Builds a 44-byte canonical WAV header for [dataBytes] of PCM16 audio. */
        fun header(sampleRate: Int, channels: Int, dataBytes: Int): ByteArray {
            val byteRate = sampleRate * channels * 2
            val blockAlign = channels * 2
            val bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            bb.put("RIFF".toByteArray(Charsets.US_ASCII))
            bb.putInt(36 + dataBytes)
            bb.put("WAVE".toByteArray(Charsets.US_ASCII))
            bb.put("fmt ".toByteArray(Charsets.US_ASCII))
            bb.putInt(16)
            bb.putShort(1) // PCM
            bb.putShort(channels.toShort())
            bb.putInt(sampleRate)
            bb.putInt(byteRate)
            bb.putShort(blockAlign.toShort())
            bb.putShort(16) // bits per sample
            bb.put("data".toByteArray(Charsets.US_ASCII))
            bb.putInt(dataBytes)
            return bb.array()
        }
    }
}
