package com.peeratvoip.app.ui.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * Thin ViewModel over [VoiceEngineHolder]. All real state lives in the holder so
 * the notification quick-controls and floating overlay stay perfectly in sync
 * with whatever the on-screen UI shows.
 */
class LiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            VoiceEngineHolder.isRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isRunning = running)
            }
        }
        viewModelScope.launch {
            VoiceEngineHolder.levelMeter.collect { level ->
                _uiState.value = _uiState.value.copy(level = level)
            }
        }
        viewModelScope.launch {
            VoiceEngineHolder.preset.collect { preset ->
                _uiState.value = _uiState.value.copy(selectedPreset = preset)
            }
        }
        viewModelScope.launch {
            VoiceEngineHolder.extraPitchSemitones.collect { v ->
                _uiState.value = _uiState.value.copy(extraPitchSemitones = v)
            }
        }
        viewModelScope.launch {
            VoiceEngineHolder.wetDryMix.collect { v ->
                _uiState.value = _uiState.value.copy(wetDryMix = v)
            }
        }
        viewModelScope.launch {
            VoiceEngineHolder.outputGain.collect { v ->
                _uiState.value = _uiState.value.copy(outputGain = v)
            }
        }
    }

    fun toggleLive(hasPermission: Boolean, requestPermission: () -> Unit) {
        if (VoiceEngineHolder.isRunning.value) {
            VoiceEngineHolder.stop()
        } else {
            if (!hasPermission) {
                requestPermission()
                _uiState.value = _uiState.value.copy(permissionDenied = true)
                return
            }
            _uiState.value = _uiState.value.copy(permissionDenied = false)
            val started = VoiceEngineHolder.start()
            if (!started) {
                _uiState.value = _uiState.value.copy(permissionDenied = true)
            }
        }
    }

    fun selectPreset(preset: VoicePreset) = VoiceEngineHolder.selectPreset(preset)

    fun setExtraPitch(semitones: Float) = VoiceEngineHolder.setExtraPitch(semitones)

    fun setWetDryMix(mix: Float) = VoiceEngineHolder.setWetDryMix(mix)

    fun setOutputGain(gain: Float) = VoiceEngineHolder.setOutputGain(gain)

    override fun onCleared() {
        super.onCleared()
        // Keep engine alive across screen navigation; only stop on app process death.
    }
}
