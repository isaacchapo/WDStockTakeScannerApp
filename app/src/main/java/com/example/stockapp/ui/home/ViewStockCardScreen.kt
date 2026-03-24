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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.data.JsonFieldExtractor
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.UploadDevice
import com.example.stockapp.ui.StockViewModel
import com.example.stockapp.ui.common.StockAppBackground
import com.example.stockapp.ui.common.ScreenAppear
import com.example.stockapp.ui.common.StockAppColors
import com.example.stockapp.ui.common.stockOutlinedTextFieldColors
import com.example.stockapp.ui.sharing.shareStockSchemaAsStyledPdf
import com.example.stockapp.ui.upload.showStockUploadSuccessNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val DEFAULT_TABLE_COLUMNS = listOf("FIELD1", "FIELD2", "FIELD3", "FIELD4")

@Composable
fun ViewStockCardScreen(
    stockViewModel: StockViewModel,
    onBack: () -> Unit,
    shareMode: Boolean = false
) {
    val inventoryGroups by stockViewModel.inventoryGroups.collectAsState()
    var selectedGroupKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var openedGroupKey by remember { mutableStateOf<String?>(null) }
    val groupListState = rememberLazyListState()
    val stockItemListState = rememberLazyListState()

    val selectedGroupsState = remember {
        derivedStateOf { inventoryGroups.filter { group -> selectedGroupKeys.contains(group.toKey()) } }
    }
    val selectedGroupState = remember {
        derivedStateOf { selectedGroupsState.value.singleOrNull() }
    }
    val openedGroupState = remember {
        derivedStateOf { inventoryGroups.firstOrNull { it.toKey() == openedGroupKey } }
    }
    val actionGroupState = remember {
        derivedStateOf { openedGroupState.value ?: selectedGroupState.value }
    }
    val selectedGroups = selectedGroupsState.value
    val selectedGroup = selectedGroupState.value
    val openedGroup = openedGroupState.value
    val actionGroup = actionGroupState.value

    val stockItems by if (openedGroup != null) {
        stockViewModel.getItemsForTable(
            location = openedGroup.location,
            sid = openedGroup.sid
        ).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val tableColumns by produceState(
        initialValue = DEFAULT_TABLE_COLUMNS,
        stockItems
    ) {
        value = withContext(Dispatchers.Default) {
            resolveTableColumns(stockItems)
        }
    }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uploadDevices by stockViewModel.uploadDevices.collectAsState()

    var showUpdateGroupDialog by remember { mutableStateOf(false) }
    var updateLocation by remember { mutableStateOf("") }

    var showChooseDeviceDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var deviceName by rememberSaveable { mutableStateOf("") }
    var deviceBaseUrl by rememberSaveable { mutableStateOf("") }
    var deviceEndpointPath by rememberSaveable { mutableStateOf("/api/stock/upload") }
    var deviceApiKey by rememberSaveable { mutableStateOf("") }
    var selectedUploadDeviceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUploadItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var isSavingDevice by remember { mutableStateOf(false) }

    fun toggleGroupSelection(groupKey: String) {
        selectedGroupKeys = if (selectedGroupKeys.contains(groupKey)) {
            selectedGroupKeys - groupKey
        } else {
            selectedGroupKeys + groupKey
        }
    }

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

    suspend fun collectItemsForGroups(groups: List<InventoryGroup>): List<StockItem> {
        return groups.flatMap { group ->
            stockViewModel.getItemsForTableSnapshot(group.location, group.sid)
        }
    }

    BackHandler(enabled = openedGroup != null || selectedGroupKeys.isNotEmpty()) {
        when {
            openedGroup != null -> openedGroupKey = null
            selectedGroupKeys.isNotEmpty() -> selectedGroupKeys = emptySet()
        }
    }

    LaunchedEffect(inventoryGroups) {
        val validKeys = inventoryGroups.mapTo(mutableSetOf()) { it.toKey() }
        selectedGroupKeys = selectedGroupKeys.filterTo(mutableSetOf()) { it in validKeys }
        if (openedGroupKey != null && openedGroupKey !in validKeys) {
            openedGroupKey = null
        }
    }

    LaunchedEffect(actionGroup) {
        if (actionGroup != null) {
            updateLocation = actionGroup.location
        }
    }

    LaunchedEffect(uploadDevices, showChooseDeviceDialog) {
        if (!showChooseDeviceDialog) return@LaunchedEffect
        val validKeys = uploadDevices.map { it.nameNormalized }.toSet()
        if (selectedUploadDeviceKey == null || selectedUploadDeviceKey !in validKeys) {
            selectedUploadDeviceKey = uploadDevices.firstOrNull()?.nameNormalized
        }
    }

    StockAppBackground {
        ScreenAppear {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                val topBarBrush = Brush.horizontalGradient(
                    colors = listOf(
                        StockAppColors.NavyMid,
                        StockAppColors.NavyBase,
                        StockAppColors.NavyDeep
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarBrush)
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
                            .background(StockAppColors.AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StockAppColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = when {
                            openedGroup != null && shareMode -> "SHARE ITEMS"
                            openedGroup != null -> "STOCK ITEMS"
                            shareMode -> "SHARE STOCKS"
                            else -> "STOCK TAKE"
                        },
                        color = StockAppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    if (openedGroup == null) {
                        TextButton(
                            onClick = {
                                selectedGroupKeys = inventoryGroups
                                    .mapTo(mutableSetOf()) { it.toKey() }
                            },
                            enabled = inventoryGroups.isNotEmpty(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                text = "Select All",
                                color = if (inventoryGroups.isNotEmpty()) {
                                    StockAppColors.AccentCyan
                                } else {
                                    StockAppColors.DisabledText
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (openedGroup != null) {
                            TableIdentityHeader(group = openedGroup)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (openedGroup == null) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.Top
                            ) {
                            LazyColumn(
                                state = groupListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = inventoryGroups,
                                    key = { _, group -> group.toKey() }
                                ) { index, group ->
                                    val groupKey = group.toKey()
                                    val isSelected = selectedGroupKeys.contains(groupKey)

                                    InventoryGroupRowCard(
                                        index = index,
                                        group = group,
                                        isSelected = isSelected,
                                        onTap = {
                                            if (selectedGroupKeys.isNotEmpty()) {
                                                toggleGroupSelection(groupKey)
                                            } else {
                                                openedGroupKey = groupKey
                                                selectedGroupKeys = emptySet()
                                            }
                                        },
                                        onLongPress = {
                                            toggleGroupSelection(groupKey)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LazyListScrollbar(
                                state = groupListState,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    } else {
                        val tableHeaderShape = RoundedCornerShape(8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(tableHeaderShape)
                                .background(StockAppColors.NavyBase)
                                .border(1.dp, StockAppColors.CardBorder, tableHeaderShape)
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell("No", 0.3f, isHeader = true, textColor = StockAppColors.TextPrimary)
                            TableCell(tableColumns[0], 1f, isHeader = true, textColor = StockAppColors.TextPrimary)
                            TableCell(tableColumns[1], 1.2f, isHeader = true, textColor = StockAppColors.TextPrimary)
                            TableCell(tableColumns[2], 1f, isHeader = true, textColor = StockAppColors.TextPrimary)
                            TableCell(tableColumns[3], 1f, isHeader = true, textColor = StockAppColors.TextPrimary, showRightDivider = false, textAlign = TextAlign.End)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalAlignment = Alignment.Top
                        ) {
                            LazyColumn(
                                state = stockItemListState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                itemsIndexed(
                                    items = stockItems,
                                    key = { _, item -> item.id }
                                ) { index, item ->
                                    val rowColor = if (index % 2 == 0) {
                                        StockAppColors.CardSurface
                                    } else {
                                        StockAppColors.FieldSurface
                                    }
                                    val itemFields = remember(item.variableData) {
                                        JsonFieldExtractor.extractAllFields(item.variableData)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .background(rowColor)
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
                                        val scannedAtLabel = remember(item.dateScanned) {
                                            formatRecordTimestamp(item.dateScanned)
                                        }
                                        Text(
                                            text = "Scanned: $scannedAtLabel",
                                            color = StockAppColors.TextSecondary,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(StockAppColors.CardBorder)
                                    )
                                }
                            }
                        }
                    }
                }

                if (openedGroup == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(1.dp)
                            .background(StockAppColors.CardBorder)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (shareMode) {
                            BottomActionButton("Share", StockAppColors.AccentCyan, enabled = selectedGroups.isNotEmpty()) {
                                val groups = selectedGroups
                                if (groups.isEmpty()) return@BottomActionButton
                                coroutineScope.launch {
                                    val items = collectItemsForGroups(groups)
                                    if (items.isEmpty()) {
                                        Toast.makeText(context, "No stock items", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val sidValues = groups.map { it.sid }.distinct()
                                        val sidHint = if (sidValues.size == 1) sidValues.first() else "MULTI"
                                        shareItemsAsPdf(items = items, sidHint = sidHint)
                                    }
                                }
                            }
                        } else {
                            BottomActionButton("Upload", StockAppColors.AccentCyan, enabled = selectedGroups.isNotEmpty()) {
                                val groups = selectedGroups
                                if (groups.isEmpty()) return@BottomActionButton
                                coroutineScope.launch {
                                    val items = collectItemsForGroups(groups)
                                    if (items.isEmpty()) {
                                        Toast.makeText(context, "No stock items", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pendingUploadItems = items
                                        selectedUploadDeviceKey = uploadDevices.firstOrNull()?.nameNormalized
                                        showChooseDeviceDialog = true
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            BottomActionButton("Update", StockAppColors.AccentAmber, enabled = selectedGroups.size == 1) {
                                val group = selectedGroup ?: return@BottomActionButton
                                updateLocation = group.location
                                showUpdateGroupDialog = true
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            BottomActionButton("Add Device", StockAppColors.AccentCyan, enabled = true) {
                                showAddDeviceDialog = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDeviceDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingDevice) showAddDeviceDialog = false },
            title = { Text("Add Device", color = StockAppColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = stockOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = deviceBaseUrl,
                        onValueChange = { deviceBaseUrl = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = stockOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = deviceEndpointPath,
                        onValueChange = { deviceEndpointPath = it },
                        label = { Text("Path (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = stockOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = deviceApiKey,
                        onValueChange = { deviceApiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = stockOutlinedTextFieldColors()
                    )
                    if (isSavingDevice) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = StockAppColors.AccentCyan
                            )
                            Text("Saving...", color = StockAppColors.TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSavingDevice &&
                        deviceName.trim().isNotBlank() &&
                        deviceBaseUrl.trim().isNotBlank() &&
                        deviceApiKey.trim().isNotBlank(),
                    onClick = {
                        isSavingDevice = true
                        stockViewModel.addUploadDevice(
                            name = deviceName,
                            baseUrl = deviceBaseUrl,
                            endpointPath = deviceEndpointPath,
                            apiKey = deviceApiKey
                        ) { result ->
                            isSavingDevice = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Device saved.", Toast.LENGTH_SHORT).show()
                                selectedUploadDeviceKey = deviceName.trim().lowercase(Locale.ROOT)
                                deviceName = ""
                                deviceBaseUrl = ""
                                deviceEndpointPath = "/api/stock/upload"
                                deviceApiKey = ""
                                showAddDeviceDialog = false
                            } else {
                                val err = result.exceptionOrNull()
                                Toast.makeText(context, err?.message ?: "Failed to save device.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Add", color = StockAppColors.AccentCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeviceDialog = false }, enabled = !isSavingDevice) {
                    Text("Cancel", color = StockAppColors.TextSecondary)
                }
            },
            containerColor = StockAppColors.CardSurface,
            titleContentColor = StockAppColors.TextPrimary,
            textContentColor = StockAppColors.TextSecondary
        )
    }

    if (showChooseDeviceDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showChooseDeviceDialog = false },
            title = { Text("Choose Device", color = StockAppColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uploadDevices.isEmpty()) {
                        Text(
                            text = "No devices available. Add a device first.",
                            color = StockAppColors.TextSecondary
                        )
                    } else {
                        uploadDevices.forEach { device ->
                            UploadDeviceRow(
                                device = device,
                                isSelected = selectedUploadDeviceKey == device.nameNormalized,
                                onSelect = { selectedUploadDeviceKey = device.nameNormalized }
                            )
                        }
                    }

                    if (isUploading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = StockAppColors.AccentCyan
                            )
                            Text("Uploading...", color = StockAppColors.TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isUploading &&
                        uploadDevices.isNotEmpty() &&
                        selectedUploadDeviceKey != null &&
                        pendingUploadItems.isNotEmpty(),
                    onClick = {
                        val device = uploadDevices
                            .firstOrNull { it.nameNormalized == selectedUploadDeviceKey }
                            ?: return@TextButton
                        val itemsForUpload = pendingUploadItems
                        isUploading = true
                        coroutineScope.launch {
                            val result = stockViewModel.uploadInventory(
                                baseUrl = device.baseUrl,
                                endpointPath = device.endpointPath,
                                apiKey = device.apiKey,
                                stockItems = itemsForUpload
                            )
                            isUploading = false
                            if (result.isSuccess) {
                                val msg = result.getOrNull().orEmpty()
                                stockViewModel.markItemsUploaded(itemsForUpload)
                                showStockUploadSuccessNotification(
                                    context = context,
                                    title = "Stock Upload Successful",
                                    message = "${itemsForUpload.size} items uploaded."
                                )
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                showChooseDeviceDialog = false
                                pendingUploadItems = emptyList()
                            } else {
                                val err = result.exceptionOrNull()
                                Toast.makeText(context, err?.message ?: "Upload failed.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Upload", color = StockAppColors.AccentCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChooseDeviceDialog = false }, enabled = !isUploading) {
                    Text("Cancel", color = StockAppColors.TextSecondary)
                }
            },
            containerColor = StockAppColors.CardSurface,
            titleContentColor = StockAppColors.TextPrimary,
            textContentColor = StockAppColors.TextSecondary
        )
    }

    if (showUpdateGroupDialog && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { showUpdateGroupDialog = false },
            title = { Text("Update Group Info", color = StockAppColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = updateLocation,
                        onValueChange = { updateLocation = it },
                        label = { Text("Location") },
                        colors = stockOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = selectedGroup.sid,
                        onValueChange = {},
                        label = { Text("SID") },
                        enabled = false,
                        colors = stockOutlinedTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val oldKey = selectedGroup.toKey()
                        stockViewModel.updateTableLocation(
                            oldLocation = selectedGroup.location,
                            oldSid = selectedGroup.sid,
                            newLocation = updateLocation.trim()
                        )
                        val newKey = listOf(selectedGroup.ownerUid, updateLocation.trim(), selectedGroup.sid).joinToString("|")
                        if (openedGroupKey == oldKey) openedGroupKey = newKey
                        selectedGroupKeys = (selectedGroupKeys - oldKey) + newKey
                        showUpdateGroupDialog = false
                    },
                    enabled = updateLocation.isNotBlank()
                ,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StockAppColors.AccentAmber,
                        disabledContainerColor = StockAppColors.DisabledSurface,
                        contentColor = StockAppColors.TextPrimary,
                        disabledContentColor = StockAppColors.DisabledText
                    )
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateGroupDialog = false }) {
                    Text("Cancel", color = StockAppColors.TextSecondary)
                }
            },
            containerColor = StockAppColors.CardSurface,
            titleContentColor = StockAppColors.TextPrimary,
            textContentColor = StockAppColors.TextSecondary
        )
    }
    }
}

@Composable
private fun BottomActionButton(
    text: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = StockAppColors.CardSurface,
            contentColor = color,
            disabledContainerColor = StockAppColors.DisabledSurface,
            disabledContentColor = StockAppColors.DisabledText
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(36.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) color else StockAppColors.DisabledText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UploadDeviceRow(
    device: UploadDevice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = device.name,
                color = StockAppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LazyListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val minThumbHeightPx = with(density) { 36.dp.toPx() }
    val metrics by remember(state, minThumbHeightPx) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
            if (viewportHeightPx == 0) return@derivedStateOf null

            val visibleItems = layoutInfo.visibleItemsInfo
            val averageItemHeightPx = if (visibleItems.isNotEmpty()) {
                var sum = 0
                for (item in visibleItems) {
                    sum += item.size
                }
                sum.toFloat() / visibleItems.size
            } else {
                viewportHeightPx.toFloat()
            }
            val estimatedContentHeightPx = if (layoutInfo.totalItemsCount > 0) {
                averageItemHeightPx * layoutInfo.totalItemsCount
            } else {
                viewportHeightPx.toFloat()
            }.coerceAtLeast(viewportHeightPx.toFloat())
            val firstVisibleItem = visibleItems.firstOrNull()
            val scrollOffsetPx = if (firstVisibleItem != null) {
                (firstVisibleItem.index * averageItemHeightPx - firstVisibleItem.offset).coerceAtLeast(0f)
            } else {
                0f
            }
            val thumbHeightPx = if (estimatedContentHeightPx == 0f) {
                viewportHeightPx.toFloat()
            } else {
                ((viewportHeightPx.toFloat() / estimatedContentHeightPx) * viewportHeightPx)
                    .coerceIn(minThumbHeightPx, viewportHeightPx.toFloat())
            }
            val maxThumbOffsetPx = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
            val maxScrollPx = (estimatedContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
            val thumbOffsetPx = if (maxThumbOffsetPx == 0f) {
                0f
            } else {
                (scrollOffsetPx / maxScrollPx).coerceIn(0f, 1f) * maxThumbOffsetPx
            }

            ScrollbarMetrics(thumbHeightPx = thumbHeightPx, thumbOffsetPx = thumbOffsetPx)
        }
    }
    val resolvedMetrics = metrics ?: return

    Box(
        modifier = modifier
            .width(7.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(StockAppColors.CardBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { resolvedMetrics.thumbHeightPx.toDp() })
                .offset { IntOffset(0, resolvedMetrics.thumbOffsetPx.roundToInt()) }
                .clip(RoundedCornerShape(50))
                .background(StockAppColors.AccentCyan.copy(alpha = 0.7f))
        )
    }
}

private data class ScrollbarMetrics(
    val thumbHeightPx: Float,
    val thumbOffsetPx: Float
)

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

private var cachedTimestampLocale: Locale? = null
private var cachedTimestampFormatter: SimpleDateFormat? = null

private fun formatRecordTimestamp(epochMillis: Long): String {
    return runCatching {
        val locale = Locale.getDefault()
        val formatter = if (cachedTimestampFormatter == null || cachedTimestampLocale != locale) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).also {
                cachedTimestampFormatter = it
                cachedTimestampLocale = locale
            }
        } else {
            cachedTimestampFormatter ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
        }
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
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StockAppColors.AccentCyan else StockAppColors.CardBorder,
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                StockAppColors.AccentCyan.copy(alpha = 0.14f)
            } else {
                StockAppColors.CardSurface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val displayTitle = if (group.stockName.isNotBlank()) {
                "${group.stockName} Stock"
            } else {
                "Stock ${index + 1}"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayTitle,
                    color = StockAppColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group.totalRecords} records",
                    color = StockAppColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "UID: ${group.ownerUid}",
                color = StockAppColors.TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "SID: ${group.sid}",
                color = StockAppColors.TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Location: ${group.location}",
                color = StockAppColors.TextSecondary,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (group.isUploaded) "Uploaded" else "Pending upload",
                    color = if (group.isUploaded) StockAppColors.AccentCyan else StockAppColors.AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TableIdentityHeader(group: InventoryGroup) {
    val headerShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(StockAppColors.CardSurface)
            .border(1.dp, StockAppColors.CardBorder, headerShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableIdentityValue(
            label = "UID",
            value = group.ownerUid,
            modifier = Modifier.weight(1f)
        )
        TableIdentityValue(
            label = "SID",
            value = group.sid,
            modifier = Modifier.weight(1f)
        )
        TableIdentityValue(
            label = "Location",
            value = group.location,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun TableIdentityValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = StockAppColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value.ifBlank { "-" },
            color = StockAppColors.AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    textColor: Color = StockAppColors.TextPrimary,
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
                .background(if (isHeader) StockAppColors.CardBorder else StockAppColors.Divider)
        )
    }
}
