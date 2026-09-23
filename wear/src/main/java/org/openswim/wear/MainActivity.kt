package org.openswim.wear

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import org.openswim.core.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenSwimApp() }
    }
}

private enum class WatchPage { HOME, LIBRARY, DETAIL, POOL, READY, SESSION, ADD_DISTANCE }
private val poolLengths = listOf(25, 50)

private data class SavedFlow(
    val page: WatchPage,
    val mode: SwimMode,
    val workout: Workout,
    val poolLength: Int,
    val session: SwimSession,
    val category: String,
    val detailOrigin: WatchPage,
    val pane: Int,
    val drillType: ManualSwimType,
    val drillAmount: Int
)

private fun phaseFrom(code: String, remaining: Int): SessionPhase = when (code) {
    "A" -> SessionPhase.Active
    "R" -> SessionPhase.Rest(remaining)
    "PA" -> SessionPhase.Paused(SessionPhase.Active)
    "PR" -> SessionPhase.Paused(SessionPhase.Rest(remaining))
    "C" -> SessionPhase.Complete
    else -> SessionPhase.NotStarted
}

private fun phaseCode(phase: SessionPhase): String = when (phase) {
    SessionPhase.Active -> "A"
    is SessionPhase.Rest -> "R"
    is SessionPhase.Paused -> if (phase.previous is SessionPhase.Rest) "PR" else "PA"
    SessionPhase.Complete -> "C"
    else -> "N"
}

private fun remainingSeconds(phase: SessionPhase): Int = when (phase) {
    is SessionPhase.Rest -> phase.remainingSeconds
    is SessionPhase.Paused -> (phase.previous as? SessionPhase.Rest)?.remainingSeconds ?: 0
    else -> 0
}

private fun restoreFlow(raw: String?): SavedFlow {
    val fallback = SampleWorkouts.all.first()
    val initial = SavedFlow(WatchPage.HOME, SwimMode.GUIDED, fallback, 25,
        SwimSession.guided(fallback, 25), "All", WatchPage.LIBRARY, 0, ManualSwimType.KICK, 100)
    return runCatching {
        val p = raw!!.split('|')
        if (p[0] == "v2") {
            val page = WatchPage.valueOf(p[1])
            val mode = SwimMode.valueOf(p[2])
            val workout = SampleWorkouts.all.first { it.id == p[3] }
            val pool = p[4].toInt()
            val phase = phaseFrom(p[8], p[9].toInt())
            val guided = if (mode == SwimMode.GUIDED) WorkoutSession(workout, phase,
                p[10].toInt(), p[11].toInt(), p[12].toInt(), p[13].toInt()) else null
            val session = SwimSession(mode, pool, guided,
                freePhase = if (mode == SwimMode.POOL) phase else SessionPhase.NotStarted,
                freeElapsedSeconds = if (mode == SwimMode.POOL) p[13].toInt() else 0,
                loggedLengths = p[14].toInt(), manualExtraDistance = p[15].toInt(),
                locked = p[16].toBoolean())
            if (guided != null) {
                require(guided.instructionIndex in 0..workout.totalRepetitions)
                require(guided.completedRepetitions == guided.instructionIndex)
            }
            SavedFlow(page, mode, workout, pool, session, p[5], WatchPage.valueOf(p[6]),
                p[7].toInt().coerceIn(0, if (mode == SwimMode.GUIDED) 2 else 1),
                ManualSwimType.valueOf(p[17]), p[18].toInt().coerceAtLeast(pool))
        } else {
            // Migrate the previous guided-only local session without discarding a swim in progress.
            val workout = SampleWorkouts.all.first { it.id == p[1] }
            val pool = p[2].toInt()
            val guided = WorkoutSession(workout, phaseFrom(p[8], p[9].toInt()),
                p[4].toInt(), p[5].toInt(), p[6].toInt(), p[7].toInt())
            SavedFlow(WatchPage.valueOf(p[0]), SwimMode.GUIDED, workout, pool,
                SwimSession.guided(workout, pool).copy(guided = guided), p[3],
                p.getOrNull(10)?.let(WatchPage::valueOf) ?: WatchPage.LIBRARY,
                0, ManualSwimType.KICK, pool * 4)
        }
    }.getOrElse { initial }
}

private fun saveFlow(flow: SavedFlow): String {
    val guided = flow.session.guided
    val phase = flow.session.phase
    return listOf("v2", flow.page.name, flow.mode.name, flow.workout.id, flow.poolLength,
        flow.category, flow.detailOrigin.name, flow.pane, phaseCode(phase), remainingSeconds(phase),
        guided?.instructionIndex ?: 0, guided?.completedDistance ?: 0,
        guided?.completedRepetitions ?: 0, flow.session.elapsedSeconds,
        flow.session.loggedLengths, flow.session.manualExtraDistance, flow.session.locked,
        flow.drillType.name, flow.drillAmount).joinToString("|")
}

@Composable
private fun OpenSwimApp() {
    val preferences = LocalContext.current.applicationContext.getSharedPreferences("open_swim_flow", 0)
    val saved = remember { restoreFlow(preferences.getString("snapshot", null)) }
    var page by remember { mutableStateOf(saved.page) }
    var mode by remember { mutableStateOf(saved.mode) }
    var workout by remember { mutableStateOf(saved.workout) }
    var poolLength by remember { mutableIntStateOf(saved.poolLength) }
    var session by remember { mutableStateOf(saved.session) }
    var category by remember { mutableStateOf(saved.category) }
    var detailOrigin by remember { mutableStateOf(saved.detailOrigin) }
    var pane by remember { mutableIntStateOf(saved.pane) }
    var drillType by remember { mutableStateOf(saved.drillType) }
    var drillAmount by remember { mutableIntStateOf(saved.drillAmount) }
    var confirmingEnd by remember { mutableStateOf(false) }
    val ticking = session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest

    LaunchedEffect(page, mode, workout, poolLength, session, category, detailOrigin, pane, drillType, drillAmount) {
        if (mode == session.mode &&
            (mode != SwimMode.GUIDED || session.workout?.id == workout.id)) {
            preferences.edit().putString("snapshot", saveFlow(SavedFlow(page, mode, workout, poolLength,
                session, category, detailOrigin, pane, drillType, drillAmount))).commit()
        }
    }
    LaunchedEffect(workout.id) {
        if (mode == SwimMode.GUIDED && !workout.supportsPoolLength(poolLength)) poolLength = 25
    }
    LaunchedEffect(ticking) {
        var lastSecond = SystemClock.elapsedRealtime()
        while (session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest) {
            delay(250)
            val now = SystemClock.elapsedRealtime()
            val wholeSeconds = ((now - lastSecond) / 1000).toInt()
            repeat(wholeSeconds) { session = session.tick() }
            lastSecond += wholeSeconds * 1000L
        }
    }

    BackHandler(page != WatchPage.HOME) {
        when (page) {
            WatchPage.LIBRARY -> page = WatchPage.HOME
            WatchPage.DETAIL -> page = detailOrigin
            WatchPage.POOL -> page = if (mode == SwimMode.GUIDED) WatchPage.DETAIL else WatchPage.HOME
            WatchPage.READY -> page = WatchPage.POOL
            WatchPage.ADD_DISTANCE -> page = WatchPage.SESSION
            WatchPage.SESSION -> when {
                session.locked -> Unit
                confirmingEnd -> confirmingEnd = false
                session.phase == SessionPhase.Complete -> page = WatchPage.HOME
                session.phase is SessionPhase.Paused -> Unit
                else -> pane = if (mode == SwimMode.GUIDED) 2 else 1
            }
            else -> Unit
        }
    }

    Crossfade(targetState = page, animationSpec = tween(180), label = "watch page") { visible ->
        when (visible) {
            WatchPage.HOME -> ScrollPage {
                Spacer(Modifier.height(18.dp))
                Heading("OpenSwim")
                Spacer(Modifier.height(19.dp))
                Eyebrow("START A SWIM", watchCyan)
                Spacer(Modifier.height(13.dp))
                WideButton("GUIDED WORKOUT", true) {
                    mode = SwimMode.GUIDED
                    workout = SampleWorkouts.all.first()
                    poolLength = 25
                    session = SwimSession.guided(workout, poolLength)
                    page = WatchPage.LIBRARY
                }
                Spacer(Modifier.height(9.dp))
                WideButton("POOL SWIM", false) {
                    mode = SwimMode.POOL
                    poolLength = 25
                    session = SwimSession.pool(25)
                    page = WatchPage.POOL
                }
            }
            WatchPage.LIBRARY -> ScrollPage {
                Spacer(Modifier.height(16.dp))
                Eyebrow("GUIDED WORKOUT", watchCyan)
                Spacer(Modifier.height(4.dp))
                Heading("Workouts")
                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("All", "Easy", "Technique", "Aerobic", "Endurance", "Threshold", "Sprint").forEach { option ->
                        FilterChip(option, category == option) { category = option }
                    }
                }
                Spacer(Modifier.height(11.dp))
                SampleWorkouts.all.filter { category == "All" || it.type == category }.forEach { item ->
                    WorkoutCard(item) {
                        workout = item
                        poolLength = 25
                        session = SwimSession.guided(item, 25)
                        detailOrigin = WatchPage.LIBRARY
                        page = WatchPage.DETAIL
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            WatchPage.DETAIL -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Eyebrow(workout.type.uppercase(), watchCyan)
                Spacer(Modifier.height(3.dp))
                Heading(workout.displayName())
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(workout.totalDistance.toString(), color = watchWhite,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("~${workout.estimatedMinutes} min", color = watchSecondary, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(12.dp))
                WideButton("CHOOSE POOL", true) { page = WatchPage.POOL }
                Spacer(Modifier.height(15.dp))
                workout.sections.forEach { section ->
                    SectionCard(section)
                    Spacer(Modifier.height(8.dp))
                }
            }
            WatchPage.POOL -> ScrollPage {
                Spacer(Modifier.height(10.dp))
                Eyebrow("POOL SETUP", watchCyan)
                Spacer(Modifier.height(3.dp))
                Heading("Pool size")
                Spacer(Modifier.height(3.dp))
                poolLengths.forEach { length ->
                    val supported = mode == SwimMode.POOL || workout.supportsPoolLength(length)
                    PoolOption("$length m", poolLength == length, supported) { poolLength = length }
                    Spacer(Modifier.height(3.dp))
                }
                Spacer(Modifier.height(3.dp))
                WideButton("CONTINUE", true) { page = WatchPage.READY }
            }
            WatchPage.READY -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Eyebrow("READY TO SWIM", watchCyan)
                Spacer(Modifier.height(5.dp))
                Heading(if (mode == SwimMode.GUIDED) workout.displayName() else "Pool Swim")
                if (mode == SwimMode.GUIDED) {
                    Text(workout.totalDistance.toString(), color = watchWhite,
                        fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(5.dp))
                    Text("$poolLength m pool  ·  ${workout.totalDistance.amount / poolLength} lengths",
                        color = watchSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    val first = workout.instructions().first()
                    Text("FIRST  ${first.set.step.distance} ${first.set.step.stroke.displayName()}",
                        color = watchCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                } else {
                    Spacer(Modifier.height(7.dp))
                    Text("$poolLength m", color = watchWhite, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("MANUAL LENGTH LOGGING", color = watchSecondary, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                WideButton("START SWIM", true) {
                    session = if (mode == SwimMode.GUIDED)
                        SwimSession.guided(workout, poolLength).start()
                    else SwimSession.pool(poolLength).start()
                    pane = 0
                    confirmingEnd = false
                    page = WatchPage.SESSION
                }
            }
            WatchPage.SESSION -> when {
                session.locked -> LockScreen { session = session.unlock() }
                confirmingEnd -> EndConfirmScreen(session,
                    onKeep = { confirmingEnd = false; session = session.resume() },
                    onFinish = { confirmingEnd = false; session = session.end() })
                session.phase is SessionPhase.Paused -> PausedScreen(session,
                    onResume = { session = session.resume() },
                    onEnd = { confirmingEnd = true })
                session.phase == SessionPhase.Complete ->
                    CompleteScreen(session) { page = WatchPage.HOME }
                session.phase == SessionPhase.Active || session.phase is SessionPhase.Rest ->
                    SessionPager(session, pane, onPane = { pane = it },
                        onAdvance = { session = session.advance() },
                        onSkip = { session = session.skipRest() },
                        onLength = { session = session.logLength() },
                        onLock = { session = session.lock() },
                        onDrill = {
                            drillType = ManualSwimType.KICK
                            drillAmount = poolLength * 4
                            page = WatchPage.ADD_DISTANCE
                        },
                        onPause = { session = session.pause() },
                        onEnd = { session = session.pause(); confirmingEnd = true })
            }
            WatchPage.ADD_DISTANCE -> AddDistanceScreen(poolLength, drillType, drillAmount,
                onType = { drillType = it },
                onAmount = { drillAmount = it },
                onAdd = {
                    session = session.logManualDistance(drillType, drillAmount)
                    pane = 0
                    page = WatchPage.SESSION
                })
        }
    }
}
