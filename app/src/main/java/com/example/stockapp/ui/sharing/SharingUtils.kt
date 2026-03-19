package com.example.stockapp.ui.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.StockItem
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

suspend fun shareStockSchemaAsStyledPdf(
    context: Context,
    sid: String,
    schemaId: String,
    stockItems: List<StockItem>
): Result<Unit> {
    val pdfFile = withContext(Dispatchers.IO) {
        generateSchemaPdf(context, sid, schemaId, stockItems)
    } ?: return Result.failure(IllegalStateException("Failed to generate PDF file."))

    return withContext(Dispatchers.Main) {
        runCatching { shareFile(context, pdfFile, "application/pdf") }
    }
}

private data class ParsedSchemaRecord(
    val location: String,
    val sid: String,
    val uid: String,
    val fields: Map<String, String>
)

private fun generateSchemaPdf(
    context: Context,
    sid: String,
    schemaId: String,
    stockItems: List<StockItem>
): File? {
    if (stockItems.isEmpty()) return null

    val parsedRecords = stockItems.map { item ->
        ParsedSchemaRecord(
            location = item.location,
            sid = item.sid,
            uid = item.ownerUid,
            fields = extractQrFields(item.variableData)
        )
    }

    val qrColumns = linkedMapOf<String, String>()
    parsedRecords.forEach { record ->
        record.fields.keys.forEach { rawKey ->
            val displayKey = rawKey.trim()
            if (displayKey.isNotBlank()) {
                val normalized = normalizeColumnKey(displayKey)
                if (!qrColumns.containsKey(normalized)) {
                    qrColumns[normalized] = displayKey
                }
            }
        }
    }

    val orderedQrColumns = qrColumns.values.toList().ifEmpty { listOf("DATA") }
    val locationValue = parsedRecords.firstOrNull { it.location.isNotBlank() }?.location.orEmpty()
    val uidValue = parsedRecords.firstOrNull { it.uid.isNotBlank() }?.uid.orEmpty()

    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f
    val titleRowHeight = 30f
    val headerRowHeight = 24f
    val bodyRowHeight = 22f
    val metadataRowHeight = 28f
    val tableWidth = pageWidth - (margin * 2f)
    val columnCount = orderedQrColumns.size.coerceAtLeast(1)
    val columnWidth = tableWidth / columnCount

    val titleText = "ZAMEFA ${locationValue.ifBlank { "-" }}".uppercase()
    val metadataText = buildMetadataText(locationValue, sid, uidValue)

    val titlePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
        isAntiAlias = true
    }
    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10f
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 9.5f
        isAntiAlias = true
    }
    val metadataPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10f
        isAntiAlias = true
    }
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    val fillPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    val document = PdfDocument()
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = margin

    fun drawMergedRow(
        text: String,
        top: Float,
        height: Float,
        textPaint: Paint,
        backgroundColor: Int,
        textColor: Int
    ) {
        fillPaint.color = backgroundColor
        canvas.drawRect(margin, top, margin + tableWidth, top + height, fillPaint)
        strokePaint.color = 0xFFB0BEC5.toInt()
        canvas.drawRect(margin, top, margin + tableWidth, top + height, strokePaint)

        textPaint.color = textColor
        val baseline = top + (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, margin + 8f, baseline, textPaint)
    }

    fun drawHeader(top: Float) {
        var x = margin
        orderedQrColumns.forEach { rawColumn ->
            fillPaint.color = 0xFF0A4A99.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + headerRowHeight, fillPaint)
            strokePaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + headerRowHeight, strokePaint)

            val headerLabel = rawColumn.uppercase()
            val maxChars = ((columnWidth - 10f) / 6f).toInt().coerceAtLeast(1)
            val display = headerLabel.take(maxChars)
            headerPaint.color = 0xFFFFFFFF.toInt()
            val baseline = top + (headerRowHeight / 2f) - ((headerPaint.descent() + headerPaint.ascent()) / 2f)
            canvas.drawText(display, x + 5f, baseline, headerPaint)
            x += columnWidth
        }
    }

    fun drawBodyRow(values: List<String>, top: Float, rowIndex: Int) {
        var x = margin
        values.forEach { rawValue ->
            fillPaint.color = if (rowIndex % 2 == 0) 0xFFF8FBFF.toInt() else 0xFFF2F7FF.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + bodyRowHeight, fillPaint)
            strokePaint.color = 0xFFDCE6F1.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + bodyRowHeight, strokePaint)

            val textValue = rawValue.trim()
            val maxChars = ((columnWidth - 10f) / 5.5f).toInt().coerceAtLeast(1)
            val display = textValue.take(maxChars)
            bodyPaint.color = 0xFF263238.toInt()
            val baseline = top + (bodyRowHeight / 2f) - ((bodyPaint.descent() + bodyPaint.ascent()) / 2f)
            canvas.drawText(display, x + 5f, baseline, bodyPaint)
            x += columnWidth
        }
    }

    fun startNewPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin
        drawMergedRow(
            text = titleText,
            top = y,
            height = titleRowHeight,
            textPaint = titlePaint,
            backgroundColor = 0xFFE8F1FF.toInt(),
            textColor = 0xFF0A4A99.toInt()
        )
        y += titleRowHeight
        drawHeader(y)
        y += headerRowHeight
    }

    drawMergedRow(
        text = titleText,
        top = y,
        height = titleRowHeight,
        textPaint = titlePaint,
        backgroundColor = 0xFFE8F1FF.toInt(),
        textColor = 0xFF0A4A99.toInt()
    )
    y += titleRowHeight
    drawHeader(y)
    y += headerRowHeight

    val dataRows = parsedRecords.map { record ->
        orderedQrColumns.map { columnName ->
            record.fields[columnName]
                ?: record.fields.entries.firstOrNull {
                    normalizeColumnKey(it.key) == normalizeColumnKey(columnName)
                }?.value
                .orEmpty()
        }
    }

    dataRows.forEachIndexed { rowIndex, rowValues ->
        val requiredBottomSpace = metadataRowHeight + 8f + margin
        if (y + bodyRowHeight > pageHeight - requiredBottomSpace) {
            startNewPage()
        }
        drawBodyRow(rowValues, y, rowIndex)
        y += bodyRowHeight
    }

    if (y + metadataRowHeight > pageHeight - margin) {
        startNewPage()
    }

    drawMergedRow(
        text = metadataText,
        top = y,
        height = metadataRowHeight,
        textPaint = metadataPaint,
        backgroundColor = 0xFFE3F2FD.toInt(),
        textColor = 0xFF0D47A1.toInt()
    )

    val fileName = buildFileName(sid, schemaId, "pdf")

    return try {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            document.finishPage(page)
            document.writeTo(out)
        }
        document.close()
        file
    } catch (e: Exception) {
        runCatching { document.close() }
        null
    }
}

private fun extractQrFields(variableData: String): Map<String, String> {
    val parsed = QrDataParser.parseQrData(variableData)
    if (parsed != null && parsed.fields.isNotEmpty()) {
        return parsed.fields
    }

    return runCatching {
        val json = JSONObject(variableData)
        val fields = linkedMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            fields[key] = json.opt(key)?.toString().orEmpty()
        }
        fields
    }.getOrDefault(emptyMap())
}

private fun buildMetadataText(location: String, sid: String, uid: String): String {
    return "LOCATION: ${location.ifBlank { "-" }} | SID: ${sid.ifBlank { "-" }} | UID: ${uid.ifBlank { "-" }}"
        .uppercase()
}

private fun buildFileName(sid: String, schemaId: String, extension: String): String {
    val safeSid = sid.ifBlank { "sid" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val safeSchemaId = schemaId.ifBlank { "schema" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
    return "stock_${safeSid}_${safeSchemaId}_${System.currentTimeMillis()}.$extension"
}

private fun normalizeColumnKey(raw: String): String {
    return raw
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]"), "")
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "com.example.stockapp.fileprovider", file)
    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Stock Data"))
}
