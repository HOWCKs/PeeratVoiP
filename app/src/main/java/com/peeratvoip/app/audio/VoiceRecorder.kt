package com.peeratvoip.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.peeratvoip.app.audio.file.WavWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

/**
 * Records the microphone, runs it through a [VoiceEffectEngine] configured with
 * the currently selected preset/tuning, and writes the transformed result to a
 * 16-bit PCM WAV file — no speaker playback, so it can be used without headsets.
 *
 * This is the reliable, no-root path for "send a voice message with an effect":
 * record here, then share the resulting file to WhatsApp/Discord/etc.
 */
class VoiceRecorder(
    private val sampleRate: Int = 44100,
) {
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(4096)

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs

    @Volatile private var outputFile: File? = null

    @SuppressLint("MissingPermission")
    fun start(
        preset: VoicePreset,
        extraPitchSemitones: Float,
        wetDryMix: Float,
        outputGain: Float,
        file: File,
    ): Boolean {
        if (_isRecording.value) return true
        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2,
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            val engine = VoiceEffectEngine(sampleRate.toFloat()).apply {
                this.preset = preset
                this.extraPitchSemitones = extraPitchSemitones
                this.wetDryMix = wetDryMix
                this.outputGain = outputGain
            }

            outputFile = file
            audioRecord = record
            record.startRecording()
            _isRecording.value = true
            _elapsedMs.value = 0L

            job = scope.launch {
                val raf = RandomAccessFile(file, "rw")
                raf.setLength(0)
                raf.write(ByteArray(44)) // placeholder header

                val chunkSamples = minBufferSize / 2
                val shortBuf = ShortArray(chunkSamples)
                val floatBuf = FloatArray(chunkSamples)
                var totalPcmBytes = 0
                val startTime = System.currentTimeMillis()

                try {
                    while (isActive && _isRecording.value) {
                        val read = record.read(shortBuf, 0, chunkSamples)
                        if (read > 0) {
                            for (i in 0 until read) floatBuf[i] = shortBuf[i] / 32768f
                            engine.process(floatBuf, read)

                            var sumSq = 0.0
                            for (i in 0 until read) {
                                val s = (floatBuf[i] * 32767f).coerceIn(-32768f, 32767f)
                                shortBuf[i] = s.toInt().toShort()
                                sumSq += (floatBuf[i] * floatBuf[i]).toDouble()
                            }
                            val rms = sqrt(sumSq / read).toFloat()
                            _level.value = (rms * 3.2f).coerceIn(0f, 1f)
                            _elapsedMs.value = System.currentTimeMillis() - startTime

                            val bytes = ByteArray(read * 2)
                            var bi = 0
                            for (i in 0 until read) {
                                val v = shortBuf[i].toInt()
                                bytes[bi++] = (v and 0xFF).toByte()
                                bytes[bi++] = ((v shr 8) and 0xFF).toByte()
                            }
                            raf.write(bytes)
                            totalPcmBytes += read * 2
                        }
                    }
                } finally {
                    // Patch the WAV header now that the data size is known.
                    raf.seek(0)
                    raf.write(WavWriter.header(sampleRate, 1, totalPcmBytes))
                    raf.close()
                    engine.reset()
                }
            }
            true
        } catch (t: Throwable) {
            Log.e("VoiceRecorder", "start() failed", t)
            stop()
            false
        }
    }

    /** Stops recording; returns the finished file (or null if nothing captured). */
    fun stop(): File? {
        if (!_isRecording.value) return outputFile
        _isRecording.value = false
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }
        audioRecord?.release()
        audioRecord = null
        _level.value = 0f
        return outputFile
    }
}
