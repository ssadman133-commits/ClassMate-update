package com.example.pdf.storage

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.pdf.model.SavedPdfItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfStorageManager {

    private const val FOLDER_NAME = "ClassMate_PDFs"

    fun getPdfsDirectory(context: Context): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val target = if (externalDir != null) {
            File(externalDir, FOLDER_NAME)
        } else {
            File(context.filesDir, FOLDER_NAME)
        }
        if (!target.exists()) {
            target.mkdirs()
        }
        return target
    }

    suspend fun listSavedPdfs(context: Context): List<SavedPdfItem> = withContext(Dispatchers.IO) {
        val dir = getPdfsDirectory(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".pdf", ignoreCase = true) }
            ?: emptyArray()

        files.sortedByDescending { it.lastModified() }.map { file ->
            val pageCount = getPdfPageCount(file)
            SavedPdfItem(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                pageCount = pageCount
            )
        }
    }

    private fun getPdfPageCount(file: File): Int {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (_: Exception) {
            1
        }
    }

    fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun deletePdf(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            file.delete()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun renamePdf(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val sanitized = newName.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "Document_${System.currentTimeMillis()}" }
        val finalName = if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"

        val parentDir = file.parentFile ?: return@withContext Result.failure(Exception("Cannot find directory"))
        val targetFile = File(parentDir, finalName)

        if (targetFile.exists() && targetFile.absolutePath != file.absolutePath) {
            return@withContext Result.failure(Exception("A file named '$finalName' already exists"))
        }

        val success = file.renameTo(targetFile)
        if (success) {
            Result.success(targetFile)
        } else {
            Result.failure(Exception("Failed to rename file"))
        }
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "pdf_processed_pages")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }
}
