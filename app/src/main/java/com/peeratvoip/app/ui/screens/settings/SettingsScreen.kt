package com.peeratvoip.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.audio.LiveVoiceFxService
import com.peeratvoip.app.overlay.FloatingControlService
import com.peeratvoip.app.ui.components.NeuButton
import com.peeratvoip.app.ui.components.NeuButtonText
import com.peeratvoip.app.ui.components.NeuCard
import com.peeratvoip.app.ui.components.NeuInputField
import com.peeratvoip.app.ui.components.NeuToggle
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.PeeratVoipTheme

@Composable
fun SettingsScreen() {
    val palette = LocalNeuPalette.current
    val context = LocalContext.current
    var nickname by remember { mutableStateOf("") }
    var noiseSuppression by remember { mutableStateOf(true) }
    var autoStartOnHeadset by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Bold,
        )

        // Floating overlay + notification quick controls
        NeuCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Layers, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Menu flutuante & atalhos",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                }
                Text(
                    "Controle a voz enquanto usa outros apps (Discord, WhatsApp…): " +
                        "uma bolha flutuante por cima de tudo e controles rápidos na notificação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )

                NeuButton(
                    onClick = {
                        if (FloatingControlService.canDrawOverlays(context)) {
                            FloatingControlService.show(context)
                        } else {
                            val intent = Intent(
                                AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    },
                    accent = true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Layers, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        NeuButtonText(
                            if (FloatingControlService.canDrawOverlays(context)) "Mostrar menu flutuante"
                            else "Permitir sobreposição",
                        )
                    }
                }

                NeuButton(
                    onClick = { FloatingControlService.hide(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NeuButtonText("Ocultar menu flutuante")
                }

                NeuButton(
                    onClick = { LiveVoiceFxService.start(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        NeuButtonText("Fixar controles na notificação")
                    }
                }
            }
        }

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
