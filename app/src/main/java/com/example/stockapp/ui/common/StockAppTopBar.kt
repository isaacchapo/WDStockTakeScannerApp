package com.example.stockapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StockAppTopBar(
    title: String,
    logoResId: Int? = null
) {
    val backgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            StockAppColors.NavyMid,
            StockAppColors.NavyBase,
            StockAppColors.NavyDeep
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundBrush)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        if (logoResId != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                Image(
                    painter = painterResource(logoResId),
                    contentDescription = "Logo",
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Text(
            text = title,
            color = StockAppColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Center)
        )

    }
}
