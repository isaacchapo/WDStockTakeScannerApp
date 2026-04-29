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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockapp.data.JsonFieldExtractor
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.InventoryGroup
import com.example.stockapp.data.local.StockItem
import com.example.stockapp.data.local.UploadDevice
import com.example.stockapp.ui.StockViewModel
import com.example.stockapp.ui.common.StockAppColors
import com.example.stockapp.ui.common.stockOutlinedTextFieldColors
import com.example.stockapp.ui.sharing.shareStockSchemaAsStyledPdf
import com.example.stockapp.ui.upload.DEFAULT_UPLOAD_ENDPOINT_PATH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DEFAULT_TABLE_COLUMNS = listOf("FIELD1", "FIELD2", "FIELD3", "FIELD4")
private val EMPTY_TABLE_VALUES = listOf("-", "-", "-", "-")
private val HTTP_PREFIX_REGEX = Regex("^https?://", RegexOption.IGNORE_CASE)

internal fun sanitizeDeviceBaseUrlInput(rawInput: String): String {
    return rawInput
        .trim()
        .trimEnd('/')
}

internal fun buildDeviceBaseUrl(baseUrlInput: String): String {
    val normalizedInput = sanitizeDeviceBaseUrlInput(baseUrlInput)
    return if (HTTP_PREFIX_REGEX.containsMatchIn(normalizedInput)) {
        normalizedInput
    } else {
        "http://$normalizedInput"
    }
}

internal fun sanitizeEndpointSuffixInput(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return ""

    val hasScheme = HTTP_PREFIX_REGEX.containsMatchIn(trimmed)
    val withoutScheme = trimmed.replaceFirst(HTTP_PREFIX_REGEX, "")
    val beforeFirstSlash = withoutScheme.substringBefore('/')
    val looksLikeHostPort = beforeFirstSlash.contains('.') ||
        beforeFirstSlash.contains(':') ||
        beforeFirstSlash.equals("localhost", ignoreCase = true)
    val pathCandidate = if ((hasScheme || looksLikeHostPort) && withoutScheme.contains('/')) {
        withoutScheme.substringAfter('/', "")
    } else {
        withoutScheme
    }
    val normalizedPath = pathCandidate.trim().trim('/')
    return normalizedPath
}

internal fun buildEndpointPath(endpointSuffixInput: String): String {
    val normalizedPath = sanitizeEndpointSuffixInput(endpointSuffixInput)
    return if (normalizedPath.isBlank()) {
        DEFAULT_UPLOAD_ENDPOINT_PATH
    } else {
        "/$normalizedPath"
    }
}

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
    val allGroupKeys = remember(inventoryGroups) {
        inventoryGroups.mapTo(mutableSetOf()) { it.toKey() }
    }
    val isAllSelected = allGroupKeys.isNotEmpty() && selectedGroupKeys.containsAll(allGroupKeys)

    val stockItems by if (openedGroup != null) {
        stockViewModel.getItemsForTableAllSchemas(
            location = openedGroup.location,
            stockName = openedGroup.stockName
        ).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val tableRenderData by produceState(
        initialValue = TableRenderData(DEFAULT_TABLE_COLUMNS, emptyMap()),
        stockItems
    ) {
        value = withContext(Dispatchers.Default) {
            buildTableRenderData(stockItems)
        }
    }
    val tableColumns = tableRenderData.columns
    val rowValuesByItemId = tableRenderData.rowValuesByItemId
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uploadDevices by stockViewModel.uploadDevices.collectAsState()

    var showUpdateGroupDialog by remember { mutableStateOf(false) }
    var updateLocation by remember { mutableStateOf("") }

    var showChooseDeviceDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var deviceName by rememberSaveable { mutableStateOf("") }
    var deviceBaseUrl by rememberSaveable { mutableStateOf("") }
    var deviceEndpointSuffix by rememberSaveable { mutableStateOf("") }
    var deviceApiKey by rememberSaveable { mutableStateOf("") }
    var selectedUploadDeviceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUploadItems by remember { mutableStateOf<List<StockItem>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var isSavingDevice by remember { mutableStateOf(false) }
    var isDeletingDevice by remember { mutableStateOf(false) }
    var devicePendingDeletion by remember { mutableStateOf<UploadDevice?>(null) }
    var showUploadSuccessDialog by remember { mutableStateOf(false) }

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
            stockViewModel.getItemsForTableSnapshot(
                location = group.location,
                stockName = group.stockName,
                identifierKey = group.identifierKey
            )
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
                // Top Bar
                val topBarBrush = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            StockAppColors.NavyMid,
                            StockAppColors.NavyBase
                        )
                    )
                }
                androidx.compose.material3.Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(topBarBrush)
                            .statusBarsPadding()
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

                        if (openedGroup == null && selectedGroupKeys.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedGroupKeys = allGroupKeys
                                },
                                enabled = inventoryGroups.isNotEmpty(),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isAllSelected) {
                                                    StockAppColors.AccentCyan
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (inventoryGroups.isNotEmpty()) {
                                                    StockAppColors.AccentCyan
                                                } else {
                                                    StockAppColors.DisabledText
                                                },
                                                RoundedCornerShape(50)
                                            )
                                    ) {
                                        if (isAllSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(5.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(StockAppColors.NavyDeep)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "All",
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
                        }
                    } else {
                        val tableContainerShape = RoundedCornerShape(12.dp)
                        val tableHeaderShape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                        val tableGridLineColor = StockAppColors.TextSecondary.copy(alpha = 0.42f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(tableContainerShape)
                                .border(1.dp, StockAppColors.CardBorder, tableContainerShape)
                                .background(StockAppColors.CardSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(tableHeaderShape)
                                    .background(StockAppColors.NavyBase)
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell("No", 0.4f, isHeader = true, textColor = StockAppColors.TextPrimary, textAlign = TextAlign.Center)
                                TableCell(tableColumns[0], 1f, isHeader = true, textColor = StockAppColors.TextPrimary)
                                TableCell(tableColumns[1], 1.2f, isHeader = true, textColor = StockAppColors.TextPrimary)
                                TableCell(tableColumns[2], 1f, isHeader = true, textColor = StockAppColors.TextPrimary)
                                TableCell(tableColumns[3], 1f, isHeader = true, textColor = StockAppColors.TextPrimary, showRightDivider = false, textAlign = TextAlign.End)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(tableGridLineColor)
                            )

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
                                        .fillMaxHeight(),
                                    userScrollEnabled = true
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
                                        val tableValues = rowValuesByItemId[item.id] ?: EMPTY_TABLE_VALUES

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
                                                TableCell((index + 1).toString(), 0.4f, textAlign = TextAlign.Center)
                                                TableCell(tableValues.getOrElse(0) { "-" }, 1f)
                                                TableCell(tableValues.getOrElse(1) { "-" }, 1.2f)
                                                TableCell(tableValues.getOrElse(2) { "-" }, 1f)
                                                TableCell(tableValues.getOrElse(3) { "-" }, 1f, showRightDivider = false, textAlign = TextAlign.End)
                                            }
                                            val scannedAtLabel = remember(item.dateScanned) {
                                                formatRecordTimestamp(item.dateScanned)
                                            }
                                            Text(
                                                text = "SID: ${item.sid.ifBlank { "-" }}  |  Scanned: $scannedAtLabel",
                                                color = StockAppColors.TextSecondary,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(tableGridLineColor)
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
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(1.dp)
                            .background(StockAppColors.CardBorder)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 12.dp),
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
                                        val sidValues = items.map { it.sid }.distinct()
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
                        onValueChange = {
                            deviceBaseUrl = it
                        },
                        label = { Text("Base URL") },
                        placeholder = { Text("http://192.168.0.0:8000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = stockOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = deviceEndpointSuffix,
                        onValueChange = { deviceEndpointSuffix = it },
                        label = { Text("Path") },
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
                            baseUrl = buildDeviceBaseUrl(deviceBaseUrl),
                            endpointPath = buildEndpointPath(deviceEndpointSuffix),
                            apiKey = deviceApiKey
                        ) { result ->
                            isSavingDevice = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Device saved.", Toast.LENGTH_SHORT).show()
                                selectedUploadDeviceKey = deviceName.trim().lowercase(Locale.ROOT)
                                deviceName = ""
                                deviceBaseUrl = ""
                                deviceEndpointSuffix = ""
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
                                onSelect = { selectedUploadDeviceKey = device.nameNormalized },
                                onDelete = { devicePendingDeletion = device },
                                deleteEnabled = !isUploading && !isDeletingDevice
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
                                stockViewModel.markItemsUploaded(itemsForUpload)
                                showChooseDeviceDialog = false
                                pendingUploadItems = emptyList()
                                showUploadSuccessDialog = true
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
                        value = selectedGroup.stockName.ifBlank { "-" },
                        onValueChange = {},
                        label = { Text("Stock") },
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
                            stockName = selectedGroup.stockName,
                            identifierKey = selectedGroup.identifierKey,
                            newLocation = updateLocation.trim()
                        )
                        val newKey = listOf(
                            selectedGroup.ownerUid,
                            updateLocation.trim(),
                            selectedGroup.stockName,
                            selectedGroup.identifierKey
                        ).joinToString("|")
                        if (openedGroupKey == oldKey) openedGroupKey = newKey
                        selectedGroupKeys = (selectedGroupKeys - oldKey) + newKey
                        showUpdateGroupDialog = false
                    },
                    enabled = updateLocation.isNotBlank()
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

    devicePendingDeletion?.let { device ->
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingDevice) {
                    devicePendingDeletion = null
                }
            },
            title = { Text("Delete Device", color = StockAppColors.TextPrimary) },
            text = {
                Text(
                    text = "Delete ${device.name} from saved devices?",
                    color = StockAppColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingDevice,
                    onClick = {
                        isDeletingDevice = true
                        stockViewModel.deleteUploadDevice(device.nameNormalized) { result ->
                            isDeletingDevice = false
                            if (result.isSuccess) {
                                if (selectedUploadDeviceKey == device.nameNormalized) {
                                    selectedUploadDeviceKey = uploadDevices
                                        .firstOrNull { it.nameNormalized != device.nameNormalized }
                                        ?.nameNormalized
                                }
                                devicePendingDeletion = null
                            } else {
                                val err = result.exceptionOrNull()
                                Toast.makeText(
                                    context,
                                    err?.message ?: "Failed to delete device.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = StockAppColors.AccentAmber)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { devicePendingDeletion = null },
                    enabled = !isDeletingDevice
                ) {
                    Text("Cancel", color = StockAppColors.TextSecondary)
                }
            },
            containerColor = StockAppColors.CardSurface,
            titleContentColor = StockAppColors.TextPrimary,
            textContentColor = StockAppColors.TextSecondary
        )
    }

    if (showUploadSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showUploadSuccessDialog = false },
            title = { Text("Upload Status", color = StockAppColors.TextPrimary) },
            text = {
                Text(
                    text = "Stock data uploaded successfully",
                    color = StockAppColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showUploadSuccessDialog = false }) {
                    Text("OK", color = StockAppColors.AccentCyan)
                }
            },
            containerColor = StockAppColors.CardSurface,
            titleContentColor = StockAppColors.TextPrimary,
            textContentColor = StockAppColors.TextSecondary
        )
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
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean
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
        IconButton(
            onClick = onDelete,
            enabled = deleteEnabled,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete ${device.name}",
                tint = if (deleteEnabled) StockAppColors.AccentAmber else StockAppColors.DisabledText
            )
        }
    }
}

private fun InventoryGroup.toKey(): String = listOf(
    ownerUid,
    location,
    stockName,
    identifierKey
).joinToString("|")

private data class TableRenderData(
    val columns: List<String>,
    val rowValuesByItemId: Map<String, List<String>>
)

private fun buildTableRenderData(items: List<StockItem>): TableRenderData {
    if (items.isEmpty()) {
        return TableRenderData(
            columns = DEFAULT_TABLE_COLUMNS,
            rowValuesByItemId = emptyMap()
        )
    }

    val parsedFieldsByItemId = LinkedHashMap<String, Map<String, String>>(items.size)
    val parsedFieldSets = ArrayList<Map<String, String>>(items.size)
    for (item in items) {
        val fields = JsonFieldExtractor.extractAllFields(item.variableData)
        parsedFieldsByItemId[item.id] = fields
        if (fields.isNotEmpty()) {
            parsedFieldSets.add(fields)
        }
    }

    val columns = resolveTableColumns(parsedFieldSets)
    val rowValuesByItemId = LinkedHashMap<String, List<String>>(items.size)
    for (item in items) {
        val fields = parsedFieldsByItemId[item.id].orEmpty()
        rowValuesByItemId[item.id] = columns.map { column -> resolveTableValue(fields, column) }
    }

    return TableRenderData(columns = columns, rowValuesByItemId = rowValuesByItemId)
}

private fun resolveTableColumns(parsedFieldSets: List<Map<String, String>>): List<String> {

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
                color = if (isSelected) StockAppColors.AccentCyan.copy(alpha = 0.7f) else StockAppColors.CardBorder,
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                StockAppColors.AccentCyan
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
                text = "Location: ${group.location}",
                color = StockAppColors.TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Stock: ${group.stockName.ifBlank { "-" }}",
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
            label = "Stock",
            value = group.stockName,
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
