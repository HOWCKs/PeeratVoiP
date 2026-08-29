package com.peeratvoip.app.ui.screens.recordings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peeratvoip.app.audio.VoiceEngineHolder
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
    val error: String? = null,
    val items: List<ProcessedItem> = emptyList(),
)

/**
 * Drives the "Áudios" screen: picks an audio file, runs it through the effect
 * engine using the currently selected preset/tuning and lists the results.
 */
class FilesViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val outputDir: File by lazy {
        File(getApplication<Application>().filesDir, "processed").apply { mkdirs() }
    }

    init {
        refresh()
    }

    fun refresh() {
        val items = outputDir.listFiles()
            ?.filter { it.isFile && it.extension == "wav" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { ProcessedItem(it, prettyPresetFromName(it.name), 0L) }
            ?: emptyList()
        _uiState.value = _uiState.value.copy(items = items)
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

    private fun prettyPresetFromName(name: String): String {
        val id = name.substringBefore('_')
        return com.peeratvoip.app.audio.VoicePreset.builtIns
            .firstOrNull { it.id == id }
            ?.let { "${it.emoji} ${it.name}" } ?: "Áudio"
    }
}
