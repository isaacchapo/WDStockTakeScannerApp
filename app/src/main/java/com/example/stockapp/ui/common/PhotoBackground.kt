package com.example.stockapp.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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

        val baseGradient = Brush.radialGradient(
            colors = listOf(
                StockAppColors.NavyMid,
                StockAppColors.NavyBase,
                StockAppColors.NavyDeep
            ),
            center = Offset(width * 0.55f, height * 0.15f),
            radius = minDim * 1.25f
        )

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
            AnimatedOrbLayer()
            CompositionLocalProvider(LocalContentColor provides StockAppColors.TextPrimary) {
                content()
            }
        }
    }
}

@Composable
private fun StaticGlowLayer(width: Float, height: Float, minDim: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(StockAppColors.SoftGlowCyan, Color.Transparent),
                    center = Offset(width * 0.2f, height * 0.2f),
                    radius = minDim * 0.9f
                )
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(StockAppColors.SoftGlowAmber, Color.Transparent),
                    center = Offset(width * 0.85f, height * 0.8f),
                    radius = minDim * 1.1f
                )
            )
    )
}

@Composable
private fun AnimatedOrbLayer() {
    val transition = rememberInfiniteTransition(label = "orbs")
    val orb1X by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1X"
    )
    val orb1Y by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1Y"
    )
    val orb1Alpha by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1Alpha"
    )

    val orb2X by transition.animateFloat(
        initialValue = 0.68f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2X"
    )
    val orb2Y by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2Y"
    )
    val orb2Alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2Alpha"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val height = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val minDim = min(width, height)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            StockAppColors.AccentCyan.copy(alpha = orb1Alpha),
                            Color.Transparent
                        ),
                        center = Offset(width * orb1X, height * orb1Y),
                        radius = minDim * 0.6f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            StockAppColors.AccentAmber.copy(alpha = orb2Alpha),
                            Color.Transparent
                        ),
                        center = Offset(width * orb2X, height * orb2Y),
                        radius = minDim * 0.65f
                    )
                )
        )
    }
}
