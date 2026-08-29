package com.peeratvoip.app.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide single source of truth for the live voice pipeline.
 *
 * It owns the single [LiveVoiceEngine] instance AND the currently selected
 * preset / tuning, exposed as [StateFlow]s so that every surface — the main
 * Compose UI, the floating overlay bubble and the notification quick-controls —
 * reads and mutates the exact same state and always stays in sync.
 */
object VoiceEngineHolder {
    val engine: LiveVoiceEngine by lazy { LiveVoiceEngine() }

    private val presets: List<VoicePreset> get() = VoicePreset.builtIns

    private val _presetIndex = MutableStateFlow(0)
    val presetIndex: StateFlow<Int> = _presetIndex.asStateFlow()

    private val _preset = MutableStateFlow(VoicePreset.NORMAL)
    val preset: StateFlow<VoicePreset> = _preset.asStateFlow()

    private val _extraPitchSemitones = MutableStateFlow(0f)
    val extraPitchSemitones: StateFlow<Float> = _extraPitchSemitones.asStateFlow()

    private val _wetDryMix = MutableStateFlow(1f)
    val wetDryMix: StateFlow<Float> = _wetDryMix.asStateFlow()

    private val _outputGain = MutableStateFlow(1f)
    val outputGain: StateFlow<Float> = _outputGain.asStateFlow()

    /** Mirrors [LiveVoiceEngine.isRunning] for convenience. */
    val isRunning: StateFlow<Boolean> get() = engine.isRunning
    val levelMeter: StateFlow<Float> get() = engine.levelMeter

    fun selectPreset(p: VoicePreset) {
        val idx = presets.indexOfFirst { it.id == p.id }.let { if (it >= 0) it else 0 }
        _presetIndex.value = idx
        _preset.value = presets[idx]
        engine.engine.preset = presets[idx]
    }

    fun selectPresetIndex(index: Int) {
        val idx = ((index % presets.size) + presets.size) % presets.size
        selectPreset(presets[idx])
    }

    fun nextPreset() = selectPresetIndex(_presetIndex.value + 1)

    fun previousPreset() = selectPresetIndex(_presetIndex.value - 1)

    fun setExtraPitch(semitones: Float) {
        _extraPitchSemitones.value = semitones
        engine.engine.extraPitchSemitones = semitones
    }

    fun setWetDryMix(mix: Float) {
        _wetDryMix.value = mix
        engine.engine.wetDryMix = mix
    }

    fun setOutputGain(gain: Float) {
        _outputGain.value = gain
        engine.engine.outputGain = gain
    }

    fun start(): Boolean = engine.start()

    fun stop() = engine.stop()

    fun toggle(): Boolean {
        return if (engine.isRunning.value) {
            engine.stop()
            false
        } else {
            engine.start()
        }
    }
}
