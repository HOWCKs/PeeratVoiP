package com.peeratvoip.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.peeratvoip.app.ui.theme.LocalNeuPalette
import com.peeratvoip.app.ui.theme.NeuStyle
import com.peeratvoip.app.ui.theme.neu

/**
 * Inset text field that reads as a physical carved slot in the surface
 * (Campos de entrada com aparência física).
 */
@Composable
fun NeuInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
) {
    val palette = LocalNeuPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .neu(
                palette = palette,
                shape = RoundedCornerShape(16.dp),
                style = NeuStyle.CONCAVE,
                elevation = 6.dp,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = palette.textDisabled, style = LocalTextStyle.current)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = palette.textPrimary, fontSize = LocalTextStyle.current.fontSize),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
