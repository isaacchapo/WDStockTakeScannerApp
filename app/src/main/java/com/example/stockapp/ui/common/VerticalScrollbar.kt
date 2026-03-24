package com.example.stockapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun VerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thickness: Dp = 2.dp,
    minThumbHeight: Dp = 24.dp,
    thumbColor: Color = StockAppColors.AccentCyan.copy(alpha = 0.75f),
    trackColor: Color = StockAppColors.CardBorder
) {
    val density = LocalDensity.current
    val minThumbPx = with(density) { minThumbHeight.toPx() }
    val metrics by remember(listState, minThumbPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            if (totalItems == 0 || visibleItems.isEmpty()) return@derivedStateOf null

            val viewportSizePx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
            if (viewportSizePx <= 0f) return@derivedStateOf null

            var sizeSum = 0
            for (item in visibleItems) {
                sizeSum += item.size
            }
            val averageItemSizePx = (sizeSum.toFloat() / visibleItems.size).coerceAtLeast(1f)
            val estimatedTotalSizePx = averageItemSizePx * totalItems
            val maxScrollPx = max(estimatedTotalSizePx - viewportSizePx, 1f)
            val scrollOffsetPx =
                listState.firstVisibleItemIndex * averageItemSizePx + listState.firstVisibleItemScrollOffset

            val thumbHeightPx =
                (viewportSizePx * viewportSizePx / estimatedTotalSizePx).coerceAtLeast(minThumbPx)
            val maxThumbOffsetPx = max(viewportSizePx - thumbHeightPx, 0f)
            val thumbOffsetPx = (scrollOffsetPx / maxScrollPx) * maxThumbOffsetPx

            ScrollbarMetrics(thumbHeightPx = thumbHeightPx, thumbOffsetPx = thumbOffsetPx)
        }
    }
    val resolvedMetrics = metrics ?: return

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(trackColor, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = resolvedMetrics.thumbOffsetPx }
                .background(thumbColor, RoundedCornerShape(50))
                .height(with(density) { resolvedMetrics.thumbHeightPx.toDp() })
        )
    }
}

private data class ScrollbarMetrics(
    val thumbHeightPx: Float,
    val thumbOffsetPx: Float
)
