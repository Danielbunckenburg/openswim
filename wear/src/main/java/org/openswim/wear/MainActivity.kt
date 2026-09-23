package org.openswim.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private enum class Page { WORKOUTS, DETAIL, READY, SESSION }
private data class Pool(val label: String, val length: Int, val unit: DistanceUnit)
private val pools = listOf(
    Pool("25 m", 25, DistanceUnit.METERS),
    Pool("50 m", 50, DistanceUnit.METERS),
    Pool("25 yd", 25, DistanceUnit.YARDS)
)

@Composable
private fun OpenSwimApp() {
    var page by remember { mutableStateOf(Page.WORKOUTS) }
    var workout by remember { mutableStateOf(SampleWorkouts.all.first()) }
    var pool by remember { mutableStateOf(pools.first()) }
    var session by remember { mutableStateOf(WorkoutSession(workout)) }
    var confirmingEnd by remember { mutableStateOf(false) }
    val ticking = session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest

    BackHandler(page != Page.WORKOUTS) {
        when (page) {
            Page.DETAIL -> page = Page.WORKOUTS
            Page.READY -> page = Page.DETAIL
            Page.SESSION -> if (session.phase == SessionPhase.Complete) page = Page.WORKOUTS else session = session.pause()
            else -> Unit
        }
    }
    LaunchedEffect(page, ticking) {
        while (page == Page.SESSION && (session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest)) {
            delay(1000)
            session = session.tick()
        }
    }

    Crossfade(targetState = page, animationSpec = tween(180), label = "screen") { visiblePage ->
        when (visiblePage) {
            Page.WORKOUTS -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("≈", color = cyan, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(5.dp))
                    Text("OpenSwim", color = white, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                }
                Spacer(Modifier.height(2.dp))
                Eyebrow("YOUR WORKOUTS", secondary)
                Spacer(Modifier.height(13.dp))
                SampleWorkouts.all.forEach { item ->
                    WorkoutCard(item) { workout = item; page = Page.DETAIL }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Page.DETAIL -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Eyebrow(workout.type, cyan)
                Spacer(Modifier.height(3.dp))
                Heading(workout.displayName())
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(workout.totalDistance.toString(), color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("~${workout.estimatedMinutes} min", color = secondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(12.dp))
                WideButton("START SWIM", true) { page = Page.READY }
                Spacer(Modifier.height(15.dp))
                workout.sections.forEach { section ->
                    SectionCard(section)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Page.READY -> ScrollPage {
                Spacer(Modifier.height(14.dp))
                Eyebrow("BEFORE YOU SWIM", cyan)
                Spacer(Modifier.height(4.dp))
                Heading(workout.displayName())
                Text(workout.totalDistance.toString(), color = white, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Eyebrow("POOL LENGTH", secondary)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    pools.forEach { choice ->
                        PoolChip(choice.label, pool == choice, Modifier.weight(1f)) { pool = choice }
                    }
                }
                Spacer(Modifier.height(5.dp))
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
                SessionPhase.Complete -> CompleteScreen(session) { page = Page.WORKOUTS }
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
        Eyebrow(workout.type, cyan)
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
        }
    }
}

@Composable
private fun ActiveScreen(session: WorkoutSession, onDone: () -> Unit, onPause: () -> Unit) {
    val current = session.current ?: return
    FixedPage(bottomPadding = 30.dp) {
        Status("SWIM  ·  ${current.section.name.uppercase()}", cyan)
        Spacer(Modifier.weight(0.55f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(current.set.step.distance.amount.toString(), color = white, fontSize = 63.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 66.sp, letterSpacing = (-2).sp)
            Spacer(Modifier.width(5.dp))
            Text(current.set.step.distance.unit.symbol, color = cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 9.dp))
        }
        Text(current.set.step.stroke.display().uppercase(), color = white, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, textAlign = TextAlign.Center, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("REP", secondary)
            Spacer(Modifier.width(6.dp))
            Text("${current.position.repetition + 1} / ${current.set.repetitions}", color = cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(0.45f))
        ThinProgress(session.completedDistance.toFloat() / session.workout.totalDistance.amount)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${session.completedDistance} / ${session.workout.totalDistance}", color = secondary, fontSize = 11.sp)
            Text(time(session.elapsedSeconds), color = secondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        ActionRow("DONE", "Ⅱ", onDone, onPause)
    }
}

@Composable
private fun RestScreen(session: WorkoutSession, phase: SessionPhase.Rest, onSkip: () -> Unit, onPause: () -> Unit) {
    val next = session.current
    val originalRest = session.workout.instructions().getOrNull(session.instructionIndex - 1)?.set?.step?.restAfter?.seconds ?: phase.remainingSeconds
    FixedPage(bottomPadding = 30.dp) {
        Status("REST", amber)
        Spacer(Modifier.weight(0.35f))
        RestDial(phase.remainingSeconds, originalRest)
        Spacer(Modifier.weight(0.25f))
        Eyebrow("UP NEXT", secondary)
        if (next != null) Text(
            "${next.set.step.distance} ${next.set.step.stroke.display()}",
            color = white, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1
        )
        Spacer(Modifier.weight(0.4f))
        ActionRow("SKIP", "Ⅱ", onSkip, onPause)
    }
}

@Composable
private fun RestDial(remaining: Int, total: Int) {
    val fraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = LinearEasing), label = "rest countdown"
    )
    Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(outline, 0f, 360f, false, Offset(inset, inset), arcSize, style = DrawStroke(stroke))
            drawArc(amber, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = DrawStroke(stroke, cap = StrokeCap.Round))
        }
        Text(time(remaining), color = white, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
    }
}

@Composable
private fun PauseScreen(session: WorkoutSession, confirmingEnd: Boolean, onResume: () -> Unit, onEnd: () -> Unit, onConfirm: () -> Unit) {
    if (confirmingEnd) {
        FixedPage(bottomPadding = 24.dp) {
            Status("END WORKOUT?", amber)
            Spacer(Modifier.weight(0.7f))
            Heading("Finish now?")
            Text("${session.completedDistance} ${session.workout.totalDistance.unit.symbol} completed", color = secondary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            WideButton("KEEP SWIMMING", true, onClick = onResume)
            Spacer(Modifier.height(7.dp))
            WideButton("CONFIRM END", false, onClick = onConfirm)
        }
    } else {
        FixedPage(bottomPadding = 24.dp) {
            Status("PAUSED", amber)
            Spacer(Modifier.weight(0.6f))
            Text("On hold", color = white, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(time(session.elapsedSeconds), color = white, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Text("${session.completedDistance} ${session.workout.totalDistance.unit.symbol} completed", color = secondary, fontSize = 13.sp)
            Spacer(Modifier.weight(0.8f))
            WideButton("RESUME", true, onClick = onResume)
            Spacer(Modifier.height(7.dp))
            WideButton("END WORKOUT", false, onClick = onEnd)
        }
    }
}

@Composable
private fun CompleteScreen(session: WorkoutSession, onBack: () -> Unit) {
    val finished = session.completedRepetitions == session.workout.totalRepetitions
    ScrollPage {
        Spacer(Modifier.height(12.dp))
        Status(if (finished) "COMPLETE" else "ENDED", cyan)
        Spacer(Modifier.height(3.dp))
        Heading(session.workout.displayName())
        Spacer(Modifier.height(1.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(session.completedDistance.toString(), color = white, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
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
        Spacer(Modifier.height(8.dp))
        SummaryExit(onBack)
    }
}

@Composable
private fun SummaryExit(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
        Box(
            Modifier.width(82.dp).height(42.dp).clip(RoundedCornerShape(21.dp))
                .background(cyanDark).border(1.dp, outline, RoundedCornerShape(21.dp))
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text("WORKOUTS", color = white, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(13.dp)).background(panel).border(1.dp, outline, RoundedCornerShape(13.dp)).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Eyebrow(label, secondary)
        Text(value, color = white, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FixedPage(bottomPadding: Dp = 15.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(background).padding(start = 24.dp, end = 24.dp, top = 15.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun Status(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Eyebrow(text, color)
    }
}

@Composable
private fun Eyebrow(text: String, color: Color) {
    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
private fun Heading(text: String) {
    Text(text, color = white, fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 28.sp)
}

@Composable
private fun ThinProgress(progress: Float) {
    val fraction by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(300), label = "workout progress")
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(outline)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(cyan))
    }
}

@Composable
private fun PoolChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) cyan else panel, label = "pool selection")
    Box(
        modifier.height(43.dp).clip(RoundedCornerShape(14.dp)).background(color)
            .border(1.dp, if (selected) cyan else outline, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) background else white, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun WideButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth(0.88f).height(43.dp).clip(RoundedCornerShape(22.dp))
            .background(if (primary) cyan else cyanDark)
            .border(1.dp, if (primary) cyan else outline, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) background else white, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
private fun ActionRow(primaryText: String, secondaryText: String, onPrimary: () -> Unit, onSecondary: () -> Unit) {
    Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.weight(1f).height(45.dp).clip(RoundedCornerShape(23.dp)).background(cyan)
                .clickable(role = Role.Button, onClick = onPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(primaryText, color = background, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp)
        }
        Box(
            Modifier.width(48.dp).height(45.dp).clip(RoundedCornerShape(23.dp)).background(cyanDark)
                .border(1.dp, outline, RoundedCornerShape(23.dp))
                .clickable(role = Role.Button, onClickLabel = "Pause workout", onClick = onSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(secondaryText, color = white, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Stroke.display() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun Workout.displayName() = name.removeSuffix(" ${totalDistance.amount}")
private fun time(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)
