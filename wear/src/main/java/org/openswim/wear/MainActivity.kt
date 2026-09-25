package org.openswim.wear

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

private enum class WatchPage { HOME, LIBRARY, DETAIL, POOL, SETTINGS, SESSION, ADD_DISTANCE }
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
    val fallback = ConceptCWorkouts.all.first()
    val initial = SavedFlow(WatchPage.HOME, SwimMode.GUIDED, fallback, 25,
        SwimSession.guided(fallback, 25), "All", WatchPage.LIBRARY, 0, ManualSwimType.KICK, 100)
    return runCatching {
        val p = raw!!.split('|')
        if (p[0] == "v2") {
            val mode = SwimMode.valueOf(p[2])
            val page = if (p[1] == "READY" || (p[1] == "POOL" && mode == SwimMode.GUIDED)) {
                if (mode == SwimMode.GUIDED) WatchPage.DETAIL else WatchPage.POOL
            } else WatchPage.valueOf(p[1])
            val workout = ConceptCWorkouts.all.first { it.id == p[3] }
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
            val workout = ConceptCWorkouts.all.first { it.id == p[1] }
            val pool = p[2].toInt()
            val guided = WorkoutSession(workout, phaseFrom(p[8], p[9].toInt()),
                p[4].toInt(), p[5].toInt(), p[6].toInt(), p[7].toInt())
            SavedFlow(if (p[0] == "READY" || p[0] == "POOL") WatchPage.DETAIL
                else WatchPage.valueOf(p[0]),
                SwimMode.GUIDED, workout, pool,
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
    var defaultPool by remember { mutableIntStateOf(preferences.getInt("default_pool_m", 25)) }
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
    var endFromPaused by remember { mutableStateOf(false) }
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
            WatchPage.DETAIL -> page = WatchPage.LIBRARY
            WatchPage.POOL, WatchPage.SETTINGS -> page = WatchPage.HOME
            WatchPage.ADD_DISTANCE -> page = WatchPage.SESSION
            WatchPage.SESSION -> when {
                session.locked -> Unit
                confirmingEnd -> {
                    confirmingEnd = false
                    if (!endFromPaused) session = session.resume()
                }
                session.phase == SessionPhase.Complete -> page = WatchPage.HOME
                session.phase is SessionPhase.Paused -> Unit
                else -> pane = if (mode == SwimMode.GUIDED) 2 else 1
            }
            else -> Unit
        }
    }

    Crossfade(targetState = page, animationSpec = tween(180), label = "watch page") { visible ->
        when (visible) {
            WatchPage.HOME -> ConceptCHome(defaultPool,
                onStart = {
                    mode = SwimMode.POOL
                    poolLength = defaultPool
                    session = SwimSession.pool(defaultPool)
                    page = WatchPage.POOL
                },
                onLibrary = {
                    mode = SwimMode.GUIDED
                    workout = ConceptCWorkouts.all.first()
                    poolLength = defaultPool.takeIf { workout.supportsPoolLength(it) } ?: 25
                    session = SwimSession.guided(workout, poolLength)
                    category = "All"
                    page = WatchPage.LIBRARY
                })
            WatchPage.LIBRARY -> ConceptCLibrary(category,
                onCategory = { category = it },
                onWorkout = { item ->
                    workout = item
                    poolLength = defaultPool.takeIf { item.supportsPoolLength(it) } ?: 25
                    session = SwimSession.guided(item, poolLength)
                    detailOrigin = WatchPage.LIBRARY
                    page = WatchPage.DETAIL
                },
                onSettings = { page = WatchPage.SETTINGS })
            WatchPage.DETAIL -> ConceptCDetail(workout, poolLength,
                onBack = { page = WatchPage.LIBRARY },
                onPool = {
                    poolLength = poolLengths.firstOrNull { it != poolLength && workout.supportsPoolLength(it) }
                        ?: poolLength
                    session = SwimSession.guided(workout, poolLength)
                },
                onStart = {
                    session = SwimSession.guided(workout, poolLength).start()
                    pane = 0
                    confirmingEnd = false
                    page = WatchPage.SESSION
                })
            WatchPage.POOL -> ScrollPage {
                Spacer(Modifier.height(17.dp))
                Eyebrow("POOL SWIM", watchCyan)
                Spacer(Modifier.height(4.dp))
                Heading("Pool length")
                Spacer(Modifier.height(15.dp))
                poolLengths.forEach { length ->
                    PoolChoiceRow(length, poolLength == length, true) { poolLength = length }
                }
                Spacer(Modifier.height(12.dp))
                WideButton("START SWIM") {
                    session = SwimSession.pool(poolLength).start()
                    pane = 0
                    confirmingEnd = false
                    page = WatchPage.SESSION
                }
                Spacer(Modifier.height(7.dp))
                Eyebrow("DISTANCE ADDED BY TAPS", watchSecondary)
            }
            WatchPage.SETTINGS -> ScrollPage {
                Spacer(Modifier.height(18.dp))
                Eyebrow("PREFERENCES", watchCyan)
                Spacer(Modifier.height(3.dp))
                Heading("Settings")
                Spacer(Modifier.height(17.dp))
                Eyebrow("DEFAULT POOL")
                Spacer(Modifier.height(5.dp))
                poolLengths.forEach { length ->
                    PoolChoiceRow(length, defaultPool == length, true) {
                        defaultPool = length
                        preferences.edit().putInt("default_pool_m", length).apply()
                    }
                }
                Spacer(Modifier.height(7.dp))
                Eyebrow("CHANGE PER SWIM AT START", watchSecondary)
            }
            WatchPage.SESSION -> when {
                session.locked -> LockScreen { session = session.unlock() }
                confirmingEnd -> EndConfirmScreen(session,
                    onKeep = {
                        confirmingEnd = false
                        if (!endFromPaused) session = session.resume()
                    },
                    onFinish = { confirmingEnd = false; session = session.end() })
                session.phase is SessionPhase.Paused -> PausedScreen(session,
                    onResume = { session = session.resume() },
                    onEnd = { endFromPaused = true; confirmingEnd = true })
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
                        onEnd = {
                            endFromPaused = false
                            session = session.pause()
                            confirmingEnd = true
                        })
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
