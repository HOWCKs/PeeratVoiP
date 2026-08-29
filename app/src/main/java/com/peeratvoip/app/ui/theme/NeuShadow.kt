package com.peeratvoip.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Style of the neumorphic depth effect. */
enum class NeuStyle { FLAT, RAISED, PRESSED, CONCAVE }

/**
 * Draws the two soft, offset shadows (light source top-left) that define the
 * neumorphic "soft-UI" look, then fills the shape with [fillColor].
 *
 * Implementation note: we intentionally DO NOT use an offscreen compositing
 * layer here. An offscreen layer is clipped to the element's rectangular bounds,
 * which cut the rounded outer shadow into hard triangles at each corner — the
 * "recortes" visible before. Drawing straight into the shared canvas lets the
 * soft shadow bleed correctly past the rounded corners.
 *
 * - [NeuStyle.RAISED]: element pops out of the surface (buttons, cards at rest).
 * - [NeuStyle.PRESSED]: shadows flip inward, simulating being pushed in.
 * - [NeuStyle.CONCAVE]: carved-in well, used for track backgrounds.
 * - [NeuStyle.FLAT]: no shadow, just the fill.
 */
@Stable
fun Modifier.neu(
    palette: NeuPalette,
    shape: Shape,
    style: NeuStyle = NeuStyle.RAISED,
    lightSource: Offset = Offset(-1f, -1f),
    elevation: Dp = 8.dp,
    fillColor: Color? = null,
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = outline.toPath()

    val e = elevation.toPx()
    val fill = fillColor ?: palette.surface

    when (style) {
        NeuStyle.FLAT -> {
            drawPath(path, color = fill, style = Fill)
        }

        NeuStyle.RAISED -> {
            // dark shadow toward the light-source-opposite corner (bottom-right)
            drawSoftShadow(path, palette.shadowDark, e, Offset(e * -lightSource.x, e * -lightSource.y))
            // light highlight toward the light source (top-left)
            drawSoftShadow(path, palette.shadowLight, e, Offset(e * lightSource.x, e * lightSource.y))
            drawPath(path, color = fill, style = Fill)
        }

        NeuStyle.PRESSED -> {
            drawPath(path, color = fill, style = Fill)
            drawInnerShadow(path, palette.shadowDark, e * 0.7f, Offset(e * -lightSource.x * 0.5f, e * -lightSource.y * 0.5f))
            drawInnerShadow(path, palette.shadowLight, e * 0.7f, Offset(e * lightSource.x * 0.5f, e * lightSource.y * 0.5f))
        }

        NeuStyle.CONCAVE -> {
            drawPath(path, color = fill, style = Fill)
            drawInnerShadow(path, palette.shadowDark, e, Offset(e * -lightSource.x * 0.6f, e * -lightSource.y * 0.6f))
            drawInnerShadow(path, palette.shadowLight, e, Offset(e * lightSource.x * 0.6f, e * lightSource.y * 0.6f))
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> Path().apply { addRect(rect) }
}

private fun DrawScope.drawSoftShadow(
    path: Path,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    if (color.alpha == 0f) return
    val paint = Paint()
    paint.color = color
    val frameworkPaint = paint.asFrameworkPaint()
    frameworkPaint.isAntiAlias = true
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

/** Approximates an inner shadow by stroking a blurred path clipped to the shape. */
private fun DrawScope.drawInnerShadow(
    path: Path,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    if (color.alpha == 0f) return
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(path)
        val paint = Paint()
        paint.color = color
        paint.style = PaintingStyle.Stroke
        paint.strokeWidth = blurRadiusPx * 1.6f
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        if (blurRadiusPx > 0f) {
            frameworkPaint.maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.translate(offset.x, offset.y)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}
