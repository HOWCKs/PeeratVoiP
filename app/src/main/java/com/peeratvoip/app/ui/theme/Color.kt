package com.peeratvoip.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Neumorphic palette. Every surface is derived from a single base color so that
 * the soft dual shadow (light source top-left, shadow bottom-right) reads
 * consistently across every card, button and input field in the app.
 */
data class NeuPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val shadowDark: Color,
    val shadowLight: Color,
    val accent: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val isDark: Boolean,
)

val LightNeuPalette = NeuPalette(
    background = Color(0xFFE6E9EF),
    surface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFFDCE0E8),
    shadowDark = Color(0xFFB9C1D0),
    shadowLight = Color(0xFFFFFFFF),
    accent = Color(0xFF7C6BFF),
    accentSecondary = Color(0xFF4FD1C5),
    textPrimary = Color(0xFF31384A),
    textSecondary = Color(0xFF6B7385),
    textDisabled = Color(0xFFA6AEC0),
    danger = Color(0xFFFF6B6B),
    success = Color(0xFF4FD18B),
    warning = Color(0xFFFFB65C),
    isDark = false,
)

val DarkNeuPalette = NeuPalette(
    background = Color(0xFF262B36),
    surface = Color(0xFF262B36),
    surfaceVariant = Color(0xFF2C323F),
    shadowDark = Color(0xFF1B1F27),
    shadowLight = Color(0xFF333B49),
    accent = Color(0xFF8E7CFF),
    accentSecondary = Color(0xFF56E0D3),
    textPrimary = Color(0xFFEDEFF5),
    textSecondary = Color(0xFF9AA2B6),
    textDisabled = Color(0xFF5A6274),
    danger = Color(0xFFFF7A7A),
    success = Color(0xFF5CE0A0),
    warning = Color(0xFFFFC373),
    isDark = true,
)
