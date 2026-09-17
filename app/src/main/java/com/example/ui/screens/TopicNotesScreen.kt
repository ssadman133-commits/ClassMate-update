package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.Course
import com.example.data.Note
import com.example.data.Topic
import com.example.ui.components.CameraPermissionRationaleDialog
import com.example.ui.components.CompactStarRating
import com.example.ui.components.ConfirmDeleteDialog
import com.example.util.DateFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicNotesScreen(
    course: Course?,
    topic: Topic?,
    notes: List<Note>,
    onBackClick: () -> Unit,
    onNoteClick: (noteId: Long) -> Unit,
    onEditTopicClick: (Topic) -> Unit,
    onDeleteTopicClick: (Topic) -> Unit,
    onRequestCameraCapture: () -> Unit,
    onGalleryPick: (List<android.net.Uri>) -> Unit,
    onShareNote: ((Note) -> Unit)? = null,
    onShareNotes: ((List<Note>) -> Unit)? = null,
    onDeleteMultipleNotes: ((List<Note>) -> Unit)? = null,
    onExportPdf: ((List<Note>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTopicMenu by remember { mutableStateOf(false) }
    var showAddNoteBottomSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { mutableStateListOf<Long>() }

    // Back press in selection mode exits selection mode first
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedNoteIds.clear()
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRequestCameraCapture()
        }
    }

    // Photo Picker Launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onGalleryPick(uris)
        }
    }

    fun handleCameraClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onRequestCameraCapture()
        } else {
            showPermissionRationale = true
        }
    }

    if (showPermissionRationale) {
        CameraPermissionRationaleDialog(
            onDismiss = { showPermissionRationale = false },
            onRequestPermission = {
                showPermissionRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    if (showDeleteSelectedDialog) {
        ConfirmDeleteDialog(
            title = "Delete Selected Notes?",
            message = "${selectedNoteIds.size} note photo(s) will be permanently deleted.",
            onDismiss = { showDeleteSelectedDialog = false },
            onConfirm = {
                showDeleteSelectedDialog = false
                val selectedNotes = notes.filter { it.id in selectedNoteIds }
                isSelectionMode = false
                selectedNoteIds.clear()
                onDeleteMultipleNotes?.invoke(selectedNotes)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isSelectionMode) {
                // Contextual Selection TopAppBar
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedNoteIds.size} notes selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedNoteIds.clear()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close selection"
                            )
                        }
                    },
                    actions = {
                        val allSelected = notes.isNotEmpty() && selectedNoteIds.size == notes.size
                        IconButton(
                            onClick = {
                                if (allSelected) {
                                    selectedNoteIds.clear()
                                } else {
                                    selectedNoteIds.clear()
                                    selectedNoteIds.addAll(notes.map { it.id })
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect all" else "Select all"
                            )
                        }

                        if (selectedNoteIds.isNotEmpty()) {
                            if (onExportPdf != null) {
                                IconButton(
                                    onClick = {
                                        val selectedList = notes.filter { selectedNoteIds.contains(it.id) }
                                        onExportPdf(selectedList)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Export selected as PDF"
                                    )
                                }
                            }

                            if (onShareNotes != null) {
                                IconButton(
                                    onClick = {
                                        val selectedList = notes.filter { selectedNoteIds.contains(it.id) }
                                        onShareNotes(selectedList)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share selected notes"
                                    )
                                }
                            }

                            if (onDeleteMultipleNotes != null) {
                                IconButton(
                                    onClick = { showDeleteSelectedDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete selected",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Normal Topic TopAppBar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = topic?.name ?: "Topic",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = course?.name ?: "Course",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("topic_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Topics"
                            )
                        }
                    },
                    actions = {
                        if (notes.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = true
                                    selectedNoteIds.clear()
                                },
                                modifier = Modifier.testTag("select_notes_action")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Select multiple notes"
                                )
                            }
                        }

                        if (topic != null) {
                            IconButton(
                                onClick = { showTopicMenu = true },
                                modifier = Modifier.testTag("topic_actions_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Topic actions"
                                )
                            }
                            DropdownMenu(
                                expanded = showTopicMenu,
                                onDismissRequest = { showTopicMenu = false }
                            ) {
                                if (notes.isNotEmpty()) {
                                    if (onExportPdf != null) {
                                        DropdownMenuItem(
                                            text = { Text("Export & Share as PDF") },
                                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                            onClick = {
                                                showTopicMenu = false
                                                onExportPdf(notes)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Share All Notes") },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                        onClick = {
                                            showTopicMenu = false
                                            if (onShareNotes != null) {
                                                onShareNotes(notes)
                                            } else if (onShareNote != null) {
                                                onShareNote(notes.first())
                                            }
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Edit Topic") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showTopicMenu = false
                                        onEditTopicClick(topic)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Topic", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showTopicMenu = false
                                        onDeleteTopicClick(topic)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val selectedNotes = notes.filter { it.id in selectedNoteIds }
                        if (selectedNotes.isNotEmpty()) {
                            if (onShareNotes != null) {
                                onShareNotes(selectedNotes)
                            } else if (onShareNote != null) {
                                onShareNote(selectedNotes.first())
                            }
                            isSelectionMode = false
                            selectedNoteIds.clear()
                        }
                    },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    text = { Text("Share (${selectedNoteIds.size} ${if (selectedNoteIds.size == 1) "note" else "notes"})") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("share_selected_fab")
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAddNoteBottomSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("+ Add Note") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_note_fab")
                )
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            EmptyNotesView(
                onAddNoteClick = { showAddNoteBottomSheet = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSelectionMode) "Select Photos to Share / Delete" else "Saved Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isSelectionMode) "${selectedNoteIds.size} selected" else "${notes.size} ${if (notes.size == 1) "photo" else "photos"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelectionMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                items(notes, key = { it.id }) { note ->
                    val isSelected = selectedNoteIds.contains(note.id)
                    NoteThumbnailCard(
                        note = note,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) {
                                    selectedNoteIds.remove(note.id)
                                } else {
                                    selectedNoteIds.add(note.id)
                                }
                            } else {
                                onNoteClick(note.id)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedNoteIds.add(note.id)
                            }
                        },
                        onShare = if (onShareNotes != null) {
                            { onShareNotes(listOf(note)) }
                        } else if (onShareNote != null) {
                            { onShareNote(note) }
                        } else null
                    )
                }
            }
        }
    }

    // Add Note Bottom Sheet
    if (showAddNoteBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddNoteBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Add Note Photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Capture board or notebook photos, or import from gallery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddNoteBottomSheet = false
                                handleCameraClick()
                            }
                            .testTag("take_photo_option"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Camera",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Take photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddNoteBottomSheet = false
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("pick_gallery_option"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Gallery",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Pick multiple",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteThumbnailCard(
    note: Note,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("note_item_${note.id}"),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
        ) {
            AsyncImage(
                model = File(note.imagePath),
                contentDescription = "Note photo thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient scrim at bottom for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Top Row: Selection Checkbox / Share Button (Top Start) & Star Rating Badge (Top End)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    // Checkbox badge
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                        border = if (isSelected) null else BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else if (onShare != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share note",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    CompactStarRating(
                        rating = note.importance,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        starSize = 12.dp
                    )
                }
            }

            // Caption and Timestamp on bottom left
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                if (!note.textNote.isNullOrBlank()) {
                    Text(
                        text = note.textNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = DateFormatter.formatShortDate(note.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EmptyNotesView(
    onAddNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No notes yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Take a photo or choose one from your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ExtendedFloatingActionButton(
            onClick = onAddNoteClick,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("+ Add Note") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("empty_add_note_button")
        )
    }
}
