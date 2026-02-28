package com.example.stockapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockapp.StockApplication
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.ui.camera.CameraScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStockCardScreen(
    loggedInUser: String,
    createStockViewModel: CreateStockViewModel = viewModel(
        factory = CreateStockViewModelFactory(
            (LocalContext.current.applicationContext as StockApplication).repository,
            loggedInUser
        )
    )
) {
    // UI State
    var isScanning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    val scannedItems by createStockViewModel.scannedItems.collectAsState()
    val pendingItem by createStockViewModel.pendingItem.collectAsState()
    val lastSid by createStockViewModel.lastSid.collectAsState()

    // Input Fields State
    var location by remember { mutableStateOf("") }
    var stockTakeId by remember { mutableStateOf(loggedInUser) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 1. Logged-In User
        Text(
            text = "User: $loggedInUser",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Input Fields and Scanner Section in a Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Scanner rolls into the same space as the input fields
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp) // Matches three input rows + spacing
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isScanning,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InputRow(
                                label = "Location:",
                                value = location,
                                onValueChange = {
                                    location = it
                                    createStockViewModel.updateSidForLocation(it)
                                },
                                enabled = !isScanning
                            )
                            InputRow(label = "UID:", value = stockTakeId, onValueChange = { stockTakeId = it }, enabled = false)
                            InputRow(label = "SID:", value = lastSid, onValueChange = { }, enabled = false)
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isScanning,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraScreen { barcode ->
                                if (!isPaused) {
                                    createStockViewModel.addScannedItem(barcode)
                                }
                            }
                        }
                    }
                }

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (!isScanning) {
                        isScanning = true
                        isPaused = false
                    } else {
                        isPaused = !isPaused
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Color(0xFFFBC02D) else Color(0xFF90CAF9)
                )
            ) {
                Text(
                    when {
                        !isScanning -> "Scan"
                        isPaused -> "Resume"
                        else -> "Pause"
                    },
                    color = Color.Black
                )
            }

            Button(
                onClick = { createStockViewModel.confirmPendingItem(location, stockTakeId) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                enabled = pendingItem != null && location.isNotBlank() && stockTakeId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pendingItem != null) Color(0xFF81C784) else Color(0xFFB0BEC5),
                    disabledContainerColor = if (pendingItem != null) Color(0xFF81C784).copy(alpha = 0.6f) else Color(0xFFB0BEC5)
                )
            ) {
                Text("Confirm", color = Color.Black)
            }
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        isScanning = false
                        isPaused = false
                        createStockViewModel.clearScannedItems()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Stop", color = Color.Black)
                }

                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFFFFF59D) else Color(0xFFB2DFDB),
                        disabledContainerColor = if (isPaused) Color(0xFFFFF59D) else Color(0xFFB2DFDB)
                    )
                ) {
                    Text(if (isPaused) "Status: Paused" else "Status: Ongoing", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Scanned Items Table
        Text(
            text = "Scanned Inventory",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes up remaining space
                .border(1.dp, Color.LightGray, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(8.dp)
            ) {
                Text("No", modifier = Modifier.weight(0.3f), color = Color.White, fontWeight = FontWeight.Bold)
                Text("Item ID", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                Text("Description", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                Text("Qty", modifier = Modifier.weight(0.4f), color = Color.White, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }

            // Table Body
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(scannedItems) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.White else Color(0xFFF5F5F5))
                            .padding(8.dp)
                    ) {
                        Text("${index + 1}", modifier = Modifier.weight(0.3f))
                        Text(item.itemId, modifier = Modifier.weight(1f))
                        Text(item.description, modifier = Modifier.weight(1f))
                        Text("${item.quantity}", modifier = Modifier.weight(0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputRow(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
            shape = RoundedCornerShape(8.dp)
        )
    }
}
