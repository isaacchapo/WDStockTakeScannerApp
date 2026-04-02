package com.example.stockapp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FooterBranding(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "www.webdevzm.tech",
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://www.webdevzm.tech")
                },
                color = StockAppColors.AccentCyan,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = " | contact: ",
                color = StockAppColors.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "0960911672",
                modifier = Modifier.clickable {
                    uriHandler.openUri("tel:0960911672")
                },
                color = StockAppColors.AccentAmber,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }
        Text(
            text = "Developed By Webdev Technologies",
            color = StockAppColors.TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.2.sp
        )
    }
}
