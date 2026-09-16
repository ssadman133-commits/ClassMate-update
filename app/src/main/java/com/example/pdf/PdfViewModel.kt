package com.example.pdf

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdf.generator.PdfGenerator
import com.example.pdf.model.DocumentFilter
import com.example.pdf.model.PdfPage
import com.example.pdf.model.SavedPdfItem
import com.example.pdf.processor.DocumentImageProcessor
import com.example.pdf.storage.PdfStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _savedPdfs = MutableStateFlow<List<SavedPdfItem>>(emptyList())
    val savedPdfs: StateFlow<List<SavedPdfItem>> = _savedPdfs.asStateFlow()

    private val _currentPages = MutableStateFlow<List<PdfPage>>(emptyList())
    val currentPages: StateFlow<List<PdfPage>> = _currentPages.asStateFlow()

    private val _pendingScannedPage = MutableStateFlow<PdfPage?>(null)
    val pendingScannedPage: StateFlow<PdfPage?> = _pendingScannedPage.asStateFlow()

    private val _activeEditingPage = MutableStateFlow<PdfPage?>(null)
    val activeEditingPage: StateFlow<PdfPage?> = _activeEditingPage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingMessage = MutableStateFlow("Processing image...")
    val processingMessage: StateFlow<String> = _processingMessage.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _createdPdfSuccess = MutableStateFlow<SavedPdfItem?>(null)
    val createdPdfSuccess: StateFlow<SavedPdfItem?> = _createdPdfSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pendingCameraTempFile: File? = null

    init {
        refreshSavedPdfs()
    }

    fun refreshSavedPdfs() {
        viewModelScope.launch {
            val list = PdfStorageManager.listSavedPdfs(getApplication())
            _savedPdfs.value = list
        }
    }

    fun prepareCameraScanUri(): Uri? {
        val context = getApplication<Application>()
        val tempDir = File(context.cacheDir, "pdf_camera_temp").apply { mkdirs() }
        val tempFile = File(tempDir, "scan_${System.currentTimeMillis()}.jpg")
        pendingCameraTempFile = tempFile
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (_: Exception) {
            null
        }
    }

    fun onCameraCaptureResult(success: Boolean) {
        val tempFile = pendingCameraTempFile
        if (!success || tempFile == null || !tempFile.exists()) {
            tempFile?.delete()
            pendingCameraTempFile = null
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Detecting document & straightening..."
            try {
                val page = DocumentImageProcessor.autoScanAndStraighten(
                    context = getApplication(),
                    inputFilePath = tempFile.absolutePath
                )
                _pendingScannedPage.value = page
            } catch (e: Exception) {
                _errorMessage.value = "Failed to process photo: ${e.message}"
            } finally {
                _isProcessing.value = false
                pendingCameraTempFile = null
            }
        }
    }

    fun confirmPendingScannedPage() {
        val page = _pendingScannedPage.value ?: return
        _currentPages.value = _currentPages.value + page
        _pendingScannedPage.value = null
    }

    fun discardPendingScannedPage() {
        _pendingScannedPage.value = null
    }

    fun addGalleryImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()

        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Importing selected images..."
            val newPages = mutableListOf<PdfPage>()

            for (uri in uris) {
                try {
                    val localPath = copyUriToCache(context, uri) ?: continue
                    // Auto enhance gallery document
                    val page = DocumentImageProcessor.autoScanAndStraighten(
                        context = context,
                        inputFilePath = localPath
                    )
                    newPages.add(page)
                } catch (_: Exception) {
                    // Fallback to direct page if auto scan fails
                    val localPath = copyUriToCache(context, uri)
                    if (localPath != null) {
                        newPages.add(
                            PdfPage(
                                originalImagePath = localPath,
                                processedImagePath = localPath,
                                filter = DocumentFilter.ORIGINAL
                            )
                        )
                    }
                }
            }

            _currentPages.value = _currentPages.value + newPages
            _isProcessing.value = false
        }
    }

    private suspend fun copyUriToCache(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "pdf_input_images").apply { mkdirs() }
            val destFile = File(cacheDir, "img_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deletePage(pageId: String) {
        _currentPages.value = _currentPages.value.filter { it.id != pageId }
    }

    fun duplicatePage(pageId: String) {
        val list = _currentPages.value.toMutableList()
        val index = list.indexOfFirst { it.id == pageId }
        if (index != -1) {
            val original = list[index]
            val duplicate = original.copy(id = UUID.randomUUID().toString())
            list.add(index + 1, duplicate)
            _currentPages.value = list
        }
    }

    fun rotatePage(pageId: String) {
        val list = _currentPages.value.toMutableList()
        val index = list.indexOfFirst { it.id == pageId }
        if (index != -1) {
            val page = list[index]
            val newRotation = (page.rotationDegrees + 90) % 360
            val updatedPage = page.copy(rotationDegrees = newRotation)
            list[index] = updatedPage
            _currentPages.value = list

            // Reprocess in background
            viewModelScope.launch {
                val reprocessed = DocumentImageProcessor.reprocessPage(getApplication(), updatedPage)
                val currentList = _currentPages.value.toMutableList()
                val currentIndex = currentList.indexOfFirst { it.id == pageId }
                if (currentIndex != -1) {
                    currentList[currentIndex] = reprocessed
                    _currentPages.value = currentList
                }
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val list = _currentPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _currentPages.value = list
        }
    }

    fun openPageDetailEditor(page: PdfPage) {
        _activeEditingPage.value = page
    }

    fun closePageDetailEditor() {
        _activeEditingPage.value = null
    }

    fun saveEditedPage(updatedPage: PdfPage) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Applying adjustments..."
            try {
                val reprocessed = DocumentImageProcessor.reprocessPage(getApplication(), updatedPage)
                val list = _currentPages.value.toMutableList()
                val index = list.indexOfFirst { it.id == updatedPage.id }
                if (index != -1) {
                    list[index] = reprocessed
                    _currentPages.value = list
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update page: ${e.message}"
            } finally {
                _isProcessing.value = false
                _activeEditingPage.value = null
            }
        }
    }

    fun replacePageImage(pageId: String, newUri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Replacing page image..."
            try {
                val localPath = copyUriToCache(context, newUri)
                if (localPath != null) {
                    val newProcessedPage = DocumentImageProcessor.autoScanAndStraighten(context, localPath)
                    val list = _currentPages.value.toMutableList()
                    val index = list.indexOfFirst { it.id == pageId }
                    if (index != -1) {
                        list[index] = newProcessedPage.copy(id = pageId)
                        _currentPages.value = list
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to replace image: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun createPdf(title: String) {
        val pages = _currentPages.value
        if (pages.isEmpty()) {
            _errorMessage.value = "No pages to create PDF"
            return
        }

        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val result = PdfGenerator.createPdfFromPages(getApplication(), pages, title)
                if (result.isSuccess) {
                    val savedItem = result.getOrThrow()
                    _createdPdfSuccess.value = savedItem
                    _currentPages.value = emptyList() // clear session
                    refreshSavedPdfs()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "PDF generation failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "PDF generation failed: ${e.message}"
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun clearPdfSuccess() {
        _createdPdfSuccess.value = null
    }

    fun clearSession() {
        _currentPages.value = emptyList()
        _pendingScannedPage.value = null
        _activeEditingPage.value = null
    }

    fun deleteSavedPdf(item: SavedPdfItem) {
        viewModelScope.launch {
            PdfStorageManager.deletePdf(item.file)
            refreshSavedPdfs()
        }
    }

    fun renameSavedPdf(item: SavedPdfItem, newName: String) {
        viewModelScope.launch {
            val result = PdfStorageManager.renamePdf(item.file, newName)
            if (result.isSuccess) {
                refreshSavedPdfs()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to rename PDF"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
