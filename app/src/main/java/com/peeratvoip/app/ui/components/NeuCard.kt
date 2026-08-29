package com.peeratvoip.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu

/**
 * A soft, floating "display card" — the base container used across screens
 * for grouping content (Neumorphic "cartão de exibição").
 */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    contentPadding: androidx.compose.ui.unit.Dp = 20.dp,
    style: NeuStyle = NeuStyle.RAISED,
    content: @Composable Box.() -> Unit,
) {
    val palette = LocalNeuPalette.current
    Box(
        modifier = modifier
            .neu(
                palette = palette,
                shape = RoundedCornerShape(cornerRadius),
                style = style,
                elevation = elevation,
            )
            .padding(contentPadding),
    ) {
        content()
    }
}
