package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Assignment
import com.example.data.CachedSponsor
import com.example.data.Exam
import com.example.data.RoutineItem
import com.example.ui.AppScreen
import com.example.ui.components.AdMobBannerAd
import com.example.ui.components.AlertCalculator
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.CompactSponsorBanner
import com.example.ui.components.CompactSponsorCarousel
import com.example.ui.components.UpcomingAlertsBottomSheet
import com.example.util.CountryDetector
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    courseCount: Int,
    upcomingAssignmentCount: Int,
    upcomingExamCount: Int,
    activeSponsor: CachedSponsor? = null,
    activeSponsors: List<CachedSponsor> = emptyList(),
    assignments: List<Assignment> = emptyList(),
    exams: List<Exam> = emptyList(),
    routineItems: List<RoutineItem> = emptyList(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onNavigateToClassNotes: () -> Unit,
    onNavigateToStudyPlanner: () -> Unit,
    onNavigateToCgpa: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onSponsorClick: (String, String) -> Unit,
    onSponsorImpression: ((String) -> Unit)? = null,
    onImportSharedNote: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showNotificationSheet by remember { mutableStateOf(false) }
    var dismissedAlertIds by remember { mutableStateOf(setOf<String>()) }

    val allAlerts = remember(assignments, exams) {
        AlertCalculator.calculateAlerts(assignments, exams)
    }

    val activeAlerts = remember(allAlerts, dismissedAlertIds) {
        allAlerts.filter { it.id !in dismissedAlertIds }
    }

    val context = LocalContext.current
    val isBangladeshUser = remember(context) {
        CountryDetector.isBangladeshUser(context)
    }

    // Featured Hero Sponsor & Promo Banner (Only displays when admin has created real active sponsors)
    val displaySponsors = remember(activeSponsors, activeSponsor) {
        val list = activeSponsors.filter { it.isValidCurrently() }
        if (list.isNotEmpty()) list
        else if (activeSponsor != null && activeSponsor.isValidCurrently()) listOf(activeSponsor)
        else emptyList()
    }

    val todayDayOfWeek = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
    val todayDayName = remember(todayDayOfWeek) {
        when (todayDayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Today"
        }
    }
    val todayClasses = remember(routineItems, todayDayOfWeek) {
        routineItems.filter { it.dayOfWeek == todayDayOfWeek }
    }

    // Pull to refresh gesture handling
    val listState = rememberLazyListState()
    var pullOffsetY by remember { mutableStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isRefreshing) 72f else pullOffsetY,
        label = "pull_offset_anim"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.example.ui.components.ClassMateAppLogo(size = 36.dp)
                        Column {
                            Text(
                                text = "ClassMate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Student Productivity Suite",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (onImportSharedNote != null) {
                        IconButton(
                            onClick = onImportSharedNote,
                            modifier = Modifier.testTag("dashboard_import_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Import Shared Note",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Refresh Button to quickly sync sponsors & updates
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("dashboard_refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Dashboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Notification Bell replacing Settings icon
                    IconButton(
                        onClick = { showNotificationSheet = true },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("notification_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (activeAlerts.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(
                                            text = if (activeAlerts.size > 9) "9+" else activeAlerts.size.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (activeAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Alerts & Reminders",
                                tint = if (activeAlerts.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Elevated Bottom Navigation Bar pinned to bottom
            AppBottomNavigationBar(
                currentScreen = AppScreen.Home,
                onNavigateToHome = { /* Already on Home */ },
                onNavigateToPlanner = onNavigateToStudyPlanner,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(isRefreshing, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                if (dragAmount > 0 || pullOffsetY > 0) {
                                    pullOffsetY = (pullOffsetY + dragAmount * 0.45f).coerceIn(0f, 130f)
                                }
                            }
                        },
                        onDragEnd = {
                            if (pullOffsetY >= 70f && !isRefreshing) {
                                onRefresh()
                            }
                            pullOffsetY = 0f
                        },
                        onDragCancel = {
                            pullOffsetY = 0f
                        }
                    )
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = animatedOffsetY
                    },
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            // Quick Search Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToSearch)
                        .testTag("home_search_bar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Notes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search notes by course, topic or caption...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Today's Live Class & Routine Highlight (Appears dynamically if user has routine entries)
            if (routineItems.isNotEmpty()) {
                item {
                    TodayScheduleHighlightCard(
                        todayClasses = todayClasses,
                        todayDayName = todayDayName,
                        onClick = onNavigateToRoutine
                    )
                }
            }

            // Academic Hub Section Header
            item {
                Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
                    Text(
                        text = "Academic Workspace",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF29B6F6))
                    )
                }
            }

            // 6 Grid Action Cards (2x3 Grid) - Balanced, Aesthetic proportions
            // Row 1: Class Notes & Photos | Study Planner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubGridCard(
                        title = "Class Notes &\nPhotos",
                        subtitle = if (courseCount > 0) "$courseCount courses available" else "Photo notes & lectures",
                        icon = Icons.Default.MenuBook,
                        iconBg = Brush.linearGradient(listOf(Color(0xFF00B4D8), Color(0xFF0077B6))),
                        badgeText = if (courseCount > 0) "$courseCount" else null,
                        chevronTint = Color(0xFF64B5F6),
                        testTag = "hub_class_notes_card",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToClassNotes
                    )

                    HubGridCard(
                        title = "Study Planner",
                        subtitle = "Weekly plan & goals",
                        icon = Icons.Default.CalendarMonth,
                        iconBg = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
                        chevronTint = Color(0xFF93C5FD),
                        testTag = "hub_study_planner_card",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToStudyPlanner
                    )
                }
            }

            // Row 2: CGPA Calculator | Assignments
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubGridCard(
                        title = "CGPA Calculator",
                        subtitle = "Target & semester GPA",
                        icon = Icons.Default.Calculate,
                        iconBg = Brush.linearGradient(listOf(Color(0xFF00BCD4), Color(0xFF00838F))),
                        chevronTint = Color(0xFF4DD0E1),
                        testTag = "hub_cgpa_card",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCgpa
                    )

                    HubGridCard(
                        title = "Assignments",
                        subtitle = if (upcomingAssignmentCount > 0) "$upcomingAssignmentCount due soon" else "Tasks & deadlines",
                        badgeText = if (upcomingAssignmentCount > 0) "$upcomingAssignmentCount Due" else null,
                        icon = Icons.Default.Assignment,
                        iconBg = Brush.linearGradient(listOf(Color(0xFF00BFA5), Color(0xFF00796B))),
                        chevronTint = Color(0xFF4DB6AC),
                        testTag = "hub_assignments_card",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAssignments
                    )
                }
            }

            // Row 3: Exams | Class Routine
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubGridCard(
                        title = "Exams",
                        subtitle = if (upcomingExamCount > 0) "$upcomingExamCount scheduled" else "Dates & countdown",
                        badgeText = if (upcomingExamCount > 0) "$upcomingExamCount" else null,
                        icon = Icons.Default.EventNote,
                        iconBg = Brush.linearGradient(listOf(Color(0xFFFF9100), Color(0xFFE65100))),
                        chevronTint = Color(0xFFFFB74D),
                        testTag = "hub_exams_card",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExams
                    )

                    HubGridCard(
                        title = "Class Routine",
                        subtitle = "Weekly timetable",
                        icon = Icons.Default.CalendarMonth,
                        iconBg = Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFF283593))),
                        chevronTint = Color(0xFF7986CB),
                        testTag = "hub_routine_card",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRoutine
                    )
                }
            }

            // 5. Smart Monetization System:
            // - Bangladesh user with active sponsors -> Show Local Sponsor Carousel
            // - Bangladesh user with no active sponsors -> Show AdMob Banner
            // - Foreign user -> Show AdMob Banner
            item {
                if (isBangladeshUser) {
                    if (displaySponsors.isNotEmpty()) {
                        CompactSponsorCarousel(
                            sponsors = displaySponsors,
                            onSponsorClick = onSponsorClick,
                            onSponsorImpression = onSponsorImpression,
                            slideIntervalMillis = 4000L,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        AdMobBannerAd(
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else {
                    AdMobBannerAd(
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Extra bottom spacer for smooth scrolling above bottom bar
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Pull to refresh top indicator banner / pill
        if (animatedOffsetY > 8f || isRefreshing) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (animatedOffsetY * 0.7f).toInt().coerceAtMost(60)) }
                    .padding(top = 8.dp)
                    .testTag("pull_to_refresh_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val releaseToRefresh = pullOffsetY >= 70f
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (releaseToRefresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    rotationZ = if (releaseToRefresh) 180f else 0f
                                }
                        )
                    }
                }
            }
        }
    }
}

    // Upcoming Alerts & Reminders Bottom Sheet
    if (showNotificationSheet) {
        UpcomingAlertsBottomSheet(
            alerts = activeAlerts,
            onDismiss = { showNotificationSheet = false },
            onClearAll = {
                dismissedAlertIds = dismissedAlertIds + allAlerts.map { it.id }
            },
            onNavigateToAssignments = {
                showNotificationSheet = false
                onNavigateToAssignments()
            },
            onNavigateToExams = {
                showNotificationSheet = false
                onNavigateToExams()
            }
        )
    }
}

@Composable
fun HubCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    gradientColors: List<Color>,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Header accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(gradientColors))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HubGridCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBg: Brush,
    iconTint: Color = Color.White,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    chevronTint: Color = Color(0xFF64B5F6),
    cardBackground: Brush? = null
) {
    val hasSubtitle = !subtitle.isNullOrBlank()

    Card(
        modifier = modifier
            .height(125.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (cardBackground != null) Modifier.background(cardBackground)
                    else Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF141F36), Color(0xFF0F172A))
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Icon + Badge or subtle Chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1976D2)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = chevronTint.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom Section: Title with clean 1-line subtitle
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 17.sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (hasSubtitle) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayScheduleHighlightCard(
    todayClasses: List<RoutineItem>,
    todayDayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("dashboard_today_schedule_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (todayClasses.isNotEmpty())
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Icon + Title + Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (todayClasses.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (todayClasses.isNotEmpty()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$todayDayName's Classes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (todayClasses.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = if (todayClasses.isNotEmpty()) "${todayClasses.size} ${if (todayClasses.size == 1) "Class" else "Classes"}" else "Free Day",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (todayClasses.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (todayClasses.isNotEmpty()) {
                // Show first 2 classes preview
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    todayClasses.take(2).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.courseName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!item.roomNumber.isNullOrBlank()) {
                                    Text(
                                        text = "Room: ${item.roomNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${item.startTime} - ${item.endTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (todayClasses.size > 2) {
                        Text(
                            text = "+ ${todayClasses.size - 2} more classes • Tap to view full routine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No classes scheduled for today. Enjoy your day! 🌴",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "View Routine →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
