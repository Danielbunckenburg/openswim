package org.openswim.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.openswim.core.CompletedWorkout
import org.openswim.core.Equipment
import org.openswim.core.RepetitionSet
import org.openswim.core.ScheduledWorkout
import org.openswim.core.Workout
import org.openswim.core.supportsPoolLength

class MainActivity : ComponentActivity() {
    private var firstResume = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CloudRepository(this)
        repository.acceptAuthRedirect(intent?.data)
        setContent { MaterialTheme { OpenSwimApp() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        repository.acceptAuthRedirect(intent.data)
    }
    override fun onResume() {
        super.onResume()
        if (firstResume) firstResume = false else repository.refresh()
    }
}

private lateinit var repository: CloudRepository

private enum class Tab(val label: String, val mark: String) {
    HOME("Home", "H"), WORKOUTS("Workouts", "W"), PROGRESS("Progress", "P"),
    PLANS("Plans", "L"), ACCOUNT("Account", "A")
}

private enum class Page { WORKOUT_DETAIL, WORKOUT_RESULT, PLAN_DETAIL, DEVICES }
private data class Route(val page: Page, val id: String = "")

@Composable
private fun OpenSwimApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val stack = remember { mutableStateListOf<Route>() }
    fun select(next: Tab) { tab = next; stack.clear() }
    fun open(page: Page, id: String = "") { stack.add(Route(page, id)) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    BackHandler(stack.isNotEmpty()) { pop() }

    Scaffold(bottomBar = {
        NavigationBar {
            Tab.entries.forEach { item ->
                NavigationBarItem(
                    selected = tab == item, onClick = { select(item) },
                    icon = { Text(item.mark) }, label = { Text(item.label) }, alwaysShowLabel = true
                )
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            repository.error?.let { InfoCard("Connection error", it) { repository.refresh() } }
            repository.notice?.let { InfoCard("Account", it) }
            if (repository.busy) Plain("Connecting to OpenSwim cloud…")
            val route = stack.lastOrNull()
            Header(route?.let { pageTitle(it) } ?: tab.label, route != null, onBack = ::pop)
            key(tab, route) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when (route?.page) {
                    Page.WORKOUT_DETAIL -> repository.workout(route.id)?.let { WorkoutDetail(it) }
                        ?: MissingRecord { pop() }
                    Page.WORKOUT_RESULT -> repository.completed.firstOrNull { it.id == route.id }
                        ?.let { WorkoutResult(it) } ?: MissingRecord { pop() }
                    Page.PLAN_DETAIL -> repository.plans.firstOrNull { it.id == route.id }
                        ?.let { PlanDetail(it, onWorkout = { id -> open(Page.WORKOUT_DETAIL, id) }) } ?: MissingRecord { pop() }
                    Page.DEVICES -> Devices()
                    null -> when (tab) {
                        Tab.HOME -> Home(
                            onWorkout = { open(Page.WORKOUT_DETAIL, it) },
                            onTab = ::select,
                            onDevices = { select(Tab.ACCOUNT); open(Page.DEVICES) }
                        )
                        Tab.WORKOUTS -> Workouts { open(Page.WORKOUT_DETAIL, it) }
                        Tab.PROGRESS -> Progress { open(Page.WORKOUT_RESULT, it) }
                        Tab.PLANS -> Plans(
                            onPlan = { open(Page.PLAN_DETAIL, it) },
                            onWorkout = { open(Page.WORKOUT_DETAIL, it) }
                        )
                        Tab.ACCOUNT -> Account { open(Page.DEVICES) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            } }
        }
    }
}

private fun pageTitle(route: Route) = when (route.page) {
    Page.WORKOUT_DETAIL -> "Workout detail"
    Page.WORKOUT_RESULT -> "Workout result"
    Page.PLAN_DETAIL -> "Plan detail"
    Page.DEVICES -> "Connected devices"
}

@Composable private fun Header(title: String, canBack: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (canBack) Text("‹ Back", Modifier.clickable(onClick = onBack))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}

@Composable private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    content()
}

@Composable private fun InfoCard(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun Plain(text: String) { Text(text, style = MaterialTheme.typography.bodyMedium) }
@Composable private fun Reserved(text: String) { Plain("Later: $text") }

@Composable private fun Home(onWorkout: (String) -> Unit, onTab: (Tab) -> Unit, onDevices: () -> Unit) {
    Section("Today's workout") {
        val today = java.time.LocalDate.now().toString()
        val scheduled = repository.scheduled.firstOrNull { it.status == "SCHEDULED" && it.date >= today && it.workoutId != null }
        val workout = scheduled?.workoutId?.let(repository::workout)
        if (scheduled != null && workout != null) InfoCard(workout.name, "${scheduled.date} · ${workout.totalDistance} · ~${workout.estimatedMinutes} min") { onWorkout(workout.id) }
        else Plain(if (repository.email == null) "Sign in to see your plan." else "No workout scheduled yet.")
    }
    Section("Quick actions") {
        InfoCard("Browse workouts", "Find a workout in the library") { onTab(Tab.WORKOUTS) }
        InfoCard("My plan", "See upcoming swims") { onTab(Tab.PLANS) }
        InfoCard("Watch", "See device status") { onDevices() }
    }
    Section("Watch status") { InfoCard("OpenSwim Watch", "Not connected · Last sync: never") { onDevices() } }
    Section("Weekly summary") {
        InfoCard("Your swims", "${repository.completed.size} recorded workouts") { onTab(Tab.PROGRESS) }
    }
}

@Composable private fun Workouts(onWorkout: (String) -> Unit) {
    var category by rememberSaveable { mutableStateOf("All") }
    Plain("Workouts from OpenSwim cloud")
    Section("Categories") {
        listOf("All", "Easy", "Technique", "Aerobic", "Endurance", "Threshold", "Sprint")
            .chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { name -> FilterChip(category == name, onClick = { category = name }, label = { Text(name) }) }
                }
            }
    }
    Section("Workout library") {
        if (repository.workouts.isEmpty() && repository.ready && !repository.busy) Plain("No published workouts yet.")
        repository.workouts.filter { category == "All" || it.type == category }.forEach { workout ->
            val intensity = workout.sections.flatMap { it.sets }.maxOf { it.step.intensity.ordinal }
            val effort = org.openswim.core.Intensity.entries[intensity].name.lowercase()
            InfoCard(workout.name, "${workout.totalDistance} · ~${workout.estimatedMinutes} min · ${workout.type} · up to $effort") { onWorkout(workout.id) }
        }
    }
}

@Composable private fun WorkoutDetail(workout: Workout) {
    Section(workout.name) {
        Plain(workout.sections.joinToString(" · ") { it.name })
        Plain("${workout.totalDistance} · ~${workout.estimatedMinutes} min · ${workout.type}")
        Plain("Fits 25 m pool: ${if (workout.supportsPoolLength(25)) "Yes" else "No"} · 50 m pool: ${if (workout.supportsPoolLength(50)) "Yes" else "No"}")
    }
    workout.sections.forEach { section ->
        Section(section.name) { section.sets.forEach { StepRow(it) } }
    }
    Section("Next actions") {
        Reserved("Send to watch / Start")
        Reserved("Save · Schedule · Share")
    }
}

@Composable private fun StepRow(set: RepetitionSet) {
    val step = set.step
    val parts = buildList {
        add("${set.repetitions} × ${step.distance}")
        add(step.stroke.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
        add(step.intensity.name.lowercase())
        if (step.restAfter.seconds > 0) add("${step.restAfter.seconds}s rest")
        step.targetInterval?.let { add("${it.seconds}s interval") }
        step.targetPace?.let { add("${it.secondsPer100}s / 100 target") }
        if (step.equipment.isNotEmpty()) add(step.equipment.joinToString { it.label() })
    }
    InfoCard(parts.joinToString(" · "), step.note ?: "")
}

private fun Equipment.label() = name.replace('_', ' ').lowercase()

@Composable private fun Progress(onResult: (String) -> Unit) {
    var range by rememberSaveable { mutableStateOf("Week") }
    val today = java.time.LocalDate.now()
    val start = when (range) {
        "Week" -> today.with(java.time.DayOfWeek.MONDAY)
        "Month" -> today.withDayOfMonth(1)
        else -> today.withDayOfYear(1)
    }
    val records = repository.completed.filter { runCatching { java.time.LocalDate.parse(it.dateLabel) >= start }.getOrDefault(false) }
    Section("Time range") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Week", "Month", "Year").forEach { value ->
                FilterChip(range == value, onClick = { range = value }, label = { Text(value) })
            }
        }
    }
    Section("Summary") {
        Plain("Cloud records · $range view")
        Plain("${records.sumOf { it.distance.amount }} m · ${records.size} workout · ${records.sumOf { it.durationMinutes }} min")
        Plain("Average pace: unavailable")
    }
    Section("Trends") { InfoCard("Distance · Pace · Frequency", "Charts will use completed swim records when available") }
    Section("Completed workouts") {
        records.forEach { result ->
            val name = repository.workout(result.workoutId)?.name ?: "Workout"
            InfoCard(name, "${result.dateLabel} · ${result.distance} · ${result.durationMinutes} min") { onResult(result.id) }
        }
    }
}

@Composable private fun WorkoutResult(result: CompletedWorkout) {
    val workout = repository.workout(result.workoutId)
    Section(workout?.name ?: "Swim") {
        if (result.isDemo) Plain("Demo record · local sample data")
        Plain(result.dateLabel)
    }
    Section("Recorded summary") {
        InfoCard("Distance", result.distance.toString())
        InfoCard("Duration", "${result.durationMinutes} min")
        result.paceSecondsPer100?.let { InfoCard("Pace", "$it sec / 100") }
        result.lengths?.let { InfoCard("Lengths", "$it") }
    }
    if (result.heartRateBpm != null || result.swolf != null || result.strokeNotes != null || result.intervalSplits.isNotEmpty()) {
        Section("Additional recorded data") {
            result.heartRateBpm?.let { Plain("Heart rate: $it bpm") }
            result.swolf?.let { Plain("SWOLF: $it") }
            result.strokeNotes?.let { Plain("Stroke: $it") }
            result.intervalSplits.forEach { Plain(it) }
        }
    }
}

@Composable private fun Plans(onPlan: (String) -> Unit, onWorkout: (String) -> Unit) {
    var view by rememberSaveable { mutableStateOf("Upcoming") }
    val plans = repository.plans
    if (repository.email == null) {
        Section("My plans") { Plain("Sign in to see your plans and scheduled swims.") }
        return
    }
    Section("My plans") {
        if (plans.isEmpty()) Plain("No plan created yet. Create one on the OpenSwim website and refresh here.")
        plans.forEach { plan ->
            InfoCard(plan.title, "${plan.durationWeeks} weeks · ${plan.workoutsPerWeek} swims/week · ${plan.schedule.size} sessions") { onPlan(plan.id) }
        }
    }
    Section("Dated swims") {
        val dates = repository.scheduled.filter { it.status == "SCHEDULED" }
        if (dates.isEmpty()) Plain("No swims scheduled on a date yet.")
        dates.forEach { item ->
            val workout = item.workoutId?.let(repository::workout)
            InfoCard(workout?.name ?: "Swim", "${item.date} · ${item.status.lowercase()}") {
                if (workout != null) onWorkout(workout.id)
            }
        }
    }
    plans.firstOrNull()?.let { plan -> Section("Latest plan schedule") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Upcoming", "Calendar").forEach { item ->
                FilterChip(view == item, onClick = { view = item }, label = { Text(item) })
            }
        }
        if (view == "Upcoming") {
            plan.schedule.filter { it.week == 1 }.forEach { ScheduledRow(it, onWorkout) }
        } else {
            plan.schedule.groupBy { it.week }.forEach { (week, days) ->
                Text("Week $week", fontWeight = FontWeight.SemiBold)
                days.forEach { ScheduledRow(it, onWorkout) }
            }
        }
    } }
}

@Composable private fun PlanDetail(plan: org.openswim.core.TrainingPlan, onWorkout: (String) -> Unit) {
    Section(plan.title) {
        Plain(plan.objective)
        Plain("${plan.durationWeeks} weeks · ${plan.workoutsPerWeek} swims/week · ${plan.difficulty}")
        Plain("${plan.schedule.size} planned sessions")
    }
    plan.schedule.groupBy { it.week }.forEach { (week, days) ->
        Section("Week $week") { days.forEach { ScheduledRow(it, onWorkout) } }
    }
}

@Composable private fun ScheduledRow(item: ScheduledWorkout, onWorkout: (String) -> Unit) {
    val workout = repository.workout(item.workoutId) ?: return
    InfoCard("${item.dayLabel} · ${workout.name}", "${workout.totalDistance} · ${item.state}") { onWorkout(workout.id) }
}

@Composable private fun Account(onDevices: () -> Unit) {
    Section("Profile") {
        if (repository.email == null) AuthForm()
        else {
            InfoCard(repository.email ?: "Account", "Signed in to OpenSwim cloud")
            Button(onClick = { repository.signOut() }, enabled = !repository.busy) { Text("Sign out") }
        }
    }
    Section("Connected devices") { InfoCard("OpenSwim Watch", "Not connected · Last sync: never") { onDevices() } }
    Section("Sync & data") {
        InfoCard("OpenSwim cloud", "${repository.workouts.size} workouts · ${repository.plans.size} plans · ${repository.scheduled.size} scheduled swims · ${repository.completed.size} completed swims")
        Button(onClick = { repository.refresh() }, enabled = !repository.busy) { Text("Refresh now") }
        Plain("Data refreshes when you open the app and after sign in.")
        Reserved("Export")
    }
    Section("Settings") { InfoCard("Preferences", "Units: meters · Default pool: 25 m · Notifications: not configured") }
    Section("About & support") { InfoCard("OpenSwim", "GitHub · Help · Privacy (future links)") }
}

@Composable private fun AuthForm() {
    var address by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(address, onValueChange = { address = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { repository.signIn(address, password) }, enabled = !repository.busy && address.isNotBlank() && password.isNotBlank()) { Text("Sign in") }
        Button(onClick = { repository.signUp(address, password) }, enabled = !repository.busy && address.isNotBlank() && password.length >= 6) { Text("Create account") }
    }
}

@Composable private fun Devices() {
    Section("OpenSwim Watch") {
        InfoCard("Not connected", "Last sync: never")
        Plain("Device pairing and synchronization will be added in a later phase.")
    }
}

@Composable private fun MissingRecord(onBack: () -> Unit) {
    Section("Unavailable") {
        Plain("This local item could not be found.")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}
