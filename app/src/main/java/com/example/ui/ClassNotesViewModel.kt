package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Assignment
import com.example.data.CachedSponsor
import com.example.data.ClassNotesRepository
import com.example.data.Course
import com.example.data.CourseWithCounts
import com.example.data.Exam
import com.example.data.Note
import com.example.data.NoteSearchResult
import com.example.data.RoutineItem
import com.example.data.SemesterRecord
import com.example.data.StudyPlan
import com.example.data.StudyPlanTask
import com.example.data.StudyPlanWithTasks
import com.example.data.Topic
import com.example.data.TopicWithCount
import com.example.util.AppThemeMode
import com.example.util.GradeScale
import com.example.util.ImageStorageManager
import com.example.util.NotificationScheduler
import com.example.util.SettingsManager
import com.example.util.SponsorSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface AppScreen {
    data object Home : AppScreen
    data object ClassNotesList : AppScreen
    data class CourseDetail(val courseId: Long) : AppScreen
    data class TopicNotes(val topicId: Long, val courseId: Long) : AppScreen
    data class NoteViewer(val initialNoteId: Long, val topicId: Long, val courseId: Long) : AppScreen
    data object NoteSearch : AppScreen
    data object CgpaCalculator : AppScreen
    data object Assignments : AppScreen
    data object Exams : AppScreen
    data object ClassRoutine : AppScreen
    data object StudyPlanner : AppScreen
    data object Settings : AppScreen
}

data class StagedPhotos(
    val imagePaths: List<String>,
    val topicId: Long,
    val initialImportance: Int = 3,
    val initialCaption: String? = null
)

class ClassNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClassNotesRepository = ClassNotesRepository(
        AppDatabase.getDatabase(application).classNotesDao()
    )
    private val settingsManager = SettingsManager(application)

    // Screen navigation stack
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Persistent Settings
    val themeMode: StateFlow<AppThemeMode> = settingsManager.themeMode
    val gradeScale: StateFlow<GradeScale> = settingsManager.gradeScale
    val assignmentRemindersEnabled: StateFlow<Boolean> = settingsManager.assignmentRemindersEnabled
    val examRemindersEnabled: StateFlow<Boolean> = settingsManager.examRemindersEnabled

    // All courses with topic and note counts
    val courses: StateFlow<List<CourseWithCounts>> = repository.allCoursesWithCounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected Course ID & Topic ID for reactive child queries
    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    val selectedCourseId: StateFlow<Long?> = _selectedCourseId.asStateFlow()

    private val _selectedTopicId = MutableStateFlow<Long?>(null)
    val selectedTopicId: StateFlow<Long?> = _selectedTopicId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCourse: StateFlow<Course?> = _selectedCourseId.flatMapLatest { id ->
        if (id != null) repository.getCourseById(id) else flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val topics: StateFlow<List<TopicWithCount>> = _selectedCourseId.flatMapLatest { id ->
        if (id != null) repository.getTopicsWithCounts(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTopic: StateFlow<Topic?> = _selectedTopicId.flatMapLatest { id ->
        if (id != null) repository.getTopicById(id) else flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _selectedTopicId.flatMapLatest { id ->
        if (id != null) repository.getNotesForTopic(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Assignments
    val assignments: StateFlow<List<Assignment>> = repository.allAssignments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Exams
    val exams: StateFlow<List<Exam>> = repository.allExams
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Class Routine
    val routineItems: StateFlow<List<RoutineItem>> = repository.allRoutineItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Study Plans & Tasks
    val studyPlans: StateFlow<List<StudyPlan>> = repository.allStudyPlans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStudyPlanTasks: StateFlow<List<StudyPlanTask>> = repository.allStudyPlanTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Semester Records (CGPA history)
    val semesterRecords: StateFlow<List<SemesterRecord>> = repository.allSemesterRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Sponsor Banners (Supports multi-sponsor auto-sliding carousel)
    val activeSponsors: StateFlow<List<CachedSponsor>> = repository.activeSponsors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeSponsor: StateFlow<CachedSponsor?> = repository.activeSponsor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Search Query & Results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<NoteSearchResult>> = _searchQuery.flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else repository.searchNotes(q)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog & UI states
    private val _isAddCourseDialogOpen = MutableStateFlow(false)
    val isAddCourseDialogOpen: StateFlow<Boolean> = _isAddCourseDialogOpen.asStateFlow()

    private val _editingCourse = MutableStateFlow<Course?>(null)
    val editingCourse: StateFlow<Course?> = _editingCourse.asStateFlow()

    private val _courseToDelete = MutableStateFlow<Course?>(null)
    val courseToDelete: StateFlow<Course?> = _courseToDelete.asStateFlow()

    private val _isAddTopicDialogOpen = MutableStateFlow(false)
    val isAddTopicDialogOpen: StateFlow<Boolean> = _isAddTopicDialogOpen.asStateFlow()

    private val _editingTopic = MutableStateFlow<Topic?>(null)
    val editingTopic: StateFlow<Topic?> = _editingTopic.asStateFlow()

    private val _topicToDelete = MutableStateFlow<Topic?>(null)
    val topicToDelete: StateFlow<Topic?> = _topicToDelete.asStateFlow()

    private val _noteToDelete = MutableStateFlow<Note?>(null)
    val noteToDelete: StateFlow<Note?> = _noteToDelete.asStateFlow()

    // Add note staging (choosing importance after capturing/selecting photo)
    private val _stagedPhotos = MutableStateFlow<StagedPhotos?>(null)
    val stagedPhotos: StateFlow<StagedPhotos?> = _stagedPhotos.asStateFlow()

    // Transient feedback messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Pending camera capture file
    private var pendingCameraTempFile: File? = null

    init {
        viewModelScope.launch {
            // Sync active promotional sponsor banner in background (if online)
            try {
                SponsorSyncManager.syncActiveSponsor(getApplication(), repository)
            } catch (_: Exception) {
                // Ignore network errors offline
            }
        }
    }

    fun recordSponsorImpression(sponsorId: String) {
        viewModelScope.launch {
            SponsorSyncManager.recordImpression(getApplication(), sponsorId)
        }
    }

    fun recordSponsorClick(sponsorId: String) {
        viewModelScope.launch {
            SponsorSyncManager.recordClick(getApplication(), sponsorId)
        }
    }

    // Pull-to-refresh state and action
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshDashboardData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Instantly sync active promotional sponsors from cloud database
                SponsorSyncManager.syncActiveSponsor(getApplication(), repository)
            } catch (_: Exception) {
                // Keep offline cache resilient
            } finally {
                // Ensure nice feedback duration
                kotlinx.coroutines.delay(600)
                _isRefreshing.value = false
            }
        }
    }

    // Navigation Methods
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun navigateToHome() {
        _selectedTopicId.value = null
        _selectedCourseId.value = null
        _currentScreen.value = AppScreen.Home
    }

    fun navigateToClassNotes() {
        _currentScreen.value = AppScreen.ClassNotesList
    }

    fun navigateToCourse(courseId: Long) {
        _selectedCourseId.value = courseId
        _selectedTopicId.value = null
        _currentScreen.value = AppScreen.CourseDetail(courseId)
    }

    fun navigateToTopic(topicId: Long, courseId: Long) {
        _selectedCourseId.value = courseId
        _selectedTopicId.value = topicId
        _currentScreen.value = AppScreen.TopicNotes(topicId, courseId)
    }

    fun navigateToNoteViewer(noteId: Long, topicId: Long, courseId: Long) {
        _selectedCourseId.value = courseId
        _selectedTopicId.value = topicId
        _currentScreen.value = AppScreen.NoteViewer(noteId, topicId, courseId)
    }

    fun navigateToSearch() {
        _currentScreen.value = AppScreen.NoteSearch
    }

    fun navigateToCgpa() {
        _currentScreen.value = AppScreen.CgpaCalculator
    }

    fun navigateToAssignments() {
        _currentScreen.value = AppScreen.Assignments
    }

    fun navigateToExams() {
        _currentScreen.value = AppScreen.Exams
    }

    fun navigateToRoutine() {
        _currentScreen.value = AppScreen.ClassRoutine
    }

    fun navigateToStudyPlanner() {
        _currentScreen.value = AppScreen.StudyPlanner
    }

    fun navigateToSettings() {
        _currentScreen.value = AppScreen.Settings
    }

    fun navigateBack(): Boolean {
        return when (val screen = _currentScreen.value) {
            is AppScreen.NoteViewer -> {
                navigateToTopic(screen.topicId, screen.courseId)
                true
            }
            is AppScreen.TopicNotes -> {
                navigateToCourse(screen.courseId)
                true
            }
            is AppScreen.CourseDetail -> {
                navigateToClassNotes()
                true
            }
            is AppScreen.ClassNotesList,
            is AppScreen.NoteSearch,
            is AppScreen.CgpaCalculator,
            is AppScreen.Assignments,
            is AppScreen.Exams,
            is AppScreen.ClassRoutine,
            is AppScreen.StudyPlanner,
            is AppScreen.Settings -> {
                navigateToHome()
                true
            }
            AppScreen.Home -> {
                false // Let system handle exit
            }
        }
    }

    // Search
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Settings
    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun saveGradeScale(scale: GradeScale) {
        viewModelScope.launch {
            settingsManager.saveGradeScale(scale)
            _userMessage.value = "Grade scale updated"
        }
    }

    fun toggleAssignmentReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAssignmentRemindersEnabled(enabled)
            if (!enabled) {
                // Cancel all assignment alarms
                assignments.value.forEach {
                    NotificationScheduler.cancelAssignmentReminder(getApplication(), it.id)
                }
            } else {
                // Reschedule upcoming
                assignments.value.filter { !it.isCompleted }.forEach {
                    NotificationScheduler.scheduleAssignmentReminder(getApplication(), it)
                }
            }
        }
    }

    fun toggleExamReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setExamRemindersEnabled(enabled)
            if (!enabled) {
                // Cancel all exam alarms
                exams.value.forEach {
                    NotificationScheduler.cancelExamReminder(getApplication(), it.id)
                }
            } else {
                // Reschedule upcoming
                exams.value.forEach {
                    NotificationScheduler.scheduleExamReminder(getApplication(), it)
                }
            }
        }
    }

    // Course Actions
    fun openAddCourseDialog() {
        _isAddCourseDialogOpen.value = true
    }

    fun closeAddCourseDialog() {
        _isAddCourseDialogOpen.value = false
    }

    fun createCourse(name: String, courseCode: String?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _userMessage.value = "Course name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.insertCourse(trimmed, courseCode)
                _isAddCourseDialogOpen.value = false
                _userMessage.value = "Course created"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create course"
            }
        }
    }

    fun openEditCourseDialog(course: Course) {
        _editingCourse.value = course
    }

    fun closeEditCourseDialog() {
        _editingCourse.value = null
    }

    fun updateCourse(newName: String, newCode: String?) {
        val course = _editingCourse.value ?: return
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _userMessage.value = "Course name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateCourse(course.copy(name = trimmed, courseCode = newCode?.trim()))
                _editingCourse.value = null
                _userMessage.value = "Course updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update course"
            }
        }
    }

    fun requestDeleteCourse(course: Course) {
        _courseToDelete.value = course
    }

    fun dismissDeleteCourse() {
        _courseToDelete.value = null
    }

    fun confirmDeleteCourse() {
        val course = _courseToDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteCourse(course)
                _courseToDelete.value = null
                _userMessage.value = "Course deleted"
                if (_currentScreen.value is AppScreen.CourseDetail) {
                    navigateToClassNotes()
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete course"
            }
        }
    }

    // Topic Actions
    fun openAddTopicDialog() {
        _isAddTopicDialogOpen.value = true
    }

    fun closeAddTopicDialog() {
        _isAddTopicDialogOpen.value = false
    }

    fun createTopic(title: String) {
        val courseId = _selectedCourseId.value ?: return
        val trimmed = title.trim()
        if (trimmed.isBlank()) {
            _userMessage.value = "Topic name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.insertTopic(courseId, trimmed)
                _isAddTopicDialogOpen.value = false
                _userMessage.value = "Topic created"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create topic"
            }
        }
    }

    fun openEditTopicDialog(topic: Topic) {
        _editingTopic.value = topic
    }

    fun closeEditTopicDialog() {
        _editingTopic.value = null
    }

    fun updateTopic(newTitle: String) {
        val topic = _editingTopic.value ?: return
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) {
            _userMessage.value = "Topic name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateTopic(topic.copy(name = trimmed))
                _editingTopic.value = null
                _userMessage.value = "Topic updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update topic"
            }
        }
    }

    fun requestDeleteTopic(topic: Topic) {
        _topicToDelete.value = topic
    }

    fun dismissDeleteTopic() {
        _topicToDelete.value = null
    }

    fun confirmDeleteTopic() {
        val topic = _topicToDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteTopic(topic)
                _topicToDelete.value = null
                _userMessage.value = "Topic deleted"
                if (_currentScreen.value is AppScreen.TopicNotes) {
                    navigateToCourse(topic.courseId)
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete topic"
            }
        }
    }

    // Note Capturing & Creation
    fun prepareCameraCapture(): Uri? {
        val context = getApplication<Application>()
        val (tempFile, uri) = ImageStorageManager.createCameraTempFileUri(context)
        pendingCameraTempFile = tempFile
        return uri
    }

    fun onCameraCaptureResult(success: Boolean) {
        val tempFile = pendingCameraTempFile
        val topicId = _selectedTopicId.value
        if (!success || tempFile == null || !tempFile.exists() || topicId == null) {
            tempFile?.delete()
            pendingCameraTempFile = null
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            val localPath = ImageStorageManager.saveCameraCaptureToLocalStorage(context, tempFile)
            pendingCameraTempFile = null
            if (localPath != null) {
                _stagedPhotos.value = StagedPhotos(
                    imagePaths = listOf(localPath),
                    topicId = topicId,
                    initialImportance = 3
                )
            } else {
                _userMessage.value = "Failed to save photo"
            }
        }
    }

    fun onGalleryPhotosSelected(uris: List<Uri>) {
        val topicId = _selectedTopicId.value ?: return
        if (uris.isEmpty()) return

        viewModelScope.launch {
            val context = getApplication<Application>()
            val savedPaths = mutableListOf<String>()
            for (uri in uris) {
                val path = ImageStorageManager.copyUriToLocalStorage(context, uri)
                if (path != null) {
                    savedPaths.add(path)
                }
            }
            if (savedPaths.isNotEmpty()) {
                _stagedPhotos.value = StagedPhotos(
                    imagePaths = savedPaths,
                    topicId = topicId,
                    initialImportance = 3
                )
            } else {
                _userMessage.value = "Could not load selected photos"
            }
        }
    }

    fun dismissStagedPhotos() {
        val staged = _stagedPhotos.value
        if (staged != null) {
            staged.imagePaths.forEach { path ->
                ImageStorageManager.deleteImageFile(path)
            }
            _stagedPhotos.value = null
        }
    }

    fun confirmStagedPhotos(importance: Int, caption: String? = null) {
        val staged = _stagedPhotos.value ?: return
        viewModelScope.launch {
            try {
                repository.addNotes(staged.topicId, staged.imagePaths, importance, caption)
                _stagedPhotos.value = null
                val count = staged.imagePaths.size
                _userMessage.value = if (count == 1) "Note added" else "$count notes added"
            } catch (e: Exception) {
                _userMessage.value = "Failed to save note"
            }
        }
    }

    // Note Importance, Caption & Deletion
    fun updateNoteImportance(noteId: Long, importance: Int) {
        viewModelScope.launch {
            try {
                repository.updateNoteImportance(noteId, importance)
            } catch (e: Exception) {
                _userMessage.value = "Failed to update importance"
            }
        }
    }

    fun updateNoteDetails(noteId: Long, importance: Int, caption: String?) {
        viewModelScope.launch {
            try {
                repository.updateNoteDetails(noteId, importance, caption)
                _userMessage.value = "Note updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update note details"
            }
        }
    }

    fun requestDeleteNote(note: Note) {
        _noteToDelete.value = note
    }

    fun dismissDeleteNote() {
        _noteToDelete.value = null
    }

    fun confirmDeleteNote() {
        val note = _noteToDelete.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteNote(note)
                _noteToDelete.value = null
                _userMessage.value = "Note deleted"

                val screen = _currentScreen.value
                if (screen is AppScreen.NoteViewer && screen.initialNoteId == note.id) {
                    val remaining = notes.value.filter { it.id != note.id }
                    if (remaining.isNotEmpty()) {
                        navigateToNoteViewer(remaining.first().id, screen.topicId, screen.courseId)
                    } else {
                        navigateToTopic(screen.topicId, screen.courseId)
                    }
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete note"
            }
        }
    }

    // Share & Import Notes
    fun shareNote(note: Note, topic: Topic?, course: Course?) {
        shareNotes(listOf(note), topic, course)
    }

    fun shareNotes(notes: List<Note>, topic: Topic?, course: Course?) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val (shareMessage, imageUris) = com.example.util.NoteSharingManager.createMultipleNotesSharePackage(
                    context = context,
                    notes = notes,
                    topic = topic,
                    course = course
                )
                com.example.util.NoteSharingManager.launchMultipleShareIntent(context, shareMessage, imageUris)
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Could not prepare notes for sharing"
            }
        }
    }

    fun exportNotesAsPdf(notes: List<Note>, topic: Topic?, course: Course?) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            try {
                _userMessage.value = "পিডিএফ তৈরি হচ্ছে... (Generating PDF)"
                val context = getApplication<Application>()
                val pdfUri = com.example.util.NoteSharingManager.exportNotesAsPdf(
                    context = context,
                    notes = notes,
                    topicTitle = topic?.name ?: "Notes",
                    courseName = course?.name ?: "Course"
                )
                if (pdfUri != null) {
                    com.example.util.NoteSharingManager.launchPdfShareIntent(
                        context = context,
                        pdfUri = pdfUri,
                        title = "${course?.name ?: "Class"} - ${topic?.name ?: "Notes"}"
                    )
                } else {
                    _userMessage.value = "Could not generate PDF"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Error creating PDF export"
            }
        }
    }

    fun deleteMultipleNotes(notes: List<Note>) {
        if (notes.isEmpty()) return
        viewModelScope.launch {
            try {
                notes.forEach { note ->
                    repository.deleteNote(note)
                    ImageStorageManager.deleteImageFile(note.imagePath)
                }
                _userMessage.value = "${notes.size} notes deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete notes"
            }
        }
    }

    fun handleIncomingSharedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val savedPaths = mutableListOf<String>()
                uris.forEach { uri ->
                    val path = ImageStorageManager.copyUriToLocalStorage(context, uri)
                    if (path != null) savedPaths.add(path)
                }
                if (savedPaths.isNotEmpty()) {
                    val activeTopicId = _selectedTopicId.value
                    if (activeTopicId != null) {
                        _stagedPhotos.value = StagedPhotos(imagePaths = savedPaths, topicId = activeTopicId)
                        _userMessage.value = "${savedPaths.size}টি ছবি যুক্ত করার জন্য প্রস্তুত!"
                    } else {
                        val activeCourseId = _selectedCourseId.value ?: repository.insertCourse("General Course", "GEN-101")
                        val targetTopic = repository.getTopicByCourseAndName(activeCourseId, "Shared Notes")
                        val topicId = targetTopic?.id ?: repository.insertTopic(activeCourseId, "Shared Notes")
                        _stagedPhotos.value = StagedPhotos(imagePaths = savedPaths, topicId = topicId)
                        navigateToTopic(topicId, activeCourseId)
                        _userMessage.value = "${savedPaths.size}টি ছবি প্রস্তুত! সেভ করতে গুরুত্ব নির্বাচন করুন।"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Could not process shared photos"
            }
        }
    }

    fun importSharedNote(codeOrText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val data = com.example.util.NoteSharingManager.parseShareCode(codeOrText)
                if (data == null) {
                    _userMessage.value = "লিংক বা কোডটি সঠিক নয়! দয়া করে আবার চেষ্টা করুন।"
                    onComplete(false)
                    return@launch
                }

                val context = getApplication<Application>()
                val topicId = com.example.util.NoteSharingManager.importNote(context, repository, data)
                
                // Switch screen directly to newly imported topic
                val course = repository.getCourseByName(data.courseName)
                if (course != null) {
                    navigateToTopic(topicId, course.id)
                }

                _userMessage.value = "'${data.courseName}' কোর্সে '${data.topicName}' যুক্ত হয়েছে!"
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "Failed to import note"
                onComplete(false)
            }
        }
    }

    // Assignment CRUD
    fun addAssignment(title: String, course: String, dueDate: Long, dueTime: String, desc: String?, reminder: Int) {
        viewModelScope.launch {
            try {
                val assignment = Assignment(
                    title = title,
                    courseName = course,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    description = desc,
                    reminderDaysBefore = reminder
                )
                val id = repository.insertAssignment(assignment)
                if (assignmentRemindersEnabled.value && reminder > 0) {
                    NotificationScheduler.scheduleAssignmentReminder(getApplication(), assignment.copy(id = id))
                }
                _userMessage.value = "Assignment saved"
            } catch (e: Exception) {
                _userMessage.value = "Failed to save assignment"
            }
        }
    }

    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch {
            try {
                repository.updateAssignment(assignment)
                if (assignmentRemindersEnabled.value && assignment.reminderDaysBefore > 0 && !assignment.isCompleted) {
                    NotificationScheduler.scheduleAssignmentReminder(getApplication(), assignment)
                } else {
                    NotificationScheduler.cancelAssignmentReminder(getApplication(), assignment.id)
                }
                _userMessage.value = "Assignment updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update assignment"
            }
        }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch {
            try {
                repository.deleteAssignment(assignment)
                NotificationScheduler.cancelAssignmentReminder(getApplication(), assignment.id)
                _userMessage.value = "Assignment deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete assignment"
            }
        }
    }

    fun toggleAssignmentCompleted(assignment: Assignment) {
        viewModelScope.launch {
            try {
                val updated = assignment.copy(isCompleted = !assignment.isCompleted)
                repository.updateAssignment(updated)
                if (updated.isCompleted) {
                    NotificationScheduler.cancelAssignmentReminder(getApplication(), assignment.id)
                } else if (assignmentRemindersEnabled.value && updated.reminderDaysBefore > 0) {
                    NotificationScheduler.scheduleAssignmentReminder(getApplication(), updated)
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to update assignment"
            }
        }
    }

    // Exam CRUD
    fun addExam(title: String, course: String, examDate: Long, examTime: String, desc: String?, reminder: Int) {
        viewModelScope.launch {
            try {
                val exam = Exam(
                    title = title,
                    courseName = course,
                    examDate = examDate,
                    examTime = examTime,
                    description = desc,
                    reminderDaysBefore = reminder
                )
                val id = repository.insertExam(exam)
                if (examRemindersEnabled.value && reminder > 0) {
                    NotificationScheduler.scheduleExamReminder(getApplication(), exam.copy(id = id))
                }
                _userMessage.value = "Exam scheduled"
            } catch (e: Exception) {
                _userMessage.value = "Failed to schedule exam"
            }
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            try {
                repository.updateExam(exam)
                if (examRemindersEnabled.value && exam.reminderDaysBefore > 0) {
                    NotificationScheduler.scheduleExamReminder(getApplication(), exam)
                } else {
                    NotificationScheduler.cancelExamReminder(getApplication(), exam.id)
                }
                _userMessage.value = "Exam updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update exam"
            }
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            try {
                repository.deleteExam(exam)
                NotificationScheduler.cancelExamReminder(getApplication(), exam.id)
                _userMessage.value = "Exam deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete exam"
            }
        }
    }

    // Routine CRUD
    fun addRoutineItem(dayOfWeek: Int, courseName: String, startTime: String, endTime: String, room: String?) {
        viewModelScope.launch {
            try {
                val item = RoutineItem(
                    dayOfWeek = dayOfWeek,
                    courseName = courseName,
                    startTime = startTime,
                    endTime = endTime,
                    roomNumber = room
                )
                repository.insertRoutineItem(item)
                _userMessage.value = "Class added to routine"
            } catch (e: Exception) {
                _userMessage.value = "Failed to add class to routine"
            }
        }
    }

    fun updateRoutineItem(item: RoutineItem) {
        viewModelScope.launch {
            try {
                repository.updateRoutineItem(item)
                _userMessage.value = "Routine updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update routine"
            }
        }
    }

    fun deleteRoutineItem(item: RoutineItem) {
        viewModelScope.launch {
            try {
                repository.deleteRoutineItem(item)
                _userMessage.value = "Class removed from routine"
            } catch (e: Exception) {
                _userMessage.value = "Failed to remove class"
            }
        }
    }

    // CGPA & Semester Record CRUD
    fun saveSemesterRecord(name: String, gpa: Double, credits: Double) {
        viewModelScope.launch {
            try {
                val record = SemesterRecord(
                    semesterName = name,
                    gpa = gpa,
                    totalCredits = credits
                )
                repository.insertSemesterRecord(record)
                _userMessage.value = "Semester result saved"
            } catch (e: Exception) {
                _userMessage.value = "Failed to save semester result"
            }
        }
    }

    fun deleteSemesterRecord(record: SemesterRecord) {
        viewModelScope.launch {
            try {
                repository.deleteSemesterRecord(record)
                _userMessage.value = "Semester record deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete record"
            }
        }
    }

    fun triggerSponsorSync() {
        viewModelScope.launch {
            try {
                SponsorSyncManager.syncActiveSponsor(getApplication(), repository)
                _userMessage.value = "Sponsor banner synced successfully!"
            } catch (e: Exception) {
                _userMessage.value = "Cloud sync checked (offline or no custom URL)"
            }
        }
    }

    // Study Plan CRUD
    fun createStudyPlan(
        title: String,
        subtitle: String = "",
        startDateMillis: Long = System.currentTimeMillis(),
        endDateMillis: Long,
        targetDays: Int,
        colorHex: String = "#8A2BE2",
        tasks: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                repository.insertStudyPlan(
                    title = title,
                    subtitle = subtitle,
                    startDateMillis = startDateMillis,
                    endDateMillis = endDateMillis,
                    targetDays = targetDays,
                    colorHex = colorHex,
                    initialTaskTitles = tasks
                )
                _userMessage.value = "Study plan created!"
            } catch (e: Exception) {
                _userMessage.value = "Failed to create study plan"
            }
        }
    }

    fun updateStudyPlan(plan: StudyPlan) {
        viewModelScope.launch {
            try {
                repository.updateStudyPlan(plan)
                _userMessage.value = "Study plan updated"
            } catch (e: Exception) {
                _userMessage.value = "Failed to update plan"
            }
        }
    }

    fun deleteStudyPlan(plan: StudyPlan) {
        viewModelScope.launch {
            try {
                repository.deleteStudyPlan(plan)
                _userMessage.value = "Study plan deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete plan"
            }
        }
    }

    fun addTaskToPlan(planId: Long, title: String, dayNumber: Int, notes: String = "") {
        viewModelScope.launch {
            try {
                repository.addTaskToPlan(planId, title, dayNumber, notes)
                _userMessage.value = "Task added"
            } catch (e: Exception) {
                _userMessage.value = "Failed to add task"
            }
        }
    }

    fun togglePlanTask(task: StudyPlanTask) {
        viewModelScope.launch {
            try {
                repository.toggleTaskDone(task)
            } catch (e: Exception) {
                _userMessage.value = "Failed to update task"
            }
        }
    }

    fun deletePlanTask(task: StudyPlanTask) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
                _userMessage.value = "Task deleted"
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete task"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
