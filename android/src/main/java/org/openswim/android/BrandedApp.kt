package org.openswim.android

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import org.openswim.core.CompletedWorkout
import org.openswim.core.TrainingPlan
import org.openswim.core.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

private val Navy = Color(0xFF061B2A)
private val Panel = Color(0xFF09283C)
private val StrokeBlue = Color(0xFF1A5677)
private val Aqua = Color(0xFF15D5EA)
private val Muted = Color(0xFFB9CCE5)
private val Green = Color(0xFF64E99C)
private val White = Color.White
private val CardShape = RoundedCornerShape(17.dp)

private enum class SwimTab(val title: String, val icon: String) {
    HOME("Home", "⌂"), WORKOUTS("Workouts", "▥"), PROGRESS("Progress", "▥"), PLANS("Plans", "▣"), MORE("More", "☰")
}
private sealed interface SwimPage {
    data class WorkoutDetail(val id: String) : SwimPage
    data class PlanDetail(val id: String) : SwimPage
    data class Result(val id: String) : SwimPage
    data class Swim(val workoutId: String? = null) : SwimPage
    data object Sync : SwimPage
    data object Settings : SwimPage
    data object Help : SwimPage
    data object About : SwimPage
    data object Notifications : SwimPage
}

@Composable
internal fun OpenSwimBrandedApp() {
    var showLanding by rememberSaveable { mutableStateOf(true) }
    var tab by rememberSaveable { mutableStateOf(SwimTab.HOME) }
    var page by remember { mutableStateOf<SwimPage?>(null) }
    var previous by remember { mutableStateOf<SwimPage?>(null) }
    fun select(next: SwimTab) { showLanding = false; tab = next; page = null; previous = null }
    fun open(next: SwimPage) { previous = page; page = next }
    fun back() { page = previous; previous = null }
    BackHandler(showLanding.not() && (page != null || tab != SwimTab.HOME)) {
        if (page != null) back() else select(SwimTab.HOME)
    }
    val activity = LocalActivity.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, !showLanding)
            window.statusBarColor = if (showLanding) android.graphics.Color.TRANSPARENT else android.graphics.Color.rgb(3, 35, 53)
            window.navigationBarColor = android.graphics.Color.rgb(5, 25, 39)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    if (showLanding) {
        HomeLanding(
            onStartSwim = { showLanding = false; open(SwimPage.Swim()) },
            onGuidedWorkout = { select(SwimTab.WORKOUTS) }
        )
        return
    }
    val immersive = page is SwimPage.Swim
    Column(Modifier.fillMaxSize().background(Navy)) {
        Box(Modifier.weight(1f)) {
            when (val current = page) {
                is SwimPage.WorkoutDetail -> repository.workout(current.id)?.let { BrandedWorkoutDetail(it, ::back) { open(SwimPage.Swim(it.id)) } }
                    ?: EmptyView("Workout unavailable", ::back)
                is SwimPage.PlanDetail -> repository.plans.firstOrNull { it.id == current.id }?.let { PlanDetailView(it, ::back) { id -> open(SwimPage.WorkoutDetail(id)) } }
                    ?: EmptyView("Plan unavailable", ::back)
                is SwimPage.Result -> repository.completed.firstOrNull { it.id == current.id }?.let { ResultView(it, ::back) }
                    ?: EmptyView("Swim unavailable", ::back)
                is SwimPage.Swim -> SwimTimerView(current.workoutId, ::back)
                SwimPage.Sync -> SimplePage("Sync & Data", ::back) {
                    BrandCard {
                        BrandText("OpenSwim cloud", 21, true)
                        BrandText("${repository.workouts.size} workouts · ${repository.plans.size} plans · ${repository.completed.size} swims", 14, color = Muted)
                        BrandText("Last sync: ${repository.lastSyncAt?.let(::relativeTime) ?: "Never"}", 14, color = Muted)
                    }
                    BrandButton("Sync now", onClick = { repository.refresh() })
                    BrandText("Your account data refreshes when the app opens and after sign in.", 14, color = Muted)
                }
                SwimPage.Settings -> SimplePage("Settings", ::back) {
                    BrandCard { BrandText("Distance", 18, true); BrandText("Meters", 15, color = Muted) }
                    BrandCard { BrandText("Pool length", 18, true); BrandText("Choose 25 m or 50 m when starting a swim.", 15, color = Muted) }
                }
                SwimPage.Help -> SimplePage("Help & Support", ::back) {
                    BrandCard { BrandText("Need help?", 20, true); BrandText("OpenSwim syncs your workouts, plans and completed swims with your account. For account help, contact the OpenSwim team through the website.", 15, color = Muted) }
                }
                SwimPage.About -> SimplePage("About OpenSwim", ::back) {
                    BrandCard { BrandText("OpenSwim", 25, true, Aqua); BrandText("Better swimming for everyone.", 16, color = Muted) }
                }
                SwimPage.Notifications -> SimplePage("Notifications", ::back) {
                    BrandCard { BrandText("No new notifications", 18, true); BrandText("Your upcoming swims are listed in Plans.", 14, color = Muted) }
                }
                null -> when (tab) {
                    SwimTab.HOME -> Dashboard(
                        onWorkout = { open(SwimPage.WorkoutDetail(it)) },
                        onWorkouts = { select(SwimTab.WORKOUTS) },
                        onPlans = { select(SwimTab.PLANS) },
                        onSwim = { open(SwimPage.Swim()) },
                        onSync = { open(SwimPage.Sync) },
                        onNotifications = { open(SwimPage.Notifications) }
                    )
                    SwimTab.WORKOUTS -> WorkoutLibrary(onBack = { select(SwimTab.HOME) }) { open(SwimPage.WorkoutDetail(it)) }
                    SwimTab.PROGRESS -> ProgressView { open(SwimPage.Result(it)) }
                    SwimTab.PLANS -> PlansView({ open(SwimPage.PlanDetail(it)) }, { open(SwimPage.WorkoutDetail(it)) })
                    SwimTab.MORE -> AccountView(
                        onPlans = { select(SwimTab.PLANS) },
                        onSync = { open(SwimPage.Sync) },
                        onSettings = { open(SwimPage.Settings) },
                        onHelp = { open(SwimPage.Help) },
                        onAbout = { open(SwimPage.About) }
                    )
                }
            }
        }
        if (!immersive && page !is SwimPage.WorkoutDetail && page !is SwimPage.PlanDetail && page !is SwimPage.Result) BottomNavigation(tab, ::select)
    }
}

@Composable private fun WaterHeader(title: String, back: (() -> Unit)? = null, action: String? = null, onAction: (() -> Unit)? = null, height: Int = 128) {
    Box(Modifier.fillMaxWidth().height(height.dp)) {
        Image(painterResource(R.drawable.pool_lane), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x9B00263E), Color(0xDC062238), Navy))))
        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            if (back != null) BrandText("←", 31, color = Aqua, modifier = Modifier.clickable(onClick = back).padding(end = 16.dp))
            BrandText(title, 26, true, modifier = Modifier.weight(1f))
            if (action != null && onAction != null) BrandText(action, 26, color = Aqua, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable private fun BrandText(value: String, size: Int, bold: Boolean = false, color: Color = White, modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) {
    Text(value, modifier, color = color, fontSize = size.sp, lineHeight = (size * 1.22f).sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = align, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable private fun BrandCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(CardShape).background(Panel).border(1.dp, StrokeBlue, CardShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp), content = content)
}

@Composable private fun BrandButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(23.dp)).background(Aqua).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        BrandText(label, 19, true, Navy)
    }
}

@Composable private fun PillRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option ->
            val active = option == selected
            Box(Modifier.weight(1f).height(39.dp).clip(RoundedCornerShape(30.dp))
                .background(if (active) Aqua else Panel).border(1.dp, if (active) Aqua else StrokeBlue, RoundedCornerShape(30.dp))
                .clickable { onSelect(option) }, contentAlignment = Alignment.Center) {
                BrandText(option, 14, active, if (active) Navy else White, maxLines = 1)
            }
        }
    }
}

@Composable private fun BottomNavigation(selected: SwimTab, onSelect: (SwimTab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(76.dp).background(Color(0xFF071E30)).border(0.5.dp, StrokeBlue), verticalAlignment = Alignment.CenterVertically) {
        SwimTab.entries.forEach { tab ->
            val active = selected == tab
            Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(tab) }.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandText(tab.icon, 27, active, if (active) Aqua else Muted)
                BrandText(tab.title, 11, active, if (active) Aqua else Muted, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                if (active) Box(Modifier.width(38.dp).height(3.dp).clip(CircleShape).background(Aqua))
            }
        }
    }
}

@Composable private fun Dashboard(onWorkout: (String) -> Unit, onWorkouts: () -> Unit, onPlans: () -> Unit, onSwim: () -> Unit, onSync: () -> Unit, onNotifications: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WaterHeader("OpenSwim", action = "♧", onAction = onNotifications, height = 122)
        Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandText("Today", 24, true, modifier = Modifier.weight(1f))
                BrandText("See all  →", 14, color = Aqua, modifier = Modifier.clickable(onClick = onWorkouts))
            }
            val featured = repository.scheduled.firstOrNull { it.date == LocalDate.now().toString() && it.workoutId != null }?.workoutId?.let(repository::workout)
                ?: repository.workouts.firstOrNull()
            if (featured != null) BrandCard(onClick = { onWorkout(featured.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BrandText(featured.name, 23, true)
                        BrandText("${featured.totalDistance}  ·  ~${featured.estimatedMinutes} min", 15, color = Muted)
                        BrandText(repository.workoutDescriptions[featured.id] ?: "${featured.type} swim session", 14, color = Muted, maxLines = 2)
                    }
                    BrandText("›", 32, color = Aqua)
                }
            } else BrandCard { BrandText("No workouts yet", 18, true); BrandText("Published workouts will appear here after sync.", 14, color = Muted) }
            BrandText("Quick Actions", 23, true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("▣", "Guided Workout", Modifier.weight(1f), onWorkouts)
                QuickAction("◉", "Pool Swim", Modifier.weight(1f), onSwim)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("▤", "My Plans", Modifier.weight(1f), onPlans)
                QuickAction("◷", "Watch Sync", Modifier.weight(1f), onSync, "Not connected")
            }
            val week = LocalDate.now().with(DayOfWeek.MONDAY)
            val thisWeek = repository.completed.filter { inRange(it, week, LocalDate.now()) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandText("This Week", 23, true, modifier = Modifier.weight(1f))
                BrandText("${thisWeek.size} workouts", 14, color = Muted)
            }
            BrandCard {
                WeeklyBars(thisWeek, week)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DashboardStat("${thisWeek.sumOf { it.distance.amount }} m", "Total distance")
                    DashboardStat("${thisWeek.size}", "Workouts")
                }
            }
            repository.error?.let { ErrorNotice(it) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable private fun QuickAction(icon: String, label: String, modifier: Modifier, onClick: () -> Unit, detail: String? = null) {
    BrandCard(modifier.height(100.dp), onClick) {
        BrandText(icon, 24, color = Aqua)
        BrandText(label, 15, true, maxLines = 1)
        if (detail != null) BrandText(detail, 11, color = Muted)
    }
}

@Composable private fun DashboardStat(value: String, label: String) {
    Column { BrandText(value, 17, true, Aqua); BrandText(label, 12, color = Muted) }
}

@Composable private fun WeeklyBars(records: List<CompletedWorkout>, monday: LocalDate) {
    val byDay = (0..6).map { offset -> records.filter { it.dateLabel == monday.plusDays(offset.toLong()).toString() }.sumOf { it.distance.amount } }
    val ceiling = max(1, byDay.maxOrNull() ?: 1)
    Row(Modifier.fillMaxWidth().height(107.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
        byDay.forEachIndexed { index, distance ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(20.dp).height((12 + 66f * distance / ceiling).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(if (distance > 0) Aqua else Color(0xFF1D445D)))
                Spacer(Modifier.height(6.dp))
                BrandText(listOf("M", "T", "W", "T", "F", "S", "S")[index], 12, color = Muted)
            }
        }
    }
}

@Composable private fun WorkoutLibrary(onBack: () -> Unit, onWorkout: (String) -> Unit) {
    var category by rememberSaveable { mutableStateOf("All") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        WaterHeader("Workouts", back = onBack, action = if (searchOpen) "×" else "⌕", onAction = { searchOpen = !searchOpen; if (!searchOpen) search = "" }, height = 130)
        if (searchOpen) BrandSearch(search, { search = it })
        Column(Modifier.padding(horizontal = 17.dp)) {
            PillRow(listOf("All", "Endurance", "Technique", "Speed"), category) { category = it }
            Spacer(Modifier.height(15.dp))
        }
        val filtered = repository.workouts.filter { workout ->
            val categoryMatch = when (category) { "All" -> true; "Speed" -> workout.type in listOf("Sprint", "Threshold"); "Endurance" -> workout.type in listOf("Endurance", "Aerobic", "Easy"); else -> workout.type == category }
            categoryMatch && workout.name.contains(search, ignoreCase = true)
        }
        LazyColumn(contentPadding = PaddingValues(start = 17.dp, end = 17.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (repository.busy && filtered.isEmpty()) item { BrandText("Syncing workouts…", 15, color = Muted) }
            else if (filtered.isEmpty()) item { BrandCard { BrandText("No matching workouts", 17, true); BrandText("Try another category or search.", 14, color = Muted) } }
            items(filtered, key = { it.id }) { workout ->
                BrandCard(onClick = { onWorkout(workout.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(75.dp).clip(RoundedCornerShape(11.dp))) {
                            Image(painterResource(R.drawable.swimmer_card), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Box(Modifier.fillMaxSize().background(Color(0x50003C62)))
                        }
                        Column(Modifier.weight(1f)) {
                            BrandText(workout.name, 18, true, maxLines = 1)
                            BrandText("${workout.totalDistance} · ~${workout.estimatedMinutes} min", 14, color = Muted)
                            BrandText(workout.type, 12, color = Aqua)
                        }
                        BrandText("›", 29, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable private fun BrandSearch(value: String, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 8.dp).clip(CardShape).background(Panel).border(1.dp, StrokeBlue, CardShape).padding(13.dp)) {
        if (value.isEmpty()) BrandText("Search workouts", 15, color = Muted)
        BasicTextField(value, onChange, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 15.sp), modifier = Modifier.fillMaxWidth())
    }
}

@Composable private fun BrandedWorkoutDetail(workout: Workout, onBack: () -> Unit, onStart: () -> Unit) {
    var section by rememberSaveable(workout.id) { mutableStateOf("Overview") }
    Column(Modifier.fillMaxSize().background(Navy)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                Image(painterResource(R.drawable.pool_lane), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66021D30), Navy))))
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandText("←", 30, color = Aqua, modifier = Modifier.clickable(onClick = onBack))
                    Spacer(Modifier.width(17.dp))
                    BrandText("Workouts", 23, true, modifier = Modifier.weight(1f))
                    BrandText("⋮", 27, color = Aqua)
                }
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    BrandText(workout.name, 31, true)
                    BrandText("${workout.totalDistance}  ·  ~${workout.estimatedMinutes} min  ·  ${workout.type}", 17, color = Muted)
                }
            }
            Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BrandText(repository.workoutDescriptions[workout.id] ?: "${workout.type} workout with ${workout.sections.size} sections.", 16, color = Muted)
                PillRow(listOf("Overview", "Sets", "Tips"), section) { section = it }
                when (section) {
                    "Tips" -> BrandCard {
                        BrandText("Before you swim", 19, true)
                        BrandText("Review each set and choose a pool length that fits the workout. Rest and target pace are listed where available.", 15, color = Muted)
                    }
                    else -> {
                        if (section == "Overview") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricCard("${workout.totalDistance}", "Distance", Modifier.weight(1f))
                                MetricCard("~${workout.estimatedMinutes} min", "Duration", Modifier.weight(1f))
                                MetricCard(workout.sections.flatMap { it.sets }.maxOf { it.step.intensity }.name.lowercase().replaceFirstChar { it.uppercase() }, "Intensity", Modifier.weight(1f))
                            }
                            BrandText("Workout Structure", 23, true)
                            BrandText("${workout.sections.sumOf { it.sets.size }} sets · ${workout.totalDistance} total", 15, color = Muted)
                        }
                        workout.sections.forEach { workoutSection ->
                            BrandText(workoutSection.name, 18, true, Aqua)
                            workoutSection.sets.forEachIndexed { index, set ->
                                Row(Modifier.fillMaxWidth().clip(CardShape).background(Panel).border(1.dp, StrokeBlue, CardShape).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(5.dp).height(39.dp).clip(CircleShape).background(listOf(Aqua, Green, Color(0xFFFFD52C), Color(0xFFC98AEE))[index % 4]))
                                    Spacer(Modifier.width(11.dp))
                                    BrandText("${set.repetitions} × ${set.step.distance}", 15, true, modifier = Modifier.weight(1f))
                                    BrandText(set.step.stroke.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, 13, modifier = Modifier.weight(1f))
                                    BrandText(if (set.step.restAfter.seconds > 0) "+${set.step.restAfter.seconds}s rest" else set.step.intensity.name.lowercase().replaceFirstChar { it.uppercase() }, 12, color = Muted)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        BrandButton("Start Workout", Modifier.padding(horizontal = 17.dp, vertical = 12.dp), onStart)
    }
}

@Composable private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(CardShape).background(Panel).border(1.dp, StrokeBlue, CardShape).padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandText(value, 15, true, align = TextAlign.Center, maxLines = 1)
        BrandText(label, 12, color = Muted)
    }
}

private fun inRange(record: CompletedWorkout, from: LocalDate, to: LocalDate): Boolean = runCatching {
    val day = LocalDate.parse(record.dateLabel)
    !day.isBefore(from) && !day.isAfter(to)
}.getOrDefault(false)

private fun relativeTime(instant: Instant): String {
    val minutes = java.time.Duration.between(instant, Instant.now()).toMinutes()
    return when { minutes < 1 -> "Just now"; minutes < 60 -> "$minutes min ago"; else -> DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault()).format(instant) }
}

@Composable private fun SwimTimerView(workoutId: String?, onBack: () -> Unit) {
    var running by rememberSaveable { mutableStateOf(true) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    var lengths by rememberSaveable { mutableIntStateOf(0) }
    var pool by rememberSaveable { mutableIntStateOf(25) }
    var startedAt by remember { mutableStateOf(Instant.now()) }
    var confirmStop by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    LaunchedEffect(running) { while (running) { delay(1000); seconds += 1 } }
    if (confirmStop) AlertDialog(
        onDismissRequest = { confirmStop = false },
        title = { Text("Finish swim?") },
        text = { Text(if (repository.email == null) "Sign in to save this swim to OpenSwim cloud." else if (lengths == 0) "Add at least one length to save your swim." else "Save ${lengths * pool} m and ${seconds / 60} min to your account?") },
        confirmButton = {
            TextButton(onClick = {
                if (repository.email != null && lengths > 0 && seconds > 0) repository.saveSwim(workoutId, lengths * pool, seconds, pool, startedAt)
                confirmStop = false; onBack()
            }) { Text(if (repository.email != null && lengths > 0 && seconds > 0) "Save & finish" else "Finish") }
        },
        dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Continue") } }
    )
    if (showOptions) AlertDialog(
        onDismissRequest = { showOptions = false },
        title = { Text("Pool swim") },
        text = {
            Column {
                Text("Pool length: $pool m")
                Row {
                    TextButton(onClick = { pool = 25; lengths = 0; showOptions = false }) { Text("25 m") }
                    TextButton(onClick = { pool = 50; lengths = 0; showOptions = false }) { Text("50 m") }
                }
                Text("Tap + Length after each pool length. Distance is entered manually; no watch or heart rate sensor is connected.")
            }
        },
        confirmButton = { TextButton(onClick = { showOptions = false }) { Text("Done") } }
    )
    Box(Modifier.fillMaxSize().background(Navy)) {
        Image(painterResource(R.drawable.pool_lane), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.22f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xB8031B2E), Navy, Color(0xED031B2E)))))
        Column(Modifier.fillMaxSize().padding(horizontal = 25.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandText("←", 30, color = Aqua, modifier = Modifier.clickable(onClick = { confirmStop = true }))
                BrandText(repository.workout(workoutId.orEmpty())?.name ?: "Pool Swim", 23, true, modifier = Modifier.weight(1f), align = TextAlign.Center)
                BrandText("⌁", 27, color = Aqua, modifier = Modifier.clickable { showOptions = true })
            }
            Spacer(Modifier.weight(.75f))
            BrandText("%02d:%02d".format(seconds / 60, seconds % 60), 74, true, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
            BrandText("Time", 19, color = Muted, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(Navy, Aqua, Navy))))
            Spacer(Modifier.height(34.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { BrandText("${lengths * pool} m", 41, true); BrandText("Distance", 17, color = Muted) }
                Box(Modifier.width(1.dp).height(60.dp).background(StrokeBlue))
                Column(horizontalAlignment = Alignment.CenterHorizontally) { BrandText("$lengths", 41, true); BrandText("Lengths", 17, color = Muted) }
            }
            Spacer(Modifier.height(22.dp))
            BrandText("$pool m pool · manually logged", 14, color = Muted, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            BrandButton("+ Length", onClick = { lengths += 1 })
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SwimControl(if (running) "Ⅱ  Pause" else "▶  Resume", Modifier.weight(1f)) { running = !running }
                SwimControl("■  Finish", Modifier.weight(1f)) { running = false; confirmStop = true }
            }
            Spacer(Modifier.weight(.7f))
        }
    }
}

@Composable private fun SwimControl(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(56.dp).clip(RoundedCornerShape(30.dp)).background(Panel).border(1.dp, StrokeBlue, RoundedCornerShape(30.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { BrandText(label, 16, true) }
}

@Composable private fun ProgressView(onResult: (String) -> Unit) {
    var category by rememberSaveable { mutableStateOf("Overview") }
    var period by rememberSaveable { mutableStateOf("7D") }
    val today = LocalDate.now()
    val start = when (period) { "7D" -> today.minusDays(6); "4W" -> today.minusDays(27); "3M" -> today.minusMonths(3).plusDays(1); else -> today.minusYears(1).plusDays(1) }
    val records = repository.completed.filter { inRange(it, start, today) }
    val distance = records.sumOf { it.distance.amount }
    val totalSeconds = records.sumOf { it.durationMinutes * 60 }
    val pace = if (distance > 0) totalSeconds * 100 / distance else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WaterHeader("Progress", height = 126)
        Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PillRow(listOf("Overview", "Workouts", "Distance", "Pace"), category) { category = it }
            PillRow(listOf("7D", "4W", "3M", "1Y"), period) { period = it }
            when (category) {
                "Workouts" -> {
                    BrandText("${records.size} Workouts", 28, true)
                    if (records.isEmpty()) BrandCard { BrandText("No completed swims in this period", 16, color = Muted) }
                    records.forEach { result ->
                        BrandCard(onClick = { onResult(result.id) }) {
                            BrandText(repository.workout(result.workoutId)?.name ?: "Pool Swim", 18, true)
                            BrandText("${result.dateLabel} · ${result.distance} · ${result.durationMinutes} min", 14, color = Muted)
                        }
                    }
                }
                else -> {
                    BrandText(when (category) { "Pace" -> "Average Pace"; "Distance" -> "Total Distance"; else -> "Total Distance" }, 24, true)
                    BrandText(when (category) { "Pace" -> pace?.let { paceLabel(it) } ?: "—"; else -> "$distance m" }, 43, true)
                    ProgressBars(records, start, today)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard("${records.size}", "Workouts", Modifier.weight(1f))
                        MetricCard(pace?.let(::paceLabel) ?: "—", "Avg pace /100m", Modifier.weight(1f))
                        MetricCard("${totalSeconds / 60} min", "Swim time", Modifier.weight(1f))
                    }
                    BrandText("Distance Breakdown", 22, true)
                    BrandCard {
                        BrandText("$distance m", 26, true, Aqua)
                        BrandText("Total recorded distance. Stroke breakdown will appear when stroke level data is recorded.", 14, color = Muted)
                    }
                }
            }
            repository.error?.let { ErrorNotice(it) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun paceLabel(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

@Composable private fun ProgressBars(records: List<CompletedWorkout>, from: LocalDate, to: LocalDate) {
    val dates = generateSequence(from) { date -> date.plusDays(1).takeUnless { it.isAfter(to) } }.toList()
    val buckets = if (dates.size <= 7) dates.map { day -> records.filter { it.dateLabel == day.toString() }.sumOf { it.distance.amount } }
        else (0..6).map { bucket -> records.filter { result -> runCatching { (java.time.temporal.ChronoUnit.DAYS.between(from, LocalDate.parse(result.dateLabel)).toInt() * 7 / dates.size) == bucket }.getOrDefault(false) }.sumOf { it.distance.amount } }
    val ceiling = max(1, buckets.maxOrNull() ?: 1)
    BrandCard {
        Row(Modifier.fillMaxWidth().height(145.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
            buckets.forEachIndexed { index, amount ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(27.dp).height((8 + 103f * amount / ceiling).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(if (amount > 0) Aqua else Color(0xFF1A465F)))
                    Spacer(Modifier.height(5.dp))
                    BrandText(if (dates.size <= 7) dates[index].dayOfWeek.name.take(1) else "${index + 1}", 11, color = Muted)
                }
            }
        }
    }
}

@Composable private fun ResultView(result: CompletedWorkout, onBack: () -> Unit) {
    SimplePage("Swim Result", onBack) {
        BrandText(repository.workout(result.workoutId)?.name ?: "Pool Swim", 25, true)
        BrandText(result.dateLabel, 15, color = Muted)
        BrandCard { BrandText("${result.distance}", 30, true, Aqua); BrandText("Distance", 15, color = Muted) }
        BrandCard { BrandText("${result.durationMinutes} min", 30, true); BrandText("Duration", 15, color = Muted) }
        result.paceSecondsPer100?.let { BrandCard { BrandText(paceLabel(it), 30, true); BrandText("Pace / 100 m", 15, color = Muted) } }
        result.lengths?.let { BrandCard { BrandText("$it", 30, true); BrandText("Lengths", 15, color = Muted) } }
    }
}

@Composable private fun PlansView(onPlan: (String) -> Unit, onWorkout: (String) -> Unit) {
    var mode by rememberSaveable { mutableStateOf("Upcoming") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WaterHeader("Training Plans", height = 131)
        Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            if (repository.email == null) BrandCard { BrandText("Sign in to see your plans", 19, true); BrandText("Open More to connect your account.", 14, color = Muted) }
            repository.plans.forEach { plan ->
                Box(Modifier.fillMaxWidth().height(166.dp).clip(CardShape).border(1.dp, StrokeBlue, CardShape).clickable { onPlan(plan.id) }) {
                    Image(painterResource(R.drawable.swimmer_card), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xE7031B2D)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(17.dp)) {
                        BrandText(plan.title, 25, true)
                        BrandText("${plan.durationWeeks} weeks · ${plan.workoutsPerWeek} workouts/week", 15, color = Muted)
                        BrandText("${plan.schedule.size} planned sessions  ›", 14, color = Aqua)
                    }
                }
            }
            if (repository.email != null && repository.plans.isEmpty()) BrandCard { BrandText("No training plan yet", 18, true); BrandText("Create a plan on the OpenSwim website and it will sync here.", 14, color = Muted) }
            PillRow(listOf("Upcoming", "Calendar"), mode) { mode = it }
            if (mode == "Upcoming") {
                BrandText("Upcoming Workouts", 22, true)
                val upcoming = repository.scheduled.filter { it.status == "SCHEDULED" && runCatching { !LocalDate.parse(it.date).isBefore(LocalDate.now()) }.getOrDefault(false) }
                if (upcoming.isEmpty()) BrandCard { BrandText("No dated swims scheduled", 16, color = Muted) }
                upcoming.forEach { item ->
                    val workout = item.workoutId?.let(repository::workout)
                    BrandCard(onClick = workout?.let { { onWorkout(it.id) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandText(item.date.takeLast(5), 15, true, Aqua, Modifier.width(70.dp))
                            Column(Modifier.weight(1f)) { BrandText(workout?.name ?: "Pool Swim", 17, true); workout?.let { BrandText("${it.totalDistance} · ~${it.estimatedMinutes} min", 13, color = Muted) } }
                            BrandText("›", 27)
                        }
                    }
                }
            } else {
                BrandText("Calendar", 22, true)
                val grouped = repository.scheduled.groupBy { it.date.take(7) }.toSortedMap()
                if (grouped.isEmpty()) BrandCard { BrandText("No swims on the calendar", 16, color = Muted) }
                grouped.forEach { (month, days) ->
                    BrandText(month, 18, true, Aqua)
                    days.forEach { day ->
                        val workout = day.workoutId?.let(repository::workout)
                        BrandCard(onClick = workout?.let { { onWorkout(it.id) } }) {
                            BrandText("${day.date} · ${workout?.name ?: "Pool Swim"}", 16, true)
                            BrandText(day.status.lowercase().replaceFirstChar { it.uppercase() }, 13, color = Muted)
                        }
                    }
                }
            }
            repository.error?.let { ErrorNotice(it) }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable private fun PlanDetailView(plan: TrainingPlan, onBack: () -> Unit, onWorkout: (String) -> Unit) {
    SimplePage(plan.title, onBack) {
        BrandCard {
            BrandText(plan.title, 25, true)
            BrandText("${plan.durationWeeks} weeks · ${plan.workoutsPerWeek} workouts/week", 15, color = Muted)
            if (plan.objective.isNotBlank()) BrandText(plan.objective, 15, color = Muted)
        }
        plan.schedule.groupBy { it.week }.forEach { (week, entries) ->
            BrandText("Week $week", 22, true)
            entries.forEach { entry ->
                val workout = repository.workout(entry.workoutId)
                BrandCard(onClick = workout?.let { { onWorkout(it.id) } }) {
                    BrandText("${entry.dayLabel} · ${workout?.name ?: "Workout unavailable"}", 17, true)
                    workout?.let { BrandText("${it.totalDistance} · ~${it.estimatedMinutes} min", 14, color = Muted) }
                }
            }
        }
    }
}

@Composable private fun AccountView(onPlans: () -> Unit, onSync: () -> Unit, onSettings: () -> Unit, onHelp: () -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WaterHeader("Account", height = 131)
        Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            if (repository.email == null) {
                BrandCard { BrandText("Welcome to OpenSwim", 22, true); BrandText("Sign in to sync plans and completed swims.", 14, color = Muted) }
                BrandedAuth()
            } else {
                BrandCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Box(Modifier.size(51.dp).clip(CircleShape).background(Aqua), contentAlignment = Alignment.Center) {
                            BrandText((repository.displayName ?: repository.email ?: "S").take(1).uppercase(), 22, true, Navy)
                        }
                        Column {
                            BrandText(repository.displayName ?: repository.email?.substringBefore('@') ?: "Swimmer", 19, true)
                            BrandText(repository.email ?: "", 14, color = Muted)
                        }
                    }
                }
            }
            BrandCard { BrandText("OpenSwim Premium", 19, true); BrandText("Premium features are not available yet.", 14, color = Muted) }
            BrandText("Connected Devices", 22, true)
            BrandCard(onClick = onSync) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandText("◷", 34, color = Aqua, modifier = Modifier.width(56.dp))
                    Column(Modifier.weight(1f)) { BrandText("OpenSwim Watch", 17, true); BrandText("Not connected", 13, color = Muted) }
                    BrandText("›", 28)
                }
            }
            BrandCard {
                AccountLink("Sync & Data", "Last sync: ${repository.lastSyncAt?.let(::relativeTime) ?: "Never"}", onSync)
                HorizontalDivider(color = StrokeBlue)
                AccountLink("My Plans", null, onPlans)
                HorizontalDivider(color = StrokeBlue)
                AccountLink("Settings", null, onSettings)
                HorizontalDivider(color = StrokeBlue)
                AccountLink("Help & Support", null, onHelp)
                HorizontalDivider(color = StrokeBlue)
                AccountLink("About OpenSwim", null, onAbout)
            }
            if (repository.email != null) BrandCard(onClick = { repository.signOut() }) { BrandText("Log Out", 17, true, Aqua) }
            repository.notice?.let { BrandCard { BrandText(it, 14, color = Green) } }
            repository.error?.let { ErrorNotice(it) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable private fun AccountLink(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { BrandText(title, 16, true); if (subtitle != null) BrandText(subtitle, 12, color = Muted) }
        BrandText("›", 25, color = Aqua)
    }
}

@Composable private fun BrandedAuth() {
    var address by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    BrandCard {
        BrandText("Email", 13, color = Muted)
        AuthField(address) { address = it }
        BrandText("Password", 13, color = Muted)
        Box(Modifier.fillMaxWidth()) {
            if (password.isEmpty()) BrandText("Enter password", 15, color = Muted)
            BasicTextField(password, { password = it }, singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 16.sp), modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(5.dp))
        BrandButton("Sign In", onClick = { repository.signIn(address, password) })
        BrandText("Create account", 15, color = Aqua, modifier = Modifier.fillMaxWidth().clickable { repository.signUp(address, password) }.padding(8.dp), align = TextAlign.Center)
    }
}

@Composable private fun AuthField(value: String, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        if (value.isEmpty()) BrandText("you@example.com", 15, color = Muted)
        BasicTextField(value, onChange, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 16.sp), modifier = Modifier.fillMaxWidth())
    }
}

@Composable private fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WaterHeader(title, back = onBack)
        Column(Modifier.padding(horizontal = 17.dp), verticalArrangement = Arrangement.spacedBy(15.dp), content = content)
    }
}

@Composable private fun EmptyView(message: String, onBack: () -> Unit) {
    SimplePage("Unavailable", onBack) { BrandCard { BrandText(message, 18, true) } }
}

@Composable private fun ErrorNotice(message: String) {
    BrandCard(onClick = { repository.refresh() }) { BrandText("Connection issue", 17, true); BrandText(message, 13, color = Muted); BrandText("Tap to retry", 13, color = Aqua) }
}
