package com.peeratvoip.app.ui.screens.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peeratvoip.app.audio.VoicePreset
import com.peeratvoip.app.ui.components.NeuCard
import com.peeratvoip.app.ui.components.NeuIconBadge
import com.peeratvoip.app.ui.components.NeuIconButton
import com.peeratvoip.app.ui.components.NeuLevelMeter
import com.peeratvoip.app.ui.components.NeuSlider
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

@Composable
fun LiveScreen(
    hasMicPermission: () -> Boolean,
    onRequestMicPermission: () -> Unit,
    onOpenPresets: () -> Unit,
    viewModel: LiveViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val palette = LocalNeuPalette.current
    val context = LocalContext.current

    // Keep the mic pipeline alive in the background (and show notification
    // quick-controls) whenever the live effect is running.
    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            com.peeratvoip.app.audio.LiveVoiceFxService.start(context)
        } else {
            com.peeratvoip.app.audio.LiveVoiceFxService.stop(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text(
                text = "PeeratVoiP",
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary,
            )
            Text(
                text = "Transforme sua voz em tempo real",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
        }

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = if (state.isRunning) "Ao vivo" else "Parado",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isRunning) palette.success else palette.textSecondary,
                    fontWeight = FontWeight.Bold,
                )

                NeuLevelMeter(level = state.level, modifier = Modifier.fillMaxWidth())

                NeuIconButton(
                    icon = if (state.isRunning) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Ligar/desligar voz ao vivo",
                    onClick = { viewModel.toggleLive(hasMicPermission(), onRequestMicPermission) },
                    size = 92.dp,
                    accent = true,
                )

                if (state.permissionDenied) {
                    Text(
                        text = "Precisamos da permissão de microfone para funcionar.",
                        color = palette.danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text = "Use fones de ouvido para evitar retorno de áudio (eco/microfonia).",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textDisabled,
                )
            }
        }

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Voz do personagem",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        text = "Ver todas ›",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent,
                        modifier = Modifier.clickableText(onOpenPresets),
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(VoicePreset.builtIns) { preset ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickableText { viewModel.selectPreset(preset) },
                        ) {
                            NeuIconBadge(
                                icon = Icons.Filled.GraphicEq,
                                contentDescription = preset.name,
                                selected = preset.id == state.selectedPreset.id,
                                size = 56.dp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${preset.emoji} ${preset.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (preset.id == state.selectedPreset.id) palette.accent else palette.textSecondary,
                            )
                        }
                    }
                }
            }
        }

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Filled.Tune, contentDescription = null, tint = palette.textSecondary)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = "  Ajuste fino",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                }

                LabeledSlider(
                    label = "Tom extra (semitons)",
                    value = state.extraPitchSemitones,
                    range = -12f..12f,
                    valueText = "${state.extraPitchSemitones.toInt()}",
                    onChange = viewModel::setExtraPitch,
                )
                LabeledSlider(
                    label = "Mistura efeito/original",
                    value = state.wetDryMix,
                    range = 0f..1f,
                    valueText = "${(state.wetDryMix * 100).toInt()}%",
                    onChange = viewModel::setWetDryMix,
                )
                LabeledSlider(
                    label = "Volume de saída",
                    value = state.outputGain,
                    range = 0f..2f,
                    valueText = "${(state.outputGain * 100).toInt()}%",
                    onChange = viewModel::setOutputGain,
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
) {
    val palette = LocalNeuPalette.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            Text(text = valueText, style = MaterialTheme.typography.bodyMedium, color = palette.accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        NeuSlider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun Modifier.clickableText(onClick: () -> Unit): Modifier = this.composed {
    androidx.compose.foundation.clickable(
        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun LiveScreenPreview() {
    PeeratVoipTheme {
        LiveScreen(hasMicPermission = { true }, onRequestMicPermission = {}, onOpenPresets = {})
    }
}
