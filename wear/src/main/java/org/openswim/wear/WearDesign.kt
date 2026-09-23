package org.openswim.wear

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import org.openswim.core.Stroke as SwimStroke
import org.openswim.core.Workout
import org.openswim.core.WorkoutSection

internal val watchBackground = Color(0xFF02080D)
internal val watchPanel = Color(0xFF0D1B24)
internal val watchPanelTop = Color(0xFF122530)
internal val watchOutline = Color(0xFF29414C)
internal val watchCyan = Color(0xFF67DEFA)
internal val watchCyanDark = Color(0xFF122D39)
internal val watchWhite = Color(0xFFF5FBFF)
internal val watchSecondary = Color(0xFFA7BBC5)
internal val watchAmber = Color(0xFFFFC878)

@Composable
internal fun isCompactWatch() = LocalConfiguration.current.screenHeightDp <= 200

@Composable
internal fun ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(watchBackground).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            content()
            Spacer(Modifier.height(24.dp))
        }
    )
}

@Composable
internal fun FixedPage(bottomPadding: Dp = 15.dp, content: @Composable ColumnScope.() -> Unit) {
    val compact = isCompactWatch()
    Column(
        Modifier.fillMaxSize().background(watchBackground)
            .padding(start = 24.dp, end = 24.dp, top = if (compact) 8.dp else 15.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
internal fun Eyebrow(text: String, color: Color = watchSecondary) {
    val compact = isCompactWatch()
    Text(text, color = color, fontSize = if (compact) 9.sp else 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = if (compact) 0.7.sp else 1.2.sp,
        textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
internal fun Status(text: String, color: Color) {
    val compact = isCompactWatch()
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!compact) {
            Box(Modifier.size(6.dp).background(color, RoundedCornerShape(3.dp)))
            Spacer(Modifier.width(6.dp))
        }
        Eyebrow(text, color)
    }
}

@Composable
internal fun Heading(text: String) {
    val compact = isCompactWatch()
    Text(text, color = watchWhite, fontSize = if (compact) 22.sp else 25.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        lineHeight = if (compact) 25.sp else 28.sp)
}

@Composable
internal fun WideButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val compact = isCompactWatch()
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth(0.88f).height(if (compact) 38.dp else 43.dp).clip(shape)
            .background(if (primary) watchCyan else watchCyanDark)
            .border(1.dp, if (primary) watchCyan else watchOutline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) watchBackground else watchWhite,
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
internal fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.clip(shape).background(if (selected) watchCyan else watchPanel)
            .border(1.dp, if (selected) watchCyan else watchOutline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) watchBackground else watchWhite,
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun PoolOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(shape)
            .background(if (selected) watchCyanDark else watchPanel)
            .border(1.dp, if (selected) watchCyan else watchOutline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (enabled) watchWhite else watchSecondary,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(if (!enabled) "25 m ONLY" else if (selected) "SELECTED" else "SELECT",
            color = if (selected) watchCyan else watchSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun WorkoutCard(workout: Workout, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(watchPanelTop, watchPanel)))
            .border(BorderStroke(1.dp, watchOutline), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Eyebrow(workout.type.uppercase(), watchCyan)
        Spacer(Modifier.height(4.dp))
        Text(workout.displayName(), color = watchWhite, fontSize = 19.sp,
            fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(workout.totalDistance.toString(), color = watchCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("~${workout.estimatedMinutes} MIN", color = watchSecondary,
                fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
internal fun SectionCard(section: WorkoutSection) {
    val shape = RoundedCornerShape(17.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(watchPanel)
        .border(1.dp, watchOutline, shape).padding(13.dp)) {
        Eyebrow(section.name.uppercase(), watchCyan)
        Spacer(Modifier.height(8.dp))
        section.sets.forEachIndexed { index, set ->
            if (index > 0) Spacer(Modifier.height(9.dp))
            Text("${if (set.repetitions > 1) "${set.repetitions} × " else ""}${set.step.distance}",
                color = watchWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
            Text(buildString {
                append(set.step.stroke.displayName())
                if (set.step.restAfter.seconds > 0) append("  ·  ${set.step.restAfter.seconds}s rest")
            }, color = watchSecondary, fontSize = 11.sp, lineHeight = 13.sp)
            set.step.note?.let { Text(it, color = watchSecondary, fontSize = 11.sp, lineHeight = 13.sp) }
            if (set.step.equipment.isNotEmpty()) {
                Text(set.step.equipment.joinToString { it.name.lowercase().replace('_', ' ') },
                    color = watchCyan, fontSize = 11.sp)
            }
        }
    }
}

@Composable
internal fun ThinProgress(progress: Float) {
    val fraction by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(300), label = "progress")
    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(watchOutline)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(watchCyan))
    }
}

@Composable
internal fun MetricTile(label: String, value: String, modifier: Modifier) {
    val compact = isCompactWatch()
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier.clip(shape).background(watchPanel).border(1.dp, watchOutline, shape)
            .padding(vertical = if (compact) 2.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Eyebrow(label)
        Text(value, color = watchWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun RestDial(remaining: Int, total: Int) {
    val compact = isCompactWatch()
    val fraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = LinearEasing), label = "rest countdown"
    )
    Box(Modifier.size(if (compact) 84.dp else 116.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(watchOutline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(watchAmber, -90f, 360f * fraction, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(formatTime(remaining), color = watchWhite, fontSize = if (compact) 30.sp else 42.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, maxLines = 1, softWrap = false)
    }
}

internal fun SwimStroke.displayName() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
internal fun Workout.displayName() = name.removeSuffix(" ${totalDistance.amount}")
internal fun formatTime(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)
