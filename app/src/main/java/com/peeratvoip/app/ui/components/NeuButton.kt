package com.peeratvoip.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu

/**
 * Tactile, pill/rounded-square neumorphic button. Presses invert the shadow
 * direction to feel like the surface is being pushed in (Botões realistas).
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    enabled: Boolean = true,
    accent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = LocalNeuPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val fill = when {
        !enabled -> palette.surfaceVariant
        accent -> palette.accent
        else -> palette.surface
    }
    val contentColor = when {
        !enabled -> palette.textDisabled
        accent -> Color.White
        else -> palette.textPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .neu(
                palette = palette,
                shape = shape,
                style = if (pressed) NeuStyle.PRESSED else NeuStyle.RAISED,
                elevation = if (pressed) 4.dp else 8.dp,
                fillColor = fill,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** Circular icon-only neumorphic button (raised "puck" look) — for transport controls. */
@Composable
fun NeuIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val palette = LocalNeuPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val fill = when {
        !enabled -> palette.surfaceVariant
        accent -> palette.accent
        else -> palette.surface
    }
    val tint = when {
        !enabled -> palette.textDisabled
        accent -> Color.White
        else -> palette.textPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .neu(
                palette = palette,
                shape = CircleShape,
                style = if (pressed) NeuStyle.PRESSED else NeuStyle.RAISED,
                elevation = if (pressed) 4.dp else 7.dp,
                fillColor = fill,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

@Composable
fun NeuButtonText(text: String, fontWeight: FontWeight = FontWeight.SemiBold) {
    Text(text = text, fontWeight = fontWeight)
}
