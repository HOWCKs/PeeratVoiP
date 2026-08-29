package com.peeratvoip.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu

/** Embossed circular icon badge — "Ícones em relevo" — used for presets, list rows, etc. */
@Composable
fun NeuIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: androidx.compose.ui.graphics.Color? = null,
    selected: Boolean = false,
) {
    val palette = LocalNeuPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .neu(
                palette = palette,
                shape = CircleShape,
                style = if (selected) NeuStyle.PRESSED else NeuStyle.RAISED,
                elevation = 6.dp,
                fillColor = if (selected) palette.accent else palette.surface,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: (if (selected) androidx.compose.ui.graphics.Color.White else palette.textSecondary),
            modifier = Modifier.size(size * 0.46f),
        )
    }
}
