package com.peeratvoip.app.ui.screens.recordings

import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peeratvoip.app.audio.VoiceEngineHolder
import com.peeratvoip.app.ui.components.NeuButton
import com.peeratvoip.app.ui.components.NeuButtonText
import com.peeratvoip.app.ui.components.NeuCard
import com.peeratvoip.app.ui.components.NeuIconBadge
import com.peeratvoip.app.ui.components.NeuIconButton
import com.peeratvoip.app.ui.components.NeuSlider
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

/**
 * "Áudios" — pick an audio file, transform it with the currently selected voice
 * preset and manage the processed results (play / share / delete). This is the
 * file-based counterpart to the live mic mode.
 */
@Composable
fun RecordingsScreen(viewModel: FilesViewModel = viewModel()) {
    val palette = LocalNeuPalette.current
    val state by viewModel.uiState.collectAsState()
    val activePreset by VoiceEngineHolder.preset.collectAsState()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) viewModel.processFile(uri) }

    val player = remember { MediaPlayer() }
    var playingPath by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    fun togglePlay(path: String) {
        runCatching {
            if (playingPath == path) {
                player.stop(); player.reset(); playingPath = null
            } else {
                player.reset()
                player.setDataSource(path)
                player.prepare()
                player.start()
                playingPath = path
                player.setOnCompletionListener { playingPath = null }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Áudios",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Transforme um áudio existente com a voz selecionada.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeuIconBadge(icon = Icons.Filled.GraphicEq, contentDescription = null, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Voz aplicada", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                        Text(
                            "${activePreset.emoji} ${activePreset.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    "Escolha a voz na aba “Vozes”; ela será aplicada ao áudio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDisabled,
                )
                NeuButton(
                    onClick = { picker.launch("audio/*") },
                    accent = true,
                    enabled = !state.processing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Filled.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        NeuButtonText(if (state.processing) "Processando…" else "Escolher áudio")
                    }
                }

                if (state.processing) {
                    NeuSlider(
                        value = state.progress,
                        onValueChange = {},
                        valueRange = 0f..1f,
                    )
                    Text(
                        "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                    )
                }
                state.error?.let {
                    Text(it, color = palette.danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(
            text = "Resultados",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )

        if (state.items.isEmpty()) {
            NeuCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                ) {
                    NeuIconBadge(icon = Icons.Filled.GraphicEq, contentDescription = null, size = 56.dp)
                    Text(
                        "Nenhum áudio processado ainda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height((state.items.size.coerceAtMost(6) * 92).dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items) { item ->
                    NeuCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NeuIconButton(
                                icon = if (playingPath == item.file.absolutePath) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = "Reproduzir",
                                onClick = { togglePlay(item.file.absolutePath) },
                                size = 46.dp,
                                accent = playingPath == item.file.absolutePath,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.presetName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    item.file.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textDisabled,
                                    maxLines = 1,
                                )
                            }
                            NeuIconButton(
                                icon = Icons.Filled.Share,
                                contentDescription = "Compartilhar",
                                onClick = {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        item.file,
                                    )
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/wav"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Compartilhar áudio"))
                                },
                                size = 42.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            NeuIconButton(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Excluir",
                                onClick = {
                                    if (playingPath == item.file.absolutePath) {
                                        runCatching { player.stop(); player.reset() }
                                        playingPath = null
                                    }
                                    viewModel.delete(item)
                                },
                                size = 42.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RecordingsScreenPreview() {
    PeeratVoipTheme { RecordingsScreen() }
}
