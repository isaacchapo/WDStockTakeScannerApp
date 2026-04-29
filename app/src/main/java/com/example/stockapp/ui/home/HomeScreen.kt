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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.R
import com.example.stockapp.ui.common.FooterBranding
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
    Column(modifier = Modifier.fillMaxSize()) {
        StockAppTopBar(title = "STOCK TAKE", logoResId = R.drawable.logo)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
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
                    icon = Icons.Filled.QrCodeScanner,
                    onClick = onCreateStockCard
                )
                HomeCard(
                    text = "View Stock Take",
                    icon = Icons.Filled.Visibility,
                    onClick = onViewStockCard
                )
                HomeCard(
                    text = "Share",
                    icon = Icons.Filled.Share,
                    onClick = onShare
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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

@Composable
private fun HomeCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StockAppColors.AccentCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = StockAppColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UserInfoCard(loggedInUser: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, StockAppColors.AccentCyan, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StockAppColors.NavyMid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = StockAppColors.AccentCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Logged in as:",
                    color = StockAppColors.TextSecondary,
                    fontSize = 12.sp
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
