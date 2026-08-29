package com.peeratvoip.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu
import kotlin.math.max

/**
 * Neumorphic live audio level meter: a carved track that fills with the accent
 * color proportional to the mic amplitude (0f..1f). Smoothly animated.
 */
@Composable
fun NeuLevelMeter(
    level: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
) {
    val palette = LocalNeuPalette.current
    val animatedLevel = remember { Animatable(0f) }
    LaunchedEffect(level) {
        animatedLevel.animateTo(level.coerceIn(0f, 1f))
    }

    val trackShape = RoundedCornerShape(12.dp)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .neu(palette = palette, shape = trackShape, style = NeuStyle.CONCAVE, elevation = 6.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            val barWidth = size.width / (barCount * 1.6f)
            val gap = barWidth * 0.6f
            val activeBars = (animatedLevel.value * barCount).toInt()
            for (i in 0 until barCount) {
                val heightFraction = max(0.08f, ((i + 1f) / barCount))
                val barHeight = size.height * heightFraction * 0.82f
                val x = i * (barWidth + gap) + gap
                val color = if (i <= activeBars) palette.accent else palette.shadowDark.copy(alpha = 0.35f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, (size.height - barHeight) / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                    style = Fill,
                )
            }
        }
    }
}
