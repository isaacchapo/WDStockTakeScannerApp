package com.example.stockapp.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.ui.StockViewModel
import com.example.stockapp.ui.upload.ApiUploadPreferences
import com.example.stockapp.ui.upload.showStockUploadSuccessNotification
import kotlinx.coroutines.launch

@Composable
fun ViewStockCardScreen(stockViewModel: StockViewModel) {
    val inventoryGroups by stockViewModel.inventoryGroups.collectAsState()
    var selectedGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    var openedGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGroup = inventoryGroups.firstOrNull { it.toKey() == selectedGroupKey }
    val openedGroup = inventoryGroups.firstOrNull { it.toKey() == openedGroupKey }
    val stockItems by (openedGroup?.let { group ->
        stockViewModel.getItemsForGroup(group.location, group.stockTakeId, group.stockCode)
    } ?: stockViewModel.allStockItems).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedUploadConfig = remember(context) { ApiUploadPreferences.load(context) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var tableMenuExpanded by remember { mutableStateOf(false) }
    var showUpdateGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var updateLocation by remember { mutableStateOf("") }
    var updateStockTakeId by remember { mutableStateOf("") }
    var updateStockCode by remember { mutableStateOf("") }
    var selectedTableItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedTableItem = stockItems.firstOrNull { it.id == selectedTableItemId }
    var showUpdateItemDialog by remember { mutableStateOf(false) }
    var showDeleteItemDialog by remember { mutableStateOf(false) }
    var updateItemId by remember { mutableStateOf("") }
    var updateItemDescription by remember { mutableStateOf("") }
    var updateItemQuantity by remember { mutableStateOf("") }
    var updateItemLocation by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadBaseUrl by rememberSaveable { mutableStateOf(savedUploadConfig.baseUrl) }
    var uploadEndpointPath by rememberSaveable { mutableStateOf(savedUploadConfig.endpointPath) }
    var pendingUploadItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var pendingUploadGroup by remember { mutableStateOf<InventoryGroup?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    BackHandler(enabled = openedGroup != null) {
        openedGroupKey = null
    }

    LaunchedEffect(selectedGroup) {
        if (selectedGroup != null) {
            updateLocation = selectedGroup.location
            updateStockTakeId = selectedGroup.stockTakeId
            updateStockCode = selectedGroup.stockCode
        }
    }

    LaunchedEffect(openedGroupKey) {
        groupMenuExpanded = false
        tableMenuExpanded = false
        if (openedGroupKey == null) {
            selectedTableItemId = null
        }
    }

    LaunchedEffect(stockItems, openedGroupKey) {
        if (openedGroupKey != null && selectedTableItemId != null && stockItems.none { it.id == selectedTableItemId }) {
            selectedTableItemId = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Available Stocks", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (openedGroup == null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(inventoryGroups) { index, group ->
                    val groupKey = group.toKey()
                    val isSelected = selectedGroupKey == groupKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(groupKey) {
                                detectTapGestures(
                                    onTap = {
                                        openedGroupKey = groupKey
                                        selectedGroupKey = null
                                    },
                                    onLongPress = {
                                        selectedGroupKey = groupKey
                                    }
                                )
                            }
                            .background(
                                if (isSelected) Color(0xFF1976D2) else Color(0xFFF0F0F0),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF0D47A1) else Color(0xFFBDBDBD),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val textColor = if (isSelected) Color.White else Color.Black
                        Text((index + 1).toString(), modifier = Modifier.weight(0.3f), color = textColor)
                        Text(group.location, modifier = Modifier.weight(1.2f), color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(group.stockCode, modifier = Modifier.weight(0.8f), color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("UID: ${group.stockTakeId}", modifier = Modifier.weight(1.2f), color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            TextButton(onClick = { openedGroupKey = null }) {
                Text("Back to Inventories")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .weight(1f)
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(Color(0xFFEFEFEF)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell("No", 0.3f, isHeader = true, textColor = Color.Black)
                        TableCell("Item ID", 1f, isHeader = true, textColor = Color.Black)
                        TableCell("Description", 1f, isHeader = true, textColor = Color.Black)
                        TableCell("Qty", 0.5f, isHeader = true, textColor = Color.Black)
                        TableCell("Location", 1f, isHeader = true, textColor = Color.Black, showRightDivider = false)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.Black)
                    )

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(stockItems) { index, item ->
                            val isSelected = selectedTableItemId == item.id
                            val rowColor = if (isSelected) Color(0xFF1976D2) else Color.White
                            val textColor = if (isSelected) Color.White else Color.Black

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .background(rowColor)
                                    .clickable {
                                        selectedTableItemId = if (isSelected) null else item.id
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell((index + 1).toString(), 0.3f, textColor = textColor)
                                TableCell(item.itemId, 1f, textColor = textColor)
                                TableCell(item.description, 1f, textColor = textColor)
                                TableCell(item.quantity.toString(), 0.5f, textColor = textColor)
                                TableCell(item.location, 1f, textColor = textColor, showRightDivider = false)
                            }

                            if (index < stockItems.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.Black)
                                )
                            }
                        }
                    }
                }
            }
        }

    if (openedGroup == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (groupMenuExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                groupMenuExpanded = false
                                val group = selectedGroup
                                if (group == null) {
                                    Toast.makeText(context, "Select a stock inventory first", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch {
                                        val itemsForGroup = stockViewModel.getItemsForGroupSnapshot(
                                            location = group.location,
                                            stockTakeId = group.stockTakeId,
                                            stockCode = group.stockCode
                                        )
                                        if (itemsForGroup.isEmpty()) {
                                            Toast.makeText(context, "No stock rows found for this inventory", Toast.LENGTH_SHORT).show()
                                        } else {
                                            pendingUploadItems = itemsForGroup
                                            pendingUploadGroup = group
                                            showUploadDialog = true
                                        }
                                    }
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                        ) {
                            Text("Upload", color = Color.White)
                        }
                        Button(
                            onClick = {
                                groupMenuExpanded = false
                                if (selectedGroup == null) {
                                    Toast.makeText(context, "Select a stock inventory first", Toast.LENGTH_SHORT).show()
                                } else {
                                    showUpdateGroupDialog = true
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                        ) {
                            Text("Update", color = Color.White)
                        }
                        Button(
                            onClick = {
                                groupMenuExpanded = false
                                if (selectedGroup == null) {
                                    Toast.makeText(context, "Select a stock inventory first", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteGroupDialog = true
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                }

                IconButton(
                    onClick = { groupMenuExpanded = !groupMenuExpanded },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More actions",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (openedGroup != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (tableMenuExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                tableMenuExpanded = false
                                if (stockItems.isEmpty()) {
                                    Toast.makeText(context, "No stock rows to upload", Toast.LENGTH_SHORT).show()
                                } else {
                                    pendingUploadItems = stockItems
                                    pendingUploadGroup = openedGroup
                                    showUploadDialog = true
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                        ) {
                            Text("Upload", color = Color.White)
                        }
                        Button(
                            onClick = {
                                tableMenuExpanded = false
                                val item = selectedTableItem
                                if (item == null) {
                                    Toast.makeText(context, "Select a table row first", Toast.LENGTH_SHORT).show()
                                } else {
                                    updateItemId = item.itemId
                                    updateItemDescription = item.description
                                    updateItemQuantity = item.quantity.toString()
                                    updateItemLocation = item.location
                                    showUpdateItemDialog = true
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                        ) {
                            Text("Update", color = Color.White)
                        }
                        Button(
                            onClick = {
                                tableMenuExpanded = false
                                if (selectedTableItem == null) {
                                    Toast.makeText(context, "Select a table row first", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteItemDialog = true
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                }

                IconButton(
                    onClick = { tableMenuExpanded = !tableMenuExpanded },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Table actions",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isUploading) {
                    showUploadDialog = false
                }
            },
            title = { Text("Upload Inventory JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your local PC API URL (for example: http://192.168.1.15:8000).")
                    OutlinedTextField(
                        value = uploadBaseUrl,
                        onValueChange = { uploadBaseUrl = it },
                        label = { Text("Base URL") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uploadEndpointPath,
                        onValueChange = { uploadEndpointPath = it },
                        label = { Text("Endpoint Path (optional)") },
                        singleLine = true
                    )
                    if (isUploading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading data...")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUploading && uploadBaseUrl.trim().isNotBlank(),
                    onClick = {
                        val itemsForUpload = pendingUploadItems
                        if (itemsForUpload.isEmpty()) {
                            Toast.makeText(context, "No stock rows to upload", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val baseUrl = uploadBaseUrl.trim()
                        val endpointPath = uploadEndpointPath.trim()
                        isUploading = true
                        ApiUploadPreferences.save(context, baseUrl, endpointPath)

                        coroutineScope.launch {
                            val result = stockViewModel.uploadInventory(
                                baseUrl = baseUrl,
                                endpointPath = endpointPath,
                                inventoryGroup = pendingUploadGroup,
                                stockItems = itemsForUpload
                            )
                            isUploading = false

                            result.onSuccess { serverMessage ->
                                val notificationMessage =
                                    "${itemsForUpload.size} stock item(s) uploaded successfully."
                                showStockUploadSuccessNotification(
                                    context = context,
                                    message = notificationMessage
                                )
                                Toast.makeText(context, serverMessage, Toast.LENGTH_SHORT).show()
                                showUploadDialog = false
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "Upload failed.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(if (isUploading) "Uploading..." else "Upload")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUploadDialog = false },
                    enabled = !isUploading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpdateGroupDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showUpdateGroupDialog = false },
            title = { Text("Update Inventory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = updateLocation,
                        onValueChange = { updateLocation = it },
                        label = { Text("Location") }
                    )
                    OutlinedTextField(
                        value = updateStockTakeId,
                        onValueChange = { updateStockTakeId = it },
                        label = { Text("UID") }
                    )
                    OutlinedTextField(
                        value = updateStockCode,
                        onValueChange = { updateStockCode = it },
                        label = { Text("SID") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val group = selectedGroup
                        val oldKey = group.toKey()
                        stockViewModel.updateGroup(
                            group.location,
                            group.stockTakeId,
                            group.stockCode,
                            updateLocation.trim(),
                            updateStockTakeId.trim(),
                            updateStockCode.trim()
                        )
                        val newKey = listOf(
                            updateLocation.trim(),
                            updateStockTakeId.trim(),
                            updateStockCode.trim()
                        ).joinToString("|")
                        if (openedGroupKey == oldKey) {
                            openedGroupKey = newKey
                        }
                        selectedGroupKey = null
                        showUpdateGroupDialog = false
                    },
                    enabled = updateLocation.isNotBlank() && updateStockTakeId.isNotBlank() && updateStockCode.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteGroupDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Inventory") },
            text = { Text("Delete all items in this inventory? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val group = selectedGroup
                        stockViewModel.deleteGroup(group.location, group.stockTakeId, group.stockCode)
                        selectedGroupKey = null
                        openedGroupKey = null
                        showDeleteGroupDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpdateItemDialog && selectedTableItem != null) {
        AlertDialog(
            onDismissRequest = { showUpdateItemDialog = false },
            title = { Text("Update Row") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = updateItemId,
                        onValueChange = { updateItemId = it },
                        label = { Text("Item ID") }
                    )
                    OutlinedTextField(
                        value = updateItemDescription,
                        onValueChange = { updateItemDescription = it },
                        label = { Text("Description") }
                    )
                    OutlinedTextField(
                        value = updateItemQuantity,
                        onValueChange = { updateItemQuantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = updateItemLocation,
                        onValueChange = { updateItemLocation = it },
                        label = { Text("Location") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = selectedTableItem
                        val quantity = updateItemQuantity.trim().toIntOrNull()
                        if (quantity == null) {
                            Toast.makeText(context, "Enter a valid quantity", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        stockViewModel.updateStockItem(
                            item.copy(
                                itemId = updateItemId.trim(),
                                description = updateItemDescription.trim(),
                                quantity = quantity,
                                location = updateItemLocation.trim()
                            )
                        )
                        showUpdateItemDialog = false
                    },
                    enabled = updateItemId.isNotBlank() &&
                        updateItemDescription.isNotBlank() &&
                        updateItemLocation.isNotBlank() &&
                        updateItemQuantity.trim().toIntOrNull() != null
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteItemDialog && selectedTableItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteItemDialog = false },
            title = { Text("Delete Row") },
            text = { Text("Delete the selected inventory row? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val item = selectedTableItem
                        stockViewModel.deleteStockItem(item.id)
                        selectedTableItemId = null
                        showDeleteItemDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun com.example.stockapp.data.local.InventoryGroup.toKey(): String {
    return listOf(location, stockTakeId, stockCode).joinToString("|")
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    textColor: Color = Color.Black,
    showRightDivider: Boolean = true
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (showRightDivider) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.Black)
        )
    }
}
