package com.peeratvoip.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Style of the neumorphic depth effect. */
enum class NeuStyle { FLAT, RAISED, PRESSED, CONCAVE }

/**
 * Draws the two soft, offset shadows (light source top-left) that define the
 * neumorphic "soft-UI" look, then fills the shape with [fillColor].
 *
 * - [NeuStyle.RAISED]: element pops out of the surface (buttons, cards at rest).
 * - [NeuStyle.PRESSED]: shadows flip inward, simulating the element being pushed in.
 * - [NeuStyle.CONCAVE]: carved-in well, used for track backgrounds (sliders, inputs).
 * - [NeuStyle.FLAT]: no shadow, just the fill (used while dragging/disabled).
 */
@Stable
fun Modifier.neu(
    palette: NeuPalette,
    shape: Shape,
    style: NeuStyle = NeuStyle.RAISED,
    lightSource: Offset = Offset(-1f, -1f),
    elevation: Dp = 8.dp,
    fillColor: Color? = null,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawBehind {
        val cornerShapeOutline = shape.createOutline(size, layoutDirection, this)
        val path = when (cornerShapeOutline) {
            is androidx.compose.ui.graphics.Outline.Generic -> cornerShapeOutline.path
            is androidx.compose.ui.graphics.Outline.Rounded -> {
                val p = androidx.compose.ui.graphics.Path()
                p.addRoundRect(cornerShapeOutline.roundRect)
                p
            }
            is androidx.compose.ui.graphics.Outline.Rectangle -> {
                val p = androidx.compose.ui.graphics.Path()
                p.addRect(cornerShapeOutline.rect)
                p
            }
        }

        val elevationPx = elevation.toPx()
        val fill = fillColor ?: palette.surface

        when (style) {
            NeuStyle.FLAT -> {
                drawPath(path, color = fill, style = Fill)
            }
            NeuStyle.RAISED -> {
                drawSoftShadow(path, palette.shadowDark, elevationPx, Offset(elevationPx * -lightSource.x, elevationPx * -lightSource.y))
                drawSoftShadow(path, palette.shadowLight, elevationPx, Offset(elevationPx * lightSource.x, elevationPx * lightSource.y))
                drawPath(path, color = fill, style = Fill)
            }
            NeuStyle.PRESSED -> {
                drawPath(path, color = fill, style = Fill)
                drawInnerShadow(path, palette.shadowDark, elevationPx * 0.7f, Offset(elevationPx * -lightSource.x * 0.5f, elevationPx * -lightSource.y * 0.5f))
                drawInnerShadow(path, palette.shadowLight, elevationPx * 0.7f, Offset(elevationPx * lightSource.x * 0.5f, elevationPx * lightSource.y * 0.5f))
            }
            NeuStyle.CONCAVE -> {
                drawPath(path, color = fill, style = Fill)
                drawInnerShadow(path, palette.shadowDark, elevationPx, Offset(elevationPx * -lightSource.x * 0.6f, elevationPx * -lightSource.y * 0.6f))
                drawInnerShadow(path, palette.shadowLight, elevationPx, Offset(elevationPx * lightSource.x * 0.6f, elevationPx * lightSource.y * 0.6f))
            }
        }
    }

private fun DrawScope.drawSoftShadow(
    path: androidx.compose.ui.graphics.Path,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    val paint = Paint()
    paint.color = color
    val frameworkPaint = paint.asFrameworkPaint()
    if (blurRadiusPx > 0f) {
        frameworkPaint.maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(offset.x, offset.y)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}

/** Approximates an inner shadow by drawing an inverted-alpha blurred ring clipped to the shape. */
private fun DrawScope.drawInnerShadow(
    path: androidx.compose.ui.graphics.Path,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    clipPath(path) {
        val paint = Paint()
        paint.color = color
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = blurRadiusPx * 1.6f
        val frameworkPaint = paint.asFrameworkPaint()
        if (blurRadiusPx > 0f) {
            frameworkPaint.maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offset.x, offset.y)
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }
}

private inline fun DrawScope.clipPath(
    path: androidx.compose.ui.graphics.Path,
    block: DrawScope.() -> Unit,
) {
    drawContext.canvas.save()
    drawContext.canvas.clipPath(path)
    block()
    drawContext.canvas.restore()
}
