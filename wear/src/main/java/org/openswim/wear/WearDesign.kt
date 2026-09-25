package org.openswim.wear

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

internal val watchBackground = Color(0xFF050D13)
internal val watchPanel = Color(0xFF162B38)
internal val watchOutline = Color(0xFF294C5D)
internal val watchCyan = Color(0xFF38D6FF)
internal val watchWhite = Color(0xFFFFFFFF)
internal val watchSecondary = Color(0xFFA7B3BE)
internal val watchAmber = Color(0xFFFFC981)
internal val watchDanger = Color(0xFFF09A9A)

@Composable
internal fun SwimMark(modifier: Modifier = Modifier, color: Color = watchCyan) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val wave = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h * 0.42f)
            cubicTo(w * 0.17f, h * 0.18f, w * 0.25f, h * 0.68f, w * 0.5f, h * 0.42f)
            cubicTo(w * 0.73f, h * 0.16f, w * 0.82f, h * 0.64f, w, h * 0.38f)
        }
        drawPath(wave, color, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun ProgressArc(fraction: Float, color: Color = watchCyan, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(watchOutline.copy(alpha = 0.65f), -135f, 270f, false,
            Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, -135f, 270f * fraction.coerceIn(0f, 1f), false,
            Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

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
            Spacer(Modifier.height(28.dp))
        }
    )
}

@Composable
internal fun FixedPage(bottomPadding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val compact = isCompactWatch()
    Column(
        Modifier.fillMaxSize().background(watchBackground)
            .padding(start = 24.dp, end = 24.dp, top = if (compact) 9.dp else 14.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
internal fun Eyebrow(text: String, color: Color = watchSecondary) {
    Text(text, color = color, fontSize = if (isCompactWatch()) 9.sp else 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
internal fun Heading(text: String) {
    Text(text, color = watchWhite, fontSize = if (isCompactWatch()) 22.sp else 25.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2,
        lineHeight = if (isCompactWatch()) 24.sp else 28.sp)
}

@Composable
internal fun WideButton(text: String, primary: Boolean = true, onClick: () -> Unit) {
    val compact = isCompactWatch()
    val shape = RoundedCornerShape(50.dp)
    Box(
        Modifier.fillMaxWidth(0.9f).height(if (compact) 37.dp else 42.dp).clip(shape)
            .background(if (primary) watchCyan else watchPanel)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) watchBackground else watchWhite,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, maxLines = 1)
    }
}

@Composable
internal fun TextAction(text: String, color: Color = watchSecondary, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(0.9f).height(if (isCompactWatch()) 24.dp else 27.dp)
        .clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = if (isCompactWatch()) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, maxLines = 1)
    }
}

@Composable
internal fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(watchOutline.copy(alpha = 0.62f)))
}

@Composable
internal fun MenuRow(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(0.9f).height(if (isCompactWatch()) 37.dp else 43.dp)
        .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = watchWhite, fontSize = if (isCompactWatch()) 17.sp else 19.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text("›", color = watchCyan, fontSize = 22.sp)
    }
}

@Composable
internal fun WorkoutListItem(workout: Workout, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (isCompactWatch()) 54.dp else 62.dp)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (isCompactWatch()) 29.dp else 35.dp)
            .clip(RoundedCornerShape(11.dp)).background(watchPanel), contentAlignment = Alignment.Center) {
            SwimMark(Modifier.size(21.dp, 15.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(workout.displayName(), color = watchWhite,
                fontSize = if (isCompactWatch()) 14.sp else 16.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${workout.totalDistance}  ·  ~${workout.estimatedMinutes} min",
                color = watchSecondary, fontSize = if (isCompactWatch()) 9.sp else 10.sp, maxLines = 1)
            Text(workout.type.uppercase(), color = watchCyan,
                fontSize = if (isCompactWatch()) 8.sp else 9.sp, maxLines = 1)
        }
        Text("›", color = watchCyan, fontSize = 21.sp)
    }
    Hairline()
}

@Composable
internal fun PoolChoiceRow(length: Int, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(0.9f).height(if (isCompactWatch()) 31.dp else 36.dp)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp))
            .background(if (selected) watchCyan else watchOutline))
        Spacer(Modifier.width(9.dp))
        Text("$length m pool", color = if (enabled) watchWhite else watchSecondary,
            fontSize = if (isCompactWatch()) 13.sp else 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(if (!enabled) "UNAVAILABLE" else if (selected) "CHANGE" else "SELECT",
            color = if (selected) watchCyan else watchSecondary, fontSize = 9.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SectionBlock(section: WorkoutSection) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Eyebrow(section.name.uppercase(), watchCyan)
        Spacer(Modifier.height(5.dp))
        section.sets.forEachIndexed { index, set ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            Text("${if (set.repetitions > 1) "${set.repetitions} × " else ""}${set.step.distance}  ${set.step.stroke.displayName()}",
                color = watchWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2,
                lineHeight = 17.sp)
            if (set.step.restAfter.seconds > 0) {
                Text("${set.step.restAfter.seconds}s rest", color = watchSecondary, fontSize = 11.sp)
            }
            set.step.note?.let { Text(it, color = watchSecondary, fontSize = 11.sp, maxLines = 2) }
            if (set.step.equipment.isNotEmpty()) {
                Text(set.step.equipment.joinToString { it.name.lowercase().replace('_', ' ') },
                    color = watchSecondary, fontSize = 11.sp)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Hairline()
}

@Composable
internal fun RestDial(remaining: Int, total: Int) {
    val compact = isCompactWatch()
    val fraction by animateFloatAsState(
        targetValue = (remaining.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(800, easing = LinearEasing), label = "rest countdown"
    )
    Box(Modifier.size(if (compact) 94.dp else 113.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(watchOutline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(watchCyan, -90f, 360f * fraction, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(formatTime(remaining), color = watchWhite, fontSize = if (compact) 34.sp else 41.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, maxLines = 1, softWrap = false)
    }
}

internal fun SwimStroke.displayName() = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
internal fun Workout.displayName() =
    if (id.startsWith("concept-")) name else name.removeSuffix(" ${totalDistance.amount}")
internal fun formatTime(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)
