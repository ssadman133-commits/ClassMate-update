package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.util.AppThemeMode
import com.example.util.GradeScale
import com.example.util.NotificationScheduler
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: AppThemeMode,
    gradeScale: GradeScale,
    assignmentRemindersEnabled: Boolean,
    examRemindersEnabled: Boolean,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onSaveGradeScale: (GradeScale) -> Unit,
    onToggleAssignmentReminders: (Boolean) -> Unit,
    onToggleExamReminders: (Boolean) -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGradeScaleDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // Calculate storage usage
    val storageInfo = remember {
        val notesDir = File(context.filesDir, "notes_photos")
        var totalBytes = 0L
        var fileCount = 0
        if (notesDir.exists() && notesDir.isDirectory) {
            notesDir.listFiles()?.forEach { file ->
                totalBytes += file.length()
                fileCount++
            }
        }
        val mb = totalBytes.toDouble() / (1024 * 1024)
        Pair(mb, fileCount)
    }

    var isNotificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationPermissionGranted = granted
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Appearance
            item {
                SettingsSectionCard(title = "Appearance") {
                    SettingsRowItem(
                        icon = Icons.Default.ColorLens,
                        title = "App Theme",
                        subtitle = when (currentThemeMode) {
                            AppThemeMode.SYSTEM -> "System Default"
                            AppThemeMode.LIGHT -> "Light Mode"
                            AppThemeMode.DARK -> "Dark Mode"
                        },
                        testTag = "settings_theme_row",
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // Section 2: Grade Scale (CGPA)
            item {
                SettingsSectionCard(title = "Academic Standards") {
                    SettingsRowItem(
                        icon = Icons.Default.Tune,
                        title = "Grade Scale Points",
                        subtitle = "A+ (4.00), A (3.75), B (3.00)... Tap to customize",
                        testTag = "settings_grade_scale_row",
                        onClick = { showGradeScaleDialog = true }
                    )
                }
            }

            // Section 3: Notification Settings
            item {
                SettingsSectionCard(title = "Notifications & Reminders") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assignment Reminders",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Receive local alerts before due dates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = assignmentRemindersEnabled,
                            onCheckedChange = onToggleAssignmentReminders,
                            modifier = Modifier.testTag("switch_assignment_reminders")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exam Reminders",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Receive local alerts before scheduled exams",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = examRemindersEnabled,
                            onCheckedChange = onToggleExamReminders,
                            modifier = Modifier.testTag("switch_exam_reminders")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // System Notification Permission Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isNotificationPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isNotificationPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isNotificationPermissionGranted) "Notification Permission: Active" else "Notification Permission: Disabled",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isNotificationPermissionGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (isNotificationPermissionGranted) "Alerts can pop up in background" else "Required for background reminders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!isNotificationPermissionGranted) {
                            OutlinedButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.testTag("button_grant_notification_permission")
                            ) {
                                Text("Enable")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Battery Optimization & Background Execution
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Background App Execution",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Set battery to 'Unrestricted' for reliable alarms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.testTag("button_battery_optimization")
                        ) {
                            Text("Settings")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Test Background Notification in 5 Seconds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Test Background Alert (5s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap, close the app, and wait 5 seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                NotificationScheduler.scheduleTestNotification(context, 5)
                                Toast.makeText(
                                    context,
                                    "Test alert will arrive in 5 seconds! Minimize the app now.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("button_test_notification")
                        ) {
                            Text("Test (5s)")
                        }
                    }
                }
            }

            // Section 4: Storage Information
            item {
                SettingsSectionCard(title = "Data & Storage") {
                    SettingsRowItem(
                        icon = Icons.Default.Folder,
                        title = "Storage Usage",
                        subtitle = String.format(Locale.US, "%.2f MB used by %d note photos", storageInfo.first, storageInfo.second),
                        testTag = "settings_storage_row",
                        onClick = { showStorageDialog = true }
                    )
                }
            }

            // Section 5: Information & About
            item {
                SettingsSectionCard(title = "Information & Support") {
                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "100% offline data protection guarantee",
                        testTag = "settings_privacy_row",
                        onClick = { showPrivacyDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "About Class Notes",
                        subtitle = "Version 1.0 • Student Productivity Suite",
                        testTag = "settings_about_row",
                        onClick = { showAboutDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsRowItem(
                        icon = Icons.Default.Feedback,
                        title = "Feedback & Contact",
                        subtitle = "Suggestions and student queries",
                        testTag = "settings_feedback_row",
                        onClick = { showFeedbackDialog = true }
                    )
                }
            }
        }
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose App Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        Pair(AppThemeMode.SYSTEM, "System Default"),
                        Pair(AppThemeMode.LIGHT, "Light Mode"),
                        Pair(AppThemeMode.DARK, "Dark Mode")
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentThemeMode == mode,
                                onClick = {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Grade Scale Editor Dialog
    if (showGradeScaleDialog) {
        GradeScaleEditorDialog(
            current = gradeScale,
            onDismiss = { showGradeScaleDialog = false },
            onSave = { newScale ->
                onSaveGradeScale(newScale)
                showGradeScaleDialog = false
            }
        )
    }

    // Storage Usage Dialog
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Storage Breakdown", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Saved Note Photos: ${storageInfo.second} files")
                    Text("• Local Disk Space: ${String.format(Locale.US, "%.2f", storageInfo.first)} MB")
                    Text("• Storage Location: Internal private sandbox (isolated from device gallery).")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "All course notes, assignments, exams, and routine items are stored safely on device and remain intact across restarts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showStorageDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Offline Student Privacy", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Completely Offline Notes:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "All student notes, captured blackboard photos, math solutions, and course data are stored exclusively in local device storage. No notes are uploaded to any server or cloud.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "2. No Login or Tracking:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "You are not required to create an account, enter personal email, or log in. Your academic records remain strictly on your device.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "3. Non-Intrusive Sponsored Banners:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Only the promotional banner connects to a public endpoint to check for active educational partners. No student device identifiers or academic contents are ever sent.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { com.example.ui.components.ClassMateAppLogo(size = 48.dp) },
            title = { Text("ClassMate", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 1.0 (ClassMate Suite)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Designed specifically for university and college students to capture, organize, and quickly retrieve blackboard photos, lecture notes, exam routines, and calculate CGPA.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Features: Course & Topic Note Organization • Multi-photo Capture & Pinch Zoom • 1-5 Star Ratings • Full-text Search • CGPA & History • Assignment & Exam Reminders • Class Routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Feedback Dialog
    if (showFeedbackDialog) {
        var feedbackText by remember { mutableStateOf("") }
        var submitted by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            icon = { Icon(Icons.Default.Feedback, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Contact & Feedback", fontWeight = FontWeight.Bold) },
            text = {
                if (submitted) {
                    Text("Thank you for your feedback! Your suggestions help us improve Class Notes for university students.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Have a suggestion or found an issue? Share your thoughts below:")
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            placeholder = { Text("Write your feedback here...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!submitted && feedbackText.isNotBlank()) {
                            submitted = true
                        } else {
                            showFeedbackDialog = false
                        }
                    }
                ) {
                    Text(if (submitted) "Done" else "Submit")
                }
            },
            dismissButton = {
                if (!submitted) {
                    TextButton(onClick = { showFeedbackDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GradeScaleEditorDialog(
    current: GradeScale,
    onDismiss: () -> Unit,
    onSave: (GradeScale) -> Unit
) {
    var aPlus by remember { mutableStateOf(current.aPlus.toString()) }
    var a by remember { mutableStateOf(current.a.toString()) }
    var aMinus by remember { mutableStateOf(current.aMinus.toString()) }
    var bPlus by remember { mutableStateOf(current.bPlus.toString()) }
    var b by remember { mutableStateOf(current.b.toString()) }
    var bMinus by remember { mutableStateOf(current.bMinus.toString()) }
    var cPlus by remember { mutableStateOf(current.cPlus.toString()) }
    var c by remember { mutableStateOf(current.c.toString()) }
    var d by remember { mutableStateOf(current.d.toString()) }
    var f by remember { mutableStateOf(current.f.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Grade Scale", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Edit point values used for CGPA calculations:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = aPlus, onValueChange = { aPlus = it }, label = { Text("A+") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("A") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = aMinus, onValueChange = { aMinus = it }, label = { Text("A-") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = bPlus, onValueChange = { bPlus = it }, label = { Text("B+") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("B") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = bMinus, onValueChange = { bMinus = it }, label = { Text("B-") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cPlus, onValueChange = { cPlus = it }, label = { Text("C+") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text("C") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("D") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = f, onValueChange = { f = it }, label = { Text("F") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scale = GradeScale(
                        aPlus = aPlus.toDoubleOrNull() ?: 4.0,
                        a = a.toDoubleOrNull() ?: 3.75,
                        aMinus = aMinus.toDoubleOrNull() ?: 3.50,
                        bPlus = bPlus.toDoubleOrNull() ?: 3.25,
                        b = b.toDoubleOrNull() ?: 3.00,
                        bMinus = bMinus.toDoubleOrNull() ?: 2.75,
                        cPlus = cPlus.toDoubleOrNull() ?: 2.50,
                        c = c.toDoubleOrNull() ?: 2.25,
                        d = d.toDoubleOrNull() ?: 2.00,
                        f = f.toDoubleOrNull() ?: 0.00
                    )
                    onSave(scale)
                },
                modifier = Modifier.testTag("save_grade_scale_button")
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
