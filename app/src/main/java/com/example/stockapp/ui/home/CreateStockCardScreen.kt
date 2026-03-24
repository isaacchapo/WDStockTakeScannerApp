package com.example.stockapp.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockapp.StockApplication
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.SavedLocation
import com.example.stockapp.ui.common.StockAppBackground
import com.example.stockapp.ui.common.ScreenAppear
import com.example.stockapp.ui.common.StockAppColors
import com.example.stockapp.ui.common.stockOutlinedTextFieldColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SCAN_BROADCAST_ACTIONS = listOf(
    "android.intent.ACTION_DECODE_DATA",
    "android.intent.action.SCANRESULT",
    "com.android.scanner.broadcast",
    "com.android.scanservice.broadcast",
    "com.android.server.scannerservice.broadcast",
    "com.seuic.scanner.scanned",
    "com.chainway.decode.broadcast",
    "com.rugged.scanner.broadcast",
    "com.rscja.scanner.action.BARCODE_DECODING_BROADCAST",
    "nlscan.action.SCANNER_RESULT",
    "com.honeywell.aidc.action.ACTION_BARCODE_READ_EVENT",
    "com.datalogic.decodewedge.decode_action",
    "com.symbol.datawedge.api.RESULT_ACTION",
    "com.android.serial.BARCODE_READ_ACTION",
    "com.barcode.sendBroadcast",
    "com.google.zxing.client.android.SCAN"
)

private val SCAN_DATA_EXTRA_KEYS = listOf(
    "data",
    "barcode",
    "barcode_data",
    "barcode_string",
    "scan_result_1",
    "scanner_result",
    "SCAN_RESULT",
    "scanData",
    "scan_result",
    "scannerdata",
    "decode_data",
    "result",
    "value",
    "SCAN_BARCODE1",
    "com.symbol.datawedge.data_string",
    "com.motorolasolutions.emdk.datawedge.data_string",
    Intent.EXTRA_TEXT
)

private val DEFAULT_PREVIEW_COLUMNS = listOf("FIELD1", "FIELD2", "FIELD3", "FIELD4")

// Intent actions to trigger hardware scanners on various industrial devices
private const val ACTION_SCANNER_SOFT_TRIGGER = "com.android.serial.BARCODE_READ_ACTION"
private const val ACTION_SCANNER_START = "com.android.scanner.ENABLED" // Generic/Handheld
private const val ACTION_SCANNER_TRIGGER = "com.motorolasolutions.emdk.datawedge.api.ACTION_SCANNER_TRIGGER" // Zebra
private const val EXTRA_PARAMETER_VALUE = "START_SCANNING"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStockCardScreen(
    loggedInUser: String,
    onBack: () -> Unit,
    createStockViewModel: CreateStockViewModel = viewModel(
        factory = CreateStockViewModelFactory(
            (LocalContext.current.applicationContext as StockApplication).repository,
            loggedInUser
        )
    )
) {
    val context = LocalContext.current

    var isScanning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isDeviceScannerBusy by remember { mutableStateOf(false) }
    var showSavedLocations by remember { mutableStateOf(false) }
    var showAddLocationForm by remember { mutableStateOf(false) }
    var addLocationDraft by remember { mutableStateOf("") }
    var isTopCardCollapsed by remember { mutableStateOf(false) }
    var selectedScannedItemId by remember { mutableStateOf<String?>(null) }
    var showEndSessionPrompt by remember { mutableStateOf(false) }
    var showExitSessionPrompt by remember { mutableStateOf(false) }
    var showStockNamePrompt by remember { mutableStateOf(false) }
    var stockNameDraft by remember { mutableStateOf("") }
    var tableTopInRoot by remember { mutableFloatStateOf(0f) }
    var tableLeftInRoot by remember { mutableFloatStateOf(0f) }
    var tableWidthPx by remember { mutableIntStateOf(0) }
    var selectedRowTopPx by remember { mutableStateOf<Int?>(null) }
    var selectedRowLeftPx by remember { mutableStateOf<Int?>(null) }
    var selectedRowWidthPx by remember { mutableStateOf<Int?>(null) }

    val scannedItems by createStockViewModel.scannedItems.collectAsState()
    val currentSid by createStockViewModel.currentSid.collectAsState()
    val savedLocations by createStockViewModel.savedLocations.collectAsState()
    val isSaving by createStockViewModel.isSaving.collectAsState()
    val selectedScannedItemState = remember {
        derivedStateOf { scannedItems.firstOrNull { it.id == selectedScannedItemId } }
    }
    val selectedScannedItem = selectedScannedItemState.value
    val density = LocalDensity.current
    val overlayWidthPx = with(density) { 236.dp.roundToPx() }
    val overlayVerticalShiftPx = with(density) { 4.dp.roundToPx() }

    var location by remember { mutableStateOf("") }
    val actionButtonWidth = 126.dp
    val normalizedLocationInput by remember {
        derivedStateOf { location.trim().lowercase(Locale.ROOT) }
    }
    val displaySid by remember {
        derivedStateOf {
            savedLocations
                .firstOrNull { it.locationNormalized == normalizedLocationInput }
                ?.sid
                ?.takeIf { it.isNotBlank() }
                ?: currentSid
        }
    }

    val canConfirm by remember {
        derivedStateOf {
            scannedItems.isNotEmpty() &&
                location.isNotBlank() &&
                !isSaving
        }
    }

    val statusText = when {
        isScanning && isPaused -> "Paused"
        isScanning -> "Scanning"
        else -> "Ready"
    }

    fun requestExitNavigation() {
        if (isScanning || isPaused) {
            showExitSessionPrompt = true
        } else {
            onBack()
        }
    }

    BackHandler {
        when {
            showAddLocationForm -> showAddLocationForm = false
            showStockNamePrompt -> showStockNamePrompt = false
            showExitSessionPrompt -> showExitSessionPrompt = false
            else -> requestExitNavigation()
        }
    }

    fun applyScanResult(rawValue: String) {
        createStockViewModel.addScannedItem(rawValue) { result ->
            if (!result.accepted) {
                val message = if (result.message?.contains("duplicate", ignoreCase = true) == true) {
                    "Duplicate not allowed"
                } else {
                    result.message
                }
                if (!message.isNullOrBlank()) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val startDeviceScanner: () -> Unit = {
        if (!isPaused && !isDeviceScannerBusy) {
            if (!isScanning) {
                isScanning = true
                isPaused = false
            }

            isDeviceScannerBusy = true

            // Trigger the hardware aiming laser/scanner via broadcast
            try {
                // Try several common trigger intents for industrial handhelds
                context.sendBroadcast(Intent(ACTION_SCANNER_SOFT_TRIGGER))
                context.sendBroadcast(Intent(ACTION_SCANNER_START))
                context.sendBroadcast(Intent(ACTION_SCANNER_TRIGGER).apply {
                    putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER_VALUE", EXTRA_PARAMETER_VALUE)
                })
                Log.d("Scanner", "Sent hardware scanner trigger broadcasts")
            } catch (e: Exception) {
                Log.e("Scanner", "Failed to send hardware trigger", e)
                Toast.makeText(context, "Could not arm scanner trigger broadcast.", Toast.LENGTH_SHORT).show()
            } finally {
                isDeviceScannerBusy = false
            }
        }
    }

    AimerScannerBroadcastEffect(
        enabled = isScanning && !isPaused,
        onBarcodeScanned = { scannedPayload ->
            Log.d("AimerScanner", "Received broadcast: $scannedPayload")
            applyScanResult(scannedPayload)
        }
    )

    HiddenScannerWedgeInputEffect(
        enabled = isScanning && !isPaused,
        onBarcodeScanned = { scannedPayload ->
            Log.d("AimerScanner", "Received wedge input: $scannedPayload")
            applyScanResult(scannedPayload)
        }
    )

    StockAppBackground {
        ScreenAppear {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CreateStockTopBar(onBack = { requestExitNavigation() })

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusIndicator(statusText = statusText)
                                if (isTopCardCollapsed) {
                                    Text(
                                        text = "LOC:${location.ifBlank { "-" }}  UID:$loggedInUser  SID:$displaySid",
                                        color = StockAppColors.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SessionChip(label = "UID", value = loggedInUser)
                                        SessionChip(label = "SID", value = displaySid)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        isTopCardCollapsed = !isTopCardCollapsed
                                        if (isTopCardCollapsed) {
                                            showSavedLocations = false
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTopCardCollapsed) {
                                            Icons.Filled.ExpandMore
                                        } else {
                                            Icons.Filled.ExpandLess
                                        },
                                        contentDescription = "Toggle card",
                                        tint = StockAppColors.AccentCyan
                                    )
                                }
                            }

                            if (!isTopCardCollapsed) {
                                LocationEntryCard(
                                    location = location,
                                    onLocationChange = { location = it },
                                    savedLocations = savedLocations,
                                    locationsExpanded = showSavedLocations,
                                    onToggleLocations = { showSavedLocations = !showSavedLocations },
                                    onDismissLocations = { showSavedLocations = false },
                                    onOpenAddLocationForm = {
                                        addLocationDraft = ""
                                        showAddLocationForm = true
                                    },
                                    onSelectLocation = { selected ->
                                        location = selected
                                        showSavedLocations = false
                                    },
                                    enabled = !isSaving
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        val previewColumns by produceState(
                            initialValue = DEFAULT_PREVIEW_COLUMNS,
                            scannedItems
                        ) {
                            value = withContext(Dispatchers.Default) {
                                resolvePreviewColumns(scannedItems)
                            }
                        }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .onGloballyPositioned { coordinates ->
                                val rootPosition = coordinates.positionInRoot()
                                tableTopInRoot = rootPosition.y
                                tableLeftInRoot = rootPosition.x
                                tableWidthPx = coordinates.size.width
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val startStopLabel = if (isScanning) "End" else "Start"
                            val startStopIcon = if (isScanning) Icons.Filled.Stop else Icons.Filled.QrCodeScanner
                            val startStopColor = if (isScanning) {
                                StockAppColors.AccentAmber
                            } else {
                                StockAppColors.AccentCyan
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (!isScanning) {
                                            if (location.isBlank()) {
                                                isTopCardCollapsed = false
                                                Toast.makeText(context, "Please enter Location", Toast.LENGTH_SHORT).show()
                                                return@OutlinedButton
                                            }
                                            isTopCardCollapsed = true
                                            showSavedLocations = false
                                            isScanning = true
                                            isPaused = false
                                            startDeviceScanner()
                                        } else {
                                            showEndSessionPrompt = true
                                        }
                                    },
                                    enabled = isScanning || !isDeviceScannerBusy,
                                    modifier = Modifier
                                        .height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, StockAppColors.CardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = StockAppColors.CardSurface,
                                        disabledContainerColor = StockAppColors.DisabledSurface,
                                        contentColor = startStopColor,
                                        disabledContentColor = StockAppColors.DisabledText
                                    )
                                ) {
                                    Icon(
                                        imageVector = startStopIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isScanning || !isDeviceScannerBusy) {
                                            startStopColor
                                        } else {
                                            StockAppColors.DisabledText
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = startStopLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isScanning || !isDeviceScannerBusy) {
                                            startStopColor
                                        } else {
                                            StockAppColors.DisabledText
                                        }
                                    )
                                }
                                Text(
                                    text = "Scanned Items",
                                    color = StockAppColors.TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StockAppColors.NavyMid, RoundedCornerShape(8.dp))
                                    .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("No", modifier = Modifier.weight(0.3f), color = StockAppColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[0], modifier = Modifier.weight(1f), color = StockAppColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[1], modifier = Modifier.weight(1f), color = StockAppColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[2], modifier = Modifier.weight(1f), color = StockAppColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[3], modifier = Modifier.weight(1f), color = StockAppColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }

                            if (scannedItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No QR scans yet. Pull trigger after starting scan.",
                                        color = StockAppColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(
                                        items = scannedItems,
                                        key = { _, item -> item.id }
                                    ) { index, item ->
                                        val isSelected = selectedScannedItemId == item.id
                                        val itemFields = remember(item.variableData) {
                                            com.example.stockapp.data.JsonFieldExtractor.extractAllFields(item.variableData)
                                        }
                                        val positionModifier = if (isSelected) {
                                            Modifier.onGloballyPositioned { coordinates ->
                                                val rowPosition = coordinates.positionInRoot()
                                                selectedRowTopPx = (rowPosition.y - tableTopInRoot).toInt()
                                                selectedRowLeftPx = (rowPosition.x - tableLeftInRoot).toInt()
                                                selectedRowWidthPx = coordinates.size.width
                                            }
                                        } else {
                                            Modifier
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isSelected -> StockAppColors.AccentCyan.copy(alpha = 0.22f)
                                                        index % 2 == 0 -> StockAppColors.CardSurface
                                                        else -> StockAppColors.FieldSurface
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) {
                                                        StockAppColors.AccentCyan.copy(alpha = 0.65f)
                                                    } else {
                                                        StockAppColors.CardBorder
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .then(positionModifier)
                                                .clickable {
                                                    selectedScannedItemId = if (isSelected) {
                                                        selectedRowTopPx = null
                                                        selectedRowLeftPx = null
                                                        selectedRowWidthPx = null
                                                        null
                                                    } else {
                                                        item.id
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    "${index + 1}",
                                                    modifier = Modifier.weight(0.3f),
                                                    fontSize = 11.sp,
                                                    color = StockAppColors.TextPrimary
                                                )
                                                Text(
                                                    resolvePreviewValue(itemFields, previewColumns[0]),
                                                    modifier = Modifier.weight(1f),
                                                    fontSize = 11.sp,
                                                    color = StockAppColors.TextPrimary
                                                )
                                                Text(
                                                    resolvePreviewValue(itemFields, previewColumns[1]),
                                                    modifier = Modifier.weight(1f),
                                                    fontSize = 11.sp,
                                                    color = StockAppColors.TextPrimary
                                                )
                                                Text(
                                                    resolvePreviewValue(itemFields, previewColumns[2]),
                                                    modifier = Modifier.weight(1f),
                                                    fontSize = 11.sp,
                                                    color = StockAppColors.TextPrimary
                                                )
                                                Text(
                                                    resolvePreviewValue(itemFields, previewColumns[3]),
                                                    modifier = Modifier.weight(1f),
                                                    fontSize = 11.sp,
                                                    textAlign = TextAlign.End,
                                                    color = StockAppColors.TextPrimary
                                                )
                                            }
                                            val scannedAtLabel = remember(item.dateScanned) {
                                                formatRecordTimestamp(item.dateScanned)
                                            }
                                            Text(
                                                text = "Scanned: $scannedAtLabel",
                                                color = StockAppColors.TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StockAppColors.FieldSurface)
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Total:${scannedItems.size}",
                                    color = StockAppColors.AccentCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (
                            selectedScannedItem != null &&
                            selectedRowTopPx != null &&
                            selectedRowLeftPx != null &&
                            selectedRowWidthPx != null
                        ) {
                            DeleteRecordOverlayPrompt(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        val rowTop = selectedRowTopPx ?: 0
                                        val rowLeft = selectedRowLeftPx ?: 0
                                        val rowWidth = selectedRowWidthPx ?: 0
                                        val maxX = (tableWidthPx - overlayWidthPx).coerceAtLeast(0)
                                        val centeredX = rowLeft + ((rowWidth - overlayWidthPx) / 2)
                                        IntOffset(
                                            x = centeredX.coerceIn(0, maxX),
                                            y = (rowTop - overlayVerticalShiftPx).coerceAtLeast(0)
                                        )
                                    }
                                    .zIndex(2f),
                                onConfirmDelete = {
                                    createStockViewModel.removeScannedItem(selectedScannedItem.id)
                                    selectedScannedItemId = null
                                    selectedRowTopPx = null
                                    selectedRowLeftPx = null
                                    selectedRowWidthPx = null
                                },
                                onCancel = {
                                    selectedScannedItemId = null
                                    selectedRowTopPx = null
                                    selectedRowLeftPx = null
                                    selectedRowWidthPx = null
                                }
                            )
                        }
                    }
                }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pauseResumeLabel = if (isPaused) "Resume" else "Pause"
                        val pauseResumeIcon = if (isPaused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle

                        TightActionButton(
                            text = pauseResumeLabel,
                            icon = pauseResumeIcon,
                            onClick = {
                                if (!isScanning) return@TightActionButton
                                isTopCardCollapsed = true
                                showSavedLocations = false
                                if (isPaused) {
                                    isPaused = false
                                    startDeviceScanner()
                                } else {
                                    isPaused = true
                                }
                            },
                            enabled = isScanning && (!isPaused || !isDeviceScannerBusy),
                            accentColor = StockAppColors.AccentAmber,
                            modifier = Modifier.width(actionButtonWidth)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        TightActionButton(
                            text = if (isSaving) "Saving..." else "Confirm",
                            icon = Icons.Filled.CheckCircle,
                            onClick = {
                                if (!canConfirm) return@TightActionButton
                                stockNameDraft = ""
                                showStockNamePrompt = true
                            },
                            enabled = canConfirm,
                            accentColor = StockAppColors.AccentCyan,
                            modifier = Modifier.width(actionButtonWidth)
                        )
                    }
                }
            }
        }
    }

    if (showStockNamePrompt) {
        StockNamePrompt(
            stockName = stockNameDraft,
            isSaving = isSaving,
            onStockNameChange = { stockNameDraft = it },
            onConfirm = {
                val trimmedStockName = stockNameDraft.trim()
                if (trimmedStockName.isBlank()) {
                    Toast.makeText(context, "Enter stock name", Toast.LENGTH_SHORT).show()
                    return@StockNamePrompt
                }
                showStockNamePrompt = false
                createStockViewModel.confirmScannedItems(location, trimmedStockName) { result ->
                    val msg = when {
                        !result.success -> "Save failed."
                        result.duplicateCount > 0 -> "Stock items already exists"
                        else -> "Inventory saved successfully!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (result.success) {
                        stockNameDraft = ""
                        selectedScannedItemId = null
                        selectedRowTopPx = null
                        selectedRowLeftPx = null
                        selectedRowWidthPx = null
                    }
                }
            }
        )
    }

    if (showAddLocationForm) {
        AlertDialog(
            onDismissRequest = {
                addLocationDraft = ""
                showAddLocationForm = false
            },
            title = { Text("Add Location", color = StockAppColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = addLocationDraft,
                    onValueChange = { addLocationDraft = it },
                    singleLine = true,
                    label = { Text("Location") },
                    colors = stockOutlinedTextFieldColors()
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val newLocation = addLocationDraft.trim()
                        createStockViewModel.addLocation(newLocation) { success ->
                            if (success) {
                                location = newLocation
                                addLocationDraft = ""
                                showAddLocationForm = false
                                Toast.makeText(context, "Location added.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Enter a valid location.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = StockAppColors.CardSurface,
                        contentColor = StockAppColors.AccentCyan,
                        disabledContainerColor = StockAppColors.DisabledSurface,
                        disabledContentColor = StockAppColors.DisabledText
                    )
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        addLocationDraft = ""
                        showAddLocationForm = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = StockAppColors.CardSurface,
                        contentColor = StockAppColors.TextSecondary,
                        disabledContainerColor = StockAppColors.DisabledSurface,
                        disabledContentColor = StockAppColors.DisabledText
                    )
                ) {
                    Text("Cancel")
                }
            },
            containerColor = StockAppColors.CardSurface,
            titleContentColor = StockAppColors.TextPrimary,
            textContentColor = StockAppColors.TextSecondary
        )
    }

    if (showEndSessionPrompt) {
        AlertDialog(
            onDismissRequest = { showEndSessionPrompt = false },
            text = {
                Text(
                    text = "Do you want to end this stock take session?",
                    color = StockAppColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            containerColor = StockAppColors.CardSurface,
            textContentColor = StockAppColors.TextSecondary,
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndSessionPrompt = false
                        isScanning = false
                        isPaused = false
                        context.sendBroadcast(Intent("com.android.scanner.DISABLED"))
                    }
                ) {
                    Text(
                        text = "Yes",
                        color = StockAppColors.AccentCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndSessionPrompt = false }
                ) {
                    Text(
                        text = "No",
                        color = StockAppColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    if (showExitSessionPrompt) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StockAppColors.Veil),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StockAppColors.NavyMid),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Do you want to proceed with this action? You will cancel the current stock session",
                        color = StockAppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showExitSessionPrompt = false }
                        ) {
                            Text(
                                text = "No",
                                color = StockAppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                showExitSessionPrompt = false
                                isScanning = false
                                isPaused = false
                                selectedScannedItemId = null
                                selectedRowTopPx = null
                                selectedRowLeftPx = null
                                selectedRowWidthPx = null
                                context.sendBroadcast(Intent("com.android.scanner.DISABLED"))
                                createStockViewModel.clearScannedItems(resetSession = true)
                                onBack()
                            }
                        ) {
                            Text(
                                text = "Proceed",
                                color = StockAppColors.AccentAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun TightActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = StockAppColors.CardSurface,
            disabledContainerColor = StockAppColors.DisabledSurface,
            contentColor = accentColor,
            disabledContentColor = StockAppColors.DisabledText
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) accentColor else StockAppColors.DisabledText
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) accentColor else StockAppColors.DisabledText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeleteRecordOverlayPrompt(
    modifier: Modifier = Modifier,
    onConfirmDelete: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = modifier.width(236.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StockAppColors.CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Do you want to delete the record?",
                color = StockAppColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "No",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StockAppColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onConfirmDelete,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = "Yes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StockAppColors.AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun StockNamePrompt(
    stockName: String,
    isSaving: Boolean,
    onStockNameChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockAppColors.Veil)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .background(StockAppColors.NavyMid, RoundedCornerShape(12.dp))
                .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter stock name",
                color = StockAppColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = stockName,
                onValueChange = onStockNameChange,
                singleLine = true,
                enabled = !isSaving,
                placeholder = { Text("Enter stock name") },
                modifier = Modifier.fillMaxWidth(),
                colors = stockOutlinedTextFieldColors()
            )
            OutlinedButton(
                onClick = onConfirm,
                enabled = stockName.trim().isNotBlank() && !isSaving,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = StockAppColors.CardSurface,
                    contentColor = StockAppColors.AccentCyan,
                    disabledContainerColor = StockAppColors.DisabledSurface,
                    disabledContentColor = StockAppColors.DisabledText
                )
            ) {
                Text("Confirm")
            }
        }
    }
}

@Composable
private fun LocationEntryCard(
    location: String,
    onLocationChange: (String) -> Unit,
    savedLocations: List<SavedLocation>,
    locationsExpanded: Boolean,
    onToggleLocations: () -> Unit,
    onDismissLocations: () -> Unit,
    onOpenAddLocationForm: () -> Unit,
    onSelectLocation: (String) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StockAppColors.FieldSurface)
            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Location",
            color = StockAppColors.AccentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(50))
                .background(if (enabled) StockAppColors.FieldSurface else StockAppColors.DisabledSurface)
                .border(1.dp, StockAppColors.FieldBorder, RoundedCornerShape(50))
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = location,
                onValueChange = onLocationChange,
                enabled = enabled,
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = if (enabled) StockAppColors.TextPrimary else StockAppColors.DisabledText
                ),
                decorationBox = { innerTextField ->
                    if (location.isBlank()) {
                        Text(
                            text = "Enter location",
                            color = StockAppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = onOpenAddLocationForm,
                enabled = enabled,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add location",
                    tint = StockAppColors.AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box {
                IconButton(
                    onClick = onToggleLocations,
                    enabled = enabled,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (locationsExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = "Toggle saved locations",
                        tint = StockAppColors.AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = locationsExpanded,
                    onDismissRequest = onDismissLocations
                ) {
                    if (savedLocations.isEmpty()) {
                        Text(
                            text = "No saved locations",
                            color = StockAppColors.TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.width(180.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            savedLocations.forEach { savedLocation ->
                                SmallLocationOverlayCard(
                                    label = savedLocation.location,
                                    onClick = { onSelectLocation(savedLocation.location) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallLocationOverlayCard(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .height(22.dp)
            .background(StockAppColors.CardSurface)
            .border(1.dp, StockAppColors.CardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = StockAppColors.TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SessionChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(StockAppColors.AccentCyan.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = StockAppColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value.ifBlank { "-" },
            color = StockAppColors.AccentCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusIndicator(statusText: String) {
    val (bgColor, textColor) = when (statusText) {
        "Scanning" -> StockAppColors.AccentCyan.copy(alpha = 0.2f) to StockAppColors.AccentCyan
        "Paused" -> StockAppColors.AccentAmber.copy(alpha = 0.2f) to StockAppColors.AccentAmber
        else -> StockAppColors.AccentCyan.copy(alpha = 0.12f) to StockAppColors.AccentCyan
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(textColor, RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText.uppercase(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun CreateStockTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StockAppColors.NavyMid)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "STOCK TAKE",
            color = StockAppColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = onBack,
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
    }
}

@Composable
private fun HiddenScannerWedgeInputEffect(
    enabled: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    if (!enabled) return

    val latestOnBarcodeScanned by rememberUpdatedState(newValue = onBarcodeScanned)

    AndroidView(
        modifier = Modifier
            .size(1.dp)
            .alpha(0f),
        factory = { context ->
            EditText(context).apply {
                isSingleLine = true
                setShowSoftInputOnFocus(false)
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NONE
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                isCursorVisible = false
                isFocusable = true
                isFocusableInTouchMode = true
                setTextColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                }

                var suppressCallbacks = false
                var commitRunnable: Runnable? = null

                fun commitCurrentScan() {
                    if (suppressCallbacks) return
                    val payload = text?.toString().orEmpty()
                        .replace("\u0000", "")
                        .trim()
                    if (payload.isBlank()) return
                    latestOnBarcodeScanned(payload)
                    suppressCallbacks = true
                    text?.clear()
                    suppressCallbacks = false
                }

                setOnEditorActionListener { _, _, _ ->
                    commitCurrentScan()
                    true
                }

                setOnKeyListener { _, keyCode, keyEvent ->
                    if (
                        keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                        keyEvent.action == android.view.KeyEvent.ACTION_UP
                    ) {
                        commitCurrentScan()
                        true
                    } else {
                        false
                    }
                }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                    override fun afterTextChanged(editable: Editable?) {
                        if (suppressCallbacks) return

                        val rawValue = editable?.toString().orEmpty()
                        val sanitized = rawValue.replace("\u0000", "")

                        val hasDelimiter = sanitized.any { it == '\n' || it == '\r' || it == '\t' }
                        if (hasDelimiter) {
                            sanitized
                                .split('\n', '\r', '\t')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .forEach(latestOnBarcodeScanned)

                            suppressCallbacks = true
                            editable?.clear()
                            suppressCallbacks = false
                            commitRunnable?.let { removeCallbacks(it) }
                            return
                        }

                        val trimmed = sanitized.trim()
                        if (trimmed.length > 4096) {
                            suppressCallbacks = true
                            editable?.clear()
                            suppressCallbacks = false
                            commitRunnable?.let { removeCallbacks(it) }
                            return
                        }

                        val looksLikeJsonPayload =
                            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                                (trimmed.startsWith("(") && trimmed.endsWith(")"))
                        if (looksLikeJsonPayload && trimmed.isNotBlank()) {
                            latestOnBarcodeScanned(trimmed)

                            suppressCallbacks = true
                            editable?.clear()
                            suppressCallbacks = false
                            commitRunnable?.let { removeCallbacks(it) }
                            return
                        }

                        if (sanitized != rawValue) {
                            suppressCallbacks = true
                            editable?.replace(0, editable.length, sanitized)
                            suppressCallbacks = false
                        }

                        if (trimmed.isNotBlank()) {
                            commitRunnable?.let { removeCallbacks(it) }
                            val runnable = Runnable { commitCurrentScan() }
                            commitRunnable = runnable
                            postDelayed(runnable, 120L)
                        }
                    }
                })
            }
        },
        update = { editText ->
            if (!editText.hasFocus()) {
                editText.requestFocus()
            }
        }
    )
}

@Composable
private fun AimerScannerBroadcastEffect(
    enabled: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val latestOnBarcodeScanned by rememberUpdatedState(newValue = onBarcodeScanned)

    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                intent ?: return
                val scannedValue = extractBarcodeFromIntent(intent) ?: return
                latestOnBarcodeScanned(scannedValue)
            }
        }

        val intentFilter = IntentFilter().apply {
            SCAN_BROADCAST_ACTIONS.forEach(::addAction)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}

private fun resolvePreviewColumns(items: List<com.example.stockapp.data.local.StockItem>): List<String> {
    val parsedFieldSets = items
        .map { com.example.stockapp.data.JsonFieldExtractor.extractAllFields(it.variableData) }
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

private fun resolvePreviewValue(fields: Map<String, String>, key: String): String {
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

private fun extractBarcodeFromIntent(intent: Intent): String? {
    val extras = intent.extras ?: return null
    for (key in SCAN_DATA_EXTRA_KEYS) {
        @Suppress("DEPRECATION")
        val value = extras.get(key)
        val stringVal = when (value) {
            is String -> value
            is ByteArray -> value.toString(Charsets.UTF_8)
            else -> null
        }
        if (!stringVal.isNullOrBlank()) return stringVal.trim()
    }

    for (key in extras.keySet()) {
        if (key.contains("scan", true) || key.contains("barcode", true) || key.contains("data", true)) {
            @Suppress("DEPRECATION")
            val value = extras.get(key)
            val stringVal = when (value) {
                is String -> value
                is ByteArray -> value.toString(Charsets.UTF_8)
                else -> null
            }
            if (!stringVal.isNullOrBlank()) return stringVal.trim()
        }
    }
    return null
}
