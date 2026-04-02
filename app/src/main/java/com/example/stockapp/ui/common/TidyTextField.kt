package com.example.stockapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TidyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    matchCardSurface: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    val backgroundColor = if (matchCardSurface) {
        StockAppColors.CardSurface
    } else {
        StockAppColors.FieldSurface
    }
    Row(
        modifier = modifier
            .height(42.dp)
            .background(
                if (enabled) backgroundColor else StockAppColors.DisabledSurface,
                RoundedCornerShape(50)
            )
            .border(
                1.dp,
                if (enabled) StockAppColors.FieldBorder else StockAppColors.CardBorder,
                RoundedCornerShape(50)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(StockAppColors.AccentCyan),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = if (enabled) StockAppColors.TextPrimary else StockAppColors.DisabledText
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = StockAppColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
