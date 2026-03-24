package com.example.stockapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.R
import com.example.stockapp.ui.common.StockAppBackground
import com.example.stockapp.ui.common.ScreenAppear
import com.example.stockapp.ui.common.StockAppColors
import com.example.stockapp.ui.common.StockAppTopBar

@Composable
fun HomeScreen(
    loggedInUser: String,
    onCreateStockCard: () -> Unit,
    onViewStockCard: () -> Unit,
    onShare: () -> Unit,
    onLogout: () -> Unit
) {
    StockAppBackground {
        ScreenAppear {
            Column(modifier = Modifier.fillMaxSize()) {
                StockAppTopBar(title = "STOCK TAKE", logoResId = R.drawable.logo)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserInfoCard(loggedInUser = loggedInUser)
                    Spacer(modifier = Modifier.height(28.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HomeCard(
                            text = "Create Stock Take",
                            subtitle = null,
                            icon = Icons.Filled.QrCodeScanner,
                            onClick = onCreateStockCard
                        )
                        HomeCard(
                            text = "View Stock Take",
                            subtitle = null,
                            icon = Icons.Filled.Visibility,
                            onClick = onViewStockCard
                        )
                        HomeCard(
                            text = "Share",
                            subtitle = null,
                            icon = Icons.Filled.Share,
                            onClick = onShare
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StockAppColors.CardSurface,
                            contentColor = StockAppColors.AccentAmber,
                            disabledContainerColor = StockAppColors.DisabledSurface,
                            disabledContentColor = StockAppColors.DisabledText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Logout",
                            tint = StockAppColors.AccentAmber,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = "Logout",
                            color = StockAppColors.AccentAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FooterBranding()
            }
        }
    }
}

@Composable
fun HomeCard(
    text: String,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clickable { onClick() }
            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StockAppColors.AccentCyan.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StockAppColors.AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = text,
                    color = StockAppColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = StockAppColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StockAppColors.AccentCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    color = StockAppColors.AccentCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UserInfoCard(loggedInUser: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(StockAppColors.AccentCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "User Photo",
                    tint = StockAppColors.AccentCyan,
                    modifier = Modifier.size(34.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Logged In User",
                    color = StockAppColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = loggedInUser.ifBlank { "Unknown User" },
                    color = StockAppColors.TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FooterBranding() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
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
                text = " | Contact: ",
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
