package com.peeratvoip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu
import kotlin.math.roundToInt

/**
 * Tactile neumorphic slider: a carved-in track with a raised, draggable knob.
 * Used for pitch/formant/rate controls on the Live screen (Elementos gráficos táteis).
 */
@Composable
fun NeuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    trackHeight: androidx.compose.ui.unit.Dp = 14.dp,
    knobSize: androidx.compose.ui.unit.Dp = 30.dp,
    accentFill: Boolean = true,
) {
    val palette = LocalNeuPalette.current
    var widthPx by remember { mutableStateOf(0f) }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    fun updateFromX(x: Float) {
        if (widthPx <= 0f) return
        val newFraction = (x / widthPx).coerceIn(0f, 1f)
        onValueChange(valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(knobSize)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(valueRange) {
                detectTapGestures { offset -> updateFromX(offset.x) }
            }
            .pointerInput(valueRange) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateFromX(change.position.x)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Carved track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .neu(
                    palette = palette,
                    shape = RoundedCornerShape(trackHeight),
                    style = NeuStyle.CONCAVE,
                    elevation = 5.dp,
                ),
        ) {
            if (accentFill) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .padding(2.dp)
                        .background(palette.accent.copy(alpha = 0.55f), RoundedCornerShape(trackHeight)),
                )
            }
        }

        // Raised knob
        Knob(fraction = fraction, widthPx = widthPx, knobSize = knobSize)
    }
}

@Composable
private fun Knob(fraction: Float, widthPx: Float, knobSize: androidx.compose.ui.unit.Dp) {
    val palette = LocalNeuPalette.current
    val density = LocalDensity.current
    val knobPx = with(density) { knobSize.toPx() }
    val xOffsetPx = (fraction * (widthPx - knobPx)).coerceAtLeast(0f)
    Box(
        modifier = Modifier
            .offset { IntOffset(xOffsetPx.roundToInt(), 0) }
            .size(knobSize)
            .neu(
                palette = palette,
                shape = CircleShape,
                style = NeuStyle.RAISED,
                elevation = 6.dp,
                fillColor = palette.accent,
            ),
    )
}
