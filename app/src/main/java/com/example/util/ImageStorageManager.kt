package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageManager {

    private const val NOTES_DIR_NAME = "notes_photos"

    fun getNotesDirectory(context: Context): File {
        val dir = File(context.filesDir, NOTES_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies an image from a content Uri (e.g. from Photo Picker / Gallery)
     * into private internal storage so it is permanently accessible offline.
     */
    fun copyUriToLocalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = getNotesDirectory(context)
            val fileName = "note_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(destFile).use { output: FileOutputStream ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a temporary file and its content Uri using FileProvider for the camera capture intent.
     */
    fun createCameraTempFileUri(context: Context): Pair<File, Uri> {
        val cacheDir = File(context.cacheDir, "camera_captures")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val tempFile = File.createTempFile("cam_${System.currentTimeMillis()}", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(tempFile, uri)
    }

    /**
     * Persists the captured camera photo to permanent internal storage.
     */
    fun saveCameraCaptureToLocalStorage(context: Context, tempFile: File): String? {
        return try {
            val dir = getNotesDirectory(context)
            val fileName = "note_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(dir, fileName)

            if (tempFile.exists()) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
                destFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a local image file from internal storage.
     */
    fun deleteImageFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
