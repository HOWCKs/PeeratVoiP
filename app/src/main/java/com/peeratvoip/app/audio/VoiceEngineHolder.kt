package com.peeratvoip.app.audio

/**
 * Process-wide singleton holding the single [LiveVoiceEngine] instance so
 * every screen (Live, Presets picker, Settings) shares the same live audio
 * pipeline without re-creating AudioRecord/AudioTrack objects.
 */
object VoiceEngineHolder {
    val engine: LiveVoiceEngine by lazy { LiveVoiceEngine() }
}
