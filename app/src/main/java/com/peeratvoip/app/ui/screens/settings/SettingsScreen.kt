package com.peeratvoip.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.components.NeuCard
import com.peeratvoip.app.ui.components.NeuInputField
import com.peeratvoip.app.ui.components.NeuToggle
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

@Composable
fun SettingsScreen() {
    val palette = LocalNeuPalette.current
    var nickname by remember { mutableStateOf("") }
    var noiseSuppression by remember { mutableStateOf(true) }
    var autoStartOnHeadset by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
        )

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Apelido", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                NeuInputField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = "Como devemos te chamar?",
                )
            }
        }

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SettingsToggleRow("Redução de ruído", noiseSuppression) { noiseSuppression = it }
                SettingsToggleRow("Iniciar ao conectar fone", autoStartOnHeadset) { autoStartOnHeadset = it }
                SettingsToggleRow("Manter tela ligada durante uso", keepScreenOn) { keepScreenOn = it }
            }
        }

        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sobre", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Text(
                    text = "PeeratVoiP transforma sua voz em tempo real com efeitos de personagem. " +
                        "Por limitações de segurança do Android, não é possível modificar o áudio de " +
                        "ligações telefônicas de operadora — use o modo Ao vivo com fones para chamadas " +
                        "de voz por apps de VoIP que usam o microfone do sistema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = LocalNeuPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
        NeuToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PeeratVoipTheme { SettingsScreen() }
}
