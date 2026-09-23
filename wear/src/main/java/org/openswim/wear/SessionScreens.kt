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
            .padding(start = 24.dp, end = 24.dp, top = if (compact) 8.dp else 12.dp,
                bottom = if (compact) 10.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PagerHeader(session.mode, pane, lastPane, onPane)
        Crossfade(targetState = pane, modifier = Modifier.weight(1f),
            animationSpec = tween(150), label = "session pane") { visible ->
            when {
                session.mode == SwimMode.GUIDED && visible == 0 ->
                    if (session.phase is SessionPhase.Rest) GuidedRest(session)
                    else GuidedCurrent(session)
                session.mode == SwimMode.GUIDED && visible == 1 -> SessionMetrics(session)
                session.mode == SwimMode.POOL && visible == 0 -> SessionMetrics(session)
                else -> ControlsScreen(session, onAdvance, onSkip, onLength, onLock, onDrill, onPause, onEnd)
            }
        }
    }
}

@Composable
private fun PagerHeader(mode: SwimMode, pane: Int, lastPane: Int, onPane: (Int) -> Unit) {
    val compact = isCompactWatch()
    val title = when {
        mode == SwimMode.GUIDED && pane == 0 -> "CURRENT"
        mode == SwimMode.GUIDED && pane == 1 -> "METRICS"
        mode == SwimMode.POOL && pane == 0 -> "METRICS"
        else -> "CONTROLS"
    }
    Row(
        Modifier.fillMaxWidth(if (compact) 0.58f else 0.62f).height(23.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", color = if (pane > 0) watchCyan else watchOutline, fontSize = 20.sp,
            modifier = Modifier.width(23.dp).clickable(enabled = pane > 0, role = Role.Button) {
                onPane(pane - 1)
            }, textAlign = TextAlign.Center)
        Eyebrow(title, watchCyan)
        Text("›", color = if (pane < lastPane) watchCyan else watchOutline, fontSize = 20.sp,
            modifier = Modifier.width(23.dp).clickable(enabled = pane < lastPane, role = Role.Button) {
                onPane(pane + 1)
            }, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GuidedCurrent(session: SwimSession) {
    val instruction = session.current ?: return
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow("SWIM  ·  ${instruction.section.name.uppercase()}", watchCyan)
        Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(instruction.set.step.distance.amount.toString(), color = watchWhite,
                fontSize = if (compact) 49.sp else 60.sp, fontWeight = FontWeight.ExtraBold,
                lineHeight = if (compact) 52.sp else 63.sp, letterSpacing = (-2).sp)
            Spacer(Modifier.width(4.dp))
            Text(instruction.set.step.distance.unit.symbol, color = watchCyan,
                fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (compact) 6.dp else 9.dp))
        }
        Text(instruction.set.step.stroke.displayName().uppercase(), color = watchWhite,
            fontSize = if (compact) 18.sp else 21.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1)
        val cue = instruction.set.step.equipment.takeIf { it.isNotEmpty() }
            ?.joinToString { it.name.lowercase().replace('_', ' ') } ?: instruction.set.step.note
        cue?.let {
            Text(it, color = watchSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("REP")
            Spacer(Modifier.width(6.dp))
            Text("${instruction.position.repetition + 1} / ${instruction.set.repetitions}",
                color = watchCyan, fontSize = if (compact) 16.sp else 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GuidedRest(session: SwimSession) {
    val phase = session.phase as? SessionPhase.Rest ?: return
    val next = session.current
    val previous = session.workout!!.instructions().getOrNull(session.guided!!.instructionIndex - 1)
    val total = previous?.set?.step?.restAfter?.seconds ?: phase.remainingSeconds
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Status("REST", watchAmber)
        Spacer(Modifier.weight(0.45f))
        RestDial(phase.remainingSeconds, total)
        Spacer(Modifier.weight(0.4f))
        Eyebrow("UP NEXT  ·  REP ${(next?.position?.repetition ?: 0) + 1}/${next?.set?.repetitions ?: 1}")
        if (next != null) {
            Text("${next.set.step.distance} ${next.set.step.stroke.displayName()}",
                color = watchWhite, fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
            next.set.step.note?.let {
                Text(it, color = watchSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(0.5f))
    }
}

@Composable
private fun SessionMetrics(session: SwimSession) {
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(if (session.mode == SwimMode.POOL) "POOL SWIM  ·  MANUAL" else "GUIDED  ·  MANUAL", watchCyan)
        Spacer(Modifier.height(if (compact) 7.dp else 10.dp))
        Text(formatTime(session.elapsedSeconds), color = watchWhite,
            fontSize = if (compact) 40.sp else 48.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp, maxLines = 1)
        Eyebrow("ELAPSED")
        Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(session.completedDistance.toString(), color = watchWhite,
                fontSize = if (compact) 30.sp else 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(4.dp))
            Text("m", color = watchCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 5.dp))
        }
        Text("${session.completedLengths} ${if (session.completedLengths == 1) "LENGTH" else "LENGTHS"} " +
            (if (session.mode == SwimMode.POOL) "LOGGED" else "COUNTED"),
            color = watchSecondary, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlsScreen(
    session: SwimSession,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    onLength: () -> Unit,
    onLock: () -> Unit,
    onDrill: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit
) {
    Column(Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(if (session.mode == SwimMode.POOL) "POOL SWIM" else "GUIDED WORKOUT", watchSecondary)
        Spacer(Modifier.height(7.dp))
        when {
            session.mode == SwimMode.POOL -> {
                WideButton("LOG LENGTH  +${session.poolLength}m", true, onLength)
                Spacer(Modifier.height(8.dp))
            }
            session.phase is SessionPhase.Rest -> {
                WideButton("SKIP REST", true, onSkip)
                Spacer(Modifier.height(8.dp))
            }
            else -> {
                WideButton("REP DONE", true, onAdvance)
                Spacer(Modifier.height(8.dp))
            }
        }
        Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ControlTile("PAUSE", Modifier.weight(1f), onPause)
            ControlTile("LOCK", Modifier.weight(1f), onLock)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ControlTile("DRILL / KICK", Modifier.weight(1f), onDrill)
            ControlTile("END", Modifier.weight(1f), onEnd)
        }
    }
}

@Composable
private fun ControlTile(text: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.height(if (isCompactWatch()) 37.dp else 40.dp).clip(shape)
        .background(watchCyanDark).border(1.dp, watchOutline, shape)
        .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = watchWhite, fontSize = if (isCompactWatch()) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun LockScreen(onUnlock: () -> Unit) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 14.dp else 24.dp) {
        Status("LOCKED", watchCyan)
        Spacer(Modifier.weight(0.7f))
        val shape = RoundedCornerShape(60.dp)
        Box(
            Modifier.size(if (compact) 96.dp else 112.dp).clip(shape)
                .background(watchCyanDark).border(2.dp, watchCyan, shape)
                .semantics { contentDescription = "Press and hold for two seconds to unlock"; role = Role.Button }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val start = SystemClock.elapsedRealtime()
                        if (tryAwaitRelease() && SystemClock.elapsedRealtime() - start >= 2000L) onUnlock()
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Text("HOLD", color = watchWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Eyebrow("HOLD 2 SEC TO UNLOCK", watchSecondary)
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
        Status("ADD DISTANCE", watchCyan)
        Spacer(Modifier.weight(0.3f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip("KICK", type == ManualSwimType.KICK) { onType(ManualSwimType.KICK) }
            FilterChip("DRILL", type == ManualSwimType.DRILL) { onType(ManualSwimType.DRILL) }
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
            Eyebrow("$poolLength m STEPS", watchSecondary)
            StepButton("+") { onAmount(amount + poolLength) }
        }
        Spacer(Modifier.weight(0.5f))
        WideButton("ADD DISTANCE", true, onAdd)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val compact = isCompactWatch()
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.width(if (compact) 40.dp else 47.dp).height(if (compact) 34.dp else 40.dp)
            .clip(shape).background(watchCyanDark).border(1.dp, watchOutline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = watchWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PausedScreen(session: SwimSession, onResume: () -> Unit, onEnd: () -> Unit) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 24.dp else 38.dp) {
        Status("PAUSED", watchAmber)
        Spacer(Modifier.weight(0.6f))
        Text(formatTime(session.elapsedSeconds), color = watchWhite,
            fontSize = if (compact) 35.sp else 42.sp, fontWeight = FontWeight.ExtraBold)
        Text("${session.completedDistance} m completed", color = watchSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(0.8f))
        WideButton("RESUME", true, onResume)
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        WideButton("END WORKOUT", false, onEnd)
    }
}

@Composable
internal fun EndConfirmScreen(session: SwimSession, onKeep: () -> Unit, onFinish: () -> Unit) {
    val compact = isCompactWatch()
    FixedPage(bottomPadding = if (compact) 24.dp else 38.dp) {
        Status(if (compact) "END?" else "END WORKOUT?", watchAmber)
        Spacer(Modifier.weight(0.7f))
        Heading("Finish now?")
        Text("${session.completedDistance} m completed", color = watchSecondary, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        WideButton("KEEP SWIMMING", true, onKeep)
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        WideButton("FINISH WORKOUT", false, onFinish)
    }
}

@Composable
internal fun CompleteScreen(session: SwimSession, onBack: () -> Unit) {
    val compact = isCompactWatch()
    val workout = session.workout
    val finished = workout == null || session.guided!!.completedRepetitions == workout.totalRepetitions
    ScrollPage {
        Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
        Status(if (finished) "COMPLETE" else "ENDED", watchCyan)
        Spacer(Modifier.height(3.dp))
        Heading(workout?.displayName() ?: "Pool Swim")
        Spacer(Modifier.height(1.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(session.completedDistance.toString(), color = watchWhite,
                fontSize = if (compact) 38.sp else 44.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
            Spacer(Modifier.width(5.dp))
            Text("m", color = watchCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp))
        }
        Eyebrow(if (workout == null) "MANUALLY LOGGED" else "OF ${workout.totalDistance} PLANNED")
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(0.88f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MetricTile("TIME", formatTime(session.elapsedSeconds), Modifier.weight(1f))
            if (workout == null) {
                MetricTile("LENGTHS", session.completedLengths.toString(), Modifier.weight(1f))
            } else {
                MetricTile("REPS", "${session.guided!!.completedRepetitions}/${workout.totalRepetitions}", Modifier.weight(1f))
                MetricTile("SETS", "${session.guided!!.completedSets}/${workout.sections.sumOf { it.sets.size }}", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        WideButton("BACK TO HOME", true, onBack)
    }
}
