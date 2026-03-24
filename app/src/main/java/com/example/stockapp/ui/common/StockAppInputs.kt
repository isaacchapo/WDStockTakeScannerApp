package com.example.stockapp.ui.common

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun stockOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StockAppColors.AccentCyan,
    unfocusedBorderColor = StockAppColors.CardBorder,
    disabledBorderColor = StockAppColors.CardBorder,
    errorBorderColor = StockAppColors.AccentAmber,
    focusedTextColor = StockAppColors.TextPrimary,
    unfocusedTextColor = StockAppColors.TextPrimary,
    disabledTextColor = StockAppColors.DisabledText,
    cursorColor = StockAppColors.AccentCyan,
    focusedContainerColor = StockAppColors.FieldSurface,
    unfocusedContainerColor = StockAppColors.FieldSurface,
    disabledContainerColor = StockAppColors.DisabledSurface,
    errorContainerColor = StockAppColors.FieldSurface,
    focusedLabelColor = StockAppColors.TextSecondary,
    unfocusedLabelColor = StockAppColors.TextSecondary,
    disabledLabelColor = StockAppColors.DisabledText,
    errorLabelColor = StockAppColors.AccentAmber
)
