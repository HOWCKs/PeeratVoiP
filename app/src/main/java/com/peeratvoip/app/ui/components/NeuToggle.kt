package com.peeratvoip.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu

/** Tactile neumorphic on/off switch — the "toggle button" from the graphic elements list. */
@Composable
fun NeuToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeuPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val knobOffset by animateDpAsState(if (checked) 24.dp else 2.dp, label = "toggleKnob")
    val trackColorFraction by animateFloatAsState(if (checked) 1f else 0f, label = "toggleColor")
    val trackColor = lerp(palette.surfaceVariant, palette.accent, trackColorFraction)

    Box(
        modifier = modifier
            .width(50.dp)
            .height(28.dp)
            .neu(
                palette = palette,
                shape = CircleShape,
                style = NeuStyle.CONCAVE,
                elevation = 5.dp,
                fillColor = trackColor,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .offset(x = knobOffset)
                .size(24.dp)
                .neu(
                    palette = palette,
                    shape = CircleShape,
                    style = NeuStyle.RAISED,
                    elevation = 5.dp,
                ),
        )
    }
}
