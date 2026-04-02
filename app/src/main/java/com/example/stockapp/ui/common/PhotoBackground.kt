package com.example.stockapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stockapp.R
import kotlin.math.min

@Composable
fun PhotoBackground(
    imageResId: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val targetWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val targetHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val request = remember(imageResId, targetWidthPx, targetHeightPx) {
        ImageRequest.Builder(context)
            .data(imageResId)
            .size(targetWidthPx, targetHeightPx)
            .build()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        content()
    }
}

@Composable
fun StockAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StockAppColors.NavyBase)
    ) {
        val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val height = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val minDim = min(width, height)

        val baseGradient = remember(width, height, minDim) {
            Brush.radialGradient(
                colors = listOf(
                    StockAppColors.NavyMid,
                    StockAppColors.NavyBase,
                    StockAppColors.NavyDeep
                ),
                center = Offset(width * 0.55f, height * 0.15f),
                radius = minDim * 1.25f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseGradient)
        )

        PhotoBackground(
            imageResId = R.drawable.stockbackground,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StockAppColors.Veil)
            )
            StaticGlowLayer(width = width, height = height, minDim = minDim)
            CompositionLocalProvider(LocalContentColor provides StockAppColors.TextPrimary) {
                content()
            }
        }
    }
}

@Composable
private fun StaticGlowLayer(width: Float, height: Float, minDim: Float) {
    val glowCyan = remember(width, height, minDim) {
        Brush.radialGradient(
            colors = listOf(StockAppColors.SoftGlowCyan, Color.Transparent),
            center = Offset(width * 0.2f, height * 0.2f),
            radius = minDim * 0.9f
        )
    }
    val glowAmber = remember(width, height, minDim) {
        Brush.radialGradient(
            colors = listOf(StockAppColors.SoftGlowAmber, Color.Transparent),
            center = Offset(width * 0.85f, height * 0.8f),
            radius = minDim * 1.1f
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glowCyan)
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glowAmber)
    )
}
