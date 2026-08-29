package com.peeratvoip.app.ui.screens.presets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peeratvoip.app.audio.VoicePreset
import com.peeratvoip.app.ui.components.NeuCard
import com.peeratvoip.app.ui.components.NeuIconBadge
import com.peeratvoip.app.ui.components.NeuIconButton
import com.peeratvoip.app.ui.screens.live.LiveViewModel
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

/** Full grid of character voice presets ("efeitos de voz de personagem"). */
@Composable
fun PresetsScreen(
    onBack: () -> Unit,
    viewModel: LiveViewModel = viewModel(),
) {
    val palette = LocalNeuPalette.current
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeuIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = "Voltar",
                onClick = onBack,
                size = 44.dp,
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = "  Vozes de personagem",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(VoicePreset.builtIns) { preset ->
                val interactionSource = remember { MutableInteractionSource() }
                val selected = preset.id == state.selectedPreset.id
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { viewModel.selectPreset(preset) },
                    contentPadding = 14.dp,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        NeuIconBadge(
                            icon = Icons.Filled.GraphicEq,
                            contentDescription = preset.name,
                            selected = selected,
                            size = 52.dp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = preset.emoji,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.accent else palette.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PresetsScreenPreview() {
    PeeratVoipTheme {
        PresetsScreen(onBack = {})
    }
}
