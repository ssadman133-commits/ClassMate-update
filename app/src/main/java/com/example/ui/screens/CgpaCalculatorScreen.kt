package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SemesterRecord
import com.example.util.DateFormatter
import com.example.util.GradeScale
import java.util.Locale

data class CourseEntry(
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var credit: String = "",
    var grade: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgpaCalculatorScreen(
    gradeScale: GradeScale,
    semesterHistory: List<SemesterRecord>,
    onSaveSemester: (name: String, gpa: Double, credits: Double) -> Unit,
    onDeleteSemester: (SemesterRecord) -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Course entries list - fresh clean state starting at 0
    val courses = remember {
        mutableStateListOf(
            CourseEntry(id = 1, name = "", credit = "", grade = ""),
            CourseEntry(id = 2, name = "", credit = "", grade = ""),
            CourseEntry(id = 3, name = "", credit = "", grade = "")
        )
    }

    // Cumulative CGPA inputs
    var prevCgpaText by remember { mutableStateOf("") }
    var prevCreditsText by remember { mutableStateOf("") }

    // Save Semester Dialog
    var showSaveDialog by remember { mutableStateOf(false) }
    var semesterNameInput by remember { mutableStateOf("Semester 1") }

    // Current Semester GPA calculation (only calculated when credit and grade are provided)
    val semesterStats by remember(gradeScale) {
        derivedStateOf {
            var totalCredits = 0.0
            var totalPoints = 0.0
            for (c in courses) {
                val cr = c.credit.toDoubleOrNull() ?: 0.0
                if (c.grade.isNotBlank() && cr > 0.0) {
                    val pt = gradeScale.getPointForGrade(c.grade)
                    totalCredits += cr
                    totalPoints += (cr * pt)
                }
            }
            val gpa = if (totalCredits > 0.0) totalPoints / totalCredits else 0.0
            Pair(gpa, totalCredits)
        }
    }

    // Cumulative CGPA calculation
    val cumulativeCgpa by remember(semesterStats, prevCgpaText, prevCreditsText) {
        derivedStateOf {
            val prevCgpa = prevCgpaText.toDoubleOrNull() ?: 0.0
            val prevCredits = prevCreditsText.toDoubleOrNull() ?: 0.0
            val curGpa = semesterStats.first
            val curCredits = semesterStats.second

            val combinedCredits = prevCredits + curCredits
            if (combinedCredits > 0.0) {
                val totalPoints = (prevCgpa * prevCredits) + (curGpa * curCredits)
                totalPoints / combinedCredits
            } else {
                0.0
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CGPA Calculator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("cgpa_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("cgpa_grade_scale_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Grade Scale Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Calculator", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    modifier = Modifier.testTag("tab_cgpa_calculator")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Target Planner", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                    modifier = Modifier.testTag("tab_cgpa_target_planner")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History (${semesterHistory.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    modifier = Modifier.testTag("tab_cgpa_history")
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Result Summary Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .testTag("cgpa_result_summary_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Semester GPA",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = if (semesterStats.second > 0.0) String.format(Locale.US, "%.2f", semesterStats.first) else "0.00",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Semester Credits",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = if (semesterStats.second > 0.0) String.format(Locale.US, "%.1f", semesterStats.second) else "0.0",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                if (prevCreditsText.isNotBlank()) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Updated Cumulative CGPA",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f", cumulativeCgpa),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            semesterNameInput = "Semester ${semesterHistory.size + 1}"
                                            showSaveDialog = true
                                        },
                                        enabled = semesterStats.second > 0.0,
                                        modifier = Modifier.testTag("save_semester_result_button")
                                    ) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Semester")
                                    }
                                }
                            }
                        }
                    }

                    // Optional Previous CGPA Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Cumulative CGPA (Optional)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = prevCgpaText,
                                        onValueChange = { prevCgpaText = it },
                                        label = { Text("Prev CGPA") },
                                        placeholder = { Text("e.g. 3.65") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("prev_cgpa_input"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = prevCreditsText,
                                        onValueChange = { prevCreditsText = it },
                                        label = { Text("Prev Credits") },
                                        placeholder = { Text("e.g. 45.0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("prev_credits_input"),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Courses Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Courses (${courses.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (courses.any { it.name.isNotBlank() || it.credit.isNotBlank() || it.grade.isNotBlank() } || prevCgpaText.isNotBlank() || prevCreditsText.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            courses.clear()
                                            courses.add(CourseEntry(id = 1, name = "", credit = "", grade = ""))
                                            courses.add(CourseEntry(id = 2, name = "", credit = "", grade = ""))
                                            courses.add(CourseEntry(id = 3, name = "", credit = "", grade = ""))
                                            prevCgpaText = ""
                                            prevCreditsText = ""
                                        },
                                        modifier = Modifier.testTag("clear_cgpa_calculator_button")
                                    ) {
                                        Text("Reset")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        courses.add(
                                            CourseEntry(
                                                id = System.currentTimeMillis(),
                                                name = "",
                                                credit = "",
                                                grade = ""
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("add_course_row_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Course")
                                }
                            }
                        }
                    }

                    // Course Rows
                    itemsIndexed(courses, key = { _, item -> item.id }) { index, course ->
                        CourseRowCard(
                            course = course,
                            index = index,
                            onUpdate = { updated ->
                                courses[index] = updated
                            },
                            onDelete = {
                                if (courses.size > 1) {
                                    courses.removeAt(index)
                                }
                            },
                            canDelete = courses.size > 1
                        )
                    }
                }
            } else if (selectedTab == 1) {
                // Target CGPA Planner Tab - fresh clean start at 0.00
                TargetCgpaPlannerView(
                    gradeScale = gradeScale
                )
            } else {
                // Semester History Tab
                if (semesterHistory.isEmpty()) {
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No Saved Semesters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Calculate your semester GPA in the Calculator tab and tap 'Save Semester' to preserve your academic history locally.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(semesterHistory, key = { _, item -> item.id }) { _, record ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("semester_history_item_${record.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = record.semesterName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", record.totalCredits)} credits • ${DateFormatter.formatDate(record.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "GPA ${String.format(Locale.US, "%.2f", record.gpa)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteSemester(record) },
                                            modifier = Modifier.testTag("delete_semester_${record.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete semester",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Semester Result", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a label for this semester result:")
                    OutlinedTextField(
                        value = semesterNameInput,
                        onValueChange = { semesterNameInput = it },
                        label = { Text("Semester Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_semester_name_input"),
                        singleLine = true
                    )
                    Text(
                        text = "GPA: ${String.format(Locale.US, "%.2f", semesterStats.first)} • Credits: ${String.format(Locale.US, "%.1f", semesterStats.second)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (semesterNameInput.isNotBlank()) {
                            onSaveSemester(
                                semesterNameInput.trim(),
                                semesterStats.first,
                                semesterStats.second
                            )
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_semester_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseRowCard(
    course: CourseEntry,
    index: Int = 0,
    onUpdate: (CourseEntry) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val gradeOptions = listOf("A+", "A", "A-", "B+", "B", "B-", "C+", "C", "D", "F")
    var gradeDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Course Name
            OutlinedTextField(
                value = course.name,
                onValueChange = { onUpdate(course.copy(name = it)) },
                placeholder = { Text("Course ${index + 1}") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )

            // Credit
            OutlinedTextField(
                value = course.credit,
                onValueChange = { onUpdate(course.copy(credit = it)) },
                label = { Text("Cr") },
                placeholder = { Text("3.0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(68.dp),
                singleLine = true
            )

            // Grade Dropdown
            ExposedDropdownMenuBox(
                expanded = gradeDropdownExpanded,
                onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded },
                modifier = Modifier.width(82.dp)
            ) {
                OutlinedTextField(
                    value = course.grade,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gr") },
                    placeholder = { Text("—") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                )

                ExposedDropdownMenu(
                    expanded = gradeDropdownExpanded,
                    onDismissRequest = { gradeDropdownExpanded = false }
                ) {
                    gradeOptions.forEach { grade ->
                        DropdownMenuItem(
                            text = { Text(grade, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onUpdate(
                                    course.copy(
                                        grade = grade,
                                        credit = if (course.credit.isBlank()) "3.0" else course.credit
                                    )
                                )
                                gradeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Delete action
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove course",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TargetCgpaPlannerView(
    gradeScale: GradeScale,
    currentCgpaSuggestion: Double = 0.0,
    completedCreditsSuggestion: Double = 0.0,
    modifier: Modifier = Modifier
) {
    var currentCgpaText by remember {
        mutableStateOf(if (currentCgpaSuggestion > 0.0) String.format(Locale.US, "%.2f", currentCgpaSuggestion) else "")
    }
    var completedCreditsText by remember {
        mutableStateOf(if (completedCreditsSuggestion > 0.0) String.format(Locale.US, "%.1f", completedCreditsSuggestion) else "")
    }
    var targetCgpaText by remember { mutableStateOf("") }
    var remainingCreditsText by remember { mutableStateOf("") }

    val curCgpa = currentCgpaText.toDoubleOrNull()
    val compCredits = completedCreditsText.toDoubleOrNull()
    val targetCgpa = targetCgpaText.toDoubleOrNull()
    val remCredits = remainingCreditsText.toDoubleOrNull()

    val hasAllInputs = curCgpa != null && compCredits != null && targetCgpa != null && remCredits != null && remCredits > 0.0 && targetCgpa > 0.0

    val requiredGpa = remember(curCgpa, compCredits, targetCgpa, remCredits, hasAllInputs) {
        if (hasAllInputs) {
            val totalCredits = compCredits!! + remCredits!!
            val totalRequiredPoints = targetCgpa!! * totalCredits
            val currentPoints = curCgpa!! * compCredits
            val requiredPoints = totalRequiredPoints - currentPoints
            requiredPoints / remCredits
        } else {
            null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Target Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("target_cgpa_result_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        requiredGpa == null -> MaterialTheme.colorScheme.surfaceVariant
                        requiredGpa > gradeScale.aPlus -> MaterialTheme.colorScheme.errorContainer
                        requiredGpa <= (curCgpa ?: 0.0) -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "REQUIRED GPA IN REMAINING CREDITS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            requiredGpa == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            requiredGpa > gradeScale.aPlus -> MaterialTheme.colorScheme.onErrorContainer
                            requiredGpa <= (curCgpa ?: 0.0) -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (requiredGpa != null) String.format(Locale.US, "%.2f", requiredGpa.coerceAtLeast(0.0)) else "0.00",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = when {
                            requiredGpa == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            requiredGpa > gradeScale.aPlus -> MaterialTheme.colorScheme.onErrorContainer
                            requiredGpa <= (curCgpa ?: 0.0) -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )

                    // Feasibility Message
                    val (statusText, adviceText) = when {
                        requiredGpa == null -> Pair("Ready for Input", "Fill in your target CGPA and credit numbers below to calculate.")
                        requiredGpa > gradeScale.aPlus -> Pair(
                            "⚠️ Target Math Unattainable",
                            "Requires higher than maximum scale (${gradeScale.aPlus}). Increase remaining credits or adjust target CGPA."
                        )
                        requiredGpa <= 0.0 -> Pair(
                            "🎉 Goal Already Achieved!",
                            "Your completed credits already ensure a CGPA above your target."
                        )
                        requiredGpa <= (curCgpa ?: 0.0) -> Pair(
                            "✅ Easily Attainable",
                            "Maintaining your current pace or scoring ${String.format(Locale.US, "%.2f", requiredGpa)}+ will secure your goal."
                        )
                        requiredGpa in 3.60..gradeScale.aPlus -> Pair(
                            "🔥 High Performance Needed",
                            "Aim for mostly A and A+ grades in your remaining ${String.format(Locale.US, "%.0f", remCredits ?: 0.0)} credits."
                        )
                        else -> Pair(
                            "🎯 Realistic Goal",
                            "Maintain a consistent B+ to A average to reach your target."
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = adviceText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Input Fields Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Academic Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (targetCgpaText.isNotBlank() || currentCgpaText.isNotBlank() || completedCreditsText.isNotBlank() || remainingCreditsText.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    targetCgpaText = ""
                                    currentCgpaText = ""
                                    completedCreditsText = ""
                                    remainingCreditsText = ""
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                    }

                    // Target CGPA with quick chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = targetCgpaText,
                            onValueChange = { targetCgpaText = it },
                            label = { Text("Target CGPA Desired") },
                            placeholder = { Text("e.g. 3.75") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("input_target_cgpa"),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("3.50", "3.65", "3.75", "4.00").forEach { preset ->
                                Surface(
                                    onClick = { targetCgpaText = preset },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (targetCgpaText == preset) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Current CGPA
                    OutlinedTextField(
                        value = currentCgpaText,
                        onValueChange = { currentCgpaText = it },
                        label = { Text("Current CGPA") },
                        placeholder = { Text("e.g. 3.25") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("input_current_cgpa"),
                        singleLine = true
                    )

                    // Completed Credits
                    OutlinedTextField(
                        value = completedCreditsText,
                        onValueChange = { completedCreditsText = it },
                        label = { Text("Completed Credits So Far") },
                        placeholder = { Text("e.g. 60.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("input_completed_credits"),
                        singleLine = true
                    )

                    // Remaining Credits with quick chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = remainingCreditsText,
                            onValueChange = { remainingCreditsText = it },
                            label = { Text("Remaining Credits to Complete") },
                            placeholder = { Text("e.g. 30.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("input_remaining_credits"),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "15.0" to "1 Sem",
                                "30.0" to "2 Sem",
                                "45.0" to "3 Sem",
                                "60.0" to "4 Sem"
                            ).forEach { (credits, label) ->
                                Surface(
                                    onClick = { remainingCreditsText = credits },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (remainingCreditsText == credits) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = credits,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Graduation Credit Outlook Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val compCr = compCredits ?: 0.0
                    val remCr = remCredits ?: 0.0
                    val totalCr = compCr + remCr

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f", totalCr),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total Degree Cr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (totalCr > 0.0) String.format(Locale.US, "%.0f%%", (compCr / totalCr) * 100) else "0%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (targetCgpaText.isNotBlank()) targetCgpaText else "0.00",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Target CGPA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
