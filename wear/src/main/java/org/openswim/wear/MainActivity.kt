package org.openswim.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import org.openswim.core.*

private val navy = Color(0xFF07131B)
private val surface = Color(0xFF142833)
private val aqua = Color(0xFF65E5D1)
private val muted = Color(0xFF9FB4BF)
private val white = Color(0xFFF5FBFC)
private val amber = Color(0xFFFFCF84)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenSwimApp() }
    }
}

private enum class Page { LIST, DETAIL, READY, SESSION }
private data class Pool(val label: String, val length: Int, val unit: DistanceUnit)
private val pools = listOf(Pool("25 m", 25, DistanceUnit.METERS), Pool("50 m", 50, DistanceUnit.METERS), Pool("25 yd", 25, DistanceUnit.YARDS))

@Composable private fun OpenSwimApp() {
    var page by remember { mutableStateOf(Page.LIST) }
    var selected by remember { mutableStateOf(SampleWorkouts.all.first()) }
    var pool by remember { mutableStateOf(pools.first()) }
    var session by remember { mutableStateOf(WorkoutSession(selected)) }
    var confirmingEnd by remember { mutableStateOf(false) }
    BackHandler(page != Page.LIST) {
        when (page) {
            Page.DETAIL -> page = Page.LIST
            Page.READY -> page = Page.DETAIL
            Page.SESSION -> { if (session.phase == SessionPhase.Complete) page = Page.LIST else session = session.pause() }
            else -> Unit
        }
    }
    LaunchedEffect(page, session.phase) {
        while (page == Page.SESSION && (session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest)) {
            delay(1000)
            session = session.tick()
        }
    }
    key(page, session.phase::class) { Column(
        Modifier.fillMaxSize().background(navy).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        when (page) {
            Page.LIST -> {
                Label("SWIM WITH PURPOSE", aqua)
                Spacer(Modifier.height(7.dp))
                Title("OpenSwim")
                Caption("Choose your session")
                Spacer(Modifier.height(14.dp))
                SampleWorkouts.all.forEach { workout ->
                    Card(Modifier.fillMaxWidth().clickable { selected = workout; page = Page.DETAIL }) {
                        Label(workout.type, aqua)
                        Text(workout.name, color = white, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${workout.totalDistance}  ·  ~${workout.estimatedMinutes} min", color = muted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Page.DETAIL -> {
                Label(selected.type, aqua)
                Title(selected.name)
                Caption("${selected.totalDistance}  ·  ~${selected.estimatedMinutes} min")
                Spacer(Modifier.height(12.dp))
                selected.sections.forEach { section ->
                    Card(Modifier.fillMaxWidth()) {
                        Label(section.name.uppercase(), aqua)
                        section.sets.forEach { set ->
                            Text("${if (set.repetitions > 1) "${set.repetitions} × " else ""}${set.step.distance.amount} ${set.step.stroke.display()}", color = white, fontSize = 15.sp)
                            if (set.step.restAfter.seconds > 0) Text("${set.step.restAfter.seconds}s rest after each", color = muted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
                Action("START", true) { page = Page.READY }
            }
            Page.READY -> {
                Label("READY TO SWIM", aqua)
                Title(selected.name)
                Caption("${selected.totalDistance}  ·  ${selected.totalRepetitions} repeats")
                Spacer(Modifier.height(16.dp))
                Label("POOL LENGTH", muted)
                Spacer(Modifier.height(8.dp))
                pools.forEach { option ->
                    Action(if (pool == option) "● ${option.label}" else option.label, pool == option) { pool = option }
                }
                Spacer(Modifier.height(8.dp))
                Action("START WORKOUT", true) { session = WorkoutSession(selected).start(); page = Page.SESSION }
                Caption("Manual progression · ${pool.label} pool")
            }
            Page.SESSION -> when (val phase = session.phase) {
                SessionPhase.Active -> {
                    val instruction = session.current
                    if (instruction != null) {
                        Label(instruction.section.name.uppercase(), aqua)
                        Spacer(Modifier.height(3.dp))
                        Text("${instruction.set.step.distance.amount} ${instruction.set.step.distance.unit.symbol}", color = white, fontSize = 48.sp, fontWeight = FontWeight.Bold, lineHeight = 51.sp)
                        Text(instruction.set.step.stroke.display().uppercase(), color = aqua, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("REP ${instruction.position.repetition + 1} / ${instruction.set.repetitions}", color = muted, fontSize = 13.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("${session.completedDistance} / ${selected.totalDistance}", color = aqua, fontSize = 14.sp)
                        Caption("${time(session.elapsedSeconds)} elapsed")
                        Spacer(Modifier.height(3.dp))
                        Action("COMPLETE REP", true) { session = session.advance() }
                        Action("PAUSE", false) { session = session.pause() }
                    }
                }
                is SessionPhase.Rest -> {
                    Label("RECOVER", amber)
                    Text("REST", color = white, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Text(time(phase.remainingSeconds), color = amber, fontSize = 47.sp, fontWeight = FontWeight.Bold)
                    Label("NEXT", muted)
                    val next = session.current
                    if (next != null) Text("${next.set.step.distance.amount} ${next.set.step.distance.unit.symbol} ${next.set.step.stroke.display()}", color = white, fontSize = 17.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Action("SKIP REST", true) { session = session.skipRest() }
                    Action("PAUSE", false) { session = session.pause() }
                }
                is SessionPhase.Paused -> {
                    Label("SESSION ON HOLD", amber)
                    Title("Paused")
                    Text(time(session.elapsedSeconds), color = white, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Caption("${session.completedDistance} ${selected.totalDistance.unit.symbol} completed")
                    Spacer(Modifier.height(12.dp))
                    if (confirmingEnd) {
                        Action("CONFIRM END", false) { session = session.end(); confirmingEnd = false }
                        Action("KEEP SWIMMING", true) { confirmingEnd = false; session = session.resume() }
                    } else {
                        Action("RESUME", true) { session = session.resume() }
                        Action("END WORKOUT", false) { confirmingEnd = true }
                    }
                }
                SessionPhase.Complete -> {
                    val finished = session.completedRepetitions == selected.totalRepetitions
                    Label(if (finished) "SESSION COMPLETE" else "WORKOUT ENDED", aqua)
                    Title(if (finished) "Swim complete" else "Session ended")
                    Caption(selected.name)
                    Spacer(Modifier.height(12.dp))
                    Metric("DISTANCE", "${session.completedDistance} / ${selected.totalDistance}")
                    Metric("TIME", time(session.elapsedSeconds))
                    Metric("REPS", "${session.completedRepetitions} / ${selected.totalRepetitions}")
                    Metric("SETS", "${session.completedSets} / ${selected.sections.sumOf { it.sets.size }}")
                    Spacer(Modifier.height(10.dp))
                    Action("BACK TO WORKOUTS", true) { page = Page.LIST }
                }
                else -> Unit
            }
        }
        Spacer(Modifier.height(20.dp))
    } }
}

private fun Stroke.display() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun time(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)

@Composable private fun Label(value: String, color: Color) { Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center) }
@Composable private fun Title(value: String) { Text(value, color = white, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 28.sp) }
@Composable private fun Caption(value: String) { Text(value, color = muted, fontSize = 12.sp, textAlign = TextAlign.Center) }
@Composable private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(surface, RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp), content = content)
}
@Composable private fun Action(value: String, primary: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp).background(if (primary) aqua else surface, RoundedCornerShape(30.dp)).clickable(onClick = onClick).padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(value, color = if (primary) navy else white, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
@Composable private fun Metric(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) { Label(label, aqua); Text(value, color = white, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    Spacer(Modifier.height(6.dp))
}
