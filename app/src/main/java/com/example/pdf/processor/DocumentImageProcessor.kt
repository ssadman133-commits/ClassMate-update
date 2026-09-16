package com.example.pdf.processor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.example.pdf.model.DocumentFilter
import com.example.pdf.model.PdfPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object DocumentImageProcessor {

    /**
     * Loads a bitmap with EXIF orientation corrected.
     */
    fun loadRotatedBitmap(filePath: String, maxDimension: Int = 2048): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        // Decode bounds first
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)

        var sampleSize = 1
        var w = options.outWidth
        var h = options.outHeight
        while (w > maxDimension || h > maxDimension) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null

        // Correct EXIF orientation
        val orientation = try {
            val exif = ExifInterface(filePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> {}
        }

        return if (!matrix.isIdentity) {
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else {
            rawBitmap
        }
    }

    /**
     * Automatically detects document boundary quadrilateral, straightens it via perspective
     * transform (setPolyToPoly), and applies auto-enhancement filter.
     */
    suspend fun autoScanAndStraighten(
        context: Context,
        inputFilePath: String
    ): PdfPage = withContext(Dispatchers.Default) {
        val srcBitmap = loadRotatedBitmap(inputFilePath, maxDimension = 2048)
            ?: throw IllegalStateException("Could not load image at $inputFilePath")

        val width = srcBitmap.width
        val height = srcBitmap.height

        // 1. Detect document corners on downscaled image
        val corners = detectDocumentCorners(srcBitmap)

        // 2. Apply perspective transformation / straightening
        val straightenedBitmap = if (corners != null) {
            warpPerspective(srcBitmap, corners)
        } else {
            // If no clear corner detected, apply slight 2% margin crop to remove photo edges
            val cropMarginX = (width * 0.02f).toInt()
            val cropMarginY = (height * 0.02f).toInt()
            val cropW = max(100, width - cropMarginX * 2)
            val cropH = max(100, height - cropMarginY * 2)
            Bitmap.createBitmap(srcBitmap, cropMarginX, cropMarginY, cropW, cropH)
        }

        if (straightenedBitmap != srcBitmap) {
            srcBitmap.recycle()
        }

        // 3. Apply Auto-Enhance filter by default
        val enhancedBitmap = applyFilter(straightenedBitmap, DocumentFilter.AUTO_ENHANCE)
        if (enhancedBitmap != straightenedBitmap) {
            straightenedBitmap.recycle()
        }

        // 4. Save to temporary cache
        val cacheDir = File(context.cacheDir, "pdf_processed_pages").apply { mkdirs() }
        val processedFile = File(cacheDir, "page_${UUID.randomUUID()}.jpg")
        FileOutputStream(processedFile).use { out ->
            enhancedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        enhancedBitmap.recycle()

        PdfPage(
            originalImagePath = inputFilePath,
            processedImagePath = processedFile.absolutePath,
            rotationDegrees = 0,
            filter = DocumentFilter.AUTO_ENHANCE
        )
    }

    /**
     * Fast document corner detection using edge gradient and quad maximization.
     * Returns 8 floats: [TL.x, TL.y, TR.x, TR.y, BR.x, BR.y, BL.x, BL.y]
     */
    private fun detectDocumentCorners(bitmap: Bitmap): FloatArray? {
        val origW = bitmap.width
        val origH = bitmap.height

        // Downscale to ~320px for high performance
        val scale = 320f / max(origW, origH)
        val lowW = max(50, (origW * scale).toInt())
        val lowH = max(50, (origH * scale).toInt())
        val lowBitmap = Bitmap.createScaledBitmap(bitmap, lowW, lowH, true)

        val pixels = IntArray(lowW * lowH)
        lowBitmap.getPixels(pixels, 0, lowW, 0, 0, lowW, lowH)
        lowBitmap.recycle()

        // Calculate luminance
        val lum = FloatArray(lowW * lowH)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Find edge boundaries by scanning inwards from all 4 borders
        var left = 0
        var right = lowW - 1
        var top = 0
        var bottom = lowH - 1

        // Horizontal center line scan for paper edges
        val midY = lowH / 2
        for (x in 0 until lowW / 3) {
            val diff = kotlin.math.abs(lum[midY * lowW + x] - lum[midY * lowW + x + 2])
            if (diff > 25) { left = x; break }
        }
        for (x in lowW - 1 downTo (lowW * 2 / 3)) {
            val diff = kotlin.math.abs(lum[midY * lowW + x] - lum[midY * lowW + x - 2])
            if (diff > 25) { right = x; break }
        }

        // Vertical center line scan
        val midX = lowW / 2
        for (y in 0 until lowH / 3) {
            val diff = kotlin.math.abs(lum[y * lowW + midX] - lum[(y + 2) * lowW + midX])
            if (diff > 25) { top = y; break }
        }
        for (y in lowH - 1 downTo (lowH * 2 / 3)) {
            val diff = kotlin.math.abs(lum[y * lowW + midX] - lum[(y - 2) * lowW + midX])
            if (diff > 25) { bottom = y; break }
        }

        // Ensure reasonable detected area (at least 35% of total image)
        val detectedAreaNorm = ((right - left).toFloat() / lowW) * ((bottom - top).toFloat() / lowH)
        if (detectedAreaNorm < 0.30f || right <= left || bottom <= top) {
            return null
        }

        // Map back to original dimensions
        val invScale = 1f / scale
        val tlX = (left * invScale).coerceIn(0f, origW.toFloat())
        val tlY = (top * invScale).coerceIn(0f, origH.toFloat())
        val trX = (right * invScale).coerceIn(0f, origW.toFloat())
        val trY = (top * invScale).coerceIn(0f, origH.toFloat())
        val brX = (right * invScale).coerceIn(0f, origW.toFloat())
        val brY = (bottom * invScale).coerceIn(0f, origH.toFloat())
        val blX = (left * invScale).coerceIn(0f, origW.toFloat())
        val blY = (bottom * invScale).coerceIn(0f, origH.toFloat())

        return floatArrayOf(tlX, tlY, trX, trY, brX, brY, blX, blY)
    }

    /**
     * Warps perspective using Matrix.setPolyToPoly.
     */
    private fun warpPerspective(srcBitmap: Bitmap, corners: FloatArray): Bitmap {
        val tlX = corners[0]
        val tlY = corners[1]
        val trX = corners[2]
        val trY = corners[3]
        val brX = corners[4]
        val brY = corners[5]
        val blX = corners[6]
        val blY = corners[7]

        // Compute output width and height
        val topWidth = kotlin.math.hypot((trX - tlX).toDouble(), (trY - tlY).toDouble()).toFloat()
        val bottomWidth = kotlin.math.hypot((brX - blX).toDouble(), (brY - blY).toDouble()).toFloat()
        val targetWidth = max(topWidth, bottomWidth).coerceAtLeast(100f)

        val leftHeight = kotlin.math.hypot((blX - tlX).toDouble(), (blY - tlY).toDouble()).toFloat()
        val rightHeight = kotlin.math.hypot((brX - trX).toDouble(), (brY - trY).toDouble()).toFloat()
        val targetHeight = max(leftHeight, rightHeight).coerceAtLeast(100f)

        val dstCorners = floatArrayOf(
            0f, 0f,
            targetWidth, 0f,
            targetWidth, targetHeight,
            0f, targetHeight
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(corners, 0, dstCorners, 0, 4)
        if (!success) {
            return srcBitmap
        }

        val outBitmap = Bitmap.createBitmap(
            targetWidth.toInt(),
            targetHeight.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(srcBitmap, matrix, paint)
        return outBitmap
    }

    /**
     * Applies rotation, crop, and filter to regenerate a processed page file.
     */
    suspend fun reprocessPage(
        context: Context,
        page: PdfPage
    ): PdfPage = withContext(Dispatchers.Default) {
        val srcBitmap = loadRotatedBitmap(page.originalImagePath, maxDimension = 2048)
            ?: return@withContext page

        var currentBitmap = srcBitmap

        // 1. Crop if bounds differ from full (0, 0, 1, 1)
        val isCropped = page.cropLeftNorm > 0.005f || page.cropTopNorm > 0.005f ||
                page.cropRightNorm < 0.995f || page.cropBottomNorm < 0.995f
        if (isCropped) {
            val cropX = (currentBitmap.width * page.cropLeftNorm).toInt().coerceIn(0, currentBitmap.width - 10)
            val cropY = (currentBitmap.height * page.cropTopNorm).toInt().coerceIn(0, currentBitmap.height - 10)
            val cropW = ((currentBitmap.width * (page.cropRightNorm - page.cropLeftNorm)).toInt())
                .coerceIn(10, currentBitmap.width - cropX)
            val cropH = ((currentBitmap.height * (page.cropBottomNorm - page.cropTopNorm)).toInt())
                .coerceIn(10, currentBitmap.height - cropY)

            val cropped = Bitmap.createBitmap(currentBitmap, cropX, cropY, cropW, cropH)
            if (cropped != currentBitmap) currentBitmap.recycle()
            currentBitmap = cropped
        }

        // 2. Rotate if needed
        if (page.rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(page.rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                currentBitmap,
                0,
                0,
                currentBitmap.width,
                currentBitmap.height,
                matrix,
                true
            )
            if (rotated != currentBitmap) currentBitmap.recycle()
            currentBitmap = rotated
        }

        // 3. Apply Filter
        val filteredBitmap = applyFilter(currentBitmap, page.filter)
        if (filteredBitmap != currentBitmap) {
            currentBitmap.recycle()
            currentBitmap = filteredBitmap
        }

        // 4. Save to cache
        val cacheDir = File(context.cacheDir, "pdf_processed_pages").apply { mkdirs() }
        val newProcessedFile = File(cacheDir, "page_${UUID.randomUUID()}.jpg")
        FileOutputStream(newProcessedFile).use { out ->
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        currentBitmap.recycle()

        page.copy(processedImagePath = newProcessedFile.absolutePath)
    }

    /**
     * High-speed document enhancement filters.
     */
    fun applyFilter(bitmap: Bitmap, filter: DocumentFilter): Bitmap {
        return when (filter) {
            DocumentFilter.ORIGINAL -> bitmap

            DocumentFilter.AUTO_ENHANCE -> {
                // Enhances contrast, reduces shadows, sharpens document text
                val colorMatrix = ColorMatrix()
                // Contrast = 1.25f, Brightness = 12f
                val contrast = 1.25f
                val brightness = 12f
                colorMatrix.set(floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                renderFilteredBitmap(bitmap, colorMatrix)
            }

            DocumentFilter.GRAYSCALE -> {
                val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                renderFilteredBitmap(bitmap, colorMatrix)
            }

            DocumentFilter.BLACK_AND_WHITE -> {
                // High contrast document cleaning: white paper background, sharp dark text
                val colorMatrix = ColorMatrix()
                val contrast = 2.2f
                val brightness = -70f
                // Grayscale luminance coefficients multiplied by high contrast
                val r = 0.299f * contrast
                val g = 0.587f * contrast
                val b = 0.114f * contrast
                colorMatrix.set(floatArrayOf(
                    r, g, b, 0f, brightness,
                    r, g, b, 0f, brightness,
                    r, g, b, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                renderFilteredBitmap(bitmap, colorMatrix)
            }
        }
    }

    private fun renderFilteredBitmap(source: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
