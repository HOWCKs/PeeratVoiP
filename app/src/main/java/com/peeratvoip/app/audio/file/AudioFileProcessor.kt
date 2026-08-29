package com.peeratvoip.app.audio.file

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.peeratvoip.app.audio.VoiceEffectEngine
import com.peeratvoip.app.audio.VoicePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes an arbitrary audio file (mp3/m4a/aac/ogg/wav — anything the platform
 * [MediaCodec] can decode), runs every sample through a [VoiceEffectEngine]
 * configured with the chosen [VoicePreset], and writes the transformed result
 * to a 16-bit PCM WAV file. This mirrors the Samsung Sound Assistant use case of
 * transforming already-recorded/received audios.
 */
object AudioFileProcessor {

    data class Result(val outputFile: File, val durationMs: Long)

    /**
     * @param onProgress fraction 0..1 (best-effort, based on presentation time).
     */
    suspend fun process(
        context: Context,
        input: Uri,
        preset: VoicePreset,
        extraPitchSemitones: Float = 0f,
        wetDryMix: Float = 1f,
        outputGain: Float = 1f,
        outputFile: File,
        onProgress: (Float) -> Unit = {},
    ): Result = withContext(Dispatchers.Default) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, input, null)

        val trackIndex = selectAudioTrack(extractor)
            ?: throw IllegalArgumentException("Nenhuma faixa de áudio encontrada no arquivo")
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalArgumentException("Formato de áudio desconhecido")
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
            format.getLong(MediaFormat.KEY_DURATION)
        } else 0L

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        // Effect engine runs at the source sample rate, mono (we downmix).
        val engine = VoiceEffectEngine(sampleRate.toFloat()).apply {
            this.preset = preset
            this.extraPitchSemitones = extraPitchSemitones
            this.wetDryMix = wetDryMix
            this.outputGain = outputGain
        }

        // Write to a temp file first, then patch the WAV header sizes.
        val raf = RandomAccessFile(outputFile, "rw")
        raf.setLength(0)
        raf.write(ByteArray(44)) // placeholder header

        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        var totalPcmBytes = 0
        var floatBuf = FloatArray(8192)

        while (!sawOutputEos) {
            if (!sawInputEos) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val inBuf = codec.getInputBuffer(inIndex)!!
                    val sampleSize = extractor.readSampleData(inBuf, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEos = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            if (outIndex >= 0) {
                if (info.size > 0) {
                    val outBuf = codec.getOutputBuffer(outIndex)!!
                    outBuf.position(info.offset)
                    outBuf.limit(info.offset + info.size)
                    val shorts = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val frameCount = shorts.remaining() / channels

                    if (floatBuf.size < frameCount) floatBuf = FloatArray(frameCount)

                    // Downmix to mono float
                    for (f in 0 until frameCount) {
                        var acc = 0f
                        for (c in 0 until channels) acc += shorts.get() / 32768f
                        floatBuf[f] = acc / channels
                    }

                    // Process in chunks the engine can handle (<= 8192).
                    var offset = 0
                    while (offset < frameCount) {
                        val chunk = minOf(8192, frameCount - offset)
                        val slice = if (offset == 0 && chunk == frameCount) {
                            floatBuf
                        } else {
                            floatBuf.copyOfRange(offset, offset + chunk)
                        }
                        engine.process(slice, chunk)
                        writePcm16(raf, slice, chunk)
                        totalPcmBytes += chunk * 2
                        offset += chunk
                    }

                    if (durationUs > 0) {
                        onProgress((info.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f))
                    }
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEos = true
                }
            }
        }

        // Patch the WAV header (mono, 16-bit) now that we know the data size.
        val header = WavWriter.header(sampleRate, 1, totalPcmBytes)
        raf.seek(0)
        raf.write(header)
        raf.close()

        codec.stop()
        codec.release()
        extractor.release()
        engine.reset()

        onProgress(1f)
        Result(outputFile, durationUs / 1000)
    }

    private fun writePcm16(raf: RandomAccessFile, samples: FloatArray, count: Int) {
        val bb = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            bb.putShort((samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        }
        raf.write(bb.array())
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }
}
