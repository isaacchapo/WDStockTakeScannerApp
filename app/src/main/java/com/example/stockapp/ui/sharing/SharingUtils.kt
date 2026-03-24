package com.example.stockapp.ui.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.stockapp.data.QrDataParser
import com.example.stockapp.data.local.StockItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val dateScanned: Long,
    val rawData: String,
    val fields: Map<String, String>
)

private data class PdfDocumentGroup(
    val location: String,
    val sid: String,
    val uid: String,
    val sections: List<PdfTableSection>
)

private data class PdfTableSection(
    val title: String,
    val columns: List<String>,
    val rows: List<List<String>>
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
            dateScanned = item.dateScanned,
            rawData = item.variableData,
            fields = extractQrFields(item.variableData)
        )
    }
    val exportedAt = System.currentTimeMillis()
    val documentGroups = buildPdfSections(parsedRecords)

    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f
    val titleRowHeight = 30f
    val headerRowHeight = 24f
    val bodyRowHeight = 22f
    val metadataRowHeight = 28f
    val sectionTitleHeight = 22f
    val sectionGap = 10f
    val tableWidth = pageWidth - (margin * 2f)

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
    var pageNumber = 0
    lateinit var page: PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = margin

    fun drawMergedRow(
        text: String,
        top: Float,
        height: Float,
        textPaint: Paint,
        backgroundColor: Int,
        textColor: Int,
        centered: Boolean = false
    ) {
        fillPaint.color = backgroundColor
        canvas.drawRect(margin, top, margin + tableWidth, top + height, fillPaint)
        strokePaint.color = 0xFFB0BEC5.toInt()
        canvas.drawRect(margin, top, margin + tableWidth, top + height, strokePaint)

        textPaint.color = textColor
        val baseline = top + (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        val startX = if (centered) {
            margin + ((tableWidth - textPaint.measureText(text)) / 2f).coerceAtLeast(8f)
        } else {
            margin + 8f
        }
        canvas.drawText(text, startX, baseline, textPaint)
    }

    fun drawHeader(columns: List<String>, top: Float) {
        val columnWidth = tableWidth / columns.size.coerceAtLeast(1)
        var x = margin
        columns.forEach { rawColumn ->
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

    fun drawSectionTitle(text: String, top: Float) {
        drawMergedRow(
            text = text,
            top = top,
            height = sectionTitleHeight,
            textPaint = metadataPaint,
            backgroundColor = 0xFFF3F7FC.toInt(),
            textColor = 0xFF34506B.toInt()
        )
    }

    fun drawBodyRow(values: List<String>, top: Float, rowIndex: Int) {
        val columnWidth = tableWidth / values.size.coerceAtLeast(1)
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

    fun startNewPage(group: PdfDocumentGroup) {
        if (pageNumber > 0) {
            document.finishPage(page)
        }
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin
        drawMergedRow(
            text = buildTitleText(group.location),
            top = y,
            height = titleRowHeight,
            textPaint = titlePaint,
            backgroundColor = 0xFFE8F1FF.toInt(),
            textColor = 0xFF0A4A99.toInt(),
            centered = true
        )
        y += titleRowHeight
    }

    documentGroups.forEach { group ->
        startNewPage(group)

        group.sections.forEach { section ->
            val sectionTitle = section.title
            val minimumSectionSpace = sectionTitleHeight + headerRowHeight + bodyRowHeight
            if (y + minimumSectionSpace > pageHeight - (metadataRowHeight + margin)) {
                startNewPage(group)
            }
            drawSectionTitle(sectionTitle, y)
            y += sectionTitleHeight
            drawHeader(section.columns, y)
            y += headerRowHeight

            section.rows.forEachIndexed { rowIndex, rowValues ->
                if (y + bodyRowHeight > pageHeight - (metadataRowHeight + margin)) {
                    startNewPage(group)
                    drawSectionTitle(sectionTitle, y)
                    y += sectionTitleHeight
                    drawHeader(section.columns, y)
                    y += headerRowHeight
                }
                drawBodyRow(rowValues, y, rowIndex)
                y += bodyRowHeight
            }

            y += sectionGap
        }

        if (y + metadataRowHeight > pageHeight - margin) {
            startNewPage(group)
        }

        drawMergedRow(
            text = buildMetadataText(
                location = group.location,
                sid = group.sid,
                uid = group.uid,
                exportedAt = exportedAt
            ),
            top = y,
            height = metadataRowHeight,
            textPaint = metadataPaint,
            backgroundColor = 0xFFE3F2FD.toInt(),
            textColor = 0xFF0D47A1.toInt()
        )
    }

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

private fun buildPdfSections(records: List<ParsedSchemaRecord>): List<PdfDocumentGroup> {
    val groupedRecords = linkedMapOf<String, MutableList<ParsedSchemaRecord>>()
    records.forEach { record ->
        val groupKey = listOf(record.location, record.sid, record.uid).joinToString("|")
        groupedRecords.getOrPut(groupKey) { mutableListOf() }.add(record)
    }

    return groupedRecords.values.map { groupRecords ->
        val schemaGroups = linkedMapOf<String, MutableList<ParsedSchemaRecord>>()
        groupRecords.forEach { record ->
            val schemaKey = buildPdfSchemaKey(record.fields)
            schemaGroups.getOrPut(schemaKey) { mutableListOf() }.add(record)
        }

        val sections = schemaGroups.values.map { schemaRecords ->
            val qrColumns = linkedMapOf<String, String>()
            schemaRecords.forEach { record ->
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

            val orderedQrColumns = qrColumns.values.toMutableList()
            if (orderedQrColumns.isEmpty()) {
                orderedQrColumns += "DATA"
            }

            val columns = orderedQrColumns + "TIMESTAMP"
            val rows = schemaRecords.map { record ->
                val baseValues = orderedQrColumns.map { columnName ->
                    if (columnName == "DATA") {
                        record.rawData
                    } else {
                        record.fields[columnName]
                            ?: record.fields.entries.firstOrNull {
                                normalizeColumnKey(it.key) == normalizeColumnKey(columnName)
                            }?.value
                            .orEmpty()
                    }
                }
                baseValues + formatPdfTimestamp(record.dateScanned)
            }

            PdfTableSection(
                title = buildSectionTitleText(columns),
                columns = columns,
                rows = rows
            )
        }

        PdfDocumentGroup(
            location = groupRecords.firstOrNull()?.location.orEmpty(),
            sid = groupRecords.firstOrNull()?.sid.orEmpty(),
            uid = groupRecords.firstOrNull()?.uid.orEmpty(),
            sections = sections
        )
    }
}

private fun buildTitleText(location: String): String {
    return "ZAMEFA ${location.ifBlank { "-" }}".uppercase()
}

private fun buildMetadataText(location: String, sid: String, uid: String, exportedAt: Long): String {
    return "LOCATION: ${location.ifBlank { "-" }} | SID: ${sid.ifBlank { "-" }} | UID: ${uid.ifBlank { "-" }} | EXPORTED: ${formatPdfTimestamp(exportedAt)}"
        .uppercase()
}

private fun buildSectionTitleText(columns: List<String>): String {
    val summary = columns
        .filterNot { it.equals("TIMESTAMP", ignoreCase = true) }
        .take(3)
        .joinToString(" / ") { it.uppercase() }

    return if (summary.isBlank()) {
        "TABLE"
    } else {
        "TABLE: $summary"
    }
}

private fun formatPdfTimestamp(epochMillis: Long): String {
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))
    }.getOrElse { "-" }
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

private fun buildPdfSchemaKey(fields: Map<String, String>): String {
    return fields.keys
        .asSequence()
        .map(::normalizeColumnKey)
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .joinToString("|")
        .ifBlank { "data" }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "stock-share", uri)
    }
    val resolveInfos = context.packageManager.queryIntentActivities(
        shareIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    resolveInfos.forEach { info ->
        context.grantUriPermission(
            info.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Stock Data"))
}
