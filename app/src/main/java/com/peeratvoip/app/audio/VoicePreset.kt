package com.peeratvoip.app.audio

/**
 * Describes one character voice / tone preset. [pitchSemitones] drives the
 * phase-vocoder pitch shifter; the rest tweak the effect chain to create a
 * distinct character (robot, chipmunk, giant, telephone, etc.).
 */
data class VoicePreset(
    val id: String,
    val name: String,
    val emoji: String,
    val pitchSemitones: Float,
    val formant: Float = 1f,
    val ringModMix: Float = 0f,
    val ringModHz: Float = 30f,
    val distortionMix: Float = 0f,
    val bitCrushBits: Int = 16,
    val bitCrushHold: Int = 1,
    val echoMix: Float = 0f,
    val echoMs: Float = 180f,
    val reverbMix: Float = 0f,
    val lowPassHz: Float = 18000f,
    val highPassHz: Float = 40f,
    val bandTelephone: Boolean = false,
    val gainDb: Float = 0f,
    val isCustom: Boolean = false,
) {
    companion object {
        val NORMAL = VoicePreset(
            id = "normal", name = "Normal", emoji = "🎙️",
            pitchSemitones = 0f,
        )
        val CHIPMUNK = VoicePreset(
            id = "chipmunk", name = "Esquilo", emoji = "🐿️",
            pitchSemitones = 9f, highPassHz = 300f,
        )
        val GIANT = VoicePreset(
            id = "giant", name = "Gigante", emoji = "🗿",
            pitchSemitones = -9f, lowPassHz = 4000f, reverbMix = 0.18f,
        )
        val ROBOT = VoicePreset(
            id = "robot", name = "Robô", emoji = "🤖",
            pitchSemitones = -2f, ringModMix = 0.55f, ringModHz = 55f,
            bitCrushBits = 8, bitCrushHold = 2,
        )
        val MONSTER = VoicePreset(
            id = "monster", name = "Monstro", emoji = "👹",
            pitchSemitones = -7f, distortionMix = 0.5f, lowPassHz = 3200f, reverbMix = 0.25f,
        )
        val ALIEN = VoicePreset(
            id = "alien", name = "Alien", emoji = "👽",
            pitchSemitones = 4f, ringModMix = 0.35f, ringModHz = 110f, reverbMix = 0.2f,
        )
        val TELEPHONE = VoicePreset(
            id = "telephone", name = "Telefone", emoji = "☎️",
            pitchSemitones = 0f, bandTelephone = true, distortionMix = 0.12f,
        )
        val RADIO = VoicePreset(
            id = "radio", name = "Rádio", emoji = "📻",
            pitchSemitones = 0f, bitCrushBits = 10, bitCrushHold = 1,
            distortionMix = 0.2f, highPassHz = 500f, lowPassHz = 6000f,
        )
        val DEEP = VoicePreset(
            id = "deep", name = "Voz Grave", emoji = "🎬",
            pitchSemitones = -5f, lowPassHz = 6000f,
        )
        val HELIUM = VoicePreset(
            id = "helium", name = "Hélio", emoji = "🎈",
            pitchSemitones = 12f, highPassHz = 400f,
        )
        val ECHO_HALL = VoicePreset(
            id = "echo_hall", name = "Salão", emoji = "🏛️",
            pitchSemitones = 0f, echoMix = 0.32f, echoMs = 260f, reverbMix = 0.35f,
        )
        val DEMON = VoicePreset(
            id = "demon", name = "Demônio", emoji = "😈",
            pitchSemitones = -11f, distortionMix = 0.35f, ringModMix = 0.15f, ringModHz = 18f, reverbMix = 0.2f,
        )

        val builtIns = listOf(NORMAL, CHIPMUNK, GIANT, ROBOT, MONSTER, ALIEN, TELEPHONE, RADIO, DEEP, HELIUM, ECHO_HALL, DEMON)
    }
}
