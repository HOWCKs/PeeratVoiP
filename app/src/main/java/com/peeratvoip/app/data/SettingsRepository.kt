package com.peeratvoip.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "peeratvoip_settings")

/** Persists the user's last selected preset & live tuning knobs across app restarts. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val PRESET_ID = stringPreferencesKey("preset_id")
        val EXTRA_PITCH = floatPreferencesKey("extra_pitch")
        val WET_DRY = floatPreferencesKey("wet_dry")
        val OUTPUT_GAIN = floatPreferencesKey("output_gain")
        val DARK_MODE_OVERRIDE = booleanPreferencesKey("dark_mode_override")
        val USE_DARK = booleanPreferencesKey("use_dark")
    }

    val selectedPresetId: Flow<String> = context.dataStore.data.map { it[Keys.PRESET_ID] ?: "normal" }
    val extraPitch: Flow<Float> = context.dataStore.data.map { it[Keys.EXTRA_PITCH] ?: 0f }
    val wetDryMix: Flow<Float> = context.dataStore.data.map { it[Keys.WET_DRY] ?: 1f }
    val outputGain: Flow<Float> = context.dataStore.data.map { it[Keys.OUTPUT_GAIN] ?: 1f }
    val darkModeOverride: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE_OVERRIDE] ?: false }
    val useDark: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_DARK] ?: false }

    suspend fun setSelectedPreset(id: String) {
        context.dataStore.edit { it[Keys.PRESET_ID] = id }
    }

    suspend fun setExtraPitch(value: Float) {
        context.dataStore.edit { it[Keys.EXTRA_PITCH] = value }
    }

    suspend fun setWetDryMix(value: Float) {
        context.dataStore.edit { it[Keys.WET_DRY] = value }
    }

    suspend fun setOutputGain(value: Float) {
        context.dataStore.edit { it[Keys.OUTPUT_GAIN] = value }
    }

    suspend fun setDarkModePreference(override: Boolean, useDark: Boolean) {
        context.dataStore.edit {
            it[Keys.DARK_MODE_OVERRIDE] = override
            it[Keys.USE_DARK] = useDark
        }
    }
}
