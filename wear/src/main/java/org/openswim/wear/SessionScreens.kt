package org.openswim.wear

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import org.openswim.core.*

@Composable
internal fun SessionPager(
    session: SwimSession,
    pane: Int,
    onPane: (Int) -> Unit,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    onLength: () -> Unit,
    onLock: () -> Unit,
    onDrill: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit
) {
    val compact = isCompactWatch()
    val lastPane = if (session.mode == SwimMode.GUIDED) 2 else 1
    var drag = 0f
    Column(
        Modifier.fillMaxSize().background(watchBackground)
            .pointerInput(pane, lastPane) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                    onDragEnd = {
                        if (drag < -36f) onPane((pane + 1).coerceAtMost(lastPane))
                        if (drag > 36f) onPane((pane - 1).coerceAtLeast(0))
                    }
                )
            }
            .padding(start = 24.dp, end = 24.dp, top = if (compact) 10.dp else 15.dp,
                bottom = if (compact) 10.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WatchClock()
        Crossfade(targetState = pane, modifier = Modifier.weight(1f),
            animationSpec = tween(150), label = "session pane") { visible ->
            when {
                session.mode == SwimMode.GUIDED && visible == 0 ->
                    if (session.phase is SessionPhase.Rest) GuidedRest(session, onSkip)
                    else GuidedCurrent(session)
                session.mode == SwimMode.GUIDED && visible == 1 ->
                    SessionMetrics(session, if (session.phase is SessionPhase.Rest) onSkip else onAdvance)
                session.mode == SwimMode.POOL && visible == 0 -> SessionMetrics(session, onLength)
                else -> ControlsScreen(onLock, onDrill, onPause, onEnd)
            }
        }
        PagerHeader(pane, lastPane, onPane)
    }
}

@Composable
private fun PagerHeader(pane: Int, lastPane: Int, onPane: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (isCompactWatch()) 13.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            (0..lastPane).forEach { index ->
                Box(Modifier.size(if (index == pane) 5.dp else 4.dp)
                    .background(if (index == pane) watchCyan else watchOutline,
                        RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button) { onPane(index) })
            }
        }
    }
}

@Composable
private fun GuidedCurrent(session: SwimSession) {
    val instruction = session.current ?: return
    val workout = session.workout ?: return
    val compact = isCompactWatch()
    val total = workout.totalRepetitions.coerceAtLeast(1)
    val position = (session.guided!!.completedRepetitions + 1).toFloat() / total
    Column(Modifier.fillMaxSize().padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(if (compact) 7.dp else 12.dp))
        Row(Modifier.fillMaxWidth(0.70f), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(workout.displayName(), color = watchCyan,
                fontSize = if (compact) 10.sp else 12.sp, maxLines = 1)
            Text("${instruction.position.repetition + 1}/${instruction.set.repetitions}",
                color = watchWhite, fontSize = if (compact) 10.sp else 12.sp)
        }
        Spacer(Modifier.weight(0.7f))
        Text("${instruction.set.step.distance} ${instruction.set.step.stroke.displayName()}",
            color = watchWhite, fontSize = if (compact) 16.sp else 19.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(if (compact) 11.dp else 14.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp))
            .background(watchOutline)) {
            Box(Modifier.fillMaxWidth(position.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(7.dp)).background(watchCyan))
        }
        Spacer(Modifier.weight(0.7f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${session.completedDistance} m", color = watchWhite,
                    fontSize = if (compact) 19.sp else 22.sp, fontWeight = FontWeight.Bold)
                Eyebrow("TOTAL")
            }
            Box(Modifier.width(1.dp).height(29.dp).background(watchOutline))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatTime(session.elapsedSeconds), color = watchWhite,
                    fontSize = if (compact) 19.sp else 22.sp, fontWeight = FontWeight.Bold)
                Eyebrow("ELAPSED")
            }
        }
        Spacer(Modifier.height(if (compact) 9.dp else 14.dp))
    }
}

@Composable
private fun GuidedRest(session: SwimSession, onSkip: () -> Unit) {
    val phase = session.phase as? SessionPhase.Rest ?: return
    val next = session.current
    val previous = session.workout!!.instructions().getOrNull(session.guided!!.instructionIndex - 1)
    val total = previous?.set?.step?.restAfter?.seconds ?: phase.remainingSeconds
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("Rest", color = watchCyan, fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
        RestDial(phase.remainingSeconds, total)
        Spacer(Modifier.height(if (compact) 7.dp else 10.dp))
        Text("Next up", color = watchSecondary, fontSize = if (compact) 11.sp else 12.sp)
        if (next != null) {
            Text("${if (next.set.repetitions > 1) "${next.set.repetitions} × " else ""}" +
                "${next.set.step.distance} ${next.set.step.stroke.displayName()}",
                color = watchWhite, fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun SessionMetrics(session: SwimSession, onAction: () -> Unit) {
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Eyebrow(if (session.mode == SwimMode.POOL) "POOL SWIM" else "GUIDED", watchCyan)
        Text(formatTime(session.elapsedSeconds), color = watchWhite,
            fontSize = if (compact) 39.sp else 47.sp, fontWeight = FontWeight.ExtraBold)
        Eyebrow("ELAPSED")
        Spacer(Modifier.height(if (compact) 5.dp else 9.dp))
        Text("${session.completedDistance} m", color = watchWhite,
            fontSize = if (compact) 28.sp else 35.sp, fontWeight = FontWeight.Bold)
        Eyebrow("${session.completedLengths} LENGTHS " +
            if (session.mode == SwimMode.POOL) "LOGGED" else "COUNTED")
        Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
        WideButton(when {
            session.mode == SwimMode.POOL -> "LOG LENGTH  +${session.poolLength}m"
            session.phase is SessionPhase.Rest -> "SKIP REST"
            else -> "REP DONE"
        }, onClick = onAction)
    }
}

@Composable
private fun ControlsScreen(
    onLock: () -> Unit,
    onDrill: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit
) {
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize().padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("Controls", color = watchWhite, fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        ControlPill("Ⅱ", "Pause", true, onPause)
        Spacer(Modifier.height(6.dp))
        ControlPill("▣", "Lock Screen", false, onLock)
        Spacer(Modifier.height(6.dp))
        ControlPill("≋", "Drill / Kick", false, onDrill)
        Spacer(Modifier.height(6.dp))
        ControlPill("■", "End Workout", false, onEnd)
    }
}

@Composable
private fun ControlPill(icon: String, label: String, primary: Boolean, onClick: () -> Unit) {
    val compact = isCompactWatch()
    val shape = RoundedCornerShape(50.dp)
    Row(Modifier.fillMaxWidth(0.94f).height(if (compact) 29.dp else 33.dp)
        .clip(shape).background(watchPanel)
        .border(if (primary) 1.dp else 0.dp, if (primary) watchCyan else watchPanel, shape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = if (compact) 15.dp else 19.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = if (primary) watchCyan else if (label == "End Workout") watchDanger else watchWhite,
            fontSize = if (compact) 13.sp else 16.sp,
            modifier = Modifier.width(if (compact) 32.dp else 39.dp))
        Text(label, color = watchWhite, fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
internal fun LockScreen(onUnlock: () -> Unit) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 14.dp else 24.dp) {
        Eyebrow("LOCKED", watchCyan)
        Spacer(Modifier.weight(0.7f))
        val shape = RoundedCornerShape(60.dp)
        Box(Modifier.size(if (compact) 96.dp else 112.dp).clip(shape)
            .background(watchPanel).border(1.dp, watchCyan, shape)
            .semantics { contentDescription = "Press and hold for two seconds to unlock"; role = Role.Button }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val start = SystemClock.elapsedRealtime()
                    if (tryAwaitRelease() && SystemClock.elapsedRealtime() - start >= 2000L) onUnlock()
                })
            }, contentAlignment = Alignment.Center) {
            Text("HOLD", color = watchWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Eyebrow("HOLD 2 SEC TO UNLOCK")
        Spacer(Modifier.weight(0.7f))
    }
}

@Composable
internal fun AddDistanceScreen(
    poolLength: Int,
    type: ManualSwimType,
    amount: Int,
    onType: (ManualSwimType) -> Unit,
    onAmount: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 14.dp else 20.dp) {
        Eyebrow("DRILL / KICK", watchCyan)
        Spacer(Modifier.weight(0.3f))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ModeTab("KICK", type == ManualSwimType.KICK) { onType(ManualSwimType.KICK) }
            ModeTab("DRILL", type == ManualSwimType.DRILL) { onType(ManualSwimType.DRILL) }
        }
        Spacer(Modifier.weight(0.4f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(amount.toString(), color = watchWhite,
                fontSize = if (compact) 45.sp else 59.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(4.dp))
            Text("m", color = watchCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 7.dp))
        }
        Spacer(Modifier.weight(0.4f))
        Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.SpaceBetween) {
            StepButton("−") { onAmount((amount - poolLength).coerceAtLeast(poolLength)) }
            Eyebrow("$poolLength m STEPS")
            StepButton("+") { onAmount(amount + poolLength) }
        }
        Spacer(Modifier.weight(0.5f))
        WideButton("ADD DISTANCE", true, onAdd)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(Modifier.width(if (isCompactWatch()) 40.dp else 47.dp)
        .height(if (isCompactWatch()) 34.dp else 40.dp)
        .clip(RoundedCornerShape(20.dp)).background(watchPanel)
        .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = watchWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(role = Role.Button, onClick = onClick).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected) watchCyan else watchSecondary,
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(29.dp).height(2.dp)
            .background(if (selected) watchCyan else watchBackground))
    }
}

@Composable
internal fun PausedScreen(session: SwimSession, onResume: () -> Unit, onEnd: () -> Unit) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 24.dp else 38.dp) {
        Eyebrow("PAUSED", watchAmber)
        Spacer(Modifier.weight(0.6f))
        Text(formatTime(session.elapsedSeconds), color = watchWhite,
            fontSize = if (compact) 35.sp else 42.sp, fontWeight = FontWeight.ExtraBold)
        Text("${session.completedDistance} m completed", color = watchSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(0.8f))
        WideButton("RESUME", true, onResume)
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        TextAction("END WORKOUT", watchDanger, onEnd)
    }
}

@Composable
internal fun EndConfirmScreen(session: SwimSession, onKeep: () -> Unit, onFinish: () -> Unit) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 24.dp else 38.dp) {
        Eyebrow("END WORKOUT?", watchDanger)
        Spacer(Modifier.weight(0.7f))
        Heading("Finish now?")
        Text("${session.completedDistance} m completed", color = watchSecondary, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        WideButton("KEEP SWIMMING", true, onKeep)
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        TextAction("FINISH WORKOUT", watchDanger, onFinish)
    }
}

@Composable
internal fun CompleteScreen(session: SwimSession, onBack: () -> Unit) {
    val compact = isCompactWatch()
    val workout = session.workout
    val finished = workout == null || session.guided!!.completedRepetitions == workout.totalRepetitions
    FixedPage(bottomPadding = if (compact) 20.dp else 30.dp) {
        Spacer(Modifier.weight(0.3f))
        Eyebrow(if (finished) "NICE SWIM" else "SWIM ENDED", watchCyan)
        Spacer(Modifier.height(5.dp))
        Text("${session.completedDistance} m", color = watchWhite,
            fontSize = if (compact) 42.sp else 52.sp, fontWeight = FontWeight.ExtraBold)
        Eyebrow("DISTANCE")
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Text(formatTime(session.elapsedSeconds), color = watchWhite,
            fontSize = if (compact) 26.sp else 31.sp, fontWeight = FontWeight.SemiBold)
        Eyebrow("ELAPSED")
        Spacer(Modifier.height(if (compact) 3.dp else 6.dp))
        Eyebrow(if (workout == null) "${session.completedLengths} LENGTHS"
            else "${session.guided!!.completedRepetitions} / ${workout.totalRepetitions} REPS")
        Spacer(Modifier.weight(0.7f))
        WideButton("DONE", true, onBack)
    }
}
