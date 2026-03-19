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
    thumbColor: Color = Color(0xFF546E7A),
    trackColor: Color = Color(0x22000000)
) {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems == 0 || visibleItems.isEmpty()) return

    val viewportSizePx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    if (viewportSizePx <= 0f) return

    val averageItemSizePx = visibleItems.map { it.size }.average().toFloat().coerceAtLeast(1f)
    val estimatedTotalSizePx = averageItemSizePx * totalItems
    val maxScrollPx = max(estimatedTotalSizePx - viewportSizePx, 1f)
    val scrollOffsetPx =
        listState.firstVisibleItemIndex * averageItemSizePx + listState.firstVisibleItemScrollOffset

    val density = LocalDensity.current
    val minThumbPx = with(density) { minThumbHeight.toPx() }
    val thumbHeightPx =
        (viewportSizePx * viewportSizePx / estimatedTotalSizePx).coerceAtLeast(minThumbPx)
    val maxThumbOffsetPx = max(viewportSizePx - thumbHeightPx, 0f)
    val thumbOffsetPx = (scrollOffsetPx / maxScrollPx) * maxThumbOffsetPx

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
                .graphicsLayer { translationY = thumbOffsetPx }
                .background(thumbColor, RoundedCornerShape(50))
                .height(with(density) { thumbHeightPx.toDp() })
        )
    }
}
