# PeeratVoiP 🎙️

App Android (Kotlin + Jetpack Compose) para **transformar sua voz em tempo real** com efeitos de personagem e tons customizáveis, com uma interface no estilo **Neumorphic UI** (botões e cards com sombras suaves, campos "físicos", ícones em relevo e controles táteis).

## ✨ Funcionalidades

- **Voz ao vivo**: capture o microfone e ouça (ou envie) sua voz já transformada em tempo real, com baixa latência (usar fones de ouvido é recomendado para evitar realimentação/eco).
- **Efeitos de personagem** prontos: Esquilo, Gigante, Robô, Monstro, Alien, Telefone, Rádio, Voz Grave, Hélio, Salão (eco/reverb), Demônio, e Normal.
- **Ajuste fino**: pitch extra em semitons, mistura efeito/original (wet/dry) e ganho de saída, tudo com sliders neumórficos.
- **Motor de DSP próprio**, sem dependências nativas externas:
  - Pitch-shifter em tempo real (phase vocoder, baseado no algoritmo clássico de Bernsee) — muda o tom sem alterar a velocidade da fala.
  - Filtros biquad (passa-baixa/alta, banda, shelving) para timbre de rádio/telefone/voz grave.
  - Ring modulator + bitcrusher para robôs/aliens.
  - Delay com feedback + reverb simples multi-tap para eco/salão.
- **Design System Neumórfico** reutilizável em `ui/theme` e `ui/components`: `NeuCard`, `NeuButton`, `NeuIconButton`, `NeuInputField`, `NeuSlider`, `NeuToggle`, `NeuLevelMeter`, `NeuBottomNavBar`.

- **Menu flutuante (overlay)**: uma bolha arrastável que fica por cima de qualquer app (estilo *buzmenow* / Samsung Sound Assistant). Toque para expandir um painel com carrossel de vozes (◀ ▶), ajuste de tom (− +) e liga/desliga — troque a voz sem sair do Discord/WhatsApp. Requer a permissão "Sobrepor a outros apps".
- **Controles rápidos na notificação**: quando a voz ao vivo está ativa, uma notificação persistente traz atalhos para pausar/ativar e trocar de preset (anterior/próxima) direto da barra de notificações.
- **Processar áudios (arquivos)**: na aba **Áudios**, escolha um arquivo (mp3/m4a/aac/ogg/wav), aplique a voz selecionada e reproduza/compartilhe/exclua o resultado — igual ao caso de uso do Sound Assistant com áudios recebidos. O resultado é salvo como WAV.

## ⚠️ Sobre "chamadas de voz ao vivo"

O Android moderno **não permite** que apps de terceiros interceptem/modifiquem o áudio de uma **ligação telefônica da operadora** (GSM/VoLTE) — isso foi bloqueado por segurança/privacidade há vários anos, mesmo para apps como o Samsung Sound Assistant fora de dispositivos Samsung/condições especiais, e não é viável sem root.

Este app, portanto, foca no cenário que **é possível e funciona de verdade**: processar o microfone do sistema em tempo real (modo "Ao vivo"), o que cobre:

- Falar ao vivo com efeito de voz enquanto grava ou pratica.
- Alimentar apps de VoIP (Discord, WhatsApp, Google Meet, etc.) que usam o microfone padrão do Android, já que o áudio processado é reproduzido pelo `AudioTrack` do sistema.

## 🏗️ Arquitetura

```
app/src/main/java/com/peeratvoip/app/
├── audio/
│   ├── dsp/              # FFT, PitchShifter, filtros biquad, delay/reverb, ring mod, bitcrusher
│   ├── VoicePreset.kt    # Definição dos personagens/tons
│   ├── VoiceEffectEngine.kt   # Cadeia de efeitos (pipeline) aplicando um VoicePreset
│   ├── LiveVoiceEngine.kt     # Captura AudioRecord -> engine -> AudioTrack em tempo real
│   ├── LiveVoiceFxService.kt  # Foreground service + controles rápidos na notificação
│   ├── VoiceEngineHolder.kt   # Fonte única de verdade (engine + preset + tom) via StateFlow
│   └── file/                  # AudioFileProcessor (MediaCodec) + WavIo (writer WAV)
├── overlay/FloatingControlService.kt # Bolha/menu flutuante por cima de outros apps
├── data/SettingsRepository.kt # Preferências persistidas via DataStore
└── ui/
    ├── theme/            # Paleta neumórfica + Modifier.neu() (sombra dupla)
    ├── components/       # Design system: NeuCard, NeuButton, NeuSlider, NeuToggle, ...
    └── screens/          # Ao vivo, Vozes (presets), Gravações, Ajustes
```

## 🔧 Build

O app é compilado automaticamente pelo **GitHub Actions** (`.github/workflows/android-build.yml`) a cada push/PR:

1. `assembleDebug` roda em todo push/PR e o `.apk` fica disponível como artifact do workflow.
2. Um `assembleRelease` (assinado) roda ao dar merge em `main`, caso os secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` e `RELEASE_KEY_PASSWORD` estejam configurados no repositório; sem eles, o release simplesmente não roda (não quebra o CI).

Para compilar localmente (necessário Android SDK + JDK 17):

```bash
./gradlew assembleDebug
```

O APK debug fica em `app/build/outputs/apk/debug/app-debug.apk`.

## 🎨 Design System Neumórfico

Toda a base visual está em `ui/theme/NeuShadow.kt`, com o `Modifier.neu(...)` que desenha duas sombras suaves deslocadas (clara e escura) para simular profundidade:

- `NeuStyle.RAISED` — elemento "pop" para fora da superfície (botões, cards).
- `NeuStyle.PRESSED` — sombra invertida ao pressionar (feedback tátil).
- `NeuStyle.CONCAVE` — poço esculpido (trilhos de slider, campos de texto).

Paletas claras/escuras em `ui/theme/Color.kt` (`LightNeuPalette` / `DarkNeuPalette`), acessíveis via `LocalNeuPalette.current`.
