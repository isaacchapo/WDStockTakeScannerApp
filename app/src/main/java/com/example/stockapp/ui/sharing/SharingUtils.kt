package com.example.stockapp.ui.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.stockapp.R
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
    val stockName: String,
    val identifierKey: String,
    val sid: String,
    val uid: String,
    val dateScanned: Long,
    val rawData: String,
    val fields: Map<String, String>
)

private data class PdfDocumentGroup(
    val location: String,
    val stockName: String,
    val sections: List<PdfTableSection>
)

private data class PdfTableSection(
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
            stockName = item.stockName,
            identifierKey = item.identifierKey,
            sid = item.sid,
            uid = item.ownerUid,
            dateScanned = item.dateScanned,
            rawData = item.variableData,
            fields = extractQrFields(item.variableData)
        )
    }
    val documentGroups = buildPdfSections(parsedRecords)

    val pageWidth = 595
    val pageHeight = 842
    val margin = 24f
    val headerRowHeight = 28f
    val bodyRowHeight = 24f
    val sectionGap = 10f
    val tableWidth = pageWidth - (margin * 2f)
    val headerLogoMaxWidth = 150f
    val headerLogoMaxHeight = 56f
    val headerTextGap = 10f
    val headerBottomGap = 16f
    val detailsBottomGap = 8f
    val detailsTextHeight = 14f
    val cellHorizontalPadding = 6f
    val cellVerticalPadding = 4f
    val footerLineTopPadding = 8f
    val footerLineBottomPadding = 8f
    val footerTextBottomPadding = 10f
    val footerReservedHeight = footerLineTopPadding + footerLineBottomPadding + 12f + footerTextBottomPadding
    val contentBottomLimit = pageHeight - margin - footerReservedHeight

    val companyNamePaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
        isAntiAlias = true
        color = 0xFF000000.toInt()
    }
    val headerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 9.5f
        isAntiAlias = true
        color = 0xFFFFFFFF.toInt()
    }
    val bodyPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 8.7f
        letterSpacing = 0.01f
        isAntiAlias = true
        color = 0xFF1F2933.toInt()
    }
    val keyCellPaint = Paint(bodyPaint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }
    val footerPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 10f
        isAntiAlias = true
        color = 0xFF455A64.toInt()
    }
    val detailsPaint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textSize = 10f
        isAntiAlias = true
        color = 0xFF263238.toInt()
    }
    val fillPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    val headerLogoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)

    val document = PdfDocument()
    var pageNumber = 0
    lateinit var page: PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = margin

    fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    // Word itself is longer than maxWidth, force break it
                    var remainingWord = word
                    while (remainingWord.isNotEmpty()) {
                        var count = paint.breakText(remainingWord, true, maxWidth, null)
                        if (count <= 0) count = 1
                        lines.add(remainingWord.take(count))
                        remainingWord = remainingWord.drop(count)
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return if (lines.isEmpty()) listOf("") else lines
    }

    fun calculateRowHeight(values: List<String>, columnWidths: List<Float>, paint: Paint): Float {
        var maxHeight = bodyRowHeight
        values.forEachIndexed { index, value ->
            val wrapped = wrapText(value, paint, columnWidths[index] - (cellHorizontalPadding * 2f))
            val height = wrapped.size * (paint.textSize * 1.25f) + (cellVerticalPadding * 2f)
            if (height > maxHeight) maxHeight = height
        }
        return maxHeight
    }

    fun calculateColumnWidths(columns: List<String>, allRows: List<List<String>>, paint: Paint): List<Float> {
        val numCols = columns.size
        if (numCols == 0) return emptyList()
        val maxWidths = FloatArray(numCols) { 0f }

        columns.forEachIndexed { i, col ->
            maxWidths[i] = when {
                isCompactColumn(col) -> paint.measureText(col) + 28f
                else -> paint.measureText(col) + 22f
            }
        }
        allRows.forEach { row ->
            row.forEachIndexed { i, value ->
                if (i < numCols) {
                    val sample = value.replace('\n', ' ').trim().take(42)
                    val measure = paint.measureText(sample) + if (isCompactColumn(columns[i])) 26f else 22f
                    if (measure > maxWidths[i]) maxWidths[i] = measure
                }
            }
        }

        val totalMeasured = maxWidths.sum()
        val finalWidths = maxWidths.map { (it / totalMeasured) * tableWidth }
        
        val minColumnWidth = (tableWidth / numCols) * 0.4f
        val adjustedWidths = finalWidths.map { it.coerceAtLeast(minColumnWidth) }
        val adjustedTotal = adjustedWidths.sum()
        
        return adjustedWidths.map { (it / adjustedTotal) * tableWidth }
    }

    fun drawWrappedCellText(
        text: String,
        x: Float,
        top: Float,
        width: Float,
        height: Float,
        paint: Paint,
        centered: Boolean
    ) {
        val wrapped = wrapText(text, paint, width - (cellHorizontalPadding * 2f))
        val lineHeight = paint.textSize * 1.25f
        val totalTextHeight = wrapped.size * lineHeight
        var currentY = top + ((height - totalTextHeight) / 2f) - paint.ascent()

        wrapped.forEach { line ->
            val drawX = if (centered) {
                x + (width - paint.measureText(line)) / 2f
            } else {
                x + cellHorizontalPadding
            }
            canvas.drawText(line, drawX, currentY, paint)
            currentY += lineHeight
        }
    }

    fun drawHeader(columns: List<String>, columnWidths: List<Float>, top: Float) {
        var x = margin
        columns.forEachIndexed { index, rawColumn ->
            val columnWidth = columnWidths[index]
            fillPaint.color = 0xFF314553.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + headerRowHeight, fillPaint)
            strokePaint.color = 0xFF23313B.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + headerRowHeight, strokePaint)

            val headerLabel = rawColumn.uppercase()
            drawWrappedCellText(
                text = headerLabel,
                x = x,
                top = top,
                width = columnWidth,
                height = headerRowHeight,
                paint = headerPaint,
                centered = true
            )
            x += columnWidth
        }
    }

    fun drawBodyRow(values: List<String>, columnWidths: List<Float>, top: Float, height: Float, rowIndex: Int) {
        var x = margin
        values.forEachIndexed { index, rawValue ->
            val columnWidth = columnWidths[index]
            val isCompact = index < columnWidths.size && isCompactColumnIndex(index, values.size)
            fillPaint.color = when {
                isCompact && rowIndex % 2 == 0 -> 0xFFEFF3F6.toInt()
                isCompact -> 0xFFE6ECEF.toInt()
                rowIndex % 2 == 0 -> 0xFFFFFFFF.toInt()
                else -> 0xFFF6F8FA.toInt()
            }
            canvas.drawRect(x, top, x + columnWidth, top + height, fillPaint)
            strokePaint.color = 0xFFD4DCE2.toInt()
            canvas.drawRect(x, top, x + columnWidth, top + height, strokePaint)

            val textValue = rawValue.trim()
            val textPaint = if (isCompact) keyCellPaint else bodyPaint
            drawWrappedCellText(
                text = textValue,
                x = x,
                top = top,
                width = columnWidth,
                height = height,
                paint = textPaint,
                centered = isCompact
            )
            x += columnWidth
        }
    }

    fun drawDocumentHeader(top: Float): Float {
        var currentY = top
        headerLogoBitmap?.let { bitmap ->
            val widthScale = headerLogoMaxWidth / bitmap.width.toFloat()
            val heightScale = headerLogoMaxHeight / bitmap.height.toFloat()
            val appliedScale = minOf(widthScale, heightScale, 1f)
            val drawWidth = bitmap.width * appliedScale
            val drawHeight = bitmap.height * appliedScale
            val left = margin + (tableWidth - drawWidth) / 2f
            val destination = RectF(left, currentY, left + drawWidth, currentY + drawHeight)
            canvas.drawBitmap(bitmap, null, destination, null)
            currentY += drawHeight + headerTextGap
        }

        val companyName = "METAL FABRICATORS OF ZAMBIA PLC"
        val baseline = currentY - companyNamePaint.ascent()
        val textX = margin + (tableWidth - companyNamePaint.measureText(companyName)) / 2f
        canvas.drawText(companyName, textX, baseline, companyNamePaint)
        return baseline + companyNamePaint.descent() + headerBottomGap
    }

    fun drawFooter() {
        val lineY = pageHeight - margin - footerReservedHeight + footerLineTopPadding
        strokePaint.color = 0xFFB0BEC5.toInt()
        canvas.drawLine(margin, lineY, margin + tableWidth, lineY, strokePaint)

        val pageLabel = "Page $pageNumber"
        val textY = lineY + footerLineBottomPadding - footerPaint.ascent()
        val textX = margin + (tableWidth - footerPaint.measureText(pageLabel)) / 2f
        canvas.drawText(pageLabel, textX, textY, footerPaint)
    }

    fun drawBottomDetails(group: PdfDocumentGroup, top: Float) {
        val details = "Stock Take: ${group.stockName.ifBlank { "-" }}"
        val baseline = top - detailsPaint.ascent()
        canvas.drawText(details, margin, baseline, detailsPaint)
    }

    fun finishCurrentPage() {
        drawFooter()
        document.finishPage(page)
    }

    fun startNewPage() {
        if (pageNumber > 0) {
            finishCurrentPage()
        }
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = drawDocumentHeader(margin)
    }

    documentGroups.forEach { group ->
        startNewPage()

        group.sections.forEach { section ->
            val columnWidths = calculateColumnWidths(section.columns, section.rows, bodyPaint)
            val minimumSectionSpace = headerRowHeight + bodyRowHeight
            if (y + minimumSectionSpace > contentBottomLimit - detailsTextHeight - detailsBottomGap) {
                startNewPage()
            }
            drawHeader(section.columns, columnWidths, y)
            y += headerRowHeight

            section.rows.forEachIndexed { rowIndex, rowValues ->
                val currentRowHeight = calculateRowHeight(rowValues, columnWidths, bodyPaint)
                if (y + currentRowHeight > contentBottomLimit - detailsTextHeight - detailsBottomGap) {
                    startNewPage()
                    drawHeader(section.columns, columnWidths, y)
                    y += headerRowHeight
                }
                drawBodyRow(rowValues, columnWidths, y, currentRowHeight, rowIndex)
                y += currentRowHeight
            }

            y += sectionGap
        }

        if (y + detailsTextHeight > contentBottomLimit) {
            startNewPage()
        }
        drawBottomDetails(group, y)
    }

    val fileName = buildFileName(sid, schemaId, "pdf")

    return try {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            finishCurrentPage()
            document.writeTo(out)
        }
        document.close()
        file
    } catch (e: Exception) {
        runCatching { document.close() }
        null
    }
}

private fun isCompactColumn(columnName: String): Boolean {
    return when (columnName.trim().uppercase(Locale.ROOT)) {
        "SID", "UID", "TIMESTAMP" -> true
        else -> false
    }
}

private fun isCompactColumnIndex(index: Int, totalColumns: Int): Boolean {
    return index == 0 || index == 1 || index == totalColumns - 1
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
        val groupKey = listOf(
            record.location,
            record.uid,
            record.stockName.trim(),
            record.identifierKey
        ).joinToString("|")
        groupedRecords.getOrPut(groupKey) { mutableListOf() }.add(record)
    }

    return groupedRecords.values.map { groupRecords ->
        val qrColumns = linkedMapOf<String, String>()
        groupRecords.forEach { record ->
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

        val orderedQrColumns = qrColumns.values
            .filterNot { key ->
                val normalized = normalizeColumnKey(key)
                normalized == "sid" || normalized == "uid" || normalized == "location"
            }
            .toMutableList()
        if (orderedQrColumns.isEmpty()) {
            orderedQrColumns += "DATA"
        }

        val columns = listOf("SID", "UID", "LOCATION") + orderedQrColumns + "TIMESTAMP"
        val rows = groupRecords
            .sortedByDescending { it.dateScanned }
            .map { record ->
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
                listOf(record.sid, record.uid, record.location) + baseValues + formatPdfTimestamp(record.dateScanned)
            }

        val sections = listOf(
            PdfTableSection(
                columns = columns,
                rows = rows
            )
        )

        PdfDocumentGroup(
            location = groupRecords.firstOrNull()?.location.orEmpty(),
            stockName = groupRecords.firstOrNull()?.stockName.orEmpty(),
            sections = sections
        )
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

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "stock-share", uri)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Stock Data"))
}
