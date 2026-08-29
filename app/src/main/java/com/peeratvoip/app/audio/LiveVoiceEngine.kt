package com.peeratvoip.app.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Captures microphone audio, runs it through [VoiceEffectEngine] in real time,
 * and plays the processed signal back out (headset recommended to avoid
 * feedback loops). This is the "mic ao vivo" pipeline — it does not intercept
 * carrier phone-call audio (not possible on modern Android without root),
 * but lets the user hear/record their voice transformed live, and can feed
 * the processed stream into a VoIP call as the mic source app-side.
 */
class LiveVoiceEngine(
    private val sampleRate: Int = 44100,
) {
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(4096)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    val engine = VoiceEffectEngine(sampleRate.toFloat())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _levelMeter = MutableStateFlow(0f)
    val levelMeter: StateFlow<Float> = _levelMeter

    private var onRecordedChunk: ((ShortArray, Int) -> Unit)? = null

    fun setRecordingSink(sink: ((ShortArray, Int) -> Unit)?) {
        onRecordedChunk = sink
    }

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (_isRunning.value) return true
        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2,
            )

            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                minBufferSize * 2,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE,
            )

            if (record.state != AudioRecord.STATE_INITIALIZED || track.state != AudioTrack.STATE_INITIALIZED) {
                record.release()
                track.release()
                return false
            }

            audioRecord = record
            audioTrack = track
            record.startRecording()
            track.play()
            _isRunning.value = true

            job = scope.launch {
                val chunkSamples = minBufferSize / 2
                val shortBuf = ShortArray(chunkSamples)
                val floatBuf = FloatArray(chunkSamples)

                while (isActive && _isRunning.value) {
                    val read = record.read(shortBuf, 0, chunkSamples)
                    if (read > 0) {
                        for (i in 0 until read) {
                            floatBuf[i] = shortBuf[i] / 32768f
                        }
                        engine.process(floatBuf, read)

                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val s = (floatBuf[i] * 32767f)
                                .coerceIn(-32768f, 32767f)
                            shortBuf[i] = s.toInt().toShort()
                            sumSq += (floatBuf[i] * floatBuf[i]).toDouble()
                        }
                        val rms = sqrt(sumSq / read).toFloat()
                        _levelMeter.value = (rms * 3.2f).coerceIn(0f, 1f)

                        track.write(shortBuf, 0, read)
                        onRecordedChunk?.invoke(shortBuf, read)
                    }
                }
            }
            true
        } catch (t: Throwable) {
            Log.e("LiveVoiceEngine", "start() failed", t)
            stop()
            false
        }
    }

    fun stop() {
        _isRunning.value = false
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }
        try {
            audioTrack?.stop()
        } catch (_: Throwable) {
        }
        audioRecord?.release()
        audioTrack?.release()
        audioRecord = null
        audioTrack = null
        engine.reset()
        _levelMeter.value = 0f
    }

    fun sampleRate(): Int = sampleRate
}
