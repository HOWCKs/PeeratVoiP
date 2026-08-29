package com.peeratvoip.app.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peeratvoip.app.audio.LiveVoiceEngine
import com.peeratvoip.app.audio.VoiceEngineHolder
import com.peeratvoip.app.audio.VoicePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveUiState(
    val isRunning: Boolean = false,
    val level: Float = 0f,
    val selectedPreset: VoicePreset = VoicePreset.NORMAL,
    val extraPitchSemitones: Float = 0f,
    val wetDryMix: Float = 1f,
    val outputGain: Float = 1f,
    val permissionDenied: Boolean = false,
)

class LiveViewModel : ViewModel() {
    private val engine: LiveVoiceEngine = VoiceEngineHolder.engine

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            engine.isRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isRunning = running)
            }
        }
        viewModelScope.launch {
            engine.levelMeter.collect { level ->
                _uiState.value = _uiState.value.copy(level = level)
            }
        }
    }

    fun toggleLive(hasPermission: Boolean, requestPermission: () -> Unit) {
        if (_uiState.value.isRunning) {
            engine.stop()
        } else {
            if (!hasPermission) {
                requestPermission()
                _uiState.value = _uiState.value.copy(permissionDenied = true)
                return
            }
            _uiState.value = _uiState.value.copy(permissionDenied = false)
            val started = engine.start()
            if (!started) {
                _uiState.value = _uiState.value.copy(permissionDenied = true)
            }
        }
    }

    fun selectPreset(preset: VoicePreset) {
        engine.engine.preset = preset
        _uiState.value = _uiState.value.copy(selectedPreset = preset)
    }

    fun setExtraPitch(semitones: Float) {
        engine.engine.extraPitchSemitones = semitones
        _uiState.value = _uiState.value.copy(extraPitchSemitones = semitones)
    }

    fun setWetDryMix(mix: Float) {
        engine.engine.wetDryMix = mix
        _uiState.value = _uiState.value.copy(wetDryMix = mix)
    }

    fun setOutputGain(gain: Float) {
        engine.engine.outputGain = gain
        _uiState.value = _uiState.value.copy(outputGain = gain)
    }

    override fun onCleared() {
        super.onCleared()
        // Keep engine alive across screen navigation; only stop on app process death.
    }
}
