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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.ui.StockViewModel
import com.example.stockapp.ui.upload.ApiUploadPreferences
import com.example.stockapp.ui.upload.showStockUploadSuccessNotification
import kotlinx.coroutines.launch

@Composable
fun ViewStockCardScreen(stockViewModel: StockViewModel, onBack: () -> Unit) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F7FC))
    ) {
        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A4A99))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = {
                    if (openedGroup != null) {
                        openedGroupKey = null
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (openedGroup != null) "INVENTORY ITEMS" else "AVAILABLE STOCKS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (openedGroup == null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(inventoryGroups) { index, group ->
                                val groupKey = group.toKey()
                                val isSelected = selectedGroupKey == groupKey

                                InventoryGroupRowCard(
                                    index = index,
                                    group = group,
                                    isSelected = isSelected,
                                    onTap = {
                                        openedGroupKey = groupKey
                                        selectedGroupKey = null
                                    },
                                    onLongPress = {
                                        selectedGroupKey = groupKey
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A4A99))
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell("No", 0.3f, isHeader = true, textColor = Color.White)
                            TableCell("Item ID", 1f, isHeader = true, textColor = Color.White)
                            TableCell("Description", 1.2f, isHeader = true, textColor = Color.White)
                            TableCell("Qty", 0.5f, isHeader = true, textColor = Color.White, textAlign = TextAlign.End)
                            TableCell("Loc", 0.8f, isHeader = true, textColor = Color.White, showRightDivider = false, textAlign = TextAlign.End)
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(stockItems) { index, item ->
                                val isSelected = selectedTableItemId == item.id
                                val rowColor = if (isSelected) Color(0xFFE3F2FD) else if (index % 2 == 0) Color.White else Color(0xFFF8FAFD)

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
                                    TableCell((index + 1).toString(), 0.3f)
                                    TableCell(item.itemId, 1f)
                                    TableCell(item.description, 1.2f)
                                    TableCell(item.quantity.toString(), 0.5f, textAlign = TextAlign.End)
                                    TableCell(item.location, 0.8f, showRightDivider = false, textAlign = TextAlign.End)
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFECEFF1)))
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            val menuExpanded = if (openedGroup == null) groupMenuExpanded else tableMenuExpanded
            if (menuExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (openedGroup == null) {
                        ActionMenuButton("Upload", Color(0xFF1E88E5)) {
                            groupMenuExpanded = false
                            val group = selectedGroup
                            if (group == null) {
                                Toast.makeText(context, "Long press a row to select first", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch {
                                    val items = stockViewModel.getItemsForGroupSnapshot(group.location, group.stockTakeId, group.stockCode)
                                    if (items.isEmpty()) Toast.makeText(context, "No stock items", Toast.LENGTH_SHORT).show()
                                    else { pendingUploadItems = items; showUploadDialog = true }
                                }
                            }
                        }
                        ActionMenuButton("Update", Color(0xFF43A047)) {
                            groupMenuExpanded = false
                            if (selectedGroup == null) Toast.makeText(context, "Long press a row to select first", Toast.LENGTH_SHORT).show()
                            else showUpdateGroupDialog = true
                        }
                        ActionMenuButton("Delete", Color(0xFFE53935)) {
                            groupMenuExpanded = false
                            if (selectedGroup == null) Toast.makeText(context, "Long press a row to select first", Toast.LENGTH_SHORT).show()
                            else showDeleteGroupDialog = true
                        }
                    } else {
                        ActionMenuButton("Upload", Color(0xFF1E88E5)) {
                            tableMenuExpanded = false
                            if (stockItems.isEmpty()) Toast.makeText(context, "No items", Toast.LENGTH_SHORT).show()
                            else { pendingUploadItems = stockItems; showUploadDialog = true }
                        }
                        ActionMenuButton("Update", Color(0xFF43A047)) {
                            tableMenuExpanded = false
                            val item = selectedTableItem
                            if (item == null) Toast.makeText(context, "Tap a row to select first", Toast.LENGTH_SHORT).show()
                            else {
                                updateItemId = item.itemId; updateItemDescription = item.description
                                updateItemQuantity = item.quantity.toString(); updateItemLocation = item.location
                                showUpdateItemDialog = true
                            }
                        }
                        ActionMenuButton("Delete", Color(0xFFE53935)) {
                            tableMenuExpanded = false
                            if (selectedTableItem == null) Toast.makeText(context, "Tap a row to select first", Toast.LENGTH_SHORT).show()
                            else showDeleteItemDialog = true
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.size(50.dp, 36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A4A99)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                IconButton(
                    onClick = { if (openedGroup == null) groupMenuExpanded = !groupMenuExpanded else tableMenuExpanded = !tableMenuExpanded },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Actions", tint = Color.White)
                }
            }
        }
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showUploadDialog = false },
            title = { Text("Upload Inventory Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter API server LAN URL (e.g., http://192.168.1.15:5000).")
                    OutlinedTextField(value = uploadBaseUrl, onValueChange = { uploadBaseUrl = it }, label = { Text("Base URL") }, singleLine = true)
                    OutlinedTextField(value = uploadEndpointPath, onValueChange = { uploadEndpointPath = it }, label = { Text("Path (optional)") }, singleLine = true)
                    if (isUploading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp)); Text("Uploading...")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUploading && uploadBaseUrl.trim().isNotBlank(),
                    onClick = {
                        val itemsForUpload = pendingUploadItems
                        val baseUrl = uploadBaseUrl.trim()
                        val endpointPath = uploadEndpointPath.trim()
                        isUploading = true
                        ApiUploadPreferences.save(context, baseUrl, endpointPath)
                        coroutineScope.launch {
                            val result = stockViewModel.uploadInventory(baseUrl, endpointPath, itemsForUpload)
                            isUploading = false
                            result.onSuccess { msg ->
                                showStockUploadSuccessNotification(
                                    context = context,
                                    title = "Stock Upload Successful",
                                    message = "${itemsForUpload.size} items uploaded."
                                )
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); showUploadDialog = false
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Upload failed.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Upload") }
            },
            dismissButton = { TextButton(onClick = { showUploadDialog = false }, enabled = !isUploading) { Text("Cancel") } }
        )
    }

    if (showUpdateGroupDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showUpdateGroupDialog = false },
            title = { Text("Update Group Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = updateLocation, onValueChange = { updateLocation = it }, label = { Text("Location") })
                    OutlinedTextField(value = updateStockTakeId, onValueChange = { updateStockTakeId = it }, label = { Text("UID") })
                    OutlinedTextField(value = updateStockCode, onValueChange = { updateStockCode = it }, label = { Text("SID") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val oldKey = selectedGroup.toKey()
                        stockViewModel.updateGroup(
                            selectedGroup.location, selectedGroup.stockTakeId, selectedGroup.stockCode,
                            updateLocation.trim(), updateStockTakeId.trim(), updateStockCode.trim()
                        )
                        val newKey = listOf(updateLocation.trim(), updateStockTakeId.trim(), updateStockCode.trim()).joinToString("|")
                        if (openedGroupKey == oldKey) openedGroupKey = newKey
                        selectedGroupKey = null; showUpdateGroupDialog = false
                    },
                    enabled = updateLocation.isNotBlank() && updateStockTakeId.isNotBlank() && updateStockCode.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showUpdateGroupDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteGroupDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Inventory Group") },
            text = { Text("Delete all items in this group? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    stockViewModel.deleteGroup(selectedGroup.location, selectedGroup.stockTakeId, selectedGroup.stockCode)
                    selectedGroupKey = null; openedGroupKey = null; showDeleteGroupDialog = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteGroupDialog = false }) { Text("Cancel") } }
        )
    }

    if (showUpdateItemDialog && selectedTableItem != null) {
        AlertDialog(
            onDismissRequest = { showUpdateItemDialog = false },
            title = { Text("Update Stock Row") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = updateItemId, onValueChange = { updateItemId = it }, label = { Text("Item ID") })
                    OutlinedTextField(value = updateItemDescription, onValueChange = { updateItemDescription = it }, label = { Text("Description") })
                    OutlinedTextField(value = updateItemQuantity, onValueChange = { updateItemQuantity = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = updateItemLocation, onValueChange = { updateItemLocation = it }, label = { Text("Location") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val quantity = updateItemQuantity.trim().toIntOrNull()
                        if (quantity == null) { Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show(); return@Button }
                        stockViewModel.updateStockItem(selectedTableItem.copy(
                            itemId = updateItemId.trim(), description = updateItemDescription.trim(),
                            quantity = quantity, location = updateItemLocation.trim()
                        )); showUpdateItemDialog = false
                    },
                    enabled = updateItemId.isNotBlank() && updateItemDescription.isNotBlank() && updateItemLocation.isNotBlank() && updateItemQuantity.toIntOrNull() != null
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showUpdateItemDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteItemDialog && selectedTableItem != null) {
        AlertDialog(
            onDismissRequest = { showDeleteItemDialog = false },
            title = { Text("Delete Stock Row") },
            text = { Text("Delete this item? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    stockViewModel.deleteStockItem(selectedTableItem.id)
                    selectedTableItemId = null; showDeleteItemDialog = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteItemDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ActionMenuButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(42.dp)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun InventoryGroup.toKey(): String = listOf(location, stockTakeId, stockCode).joinToString("|")

@Composable
private fun InventoryGroupRowCard(
    index: Int,
    group: InventoryGroup,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val cardShape = RoundedCornerShape(10.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .pointerInput(group.toKey()) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF0A4A99) else Color(0xFFE0E6ED),
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventory ${index + 1}",
                color = Color(0xFF0A4A99),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Location: ${group.location}",
                color = Color(0xFF455A64),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    textColor: Color = Color.Black,
    showRightDivider: Boolean = true,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = when (textAlign) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = if (isHeader) 13.sp else 12.sp,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showRightDivider) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(if (isHeader) Color.White.copy(alpha = 0.3f) else Color(0xFFECEFF1))
        )
    }
}
