package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.ClassNotesRepository
import com.example.data.Course
import com.example.data.Note
import com.example.data.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

data class ShareableNoteData(
    val courseName: String,
    val courseCode: String? = null,
    val topicName: String,
    val caption: String? = null,
    val importance: Int = 3,
    val imageBase64: String? = null,
    val noteCount: Int = 1
)

object NoteSharingManager {

    private const val SHARE_CODE_PREFIX = "CMNOTE:"
    const val DEFAULT_APP_DOWNLOAD_LINK = "https://ais-pre-qrm734r2fg5sgncyghbqi4-206919312131.asia-southeast1.run.app"
    private const val DEEP_LINK_BASE = "classmate://note"

    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }

    private fun urlDecode(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }

    /**
     * Builds a clean, portable deep-link URL for one-click note import.
     */
    fun buildDeepLink(
        courseName: String,
        courseCode: String?,
        topicName: String,
        importance: Int,
        caption: String? = null,
        noteCount: Int = 1
    ): String {
        val params = StringBuilder("?c=${urlEncode(courseName)}")
        if (!courseCode.isNullOrBlank()) {
            params.append("&cc=${urlEncode(courseCode)}")
        }
        params.append("&t=${urlEncode(topicName)}")
        params.append("&imp=$importance")
        if (noteCount > 1) {
            params.append("&n=$noteCount")
        }
        if (!caption.isNullOrBlank()) {
            params.append("&cap=${urlEncode(caption.take(80))}")
        }
        return "$DEEP_LINK_BASE$params"
    }

    /**
     * Prepares sharing for multiple notes with real image attachments and a clean viral message.
     */
    suspend fun createMultipleNotesSharePackage(
        context: Context,
        notes: List<Note>,
        topic: Topic?,
        course: Course?
    ): Pair<String, List<Uri>> = withContext(Dispatchers.IO) {
        val courseName = course?.name ?: "General Studies"
        val courseCode = course?.courseCode
        val topicName = topic?.name ?: "Class Notes"
        val firstCaption = notes.firstOrNull()?.textNote
        val noteCount = notes.size.coerceAtLeast(1)

        val avgImportance = if (notes.isNotEmpty()) {
            notes.map { it.importance }.average().toInt().coerceIn(1, 5)
        } else {
            3
        }

        // 1. Prepare FileProvider Uris for all selected notes
        val shareDir = File(context.cacheDir, "shared_notes").apply {
            if (!exists()) mkdirs()
        }

        val imageUris = mutableListOf<Uri>()
        notes.forEachIndexed { index, note ->
            val imageFile = File(note.imagePath)
            if (imageFile.exists()) {
                try {
                    val sharedFile = File(shareDir, "share_${System.currentTimeMillis()}_$index.jpg")
                    imageFile.copyTo(sharedFile, overwrite = true)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        sharedFile
                    )
                    imageUris.add(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Build clean deep link (no giant base64 text)
        val deepLink = buildDeepLink(
            courseName = courseName,
            courseCode = courseCode,
            topicName = topicName,
            importance = avgImportance,
            caption = firstCaption,
            noteCount = noteCount
        )

        // 3. Attractive, compact, professional English share message
        val shareMessage = buildString {
            append("📚 *Course:* $courseName")
            if (!courseCode.isNullOrBlank()) append(" ($courseCode)")
            append("\n📌 *Topic:* $topicName")
            if (noteCount > 1) {
                append("\n📸 *$noteCount class note photos attached*")
            } else if (!firstCaption.isNullOrBlank()) {
                append("\n📝 \"$firstCaption\"")
            }
            append("\n⭐ Priority: ${"★".repeat(avgImportance)}")
            append("\n\n")
            append("Shared via ClassMate — Student Productivity Suite.\n")
            append("📲 Download the app to organize and view all course notes:\n")
            append("👉 $DEFAULT_APP_DOWNLOAD_LINK\n\n")
            append("⚡ *Direct Link to Open & Save in ClassMate:*\n")
            append(deepLink)
        }

        Pair(shareMessage, imageUris)
    }

    /**
     * Backward-compatible single note share package
     */
    suspend fun createNoteSharePackage(
        context: Context,
        note: Note,
        topic: Topic?,
        course: Course?
    ): Pair<String, Uri?> = withContext(Dispatchers.IO) {
        val (message, uris) = createMultipleNotesSharePackage(context, listOf(note), topic, course)
        Pair(message, uris.firstOrNull())
    }

    /**
     * Launches Android Share Intent for multiple images or single image with clean text.
     */
    fun launchMultipleShareIntent(
        context: Context,
        shareText: String,
        imageUris: List<Uri>
    ) {
        val intent = when {
            imageUris.isEmpty() -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            }
            imageUris.size == 1 -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_STREAM, imageUris[0])
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            else -> {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }

        val chooser = Intent.createChooser(intent, "Share Class Notes")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Backward-compatible single share launch
     */
    fun launchShareIntent(
        context: Context,
        shareText: String,
        imageUri: Uri?
    ) {
        launchMultipleShareIntent(
            context = context,
            shareText = shareText,
            imageUris = if (imageUri != null) listOf(imageUri) else emptyList()
        )
    }

    /**
     * Parses a shared note URL, deep link, or message text into ShareableNoteData.
     * Supports:
     * 1. Smart Deep Link URLs (e.g. /note?c=SPL&cc=123&t=Loop&imp=3)
     * 2. Direct Query text
     * 3. Legacy CMNOTE Base64 payload
     * 4. Text messages containing Course and Topic labels
     */
    fun parseShareCode(rawInput: String): ShareableNoteData? {
        try {
            val cleanInput = rawInput.trim()
            if (cleanInput.isBlank()) return null

            // 1. Try parsing Deep Link / Web URL with query parameters
            val urlCandidate = when {
                cleanInput.contains("/note?") -> cleanInput.substringAfter("/note?").substringBefore(" ")
                cleanInput.contains("classmate://note?") -> cleanInput.substringAfter("classmate://note?").substringBefore(" ")
                cleanInput.startsWith("?c=") || cleanInput.contains("?c=") -> cleanInput.substringAfter("?c=").let { "c=$it" }.substringBefore(" ")
                else -> null
            }

            if (urlCandidate != null) {
                val queryPairs = urlCandidate.split("&")
                val params = mutableMapOf<String, String>()
                for (pair in queryPairs) {
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        params[parts[0]] = urlDecode(parts[1])
                    }
                }

                val courseName = params["c"] ?: params["course"] ?: "Imported Course"
                val courseCode = params["cc"] ?: params["courseCode"]
                val topicName = params["t"] ?: params["topic"] ?: "Class Notes"
                val importance = params["imp"]?.toIntOrNull()?.coerceIn(1, 5) ?: 3
                val caption = params["cap"]
                val noteCount = params["n"]?.toIntOrNull() ?: 1

                return ShareableNoteData(
                    courseName = courseName,
                    courseCode = if (courseCode.isNullOrBlank() || courseCode == "null") null else courseCode,
                    topicName = topicName,
                    caption = if (caption.isNullOrBlank() || caption == "null") null else caption,
                    importance = importance,
                    noteCount = noteCount
                )
            }

            // 2. Try parsing plain text message format (Extracting Course and Topic lines)
            if (cleanInput.contains("Topic:") || cleanInput.contains("Course:") || cleanInput.contains("📚")) {
                var detectedCourse: String? = null
                var detectedTopic: String? = null
                var detectedCode: String? = null

                cleanInput.lines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.contains("Course:") || trimmed.startsWith("📚")) {
                        val raw = trimmed.replace("📚", "").replace("Course:", "").replace("*", "").trim()
                        if (raw.contains("(") && raw.contains(")")) {
                            detectedCourse = raw.substringBefore("(").trim()
                            detectedCode = raw.substringAfter("(").substringBefore(")").trim()
                        } else {
                            detectedCourse = raw
                        }
                    } else if (trimmed.contains("Topic:") || trimmed.startsWith("📌")) {
                        detectedTopic = trimmed.replace("📌", "").replace("Topic:", "").replace("*", "").trim()
                    }
                }

                if (!detectedCourse.isNullOrBlank() && !detectedTopic.isNullOrBlank()) {
                    return ShareableNoteData(
                        courseName = detectedCourse ?: "Imported Course",
                        courseCode = detectedCode,
                        topicName = detectedTopic ?: "Class Notes",
                        importance = 3
                    )
                }
            }

            // 3. Fallback: Check Legacy CMNOTE:... Base64 encoding
            if (cleanInput.contains(SHARE_CODE_PREFIX)) {
                val code = cleanInput.substringAfter(SHARE_CODE_PREFIX).trim().lines().firstOrNull()?.trim() ?: ""
                if (code.isNotBlank()) {
                    val decodedBytes = Base64.decode(code, Base64.DEFAULT)
                    val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                    val courseName = json.optString("cName", "Imported Course")
                    val courseCode = json.optString("cCode", null)
                    val topicName = json.optString("tName", "Class Notes")
                    val caption = json.optString("cap", null)
                    val importance = json.optInt("imp", 3).coerceIn(1, 5)
                    val imageBase64 = json.optString("img", null)

                    return ShareableNoteData(
                        courseName = courseName,
                        courseCode = if (courseCode.isNullOrBlank() || courseCode == "null") null else courseCode,
                        topicName = topicName,
                        caption = if (caption.isNullOrBlank() || caption == "null") null else caption,
                        importance = importance,
                        imageBase64 = if (imageBase64.isNullOrBlank()) null else imageBase64
                    )
                }
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Imports a shared note into the Room Database & local storage.
     * Automatically creates course and topic if they don't exist yet!
     */
    suspend fun importNote(
        context: Context,
        repository: ClassNotesRepository,
        data: ShareableNoteData
    ): Long = withContext(Dispatchers.IO) {
        // 1. Find or create Course
        val existingCourse = repository.getCourseByName(data.courseName)
        val courseId = if (existingCourse != null) {
            existingCourse.id
        } else {
            repository.insertCourse(data.courseName, data.courseCode)
        }

        // 2. Find or create Topic
        val existingTopic = repository.getTopicByCourseAndName(courseId, data.topicName)
        val topicId = if (existingTopic != null) {
            existingTopic.id
        } else {
            repository.insertTopic(courseId, data.topicName)
        }

        // 3. Save Image file if base64 was present (legacy)
        var localImagePath: String? = null
        if (!data.imageBase64.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(data.imageBase64, Base64.DEFAULT)
                val dir = ImageStorageManager.getNotesDirectory(context)
                val fileName = "imported_note_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
                val destFile = File(dir, fileName)
                FileOutputStream(destFile).use { out ->
                    out.write(imageBytes)
                }
                localImagePath = destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Save note in Room database if an image was recovered
        if (localImagePath != null) {
            repository.addNotes(
                topicId = topicId,
                imagePaths = listOf(localImagePath),
                importance = data.importance,
                textNote = data.caption
            )
        }

        topicId
    }

    /**
     * Exports a list of notes into a single multi-page PDF document.
     * Returns the Uri of the generated PDF file.
     */
    suspend fun exportNotesAsPdf(
        context: Context,
        notes: List<Note>,
        topicTitle: String,
        courseName: String
    ): Uri? = withContext(Dispatchers.IO) {
        if (notes.isEmpty()) return@withContext null
        try {
            val pdfDir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
            val cleanTitle = topicTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(25)
            val pdfFile = File(pdfDir, "${cleanTitle}_Notes_${System.currentTimeMillis()}.pdf")

            val document = android.graphics.pdf.PdfDocument()
            val pageWidth = 595 // Standard A4 pt width at 72dpi
            val pageHeight = 842 // Standard A4 pt height at 72dpi

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            notes.forEachIndexed { index, note ->
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Header bar
                canvas.drawText("$courseName — $topicTitle", 36f, 40f, titlePaint)
                val captionPreview = if (!note.textNote.isNullOrBlank()) "  •  ${note.textNote}" else ""
                canvas.drawText("Page ${index + 1} of ${notes.size}$captionPreview", 36f, 56f, subtitlePaint)
                canvas.drawLine(36f, 66f, (pageWidth - 36).toFloat(), 66f, subtitlePaint)

                // Draw note image
                val imgFile = File(note.imagePath)
                if (imgFile.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                    if (bitmap != null) {
                        val maxW = (pageWidth - 72).toFloat()
                        val maxH = (pageHeight - 120).toFloat()
                        val ratio = minOf(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())
                        val drawW = bitmap.width * ratio
                        val drawH = bitmap.height * ratio
                        val left = (pageWidth - drawW) / 2f
                        val top = 80f + ((maxH - drawH) / 2f)

                        val destRect = android.graphics.RectF(left, top, left + drawW, top + drawH)
                        canvas.drawBitmap(bitmap, null, destRect, null)
                        bitmap.recycle()
                    }
                }

                document.finishPage(page)
            }

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launches share intent specifically for PDF export.
     */
    fun launchPdfShareIntent(context: Context, pdfUri: Uri, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "ClassMate PDF Notes: $title")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Export / Share PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
