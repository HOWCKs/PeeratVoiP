package com.peeratvoip.app.ui.screens.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.components.NeuCard
import com.peeratvoip.app.ui.components.NeuIconBadge
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

/**
 * Placeholder gallery for saved voice-effect recordings. Recording-to-file
 * hook already exists in [com.peeratvoip.app.audio.LiveVoiceEngine.setRecordingSink];
 * wire it to a WAV writer here in a follow-up iteration.
 */
@Composable
fun RecordingsScreen() {
    val palette = LocalNeuPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Gravações",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Suas vozes salvas aparecerão aqui.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            ) {
                NeuIconBadge(icon = Icons.Filled.MusicNote, contentDescription = null, size = 64.dp)
                Text(
                    text = "Nenhuma gravação ainda",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = "Toque em gravar na tela Ao vivo para salvar sua voz transformada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textDisabled,
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RecordingsScreenPreview() {
    PeeratVoipTheme { RecordingsScreen() }
}
