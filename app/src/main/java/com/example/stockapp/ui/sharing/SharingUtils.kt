package com.example.stockapp.ui.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.stockapp.data.local.StockItem
import java.io.File
import java.io.FileOutputStream

fun shareStockDataAsPdf(context: Context, stockItems: List<StockItem>) {
    val pdfFile = generatePdf(context, stockItems)
    if (pdfFile != null) {
        shareFile(context, pdfFile, "application/pdf")
    }
}

private fun generatePdf(context: Context, stockItems: List<StockItem>): File? {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    // Title
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 18f
    canvas.drawText("Recorded stock Item", 20f, 40f, paint)

    // Table Header
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f
    canvas.drawText("Item ID", 20f, 80f, paint)
    canvas.drawText("Description", 140f, 80f, paint)
    canvas.drawText("Qty", 320f, 80f, paint)
    canvas.drawText("Location", 380f, 80f, paint)
    canvas.drawText("SID", 500f, 80f, paint)
    canvas.drawLine(20f, 90f, 575f, 90f, paint)

    // Table Content
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    var yPosition = 110f
    for (item in stockItems) {
        canvas.drawText(item.itemId, 20f, yPosition, paint)
        canvas.drawText(item.description, 140f, yPosition, paint)
        canvas.drawText(item.quantity.toString(), 320f, yPosition, paint)
        canvas.drawText(item.location, 380f, yPosition, paint)
        canvas.drawText(item.stockCode, 500f, yPosition, paint)
        canvas.drawLine(20f, yPosition + 10, 575f, yPosition + 10, paint)
        yPosition += 30f
    }

    document.finishPage(page)

    return try {
        val file = File(context.cacheDir, "stock_data.pdf")
        val fileOutputStream = FileOutputStream(file)
        document.writeTo(fileOutputStream)
        document.close()
        fileOutputStream.close()
        file
    } catch (e: Exception) {
        null
    }
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
