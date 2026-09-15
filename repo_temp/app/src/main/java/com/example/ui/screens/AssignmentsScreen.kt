package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.Assignment
import com.example.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    assignments: List<Assignment>,
    onAddAssignment: (title: String, course: String, dueDate: Long, dueTime: String, desc: String?, reminder: Int) -> Unit,
    onUpdateAssignment: (Assignment) -> Unit,
    onDeleteAssignment: (Assignment) -> Unit,
    onToggleCompleted: (Assignment) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var filterTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Upcoming, 2 = Completed

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val filteredAssignments = remember(assignments, filterTab) {
        when (filterTab) {
            1 -> assignments.filter { !it.isCompleted }
            2 -> assignments.filter { it.isCompleted }
            else -> assignments
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Assignments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("assignments_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    checkNotificationPermission()
                    editingAssignment = null
                    showAddEditDialog = true
                },
                modifier = Modifier.testTag("add_assignment_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Assignment")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterTab == 0,
                    onClick = { filterTab = 0 },
                    label = { Text("All (${assignments.size})") },
                    modifier = Modifier.testTag("filter_all_assignments")
                )
                FilterChip(
                    selected = filterTab == 1,
                    onClick = { filterTab = 1 },
                    label = { Text("Upcoming (${assignments.count { !it.isCompleted }})") },
                    modifier = Modifier.testTag("filter_upcoming_assignments")
                )
                FilterChip(
                    selected = filterTab == 2,
                    onClick = { filterTab = 2 },
                    label = { Text("Completed (${assignments.count { it.isCompleted }})") },
                    modifier = Modifier.testTag("filter_completed_assignments")
                )
            }

            if (filteredAssignments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (filterTab == 2) "No Completed Assignments" else "No Assignments Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (filterTab == 2) "Mark assignments as finished to see them here." else "Tap the '+' button to track course assignments with due dates and notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAssignments, key = { it.id }) { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onToggleCompleted = { onToggleCompleted(assignment) },
                            onEdit = {
                                editingAssignment = assignment
                                showAddEditDialog = true
                            },
                            onDelete = { onDeleteAssignment(assignment) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditAssignmentDialog(
            existing = editingAssignment,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, course, dueDate, dueTime, desc, reminder ->
                if (editingAssignment != null) {
                    onUpdateAssignment(
                        editingAssignment!!.copy(
                            title = title,
                            courseName = course,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            description = desc,
                            reminderDaysBefore = reminder
                        )
                    )
                } else {
                    onAddAssignment(title, course, dueDate, dueTime, desc, reminder)
                }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assignment_card_${assignment.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (assignment.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(28.dp).testTag("toggle_assignment_completed_${assignment.id}")
            ) {
                Icon(
                    imageVector = if (assignment.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (assignment.isCompleted) "Completed" else "Not completed",
                    tint = if (assignment.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = assignment.courseName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (assignment.reminderDaysBefore > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${assignment.reminderDaysBefore}d reminder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Text(
                    text = assignment.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (assignment.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (assignment.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                if (!assignment.description.isNullOrBlank()) {
                    Text(
                        text = assignment.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Due: ${DateFormatter.formatDate(assignment.dueDate)} at ${assignment.dueTime}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (assignment.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp).testTag("assignment_menu_${assignment.id}")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssignmentDialog(
    existing: Assignment?,
    onDismiss: () -> Unit,
    onSave: (title: String, course: String, dueDate: Long, dueTime: String, desc: String?, reminder: Int) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var courseName by remember { mutableStateOf(existing?.courseName ?: "") }
    var dueTime by remember { mutableStateOf(existing?.dueTime ?: "11:59 PM") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var reminderDays by remember { mutableIntStateOf(existing?.reminderDaysBefore ?: 1) }

    // Date in days from now (or existing)
    val cal = Calendar.getInstance()
    if (existing != null) {
        cal.timeInMillis = existing.dueDate
    } else {
        cal.add(Calendar.DAY_OF_YEAR, 3) // Default to 3 days from now
    }
    var selectedDateMillis by remember { mutableStateOf(cal.timeInMillis) }
    var reminderDropdownExpanded by remember { mutableStateOf(false) }

    val reminderOptions = listOf(
        Pair(0, "No Reminder"),
        Pair(1, "1 Day Before"),
        Pair(3, "3 Days Before"),
        Pair(7, "7 Days Before")
    )

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val context = LocalContext.current

    // DatePicker dialog setup
    val datePickerDialog = remember(context, selectedDateMillis) {
        val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDateMillis = newCal.timeInMillis
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // TimePickerDialog setup
    val timePickerDialog = remember(context, dueTime) {
        val timeCal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("hh:mm a", Locale.US).parse(dueTime.trim())
            if (parsed != null) timeCal.time = parsed
        } catch (_: Exception) {}

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val chosenCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                dueTime = SimpleDateFormat("hh:mm a", Locale.US).format(chosenCal.time)
            },
            timeCal.get(Calendar.HOUR_OF_DAY),
            timeCal.get(Calendar.MINUTE),
            false // 12-hour format with AM/PM picker
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing != null) "Edit Assignment" else "New Assignment",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assignment Title *") },
                    placeholder = { Text("e.g. Calculus Problem Set 3") },
                    modifier = Modifier.fillMaxWidth().testTag("assignment_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name *") },
                    placeholder = { Text("e.g. Mathematics II") },
                    modifier = Modifier.fillMaxWidth().testTag("assignment_course_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Due Date interactive picker
                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = dateFormat.format(selectedDateMillis),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Due Date") },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Transparent clickable overlay to trigger date picker on whole field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { datePickerDialog.show() }
                        )
                    }

                    // Due Time interactive picker with AM/PM
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dueTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Due Time") },
                            trailingIcon = {
                                IconButton(onClick = { timePickerDialog.show() }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select Time", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        // Transparent clickable overlay to trigger time picker on whole field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { timePickerDialog.show() }
                        )
                    }
                }

                // Reminder Dropdown
                ExposedDropdownMenuBox(
                    expanded = reminderDropdownExpanded,
                    onExpandedChange = { reminderDropdownExpanded = !reminderDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = reminderOptions.firstOrNull { it.first == reminderDays }?.second ?: "1 Day Before",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reminder Notification") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = reminderDropdownExpanded,
                        onDismissRequest = { reminderDropdownExpanded = false }
                    ) {
                        reminderOptions.forEach { (days, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    reminderDays = days
                                    reminderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Submit PDF on portal") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && courseName.isNotBlank()) {
                        onSave(
                            title.trim(),
                            courseName.trim(),
                            selectedDateMillis,
                            dueTime.trim(),
                            description.ifBlank { null },
                            reminderDays
                        )
                    }
                },
                enabled = title.isNotBlank() && courseName.isNotBlank(),
                modifier = Modifier.testTag("save_assignment_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
