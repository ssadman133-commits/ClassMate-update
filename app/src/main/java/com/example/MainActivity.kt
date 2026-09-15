package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.ClassNotesViewModel
import com.example.ui.components.AddEditCourseDialog
import com.example.ui.components.AddEditTopicDialog
import com.example.ui.components.AddNoteStagingDialog
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.ImportSharedNoteDialog
import com.example.ui.screens.AssignmentsScreen
import com.example.ui.screens.CgpaCalculatorScreen
import com.example.ui.screens.ClassRoutineScreen
import com.example.ui.screens.CourseScreen
import com.example.ui.screens.ExamsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.NoteSearchScreen
import com.example.ui.screens.NoteViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TopicNotesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationScheduler
import com.example.util.AppThemeMode

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ClassNotesViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            // Handle incoming deep link or shared notes/images from external apps
            LaunchedEffect(intent) {
                val action = intent?.action
                val type = intent?.type
                val data = intent?.data

                when {
                    // 1. Deep link: classmate://note?... or https://.../note?...
                    action == Intent.ACTION_VIEW && data != null -> {
                        viewModel.importSharedNote(data.toString()) {}
                    }

                    // 2. Shared text or link from WhatsApp, Messenger, etc.
                    action == Intent.ACTION_SEND && type?.startsWith("text/") == true -> {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (!sharedText.isNullOrBlank()) {
                            viewModel.importSharedNote(sharedText) {}
                        }
                    }

                    // 3. Shared Single Image from another app
                    action == Intent.ACTION_SEND && type?.startsWith("image/") == true -> {
                        val imageUri: android.net.Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(Intent.EXTRA_STREAM)
                        }
                        if (imageUri != null) {
                            viewModel.handleIncomingSharedImages(listOf(imageUri))
                        }
                    }

                    // 4. Shared Multiple Images from Gallery/WhatsApp
                    action == Intent.ACTION_SEND_MULTIPLE && type?.startsWith("image/") == true -> {
                        val imageUris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                        }
                        if (!imageUris.isNullOrEmpty()) {
                            viewModel.handleIncomingSharedImages(imageUris)
                        }
                    }
                }
            }

            MyApplicationTheme(darkTheme = isDark) {
                ClassNotesApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ClassNotesApp(
    viewModel: ClassNotesViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val currentCourse by viewModel.currentCourse.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val currentTopic by viewModel.currentTopic.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val routineItems by viewModel.routineItems.collectAsStateWithLifecycle()
    val semesterRecords by viewModel.semesterRecords.collectAsStateWithLifecycle()
    val activeSponsor by viewModel.activeSponsor.collectAsStateWithLifecycle()
    val activeSponsors by viewModel.activeSponsors.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val gradeScale by viewModel.gradeScale.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val assignmentRemindersEnabled by viewModel.assignmentRemindersEnabled.collectAsStateWithLifecycle()
    val examRemindersEnabled by viewModel.examRemindersEnabled.collectAsStateWithLifecycle()

    val isAddCourseDialogOpen by viewModel.isAddCourseDialogOpen.collectAsStateWithLifecycle()
    val editingCourse by viewModel.editingCourse.collectAsStateWithLifecycle()
    val courseToDelete by viewModel.courseToDelete.collectAsStateWithLifecycle()

    val isAddTopicDialogOpen by viewModel.isAddTopicDialogOpen.collectAsStateWithLifecycle()
    val editingTopic by viewModel.editingTopic.collectAsStateWithLifecycle()
    val topicToDelete by viewModel.topicToDelete.collectAsStateWithLifecycle()

    val noteToDelete by viewModel.noteToDelete.collectAsStateWithLifecycle()
    val stagedPhotos by viewModel.stagedPhotos.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showImportDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    // System Back Press handling
    BackHandler(enabled = currentScreen !is AppScreen.Home) {
        viewModel.navigateBack()
    }

    // Snackbar notifications
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Notification channel & runtime permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        NotificationScheduler.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Camera picture take launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraCaptureResult(success)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is AppScreen.Home -> {
                        MainDashboardScreen(
                            courseCount = courses.size,
                            upcomingAssignmentCount = assignments.count { !it.isCompleted },
                            upcomingExamCount = exams.size,
                            activeSponsor = activeSponsor,
                            activeSponsors = activeSponsors,
                            assignments = assignments,
                            exams = exams,
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshDashboardData() },
                            onNavigateToClassNotes = { viewModel.navigateToClassNotes() },
                            onNavigateToCgpa = { viewModel.navigateToCgpa() },
                            onNavigateToAssignments = { viewModel.navigateToAssignments() },
                            onNavigateToExams = { viewModel.navigateToExams() },
                            onNavigateToRoutine = { viewModel.navigateToRoutine() },
                            onNavigateToSettings = { viewModel.navigateToSettings() },
                            onNavigateToSearch = { viewModel.navigateToSearch() },
                            onSponsorClick = { url, sponsorId ->
                                viewModel.recordSponsorClick(sponsorId)
                                try {
                                    val safeUrl = when {
                                        url.isBlank() -> "https://google.com"
                                        url.startsWith("http://") || url.startsWith("https://") -> url
                                        else -> "https://$url"
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onSponsorImpression = { sponsorId ->
                                viewModel.recordSponsorImpression(sponsorId)
                            },
                            onImportSharedNote = { showImportDialog = true }
                        )
                    }

                    is AppScreen.ClassNotesList -> {
                        HomeScreen(
                            courses = courses,
                            onCourseClick = { courseId -> viewModel.navigateToCourse(courseId) },
                            onAddCourseClick = { viewModel.openAddCourseDialog() },
                            onEditCourseClick = { course -> viewModel.openEditCourseDialog(course) },
                            onDeleteCourseClick = { course -> viewModel.requestDeleteCourse(course) },
                            onBack = { viewModel.navigateBack() },
                            onImportSharedNote = { showImportDialog = true },
                            bottomBar = {
                                AppBottomNavigationBar(
                                    currentScreen = screen,
                                    onNavigateToHome = { viewModel.navigateToHome() },
                                    onNavigateToNotes = { /* already here */ },
                                    onNavigateToRoutine = { viewModel.navigateToRoutine() },
                                    onNavigateToSettings = { viewModel.navigateToSettings() }
                                )
                            }
                        )
                    }

                    is AppScreen.CourseDetail -> {
                        CourseScreen(
                            course = currentCourse,
                            topics = topics,
                            onBackClick = { viewModel.navigateBack() },
                            onTopicClick = { topicId -> viewModel.navigateToTopic(topicId, screen.courseId) },
                            onAddTopicClick = { viewModel.openAddTopicDialog() },
                            onEditTopicClick = { topic -> viewModel.openEditTopicDialog(topic) },
                            onDeleteTopicClick = { topic -> viewModel.requestDeleteTopic(topic) },
                            onEditCourseClick = { course -> viewModel.openEditCourseDialog(course) },
                            onDeleteCourseClick = { course -> viewModel.requestDeleteCourse(course) }
                        )
                    }

                    is AppScreen.TopicNotes -> {
                        TopicNotesScreen(
                            course = currentCourse,
                            topic = currentTopic,
                            notes = notes,
                            onBackClick = { viewModel.navigateBack() },
                            onNoteClick = { noteId ->
                                viewModel.navigateToNoteViewer(noteId, screen.topicId, screen.courseId)
                            },
                            onEditTopicClick = { topic -> viewModel.openEditTopicDialog(topic) },
                            onDeleteTopicClick = { topic -> viewModel.requestDeleteTopic(topic) },
                            onRequestCameraCapture = {
                                val uri = viewModel.prepareCameraCapture()
                                if (uri != null) {
                                    takePictureLauncher.launch(uri)
                                }
                            },
                            onGalleryPick = { uris -> viewModel.onGalleryPhotosSelected(uris) },
                            onShareNotes = { selectedNotes ->
                                viewModel.shareNotes(selectedNotes, currentTopic, currentCourse)
                            },
                            onDeleteMultipleNotes = { selectedNotes ->
                                viewModel.deleteMultipleNotes(selectedNotes)
                            }
                        )
                    }

                    is AppScreen.NoteViewer -> {
                        NoteViewerScreen(
                            notes = notes,
                            initialNoteId = screen.initialNoteId,
                            onClose = { viewModel.navigateBack() },
                            onImportanceChange = { noteId, importance ->
                                viewModel.updateNoteImportance(noteId, importance)
                            },
                            onDeleteNote = { note -> viewModel.requestDeleteNote(note) },
                            onUpdateNoteDetails = { noteId, importance, caption ->
                                viewModel.updateNoteDetails(noteId, importance, caption)
                            },
                            onShareNote = { note ->
                                viewModel.shareNote(note, currentTopic, currentCourse)
                            }
                        )
                    }

                    is AppScreen.NoteSearch -> {
                        NoteSearchScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onNoteClick = { noteId, topicId, courseId ->
                                viewModel.navigateToNoteViewer(noteId, topicId, courseId)
                            },
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    is AppScreen.CgpaCalculator -> {
                        CgpaCalculatorScreen(
                            gradeScale = gradeScale,
                            semesterHistory = semesterRecords,
                            onSaveSemester = { name, gpa, credits ->
                                viewModel.saveSemesterRecord(name, gpa, credits)
                            },
                            onDeleteSemester = { viewModel.deleteSemesterRecord(it) },
                            onNavigateToSettings = { viewModel.navigateToSettings() },
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    is AppScreen.Assignments -> {
                        AssignmentsScreen(
                            assignments = assignments,
                            onAddAssignment = { title, course, dueDate, dueTime, desc, reminder ->
                                viewModel.addAssignment(title, course, dueDate, dueTime, desc, reminder)
                            },
                            onUpdateAssignment = { viewModel.updateAssignment(it) },
                            onDeleteAssignment = { viewModel.deleteAssignment(it) },
                            onToggleCompleted = { viewModel.toggleAssignmentCompleted(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    is AppScreen.Exams -> {
                        ExamsScreen(
                            exams = exams,
                            onAddExam = { title, course, examDate, examTime, desc, reminder ->
                                viewModel.addExam(title, course, examDate, examTime, desc, reminder)
                            },
                            onUpdateExam = { viewModel.updateExam(it) },
                            onDeleteExam = { viewModel.deleteExam(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    is AppScreen.ClassRoutine -> {
                        ClassRoutineScreen(
                            routineItems = routineItems,
                            onAddRoutineItem = { dayOfWeek, course, start, end, room ->
                                viewModel.addRoutineItem(dayOfWeek, course, start, end, room)
                            },
                            onUpdateRoutineItem = { viewModel.updateRoutineItem(it) },
                            onDeleteRoutineItem = { viewModel.deleteRoutineItem(it) },
                            onBack = { viewModel.navigateBack() },
                            bottomBar = {
                                AppBottomNavigationBar(
                                    currentScreen = screen,
                                    onNavigateToHome = { viewModel.navigateToHome() },
                                    onNavigateToNotes = { viewModel.navigateToClassNotes() },
                                    onNavigateToRoutine = { /* already here */ },
                                    onNavigateToSettings = { viewModel.navigateToSettings() }
                                )
                            }
                        )
                    }

                    is AppScreen.Settings -> {
                        SettingsScreen(
                            currentThemeMode = themeMode,
                            gradeScale = gradeScale,
                            assignmentRemindersEnabled = assignmentRemindersEnabled,
                            examRemindersEnabled = examRemindersEnabled,
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            onSaveGradeScale = { viewModel.saveGradeScale(it) },
                            onToggleAssignmentReminders = { viewModel.toggleAssignmentReminders(it) },
                            onToggleExamReminders = { viewModel.toggleExamReminders(it) },
                            onBack = { viewModel.navigateBack() },
                            bottomBar = {
                                AppBottomNavigationBar(
                                    currentScreen = screen,
                                    onNavigateToHome = { viewModel.navigateToHome() },
                                    onNavigateToNotes = { viewModel.navigateToClassNotes() },
                                    onNavigateToRoutine = { viewModel.navigateToRoutine() },
                                    onNavigateToSettings = { /* already here */ }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (isAddCourseDialogOpen) {
        AddEditCourseDialog(
            isEditing = false,
            onDismiss = { viewModel.closeAddCourseDialog() },
            onConfirm = { name, code -> viewModel.createCourse(name, code) }
        )
    }

    editingCourse?.let { course ->
        AddEditCourseDialog(
            initialName = course.name,
            initialCode = course.courseCode,
            isEditing = true,
            onDismiss = { viewModel.closeEditCourseDialog() },
            onConfirm = { name, code -> viewModel.updateCourse(name, code) }
        )
    }

    courseToDelete?.let { course ->
        ConfirmDeleteDialog(
            title = "Delete Course?",
            message = "Are you sure you want to delete \"${course.name}\"? All its topics and saved note photos will also be permanently deleted.",
            onDismiss = { viewModel.dismissDeleteCourse() },
            onConfirm = { viewModel.confirmDeleteCourse() }
        )
    }

    if (isAddTopicDialogOpen) {
        AddEditTopicDialog(
            isEditing = false,
            onDismiss = { viewModel.closeAddTopicDialog() },
            onConfirm = { name -> viewModel.createTopic(name) }
        )
    }

    editingTopic?.let { topic ->
        AddEditTopicDialog(
            initialName = topic.name,
            isEditing = true,
            onDismiss = { viewModel.closeEditTopicDialog() },
            onConfirm = { name -> viewModel.updateTopic(name) }
        )
    }

    topicToDelete?.let { topic ->
        ConfirmDeleteDialog(
            title = "Delete Topic?",
            message = "Are you sure you want to delete \"${topic.name}\"? All notes in this topic will also be permanently deleted.",
            onDismiss = { viewModel.dismissDeleteTopic() },
            onConfirm = { viewModel.confirmDeleteTopic() }
        )
    }

    noteToDelete?.let {
        ConfirmDeleteDialog(
            title = "Delete Note?",
            message = "Are you sure you want to delete this note photo? This cannot be undone.",
            onDismiss = { viewModel.dismissDeleteNote() },
            onConfirm = { viewModel.confirmDeleteNote() }
        )
    }

    stagedPhotos?.let { staged ->
        AddNoteStagingDialog(
            imagePaths = staged.imagePaths,
            initialImportance = staged.initialImportance,
            initialCaption = staged.initialCaption ?: "",
            onDismiss = { viewModel.dismissStagedPhotos() },
            onConfirm = { importance, caption -> viewModel.confirmStagedPhotos(importance, caption) }
        )
    }

    if (showImportDialog) {
        ImportSharedNoteDialog(
            onDismiss = { showImportDialog = false },
            onImport = { codeOrText ->
                viewModel.importSharedNote(codeOrText) { success ->
                    if (success) {
                        showImportDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Class Notes: $name", modifier = modifier)
}
