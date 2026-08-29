package com.peeratvoip.app.ui.screens.recordings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peeratvoip.app.audio.VoiceEngineHolder
import com.peeratvoip.app.audio.VoiceRecorder
import com.peeratvoip.app.audio.file.AudioFileProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ProcessedItem(
    val file: File,
    val presetName: String,
    val durationMs: Long,
)

data class FilesUiState(
    val processing: Boolean = false,
    val progress: Float = 0f,
    val recording: Boolean = false,
    val recordLevel: Float = 0f,
    val recordElapsedMs: Long = 0L,
    val error: String? = null,
    val items: List<ProcessedItem> = emptyList(),
)

/**
 * Drives the "Áudios" screen: record a voice message with the current effect,
 * OR pick an existing audio file and transform it — then list/play/share results.
 */
class FilesViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val recorder = VoiceRecorder()

    private val outputDir: File by lazy {
        File(getApplication<Application>().filesDir, "processed").apply { mkdirs() }
    }

    init {
        refresh()
        viewModelScope.launch {
            recorder.isRecording.collect { r -> _uiState.value = _uiState.value.copy(recording = r) }
        }
        viewModelScope.launch {
            recorder.level.collect { l -> _uiState.value = _uiState.value.copy(recordLevel = l) }
        }
        viewModelScope.launch {
            recorder.elapsedMs.collect { e -> _uiState.value = _uiState.value.copy(recordElapsedMs = e) }
        }
    }

    fun refresh() {
        val items = outputDir.listFiles()
            ?.filter { it.isFile && it.extension == "wav" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { ProcessedItem(it, prettyPresetFromName(it.name), 0L) }
            ?: emptyList()
        _uiState.value = _uiState.value.copy(items = items)
    }

    /** Starts recording the mic with the currently selected voice effect. */
    fun toggleRecording(hasPermission: Boolean, requestPermission: () -> Unit): Boolean {
        if (recorder.isRecording.value) {
            recorder.stop()
            refresh()
            return false
        }
        if (!hasPermission) {
            requestPermission()
            return false
        }
        // The live engine and the recorder can't both own the mic.
        if (VoiceEngineHolder.isRunning.value) VoiceEngineHolder.stop()

        val preset = VoiceEngineHolder.preset.value
        val out = File(outputDir, "rec_${preset.id}_${System.currentTimeMillis()}.wav")
        val ok = recorder.start(
            preset = preset,
            extraPitchSemitones = VoiceEngineHolder.extraPitchSemitones.value,
            wetDryMix = VoiceEngineHolder.wetDryMix.value,
            outputGain = VoiceEngineHolder.outputGain.value,
            file = out,
        )
        if (!ok) {
            _uiState.value = _uiState.value.copy(error = "Não foi possível acessar o microfone")
        }
        return ok
    }

    fun processFile(uri: Uri) {
        if (_uiState.value.processing) return
        val preset = VoiceEngineHolder.preset.value
        val extraPitch = VoiceEngineHolder.extraPitchSemitones.value
        val wetDry = VoiceEngineHolder.wetDryMix.value
        val gain = VoiceEngineHolder.outputGain.value

        _uiState.value = _uiState.value.copy(processing = true, progress = 0f, error = null)
        viewModelScope.launch {
            try {
                val safeName = "${preset.id}_${System.currentTimeMillis()}.wav"
                val out = File(outputDir, safeName)
                AudioFileProcessor.process(
                    context = getApplication(),
                    input = uri,
                    preset = preset,
                    extraPitchSemitones = extraPitch,
                    wetDryMix = wetDry,
                    outputGain = gain,
                    outputFile = out,
                    onProgress = { p ->
                        _uiState.value = _uiState.value.copy(progress = p)
                    },
                )
                _uiState.value = _uiState.value.copy(processing = false, progress = 1f)
                refresh()
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    processing = false,
                    error = t.message ?: "Falha ao processar o áudio",
                )
            }
        }
    }

    fun delete(item: ProcessedItem) {
        runCatching { item.file.delete() }
        refresh()
    }

    override fun onCleared() {
        super.onCleared()
        if (recorder.isRecording.value) recorder.stop()
    }

    private fun prettyPresetFromName(name: String): String {
        val core = name.removePrefix("rec_")
        val id = core.substringBefore('_')
        return com.peeratvoip.app.audio.VoicePreset.builtIns
            .firstOrNull { it.id == id }
            ?.let { "${it.emoji} ${it.name}" } ?: "Áudio"
    }
}
