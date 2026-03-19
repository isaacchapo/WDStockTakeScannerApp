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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockapp.StockApplication
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.SavedLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var scannerMessage by remember { mutableStateOf<String?>(null) }
    var isDeviceScannerBusy by remember { mutableStateOf(false) }
    var showSavedLocations by remember { mutableStateOf(false) }
    var showAddLocationForm by remember { mutableStateOf(false) }
    var addLocationDraft by remember { mutableStateOf("") }
    var isTopCardCollapsed by remember { mutableStateOf(false) }
    var selectedScannedItemId by remember { mutableStateOf<String?>(null) }

    val scannedItems by createStockViewModel.scannedItems.collectAsState()
    val currentSid by createStockViewModel.currentSid.collectAsState()
    val savedLocations by createStockViewModel.savedLocations.collectAsState()
    val isSaving by createStockViewModel.isSaving.collectAsState()

    var location by remember { mutableStateOf("") }

    val canConfirm = scannedItems.isNotEmpty() &&
        location.isNotBlank() &&
        !isSaving

    val statusText = when {
        isScanning && isPaused -> "Paused"
        isScanning -> "Scanning"
        else -> "Ready"
    }

    fun applyScanResult(rawValue: String) {
        val result = createStockViewModel.addScannedItem(rawValue)
        scannerMessage = result.message
    }

    val startDeviceScanner: () -> Unit = {
        if (!isPaused && !isDeviceScannerBusy) {
            if (!isScanning) {
                isScanning = true
                isPaused = false
            }

            isDeviceScannerBusy = true
            scannerMessage = null

            // Trigger the hardware aiming laser/scanner via broadcast
            try {
                // Try several common trigger intents for industrial handhelds
                context.sendBroadcast(Intent(ACTION_SCANNER_SOFT_TRIGGER))
                context.sendBroadcast(Intent(ACTION_SCANNER_START))
                context.sendBroadcast(Intent(ACTION_SCANNER_TRIGGER).apply {
                    putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER_VALUE", EXTRA_PARAMETER_VALUE)
                })
                Log.d("Scanner", "Sent hardware scanner trigger broadcasts")
                scannerMessage = "Scanner armed. Pull trigger to scan."
            } catch (e: Exception) {
                Log.e("Scanner", "Failed to send hardware trigger", e)
                scannerMessage = "Could not arm scanner trigger broadcast."
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateStockTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    text = "LOC:${location.ifBlank { "-" }}  UID:$loggedInUser  SID:$currentSid",
                                    color = Color(0xFF4F6787),
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
                                    SessionChip(label = "SID", value = currentSid)
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
                                    tint = Color(0xFF0A4A99)
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
                                    addLocationDraft = location
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
                        .weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val previewColumns = remember(scannedItems) {
                        resolvePreviewColumns(scannedItems)
                    }
                    val selectedScannedItem = scannedItems.firstOrNull { it.id == selectedScannedItemId }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "Temporary Scanned Data",
                                color = Color(0xFF0A4A99),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0A4A99), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("No", modifier = Modifier.weight(0.3f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[0], modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[1], modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[2], modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(previewColumns[3], modifier = Modifier.weight(1f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
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
                                        color = Color(0xFF607D8B),
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
                                    itemsIndexed(scannedItems) { index, item ->
                                        val isSelected = selectedScannedItemId == item.id
                                        val itemFields = remember(item.variableData) {
                                            com.example.stockapp.data.JsonFieldExtractor.extractAllFields(item.variableData)
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFFE3F2FD)
                                                        index % 2 == 0 -> Color(0xFFF8FBFF)
                                                        else -> Color.White
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF90CAF9) else Color(0xFFE2E8F0),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    selectedScannedItemId = if (isSelected) null else item.id
                                                }
                                                .padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Text("${index + 1}", modifier = Modifier.weight(0.3f), fontSize = 11.sp)
                                                Text(resolvePreviewValue(itemFields, previewColumns[0]), modifier = Modifier.weight(1f), fontSize = 11.sp)
                                                Text(resolvePreviewValue(itemFields, previewColumns[1]), modifier = Modifier.weight(1f), fontSize = 11.sp)
                                                Text(resolvePreviewValue(itemFields, previewColumns[2]), modifier = Modifier.weight(1f), fontSize = 11.sp)
                                                Text(resolvePreviewValue(itemFields, previewColumns[3]), modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                            }
                                            Text(
                                                text = "Scanned: ${formatRecordTimestamp(item.dateScanned)}",
                                                color = Color(0xFF607D8B),
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
                                    .background(Color(0xFFEAF2FF))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Total scanned: ${scannedItems.size} QR code(s)",
                                    color = Color(0xFF0A4A99),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (selectedScannedItem != null) {
                            DeleteRecordOverlayPrompt(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 6.dp),
                                onConfirmDelete = {
                                    createStockViewModel.removeScannedItem(selectedScannedItem.id)
                                    selectedScannedItemId = null
                                },
                                onCancel = {
                                    selectedScannedItemId = null
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scanLabel = when {
                        !isScanning -> "Start Scan"
                        isPaused -> "Resume Scan"
                        else -> "Pause Scan"
                    }

                    TightActionButton(
                        text = scanLabel,
                        icon = Icons.Filled.QrCodeScanner,
                        onClick = {
                            isTopCardCollapsed = true
                            showSavedLocations = false
                            when {
                                !isScanning -> {
                                    isScanning = true
                                    isPaused = false
                                    scannerMessage = null
                                    startDeviceScanner()
                                }
                                isPaused -> {
                                    isPaused = false
                                    startDeviceScanner()
                                }
                                else -> {
                                    isPaused = true
                                }
                            }
                        },
                        enabled = !isDeviceScannerBusy,
                        accentColor = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )

                    TightActionButton(
                        text = "Stop Scan",
                        icon = Icons.Filled.Stop,
                        onClick = {
                            isScanning = false
                            isPaused = false
                            scannerMessage = null
                            context.sendBroadcast(Intent("com.android.scanner.DISABLED"))
                        },
                        enabled = isScanning,
                        accentColor = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )

                    TightActionButton(
                        text = if (isSaving) "Saving..." else "Confirm & Save",
                        icon = Icons.Filled.Done,
                        onClick = {
                            if (!canConfirm) return@TightActionButton
                            createStockViewModel.confirmScannedItems(location) { saved ->
                                val msg = if (saved) "Inventory saved successfully!" else "Save failed."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (saved) {
                                    selectedScannedItemId = null
                                }
                            }
                        },
                        enabled = canConfirm,
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }

                scannerMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }

    if (showAddLocationForm) {
        AlertDialog(
            onDismissRequest = {
                showAddLocationForm = false
            },
            title = { Text("Add Location") },
            text = {
                OutlinedTextField(
                    value = addLocationDraft,
                    onValueChange = { addLocationDraft = it },
                    singleLine = true,
                    label = { Text("Location") }
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        createStockViewModel.addLocation(addLocationDraft) { success ->
                            if (success) {
                                location = addLocationDraft.trim()
                                showAddLocationForm = false
                                Toast.makeText(context, "Location added.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Enter a valid location.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddLocationForm = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
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
            containerColor = Color(0xFFF8FBFF),
            disabledContainerColor = Color(0xFFF1F5FA),
            contentColor = accentColor,
            disabledContentColor = Color(0xFF90A4AE)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) accentColor else Color(0xFF90A4AE)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) accentColor else Color(0xFF90A4AE)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFEFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFD8E5F7), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Do you want to delete the record?",
                color = Color(0xFF102A43),
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
                        color = Color(0xFF546E7A)
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
                        color = Color(0xFF0A4A99)
                    )
                }
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
            .background(Color(0xFFF8FBFF))
            .border(1.dp, Color(0xFFD8E5F7), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Location",
            color = Color(0xFF0A4A99),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(50))
                .background(if (enabled) Color.White else Color(0xFFEEF2F7))
                .border(1.dp, Color(0xFFC7D3E3), RoundedCornerShape(50))
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
                    color = if (enabled) Color(0xFF102A43) else Color(0xFF7B8794)
                ),
                decorationBox = { innerTextField ->
                    if (location.isBlank()) {
                        Text(
                            text = "Enter location",
                            color = Color(0xFF9AA5B1),
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
                    tint = Color(0xFF0A4A99),
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
                        tint = Color(0xFF0A4A99),
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
                            color = Color(0xFF7B8794),
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
            .background(Color(0xFFF8FBFF))
            .border(1.dp, Color(0xFFDDE7F2), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = Color(0xFF102A43),
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
            .background(Color(0xFFEAF2FF))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = Color(0xFF4F6787),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value.ifBlank { "-" },
            color = Color(0xFF0A4A99),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusIndicator(statusText: String) {
    val (bgColor, textColor) = when (statusText) {
        "Scanning" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "Paused" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
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
            .background(Color(0xFF1565C0))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "CREATE STOCK",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            onClick = onBack,
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

private fun formatRecordTimestamp(epochMillis: Long): String {
    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
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
