package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Note
import com.example.ui.components.InteractiveStarRating
import com.example.util.DateFormatter
import java.io.File

@Composable
fun NoteViewerScreen(
    notes: List<Note>,
    initialNoteId: Long,
    onClose: () -> Unit,
    onImportanceChange: (noteId: Long, newImportance: Int) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onUpdateNoteDetails: ((noteId: Long, importance: Int, caption: String?) -> Unit)? = null,
    onShareNote: ((Note) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val initialIndex = remember(notes, initialNoteId) {
        val idx = notes.indexOfFirst { it.id == initialNoteId }
        if (idx >= 0) idx else 0
    }

    if (notes.isEmpty()) {
        onClose()
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, notes.lastIndex),
        pageCount = { notes.size }
    )

    var isCurrentZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentZoomed = false
    }

    val currentNote = notes.getOrNull(pagerState.currentPage) ?: notes.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full screen swipeable photo viewer
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isCurrentZoomed
        ) { page ->
            val note = notes[page]
            val isCurrent = page == pagerState.currentPage
            ZoomableImage(
                imagePath = note.imagePath,
                contentDescription = "Full note photo",
                isCurrentPage = isCurrent,
                onZoomStateChanged = { zoomed ->
                    if (isCurrent) {
                        isCurrentZoomed = zoomed
                    }
                }
            )
        }

        // Top Navigation Bar (Scrim + Close + Counter + Delete)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_viewer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close viewer",
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${notes.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onShareNote != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        IconButton(
                            onClick = { onShareNote(currentNote) },
                            modifier = Modifier.testTag("share_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share note with friends",
                                tint = Color.White
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit note",
                            tint = Color.White
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    IconButton(
                        onClick = { onDeleteNote(currentNote) },
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Bottom Details Panel (Caption + Date & Time + Interactive Star Rating)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.85f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Caption if present
                if (!currentNote.textNote.isNullOrBlank()) {
                    Text(
                        text = "\"${currentNote.textNote}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Creation Date & Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateFormatter.formatSingleLineDateTime(currentNote.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Importance Stars Section
                InteractiveStarRating(
                    rating = currentNote.importance,
                    onRatingChanged = { newRating ->
                        onImportanceChange(currentNote.id, newRating)
                    },
                    starSize = 30.dp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = when (currentNote.importance) {
                        1 -> "⭐ Less Important"
                        2 -> "⭐⭐ Fair"
                        3 -> "⭐⭐⭐ Important"
                        4 -> "⭐⭐⭐⭐ Very Important"
                        else -> "⭐⭐⭐⭐⭐ Most Important"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    if (showEditDialog) {
        com.example.ui.components.EditNoteDetailsDialog(
            initialImportance = currentNote.importance,
            initialCaption = currentNote.textNote,
            onDismiss = { showEditDialog = false },
            onConfirm = { newImportance, newCaption ->
                onUpdateNoteDetails?.invoke(currentNote.id, newImportance, newCaption)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ZoomableImage(
    imagePath: String,
    contentDescription: String?,
    isCurrentPage: Boolean,
    onZoomStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when navigating between notes
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
            onZoomStateChanged(false)
        }
    }

    LaunchedEffect(scale) {
        onZoomStateChanged(scale > 1.05f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    val maxOffsetX = (newScale - 1f) * 600f
                    val maxOffsetY = (newScale - 1f) * 900f
                    val newOffset = if (newScale > 1f) {
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        Offset.Zero
                    }
                    scale = newScale
                    offset = newOffset
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )

        // Floating quick reset button when zoomed in
        if (scale > 1.05f) {
            Surface(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 68.dp, end = 16.dp)
            ) {
                Text(
                    text = "🔍 1x Reset",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
