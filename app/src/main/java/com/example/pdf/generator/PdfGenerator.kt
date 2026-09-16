package com.example.pdf.generator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.pdf.model.PdfPage
import com.example.pdf.model.SavedPdfItem
import com.example.pdf.storage.PdfStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfGenerator {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842

    suspend fun createPdfFromPages(
        context: Context,
        pages: List<PdfPage>,
        pdfTitle: String
    ): Result<SavedPdfItem> = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot create PDF with 0 pages."))
        }

        // Clean & sanitize filename
        val cleanName = pdfTitle.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "Document_${System.currentTimeMillis()}" }
        val filename = if (cleanName.endsWith(".pdf", ignoreCase = true)) cleanName else "$cleanName.pdf"

        val pdfDir = PdfStorageManager.getPdfsDirectory(context)
        val targetFile = File(pdfDir, filename)

        // Ensure unique filename if already exists
        var finalFile = targetFile
        var counter = 1
        val baseName = filename.removeSuffix(".pdf")
        while (finalFile.exists()) {
            finalFile = File(pdfDir, "${baseName}_$counter.pdf")
            counter++
        }

        val pdfDocument = PdfDocument()

        try {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

            for ((index, page) in pages.withIndex()) {
                val pageImagePath = page.processedImagePath.ifBlank { page.originalImagePath }
                val imageFile = File(pageImagePath)
                if (!imageFile.exists()) continue

                // Decode bitmap safely
                val decodeOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(pageImagePath, decodeOptions) ?: continue

                val isLandscape = bitmap.width > bitmap.height
                val pageWidth = if (isLandscape) A4_HEIGHT_PT else A4_WIDTH_PT
                val pageHeight = if (isLandscape) A4_WIDTH_PT else A4_HEIGHT_PT

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = pdfPage.canvas

                // Calculate aspect ratio fit with small 16pt margin
                val margin = 16f
                val availableW = pageWidth - (margin * 2)
                val availableH = pageHeight - (margin * 2)

                val scale = minOf(availableW / bitmap.width.toFloat(), availableH / bitmap.height.toFloat())
                val destW = bitmap.width * scale
                val destH = bitmap.height * scale
                val destX = margin + (availableW - destW) / 2f
                val destY = margin + (availableH - destH) / 2f

                val destRect = RectF(destX, destY, destX + destW, destY + destH)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(pdfPage)
                bitmap.recycle()
            }

            FileOutputStream(finalFile).use { out ->
                pdfDocument.writeTo(out)
            }

            val savedItem = SavedPdfItem(
                file = finalFile,
                name = finalFile.name,
                sizeBytes = finalFile.length(),
                lastModified = finalFile.lastModified(),
                pageCount = pages.size
            )
            Result.success(savedItem)
        } catch (e: Exception) {
            finalFile.delete()
            Result.failure(IOException("Failed to generate PDF: ${e.message}", e))
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }
    }
}
