package com.example.pdf.model

import java.io.File
import java.util.UUID

enum class DocumentFilter(val displayName: String) {
    ORIGINAL("Original"),
    AUTO_ENHANCE("Auto Enhance"),
    GRAYSCALE("Grayscale"),
    BLACK_AND_WHITE("B&W Clean")
}

data class PdfPage(
    val id: String = UUID.randomUUID().toString(),
    val originalImagePath: String,
    val processedImagePath: String,
    val rotationDegrees: Int = 0,
    val filter: DocumentFilter = DocumentFilter.AUTO_ENHANCE,
    val cropLeftNorm: Float = 0f,
    val cropTopNorm: Float = 0f,
    val cropRightNorm: Float = 1f,
    val cropBottomNorm: Float = 1f
)

data class SavedPdfItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val pageCount: Int = 1
) {
    val formattedSize: String
        get() {
            return when {
                sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
                sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
                else -> "$sizeBytes B"
            }
        }
}
