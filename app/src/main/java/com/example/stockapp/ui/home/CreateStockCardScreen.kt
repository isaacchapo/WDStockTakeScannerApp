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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockapp.StockApplication
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

private val SCAN_BROADCAST_ACTIONS = listOf(
    "android.intent.ACTION_DECODE_DATA",
    "com.android.scanner.broadcast",
    "com.android.scanservice.broadcast",
    "com.android.server.scannerservice.broadcast",
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

    val scannedItems by createStockViewModel.scannedItems.collectAsState()
    val lastSid by createStockViewModel.lastSid.collectAsState()
    val isSaving by createStockViewModel.isSaving.collectAsState()

    var location by remember { mutableStateOf("") }
    var stockTakeId by remember { mutableStateOf(loggedInUser) }

    val canConfirm = scannedItems.isNotEmpty() &&
        location.isNotBlank() &&
        stockTakeId.isNotBlank() &&
        !isSaving

    val statusText = when {
        isScanning && isPaused -> "Paused"
        isScanning -> "Scanning"
        else -> "Ready"
    }

    val gmsBarcodeScanner: GmsBarcodeScanner = remember(context) {
        val scannerOptions = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        GmsBarcodeScanning.getClient(context, scannerOptions)
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
            } catch (e: Exception) {
                Log.e("Scanner", "Failed to send hardware trigger", e)
            }

            // Fallback: Start the software scanner if hardware broadcast isn't handled by the device system
            gmsBarcodeScanner
                .startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        createStockViewModel.addScannedItem(rawValue)
                    } else {
                        scannerMessage = "Scanner returned an empty value."
                    }
                }
                .addOnFailureListener { error ->
                    scannerMessage = error.localizedMessage ?: "Device scan failed."
                }
                .addOnCompleteListener {
                    isDeviceScannerBusy = false
                }
        }
    }

    AimerScannerBroadcastEffect(
        enabled = isScanning && !isPaused,
        onBarcodeScanned = { scannedPayload ->
            Log.d("AimerScanner", "Received broadcast: $scannedPayload")
            createStockViewModel.addScannedItem(scannedPayload)
            scannerMessage = null
        }
    )

    HiddenScannerWedgeInputEffect(
        enabled = isScanning && !isPaused,
        onBarcodeScanned = { scannedPayload ->
            Log.d("AimerScanner", "Received wedge input: $scannedPayload")
            createStockViewModel.addScannedItem(scannedPayload)
            scannerMessage = null
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusIndicator(statusText = statusText)

                            Button(
                                onClick = {
                                    if (!isPaused && !isDeviceScannerBusy) {
                                        startDeviceScanner()
                                    }
                                },
                                modifier = Modifier.height(34.dp),
                                enabled = !isPaused && !isDeviceScannerBusy,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF455A64),
                                    disabledContainerColor = Color(0xFFB0BEC5),
                                    contentColor = Color.White
                                ),
                                contentPadding = paddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isDeviceScannerBusy) "..." else "Scan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        InputRow(
                            label = "Location",
                            value = location,
                            onValueChange = {
                                location = it
                                createStockViewModel.updateSidForLocation(it)
                            },
                            enabled = !isScanning,
                            placeholder = "Enter Location"
                        )
                        InputRow(
                            label = "UID",
                            value = stockTakeId,
                            onValueChange = { stockTakeId = it },
                            enabled = false
                        )
                        InputRow(
                            label = "SID",
                            value = lastSid,
                            onValueChange = { },
                            enabled = false
                        )

                        scannerMessage?.let { message ->
                            Text(
                                text = message,
                                color = Color(0xFFD32F2F),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isScanning) {
                                isScanning = true
                                isPaused = false
                                scannerMessage = null
                                // Optional: automatically trigger laser on "START SCAN" if desired
                                // startDeviceScanner()
                            } else {
                                isPaused = !isPaused
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                !isScanning -> Color(0xFF1565C0)
                                isPaused -> Color(0xFF2E7D32)
                                else -> Color(0xFFE65100)
                            }
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = when {
                                !isScanning -> "START SCAN"
                                isPaused -> "RESUME"
                                else -> "PAUSE"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = {
                            isScanning = false
                            isPaused = false
                            scannerMessage = null
                            
                            // Stop the laser/scanner hardware if possible
                            context.sendBroadcast(Intent("com.android.scanner.DISABLED"))
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC62828),
                            disabledContainerColor = Color(0xFFCFD8DC)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("STOP", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scanned Inventory",
                                color = Color(0xFF1565C0),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = {
                                    if (!canConfirm) return@Button
                                    createStockViewModel.confirmScannedItems(location, stockTakeId) { saved ->
                                        val msg = if (saved) "Inventory saved successfully!" else "Save failed."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canConfirm) Color(0xFF2E7D32) else Color(0xFFB0BEC5),
                                    contentColor = Color.White
                                ),
                                contentPadding = paddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(if (isSaving) "..." else "Confirm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        val tableShape = RoundedCornerShape(8.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(tableShape)
                                .border(1.dp, Color(0xFFECEFF1), tableShape)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1565C0))
                                    .padding(8.dp)
                            ) {
                                Text("No", modifier = Modifier.weight(0.25f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Item ID", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Description", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "Qty",
                                    modifier = Modifier.weight(0.35f),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    fontSize = 12.sp
                                )
                            }

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(scannedItems) { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (index % 2 == 0) Color.White else Color(0xFFF8FAFD))
                                            .padding(horizontal = 8.dp, vertical = 10.dp)
                                    ) {
                                        Text("${index + 1}", modifier = Modifier.weight(0.25f), fontSize = 12.sp)
                                        Text(item.itemId, modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(item.description, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                        Text("${item.quantity}", modifier = Modifier.weight(0.35f), textAlign = TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun paddingValues(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp) = 
    androidx.compose.foundation.layout.PaddingValues(horizontal = horizontal, vertical = vertical)

@Composable
private fun StatusIndicator(statusText: String) {
    val (bgColor, textColor) = when (statusText) {
        "Scanning" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "Paused" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
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
fun InputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFF546E7A),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(
                    if (enabled) Color(0xFFF5F8FB) else Color(0xFFEEEEEE),
                    RoundedCornerShape(6.dp)
                )
                .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            singleLine = true,
            enabled = enabled,
            textStyle = TextStyle(fontSize = 15.sp, color = if (enabled) Color.Black else Color(0xFF757575)),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, color = Color(0xFF9E9E9E), fontSize = 15.sp)
                }
                innerTextField()
            }
        )
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
                            return
                        }

                        val trimmed = sanitized.trim()
                        if (trimmed.length > 4096) {
                            suppressCallbacks = true
                            editable?.clear()
                            suppressCallbacks = false
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
                            return
                        }

                        if (sanitized != rawValue) {
                            suppressCallbacks = true
                            editable?.replace(0, editable.length, sanitized)
                            suppressCallbacks = false
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
