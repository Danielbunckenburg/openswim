package org.openswim.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import org.openswim.core.Workout
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun WatchClock() {
    val formatter = DateTimeFormatter.ofPattern("H:mm")
    val time by produceState(initialValue = LocalTime.now().format(formatter)) {
        while (true) {
            delay(1000)
            val next = LocalTime.now().format(formatter)
            if (value != next) value = next
        }
    }
    Text(time, color = watchWhite,
        fontSize = if (isCompactWatch()) 9.sp else 10.sp)
}

@Composable
internal fun ConceptCHome(
    poolLength: Int,
    onStart: () -> Unit,
    onLibrary: () -> Unit
) {
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize().background(watchBackground)
        .padding(horizontal = 24.dp)
        .padding(top = if (compact) 9.dp else 13.dp,
            bottom = if (compact) 7.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        WatchClock()
        Spacer(Modifier.height(if (compact) 3.dp else 6.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Open", color = watchWhite, fontSize = if (compact) 17.sp else 19.sp,
                    fontWeight = FontWeight.Bold)
                Text("Swim", color = watchCyan, fontSize = if (compact) 17.sp else 19.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(if (compact) 96.dp else 110.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(watchCyan.copy(alpha = 0.29f),
                watchCyan.copy(alpha = 0.08f), Color.Transparent)))
            .clickable(role = Role.Button, onClick = onStart), contentAlignment = Alignment.Center) {
            Box(Modifier.size(if (compact) 84.dp else 95.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF075578), Color(0xFF042B40))))
                .border(1.dp, watchCyan, CircleShape)
                .clickable(role = Role.Button, onClick = onStart),
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(if (compact) 44.dp else 52.dp)) {
                    val w = size.width
                    val h = size.height
                    drawCircle(watchCyan, radius = w * 0.105f, center = Offset(w * 0.65f, h * 0.28f))
                    val stroke = 4.dp.toPx()
                    drawLine(watchCyan, Offset(w * 0.30f, h * 0.59f),
                        Offset(w * 0.68f, h * 0.42f), stroke, cap = StrokeCap.Round)
                    drawLine(watchCyan, Offset(w * 0.43f, h * 0.58f),
                        Offset(w * 0.22f, h * 0.76f), stroke, cap = StrokeCap.Round)
                    drawLine(watchCyan, Offset(w * 0.55f, h * 0.52f),
                        Offset(w * 0.83f, h * 0.67f), stroke, cap = StrokeCap.Round)
                    drawArc(watchCyan, 8f, 164f, false,
                        topLeft = Offset(w * 0.07f, h * 0.57f),
                        size = androidx.compose.ui.geometry.Size(w * 0.85f, h * 0.31f),
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
        Text("Start a Swim", color = watchWhite, fontSize = if (compact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(role = Role.Button, onClick = onStart))
        Spacer(Modifier.height(if (compact) 3.dp else 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SwimMark(Modifier.size(15.dp, 12.dp))
            Spacer(Modifier.width(5.dp))
            Text("Pool  ·  $poolLength m", color = watchSecondary,
                fontSize = if (compact) 11.sp else 12.sp)
        }
        Spacer(Modifier.weight(0.6f))
        Text("⌃", color = watchCyan, fontSize = if (compact) 18.sp else 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(role = Role.Button, onClick = onLibrary))
        Spacer(Modifier.height(if (compact) 2.dp else 5.dp))
    }
}

@Composable
internal fun ConceptCLibrary(
    category: String,
    onCategory: (String) -> Unit,
    onWorkout: (Workout) -> Unit,
    onSettings: () -> Unit
) {
    val compact = isCompactWatch()
    Column(Modifier.fillMaxSize().background(watchBackground)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = if (compact) 23.dp else 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(if (compact) 7.dp else 10.dp))
        WatchClock()
        Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
        Text("Workouts", color = watchCyan, fontSize = if (compact) 16.sp else 17.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("All", "Easy", "Technique").forEach { option ->
                Box(Modifier.height(if (compact) 23.dp else 24.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (option == category) watchCyan else watchPanel)
                    .clickable(role = Role.Button) { onCategory(option) }
                    .padding(horizontal = if (compact) 10.dp else 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text(option, color = if (option == category) watchBackground else watchWhite,
                        fontSize = if (compact) 10.sp else 11.sp)
                }
            }
        }
        Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
        SampleConceptRows(category, onWorkout)
        TextAction("SETTINGS", onClick = onSettings)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SampleConceptRows(category: String, onWorkout: (Workout) -> Unit) {
    val compact = isCompactWatch()
    val workouts = ConceptCWorkouts.all
        .filter { category == "All" || it.type == category }
    workouts.forEach { workout ->
        val accent = when {
            workout.id == "concept-technique-1200" -> watchCyan
            workout.id == "concept-threshold-1800" -> Color(0xFFFFBA55)
            else -> when (workout.type) {
            "Easy" -> watchCyan
            "Technique" -> Color(0xFFB8E953)
            "Threshold", "Sprint" -> Color(0xFFFFBA55)
            else -> watchCyan
            }
        }
        Row(Modifier.fillMaxWidth().height(if (compact) 36.dp else 39.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(watchPanel)
            .clickable(role = Role.Button) { onWorkout(workout) }
            .padding(horizontal = if (compact) 12.dp else 15.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
                when (workout.id) {
                    "concept-aerobic-1500" -> SwimMark(Modifier.size(21.dp, 15.dp), accent)
                    "concept-threshold-1800" ->
                        Text("◔", color = accent, fontSize = 22.sp)
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom) {
                        listOf(7.dp, 12.dp, 17.dp).forEach { height ->
                            Box(Modifier.width(3.dp).height(height)
                                .clip(RoundedCornerShape(2.dp)).background(accent))
                        }
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                Text(workout.displayName(), color = watchWhite,
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(if (workout.id.startsWith("concept-"))
                        "~${workout.estimatedMinutes} min  ·  25 m"
                    else "${workout.totalDistance}  ·  ~${workout.estimatedMinutes} min",
                    color = watchSecondary, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
            }
            Text("›", color = watchWhite, fontSize = 19.sp)
        }
        Spacer(Modifier.height(if (compact) 4.dp else 5.dp))
    }
}

@Composable
internal fun ConceptCDetail(
    workout: Workout,
    poolLength: Int,
    onBack: () -> Unit,
    onPool: () -> Unit,
    onStart: () -> Unit
) {
    val compact = isCompactWatch()
    Box(Modifier.fillMaxSize().background(watchBackground)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = if (compact) 29.dp else 33.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
            WatchClock()
            Spacer(Modifier.height(if (compact) 7.dp else 10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("‹", color = watchCyan, fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(start = if (compact) 10.dp else 12.dp)
                        .clickable(role = Role.Button, onClick = onBack))
                Text(workout.displayName(), color = watchWhite,
                    fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
            Text("GUIDED WORKOUT", color = watchSecondary,
                fontSize = if (compact) 8.sp else 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
            ConceptCStat("▥", String.format(Locale.US, "%,d m", workout.totalDistance.amount))
            ConceptCStat("◷", "~${workout.estimatedMinutes} min")
            ConceptCStat("≋", workout.sections.joinToString(" + ") { it.name }
                .replace("Warm up", "Warm").replace("Main set", "Main")
                .replace("Cool down", "Cool"))
            Spacer(Modifier.height(if (compact) 4.dp else 5.dp))
            Row(Modifier.fillMaxWidth().height(if (compact) 29.dp else 32.dp)
                .clip(RoundedCornerShape(50.dp)).background(watchPanel)
                .clickable(role = Role.Button, onClick = onPool)
                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Pool length", color = watchSecondary, fontSize = if (compact) 10.sp else 11.sp)
                Spacer(Modifier.weight(1f))
                Text("$poolLength m  ⌄", color = watchWhite,
                    fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            workout.sections.forEach { SectionBlock(it); Spacer(Modifier.height(7.dp)) }
            Spacer(Modifier.height(52.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .height(if (compact) 48.dp else 53.dp)
            .background(watchBackground)
            .padding(start = 24.dp, end = 24.dp, top = 2.dp),
            contentAlignment = Alignment.TopCenter) {
            WideButton("START WORKOUT", onClick = onStart)
        }
    }
}

@Composable
private fun ConceptCStat(icon: String, value: String) {
    Row(Modifier.fillMaxWidth().height(if (isCompactWatch()) 16.dp else 17.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = watchCyan, fontSize = 13.sp, modifier = Modifier.width(24.dp))
        Text(value, color = watchWhite, fontSize = if (isCompactWatch()) 11.sp else 12.sp,
            maxLines = 1)
    }
}
