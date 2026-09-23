package org.openswim.wear

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import org.openswim.core.*

private val background = Color(0xFF02080D)
private val panel = Color(0xFF0D1B24)
private val panelTop = Color(0xFF122530)
private val outline = Color(0xFF29414C)
private val cyan = Color(0xFF67DEFA)
private val cyanDark = Color(0xFF122D39)
private val white = Color(0xFFF5FBFF)
private val secondary = Color(0xFFA7BBC5)
private val amber = Color(0xFFFFC878)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenSwimApp() }
    }
}

private enum class Page { HOME, LIBRARY, DETAIL, POOL, READY, SESSION }
private data class Pool(val label: String, val length: Int, val unit: DistanceUnit)
private val pools = listOf(
    Pool("25 m", 25, DistanceUnit.METERS),
    Pool("50 m", 50, DistanceUnit.METERS)
)

private data class SavedFlow(val page: Page, val workout: Workout, val pool: Pool,
                             val session: WorkoutSession, val category: String, val detailOrigin: Page)

private fun restoreFlow(raw: String?): SavedFlow {
    val fallback = SampleWorkouts.all.first()
    return runCatching {
        val parts = raw!!.split('|')
        val workout = SampleWorkouts.all.first { it.id == parts[1] }
        val pool = pools.first { it.length == parts[2].toInt() }
        val remaining = parts[9].toInt()
        val phase: SessionPhase = when (parts[8]) {
            "A" -> SessionPhase.Active
            "R" -> SessionPhase.Rest(remaining)
            "PA" -> SessionPhase.Paused(SessionPhase.Active)
            "PR" -> SessionPhase.Paused(SessionPhase.Rest(remaining))
            "C" -> SessionPhase.Complete
            else -> SessionPhase.NotStarted
        }
        val session = WorkoutSession(workout, phase, parts[4].toInt(), parts[5].toInt(),
            parts[6].toInt(), parts[7].toInt())
        require(session.instructionIndex in 0..workout.totalRepetitions)
        require(session.completedRepetitions == session.instructionIndex)
        require(session.completedDistance in 0..workout.totalDistance.amount)
        SavedFlow(Page.valueOf(parts[0]), workout, pool, session, parts[3],
            parts.getOrNull(10)?.let(Page::valueOf) ?: Page.LIBRARY)
    }.getOrElse { SavedFlow(Page.HOME, fallback, pools.first(), WorkoutSession(fallback), "All", Page.LIBRARY) }
}

private fun saveFlow(page: Page, workout: Workout, pool: Pool, session: WorkoutSession,
                     category: String, detailOrigin: Page): String {
    val phase = when (val state = session.phase) {
        SessionPhase.Active -> "A"
        is SessionPhase.Rest -> "R"
        is SessionPhase.Paused -> if (state.previous is SessionPhase.Rest) "PR" else "PA"
        SessionPhase.Complete -> "C"
        else -> "N"
    }
    val remaining = when (val state = session.phase) {
        is SessionPhase.Rest -> state.remainingSeconds
        is SessionPhase.Paused -> (state.previous as? SessionPhase.Rest)?.remainingSeconds ?: 0
        else -> 0
    }
    return listOf(page.name, workout.id, pool.length, category, session.instructionIndex,
        session.completedDistance, session.completedRepetitions, session.elapsedSeconds, phase,
        remaining, detailOrigin.name).joinToString("|")
}

@Composable
private fun OpenSwimApp() {
    val preferences = LocalContext.current.applicationContext.getSharedPreferences("open_swim_flow", 0)
    val saved = remember { restoreFlow(preferences.getString("snapshot", null)) }
    var page by remember { mutableStateOf(saved.page) }
    var workout by remember { mutableStateOf(saved.workout) }
    var pool by remember { mutableStateOf(saved.pool) }
    var session by remember { mutableStateOf(saved.session) }
    var confirmingEnd by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(saved.category) }
    var detailOrigin by remember { mutableStateOf(saved.detailOrigin) }
    val ticking = session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest
    LaunchedEffect(page, workout, pool, session, category, detailOrigin) {
        preferences.edit().putString("snapshot", saveFlow(page, workout, pool, session, category, detailOrigin)).apply()
    }
    LaunchedEffect(workout.id) {
        if (!workout.supportsPoolLength(pool.length)) pool = pools.first()
    }

    BackHandler(page != Page.HOME) {
        when (page) {
            Page.LIBRARY -> page = Page.HOME
            Page.DETAIL -> page = detailOrigin
            Page.POOL -> page = Page.DETAIL
            Page.READY -> page = Page.POOL
            Page.SESSION -> when {
                session.phase == SessionPhase.Complete -> page = Page.HOME
                confirmingEnd -> confirmingEnd = false
                else -> session = session.pause()
            }
            else -> Unit
        }
    }
    LaunchedEffect(page, ticking) {
        var lastSecond = SystemClock.elapsedRealtime()
        while (page == Page.SESSION && (session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest)) {
            delay(250)
            val now = SystemClock.elapsedRealtime()
            val wholeSeconds = ((now - lastSecond) / 1000).toInt()
            repeat(wholeSeconds) { session = session.tick() }
            lastSecond += wholeSeconds * 1000L
        }
    }

    Crossfade(targetState = page, animationSpec = tween(180), label = "screen") { visiblePage ->
        when (visiblePage) {
            Page.HOME -> ScrollPage {
                Spacer(Modifier.height(18.dp))
                Eyebrow("OPEN / SWIM", cyan)
                Spacer(Modifier.height(5.dp))
                Heading("Let's swim")
                Spacer(Modifier.height(3.dp))
                Text("Pool-ready plans.", color = secondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                WideButton("BROWSE WORKOUTS", true) { page = Page.LIBRARY }
                Spacer(Modifier.height(14.dp))
                WideButton("EASY RESET · 1000m", false) {
                    workout = SampleWorkouts.all.first()
                    session = WorkoutSession(workout)
                    pool = pools.first()
                    detailOrigin = Page.HOME
                    page = Page.DETAIL
                }
            }
            Page.LIBRARY -> ScrollPage {
                Spacer(Modifier.height(16.dp))
                Eyebrow("LOCAL LIBRARY", cyan)
                Spacer(Modifier.height(4.dp))
                Heading("Workouts")
                Spacer(Modifier.height(11.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("All", "Easy", "Technique", "Aerobic", "Endurance", "Threshold", "Sprint").forEach { option ->
                        FilterChip(option, category == option) { category = option }
                    }
                }
                Spacer(Modifier.height(11.dp))
                SampleWorkouts.all.filter { category == "All" || it.type == category }.forEach { item ->
                    WorkoutCard(item) {
                        workout = item
                        session = WorkoutSession(item)
                        pool = pools.first()
                        detailOrigin = Page.LIBRARY
                        page = Page.DETAIL
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Page.DETAIL -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Eyebrow(workout.type.uppercase(), cyan)
                Spacer(Modifier.height(3.dp))
                Heading(workout.displayName())
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(workout.totalDistance.toString(), color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("~${workout.estimatedMinutes} min", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(12.dp))
                WideButton("CHOOSE POOL", true) { page = Page.POOL }
                Spacer(Modifier.height(15.dp))
                workout.sections.forEach { section ->
                    SectionCard(section)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Page.POOL -> ScrollPage {
                Spacer(Modifier.height(10.dp))
                Eyebrow("POOL SETUP", cyan)
                Spacer(Modifier.height(3.dp))
                Heading("Pool size")
                Spacer(Modifier.height(3.dp))
                pools.forEach { choice ->
                    val supported = workout.supportsPoolLength(choice.length)
                    PoolOption(choice, pool == choice, supported) { pool = choice }
                    Spacer(Modifier.height(3.dp))
                }
                Spacer(Modifier.height(3.dp))
                WideButton("CONTINUE", true) { page = Page.READY }
            }
            Page.READY -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Eyebrow("READY TO SWIM", cyan)
                Spacer(Modifier.height(5.dp))
                Heading(workout.displayName())
                Text(workout.totalDistance.toString(), color = white, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text("${pool.label} pool  •  ${workout.totalDistance.amount / pool.length} lengths", color = secondary, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                val first = workout.instructions().first()
                Text("FIRST  ${first.set.step.distance} ${first.set.step.stroke.display()}", color = cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                WideButton("START WORKOUT", true) {
                    session = WorkoutSession(workout).start()
                    confirmingEnd = false
                    page = Page.SESSION
                }
            }
            Page.SESSION -> when (val phase = session.phase) {
                SessionPhase.Active -> ActiveScreen(session, onDone = { session = session.advance() }, onPause = { session = session.pause() })
                is SessionPhase.Rest -> RestScreen(
                    session,
                    phase,
                    onSkip = { session = session.skipRest() },
                    onPause = { session = session.pause() }
                )
                is SessionPhase.Paused -> PauseScreen(
                    session,
                    confirmingEnd,
                    onResume = { confirmingEnd = false; session = session.resume() },
                    onEnd = { confirmingEnd = true },
                    onConfirm = { confirmingEnd = false; session = session.end() }
                )
                SessionPhase.Complete -> CompleteScreen(session) { page = Page.HOME }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(background).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            content()
            Spacer(Modifier.height(24.dp))
        }
    )
}

@Composable
private fun WorkoutCard(workout: Workout, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(panelTop, panel)))
            .border(BorderStroke(1.dp, outline), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Eyebrow(workout.type.uppercase(), cyan)
        Spacer(Modifier.height(4.dp))
        Text(workout.displayName(), color = white, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(workout.totalDistance.toString(), color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("~${workout.estimatedMinutes} MIN", color = secondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.clip(shape).background(if (selected) cyan else panel)
            .border(1.dp, if (selected) cyan else outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) background else white, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PoolOption(choice: Pool, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(shape)
            .background(if (selected) cyanDark else panel)
            .border(1.dp, if (selected) cyan else outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(choice.label, color = if (enabled) white else secondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(if (!enabled) "25 m ONLY" else if (selected) "SELECTED" else "SELECT",
            color = if (selected) cyan else secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(section: WorkoutSection) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(panel)
            .border(1.dp, outline, RoundedCornerShape(17.dp)).padding(13.dp)
    ) {
        Eyebrow(section.name.uppercase(), cyan)
        Spacer(Modifier.height(8.dp))
        section.sets.forEachIndexed { index, set ->
            if (index > 0) Spacer(Modifier.height(9.dp))
            Text(
                "${if (set.repetitions > 1) "${set.repetitions} × " else ""}${set.step.distance}",
                color = white, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp
            )
            Text(
                buildString {
                    append(set.step.stroke.display())
                    if (set.step.restAfter.seconds > 0) append("  ·  ${set.step.restAfter.seconds}s rest")
                },
                color = secondary, fontSize = 11.sp, lineHeight = 13.sp
            )
            set.step.note?.let {
                Text(it, color = secondary, fontSize = 11.sp, lineHeight = 13.sp)
            }
            if (set.step.equipment.isNotEmpty()) {
                Text(set.step.equipment.joinToString { it.name.lowercase().replace('_', ' ') },
                    color = cyan, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ActiveScreen(session: WorkoutSession, onDone: () -> Unit, onPause: () -> Unit) {
    val current = session.current ?: return
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    FixedPage(bottomPadding = if (compact) 14.dp else 30.dp) {
        Status(if (compact) "SWIM" else "SWIM  ·  ${current.section.name.uppercase()}", cyan)
        Spacer(Modifier.weight(0.35f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(current.set.step.distance.amount.toString(), color = white,
                fontSize = if (compact) 46.sp else 63.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = if (compact) 49.sp else 66.sp, letterSpacing = (-2).sp)
            Spacer(Modifier.width(5.dp))
            Text(current.set.step.distance.unit.symbol, color = cyan,
                fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (compact) 5.dp else 9.dp))
        }
        Text(current.set.step.stroke.display().uppercase(), color = white,
            fontSize = if (compact) 17.sp else 21.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp, textAlign = TextAlign.Center, maxLines = 1)
        val cue = current.set.step.equipment.takeIf { it.isNotEmpty() }
            ?.joinToString { it.name.lowercase().replace('_', ' ') } ?: current.set.step.note
        cue?.let { Text(it, color = secondary, fontSize = if (compact) 10.sp else 11.sp,
            textAlign = TextAlign.Center, maxLines = 1) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("REP", secondary)
            Spacer(Modifier.width(6.dp))
            Text("${current.position.repetition + 1} / ${current.set.repetitions}", color = cyan,
                fontSize = if (compact) 15.sp else 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(0.35f))
        ThinProgress(session.completedDistance.toFloat() / session.workout.totalDistance.amount)
        Spacer(Modifier.height(if (compact) 2.dp else 5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${session.completedDistance} / ${session.workout.totalDistance}", color = secondary, fontSize = 11.sp)
            Text(time(session.elapsedSeconds), color = secondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
        ActionRow("REP DONE", "Ⅱ", onDone, onPause)
    }
}

@Composable
private fun RestScreen(session: WorkoutSession, phase: SessionPhase.Rest, onSkip: () -> Unit, onPause: () -> Unit) {
    val next = session.current
    val originalRest = session.workout.instructions().getOrNull(session.instructionIndex - 1)?.set?.step?.restAfter?.seconds ?: phase.remainingSeconds
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    FixedPage(bottomPadding = if (compact) 14.dp else 30.dp) {
        Status("REST", amber)
        Spacer(Modifier.weight(0.35f))
        RestDial(phase.remainingSeconds, originalRest, compact)
        Spacer(Modifier.weight(0.25f))
        Eyebrow("UP NEXT  ·  REP ${(next?.position?.repetition ?: 0) + 1}/${next?.set?.repetitions ?: 1}", secondary)
        if (next != null) Text(
            "${next.set.step.distance} ${next.set.step.stroke.display()}",
            color = white, fontSize = if (compact) 15.sp else 18.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1
        )
        next?.set?.step?.note?.let { Text(it, color = secondary, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1) }
        Spacer(Modifier.weight(0.4f))
        ActionRow("SKIP", "Ⅱ", onSkip, onPause)
    }
}

@Composable
private fun RestDial(remaining: Int, total: Int, compact: Boolean) {
    val fraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = LinearEasing), label = "rest countdown"
    )
    Box(Modifier.size(if (compact) 84.dp else 116.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(outline, 0f, 360f, false, Offset(inset, inset), arcSize, style = DrawStroke(stroke))
            drawArc(amber, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = DrawStroke(stroke, cap = StrokeCap.Round))
        }
        Text(time(remaining), color = white, fontSize = if (compact) 30.sp else 42.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun PauseScreen(session: WorkoutSession, confirmingEnd: Boolean, onResume: () -> Unit, onEnd: () -> Unit, onConfirm: () -> Unit) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    if (confirmingEnd) {
        FixedPage(bottomPadding = if (compact) 14.dp else 24.dp) {
            Status(if (compact) "END?" else "END WORKOUT?", amber)
            Spacer(Modifier.weight(0.7f))
            Heading("Finish now?")
            Text("${session.completedDistance} ${session.workout.totalDistance.unit.symbol} completed", color = secondary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            WideButton("KEEP SWIMMING", true, onClick = onResume)
            Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
            WideButton("CONFIRM END", false, onClick = onConfirm)
        }
    } else {
        FixedPage(bottomPadding = if (compact) 14.dp else 24.dp) {
            Status("PAUSED", amber)
            Spacer(Modifier.weight(0.6f))
            Text("On hold", color = white, fontSize = if (compact) 24.sp else 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(time(session.elapsedSeconds), color = white, fontSize = if (compact) 35.sp else 42.sp, fontWeight = FontWeight.ExtraBold)
            Text("${session.completedDistance} ${session.workout.totalDistance.unit.symbol} completed", color = secondary, fontSize = 13.sp)
            Spacer(Modifier.weight(0.8f))
            WideButton("RESUME", true, onClick = onResume)
            Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
            WideButton("END WORKOUT", false, onClick = onEnd)
        }
    }
}

@Composable
private fun CompleteScreen(session: WorkoutSession, onBack: () -> Unit) {
    val finished = session.completedRepetitions == session.workout.totalRepetitions
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    ScrollPage {
        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
        Status(if (finished) "COMPLETE" else "ENDED", cyan)
        Spacer(Modifier.height(3.dp))
        Heading(session.workout.displayName())
        Spacer(Modifier.height(1.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(session.completedDistance.toString(), color = white,
                fontSize = if (compact) 38.sp else 44.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
            Spacer(Modifier.width(5.dp))
            Text(session.workout.totalDistance.unit.symbol, color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        }
        Eyebrow("OF ${session.workout.totalDistance} PLANNED", secondary)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MetricTile("TIME", time(session.elapsedSeconds), Modifier.weight(1f))
            MetricTile("REPS", "${session.completedRepetitions}/${session.workout.totalRepetitions}", Modifier.weight(1f))
            MetricTile("SETS", "${session.completedSets}/${session.workout.sections.sumOf { it.sets.size }}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        SummaryExit(onBack)
    }
}

@Composable
private fun SummaryExit(onClick: () -> Unit) {
    WideButton("BACK TO HOME", true, onClick)
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).background(panel)
            .border(1.dp, outline, RoundedCornerShape(13.dp))
            .padding(vertical = if (compact) 2.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Eyebrow(label, secondary)
        Text(value, color = white, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FixedPage(bottomPadding: Dp = 15.dp, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    Column(
        Modifier.fillMaxSize().background(background).padding(start = 24.dp, end = 24.dp,
            top = if (compact) 8.dp else 15.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun Status(text: String, color: Color) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!compact) {
            Box(Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(6.dp))
        }
        Eyebrow(text, color)
    }
}

@Composable
private fun Eyebrow(text: String, color: Color) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    Text(text, color = color, fontSize = if (compact) 9.sp else 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = if (compact) 0.7.sp else 1.2.sp,
        textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
private fun Heading(text: String) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    Text(text, color = white, fontSize = if (compact) 22.sp else 25.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        lineHeight = if (compact) 25.sp else 28.sp)
}

@Composable
private fun ThinProgress(progress: Float) {
    val fraction by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(300), label = "workout progress")
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(outline)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(cyan))
    }
}

@Composable
private fun WideButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    Box(
        Modifier.fillMaxWidth(0.88f).height(if (compact) 38.dp else 43.dp).clip(RoundedCornerShape(22.dp))
            .background(if (primary) cyan else cyanDark)
            .border(1.dp, if (primary) cyan else outline, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) background else white,
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
private fun ActionRow(primaryText: String, secondaryText: String, onPrimary: () -> Unit, onSecondary: () -> Unit) {
    val compact = LocalConfiguration.current.screenHeightDp <= 200
    val height = if (compact) 38.dp else 45.dp
    Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.weight(1f).height(height).clip(RoundedCornerShape(23.dp)).background(cyan)
                .clickable(role = Role.Button, onClick = onPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(primaryText, color = background, fontSize = if (compact) 12.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp)
        }
        Box(
            Modifier.width(if (compact) 40.dp else 48.dp).height(height)
                .clip(RoundedCornerShape(23.dp)).background(cyanDark)
                .border(1.dp, outline, RoundedCornerShape(23.dp))
                .clickable(role = Role.Button, onClickLabel = "Pause workout", onClick = onSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(secondaryText, color = white, fontSize = if (compact) 17.sp else 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Stroke.display() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun Workout.displayName() = name.removeSuffix(" ${totalDistance.amount}")
private fun time(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)
