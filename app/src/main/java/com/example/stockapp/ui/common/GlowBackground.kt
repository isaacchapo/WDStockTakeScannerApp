package com.example.stockapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.min

@androidx.compose.runtime.Composable
fun GlowBackground(
    baseColor: Color,
    modifier: Modifier = Modifier,
    content: @androidx.compose.runtime.Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val height = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val minDim = min(width, height)
        val glowPrimary = StockAppColors.SoftGlowCyan.copy(alpha = 0.32f)
        val glowSecondary = StockAppColors.SoftGlowAmber.copy(alpha = 0.24f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowPrimary, Color.Transparent),
                        center = Offset(width * 0.2f, height * 0.2f),
                        radius = minDim * 0.95f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowSecondary, Color.Transparent),
                        center = Offset(width * 0.85f, height * 0.8f),
                        radius = minDim * 1.1f
                    )
                )
        )
        content()
    }
}
