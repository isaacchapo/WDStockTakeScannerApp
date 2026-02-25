package com.example.stockapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import android.widget.Toast
import com.example.stockapp.ui.StockViewModel
import com.example.stockapp.ui.sharing.shareStockDataAsPdf

@Composable
fun ViewStockCardScreen(stockViewModel: StockViewModel) {
    val inventoryGroups by stockViewModel.inventoryGroups.collectAsState()
    var selectedGroup by remember { mutableStateOf<com.example.stockapp.data.local.InventoryGroup?>(null) }
    val stockItems by (selectedGroup?.let {
        stockViewModel.getItemsForGroup(it.location, it.stockTakeId, it.stockCode)
    } ?: stockViewModel.allStockItems).collectAsState(initial = emptyList())
    val context = LocalContext.current

    BackHandler(enabled = selectedGroup != null) {
        selectedGroup = null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Available Stocks", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedGroup == null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(inventoryGroups) { index, group ->
                    val isSelected = selectedGroup == group
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGroup = group }
                            .background(
                                if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF0F0F0),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF64B5F6) else Color(0xFFBDBDBD),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((index + 1).toString(), modifier = Modifier.weight(0.3f))
                        Text(group.location, modifier = Modifier.weight(1.2f))
                        Text(group.stockCode, modifier = Modifier.weight(0.8f))
                        Text("UID: ${group.stockTakeId}", modifier = Modifier.weight(1.2f))
                    }
                }
            }
        } else {
            TextButton(onClick = { selectedGroup = null }) {
                Text("Back to Inventories")
            }

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("No", modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold)
                Text("Item ID", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Description", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Qty", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold)
                Text("Location", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }

            // Data Rows
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(stockItems) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text((index + 1).toString(), modifier = Modifier.weight(0.3f))
                        Text(item.itemId, modifier = Modifier.weight(1f))
                        Text(item.description, modifier = Modifier.weight(1f))
                        Text(item.quantity.toString(), modifier = Modifier.weight(0.5f))
                        Text(item.location, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Share Button
        Button(
            onClick = {
                val group = selectedGroup
                if (group == null) {
                    Toast.makeText(context, "Select a stock inventory to share", Toast.LENGTH_SHORT).show()
                } else {
                    shareStockDataAsPdf(context, stockItems)
                }
            },
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB))
        ) {
            Text("Upload")
        }
    }
}
