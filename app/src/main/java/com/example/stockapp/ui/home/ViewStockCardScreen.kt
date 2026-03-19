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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.data.JsonFieldExtractor
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.ui.StockViewModel
import com.example.stockapp.ui.sharing.shareStockSchemaAsStyledPdf
import com.example.stockapp.ui.upload.ApiUploadPreferences
import com.example.stockapp.ui.upload.showStockUploadSuccessNotification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewStockCardScreen(
    stockViewModel: StockViewModel,
    onBack: () -> Unit,
    shareMode: Boolean = false
) {
    val inventoryGroups by stockViewModel.inventoryGroups.collectAsState()
    var selectedGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    var openedGroupKey by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedGroup = inventoryGroups.firstOrNull { it.toKey() == selectedGroupKey }
    val openedGroup = inventoryGroups.firstOrNull { it.toKey() == openedGroupKey }
    val actionGroup = openedGroup ?: selectedGroup

    val stockItems by if (openedGroup != null) {
        stockViewModel.getItemsForTable(
            location = openedGroup.location,
            sid = openedGroup.sid
        ).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val tableColumns = remember(stockItems) { resolveTableColumns(stockItems) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedUploadConfig = remember(context) { ApiUploadPreferences.load(context) }
    
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var tableMenuExpanded by remember { mutableStateOf(false) }
    
    var showUpdateGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var updateLocation by remember { mutableStateOf("") }
    
    var selectedTableItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTableItem = stockItems.firstOrNull { it.id == selectedTableItemId }
    
    var showUpdateItemDialog by remember { mutableStateOf(false) }
    var showDeleteItemDialog by remember { mutableStateOf(false) }
    var updateItemSid by remember { mutableStateOf("") }
    var updateItemSchemaId by remember { mutableStateOf("") }
    var updateItemOrderNo by remember { mutableStateOf("") }
    var updateItemLocation by remember { mutableStateOf("") }
    
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadBaseUrl by rememberSaveable { mutableStateOf(savedUploadConfig.baseUrl) }
    var uploadEndpointPath by rememberSaveable { mutableStateOf(savedUploadConfig.endpointPath) }
    var uploadApiKey by rememberSaveable { mutableStateOf(savedUploadConfig.apiKey) }
    var pendingUploadItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    fun shareItemsAsPdf(items: List<StockItem>, preferredSchemaId: String? = null, sidHint: String? = null) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No items to share", Toast.LENGTH_SHORT).show()
            return
        }
        val sidValue = sidHint ?: items.firstOrNull()?.sid.orEmpty()
        val schemaLabel = preferredSchemaId
            ?.takeIf { it.isNotBlank() }
            ?: "ALL_RECORDS"

        coroutineScope.launch {
            shareStockSchemaAsStyledPdf(
                context = context,
                sid = sidValue,
                schemaId = schemaLabel,
                stockItems = items
            ).onSuccess {
                Toast.makeText(context, "PDF ready to share", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "Failed to share PDF", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler(enabled = openedGroup != null) {
        if (openedGroup != null) {
            openedGroupKey = null
        }
    }

    LaunchedEffect(actionGroup) {
        if (actionGroup != null) {
            updateLocation = actionGroup.location
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
                text = when {
                    openedGroup != null && shareMode -> "SHARE ITEMS"
                    openedGroup != null -> "INVENTORY ITEMS"
                    shareMode -> "SHARE STOCKS"
                    else -> "AVAILABLE STOCKS"
                },
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
                    if (openedGroup != null) {
                        TableIdentityHeader(group = openedGroup)
                    }

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
                            TableCell(tableColumns[0], 1f, isHeader = true, textColor = Color.White)
                            TableCell(tableColumns[1], 1.2f, isHeader = true, textColor = Color.White)
                            TableCell(tableColumns[2], 1f, isHeader = true, textColor = Color.White)
                            TableCell(tableColumns[3], 1f, isHeader = true, textColor = Color.White, showRightDivider = false, textAlign = TextAlign.End)
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(stockItems) { index, item ->
                                val isSelected = selectedTableItemId == item.id
                                val rowColor = if (isSelected) Color(0xFFE3F2FD) else if (index % 2 == 0) Color.White else Color(0xFFF8FAFD)
                                val itemFields = remember(item.variableData) {
                                    JsonFieldExtractor.extractAllFields(item.variableData)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .background(rowColor)
                                        .clickable {
                                            selectedTableItemId = if (isSelected) null else item.id
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TableCell((index + 1).toString(), 0.3f)
                                        TableCell(resolveTableValue(itemFields, tableColumns[0]), 1f)
                                        TableCell(resolveTableValue(itemFields, tableColumns[1]), 1.2f)
                                        TableCell(resolveTableValue(itemFields, tableColumns[2]), 1f)
                                        TableCell(resolveTableValue(itemFields, tableColumns[3]), 1f, showRightDivider = false, textAlign = TextAlign.End)
                                    }
                                    Text(
                                        text = "Scanned: ${formatRecordTimestamp(item.dateScanned)}",
                                        color = Color(0xFF607D8B),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                    )
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (openedGroup == null) {
                        ActionMenuButton(
                            if (shareMode) "Share PDF" else "Upload",
                            Color(0xFF1E88E5)
                        ) {
                            groupMenuExpanded = false
                            val group = actionGroup
                            if (group == null) {
                                Toast.makeText(context, "Select or open a table first", Toast.LENGTH_SHORT).show()
                            } else {
                                coroutineScope.launch {
                                    val items = stockViewModel.getItemsForTableSnapshot(group.location, group.sid)
                                    if (items.isEmpty()) {
                                        Toast.makeText(context, "No stock items", Toast.LENGTH_SHORT).show()
                                    } else if (shareMode) {
                                        shareItemsAsPdf(items, sidHint = group.sid)
                                    } else {
                                        pendingUploadItems = items
                                        showUploadDialog = true
                                    }
                                }
                            }
                        }
                        if (!shareMode) {
                            ActionMenuButton("Update", Color(0xFF43A047)) {
                                groupMenuExpanded = false
                                if (actionGroup == null) {
                                    Toast.makeText(context, "Select or open a table first", Toast.LENGTH_SHORT).show()
                                } else {
                                    updateLocation = actionGroup.location
                                    showUpdateGroupDialog = true
                                }
                            }
                            ActionMenuButton("Delete", Color(0xFFE53935)) {
                                groupMenuExpanded = false
                                if (actionGroup == null) {
                                    Toast.makeText(context, "Select or open a table first", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteGroupDialog = true
                                }
                            }
                        }
                    } else {
                        ActionMenuButton(if (shareMode) "Share PDF" else "Upload", Color(0xFF1E88E5)) {
                            tableMenuExpanded = false
                            if (stockItems.isEmpty()) {
                                Toast.makeText(context, "No items", Toast.LENGTH_SHORT).show()
                            } else if (shareMode) {
                                shareItemsAsPdf(
                                    items = stockItems,
                                    preferredSchemaId = selectedTableItem?.identifierKey,
                                    sidHint = openedGroup.sid
                                )
                            } else {
                                pendingUploadItems = stockItems
                                showUploadDialog = true
                            }
                        }
                        if (!shareMode) {
                            ActionMenuButton("Update", Color(0xFF43A047)) {
                                tableMenuExpanded = false
                                val item = selectedTableItem
                                if (item == null) {
                                    Toast.makeText(context, "Tap a row to select first", Toast.LENGTH_SHORT).show()
                                } else {
                                    updateItemSid = item.sid
                                    updateItemSchemaId = item.identifierKey
                                    updateItemOrderNo = item.orderNo.orEmpty()
                                    updateItemLocation = item.location
                                    showUpdateItemDialog = true
                                }
                            }
                            ActionMenuButton("Delete", Color(0xFFE53935)) {
                                tableMenuExpanded = false
                                if (selectedTableItem == null) {
                                    Toast.makeText(context, "Tap a row to select first", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteItemDialog = true
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8E5F7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                IconButton(
                    onClick = {
                        if (openedGroup == null) {
                            groupMenuExpanded = !groupMenuExpanded
                        } else {
                            tableMenuExpanded = !tableMenuExpanded
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Actions", tint = Color(0xFF0A4A99))
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
                    OutlinedTextField(value = uploadApiKey, onValueChange = { uploadApiKey = it }, label = { Text("API Key") }, singleLine = true)
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
                    enabled = !isUploading && uploadBaseUrl.trim().isNotBlank() && uploadApiKey.trim().isNotBlank(),
                    onClick = {
                        val itemsForUpload = pendingUploadItems
                        val baseUrl = uploadBaseUrl.trim()
                        val endpointPath = uploadEndpointPath.trim()
                        val apiKey = uploadApiKey.trim()
                        isUploading = true
                        ApiUploadPreferences.save(context, baseUrl, endpointPath, apiKey)
                        coroutineScope.launch {
                            val result = stockViewModel.uploadInventory(
                                baseUrl = baseUrl,
                                endpointPath = endpointPath,
                                apiKey = apiKey,
                                stockItems = itemsForUpload
                            )
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

    if (showUpdateGroupDialog && actionGroup != null) {
        AlertDialog(
            onDismissRequest = { showUpdateGroupDialog = false },
            title = { Text("Update Group Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = updateLocation, onValueChange = { updateLocation = it }, label = { Text("Location") })
                    OutlinedTextField(value = actionGroup.sid, onValueChange = {}, label = { Text("SID") }, enabled = false)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val oldKey = actionGroup.toKey()
                        stockViewModel.updateTableLocation(
                            oldLocation = actionGroup.location,
                            oldSid = actionGroup.sid,
                            newLocation = updateLocation.trim()
                        )
                        val newKey = listOf(actionGroup.ownerUid, updateLocation.trim(), actionGroup.sid).joinToString("|")
                        if (openedGroupKey == oldKey) openedGroupKey = newKey
                        selectedGroupKey = null; showUpdateGroupDialog = false
                    },
                    enabled = updateLocation.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showUpdateGroupDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteGroupDialog && actionGroup != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Inventory Group") },
            text = { Text("Delete all items in this group? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    stockViewModel.deleteTableGroup(actionGroup.location, actionGroup.sid)
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
                    OutlinedTextField(value = updateItemSid, onValueChange = { updateItemSid = it }, label = { Text("SID") })
                    OutlinedTextField(value = updateItemSchemaId, onValueChange = { updateItemSchemaId = it }, label = { Text("Schema ID") })
                    OutlinedTextField(value = updateItemOrderNo, onValueChange = { updateItemOrderNo = it }, label = { Text("Order No (optional)") })
                    OutlinedTextField(value = updateItemLocation, onValueChange = { updateItemLocation = it }, label = { Text("Location") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        stockViewModel.updateStockItem(
                            selectedTableItem.copy(
                                sid = updateItemSid.trim(),
                                identifierKey = updateItemSchemaId.trim(),
                                orderNo = updateItemOrderNo.trim().ifBlank { null },
                                location = updateItemLocation.trim()
                            )
                        )
                        showUpdateItemDialog = false
                    },
                    enabled = updateItemSid.isNotBlank() && updateItemSchemaId.isNotBlank() && updateItemLocation.isNotBlank()
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
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFFF8FBFF),
            contentColor = color
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .height(36.dp)
            .width(122.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun InventoryGroup.toKey(): String = listOf(ownerUid, location, sid).joinToString("|")

private fun resolveTableColumns(items: List<StockItem>): List<String> {
    val parsedFieldSets = items
        .map { JsonFieldExtractor.extractAllFields(it.variableData) }
        .filter { it.isNotEmpty() }

    val preferred = parsedFieldSets
        .firstOrNull()
        ?.let { fields -> QrDataParser.selectMajorIdentifierKeys(fields, requiredCount = 4) }
        .orEmpty()

    val fallback = parsedFieldSets
        .flatMap { it.keys }
        .distinct()
        .take(4)

    val columns = (preferred + fallback)
        .filter { it.isNotBlank() }
        .distinct()
        .toMutableList()

    while (columns.size < 4) {
        columns.add("FIELD${columns.size + 1}")
    }

    return columns.take(4)
}

private fun resolveTableValue(fields: Map<String, String>, key: String): String {
    if (key.isBlank()) return "-"
    return QrDataParser.getFieldValue(fields, key).ifBlank { "-" }
}

private fun formatRecordTimestamp(epochMillis: Long): String {
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(epochMillis))
    }.getOrElse { "-" }
}

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Table ${index + 1}",
                    color = Color(0xFF0A4A99),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group.totalRecords} records",
                    color = Color(0xFF455A64),
                    fontSize = 12.sp
                )
            }
            Text(
                text = "UID: ${group.ownerUid}",
                color = Color(0xFF455A64),
                fontSize = 12.sp
            )
            Text(
                text = "SID: ${group.sid}",
                color = Color(0xFF455A64),
                fontSize = 12.sp
            )
            Text(
                text = "Location: ${group.location}",
                color = Color(0xFF455A64),
                fontSize = 12.sp
            )
            Text(
                text = "Schemas: ${group.schemaCount}",
                color = Color(0xFF607D8B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TableIdentityHeader(group: InventoryGroup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEAF2FF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "UID: ${group.ownerUid}",
            color = Color(0xFF0A4A99),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "SID: ${group.sid}",
            color = Color(0xFF0A4A99),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Location: ${group.location}",
            color = Color(0xFF0A4A99),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
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
