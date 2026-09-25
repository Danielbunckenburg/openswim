package org.openswim.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cyan = Color(0xFF18D9EF)
private val DeepNavy = Color(0xFF061C2B)

@Composable
internal fun HomeLanding(onStartSwim: () -> Unit, onGuidedWorkout: () -> Unit, onAccount: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(DeepNavy)) {
        Image(
            painter = painterResource(R.drawable.home_water),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0xB9041929),
                    0.28f to Color(0x18041929),
                    0.58f to Color.Transparent,
                    0.73f to Color(0x49041929),
                    1f to Color(0xF2051825)
                )
            )
        )

        Text(
            "Sign in / Create account",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 43.dp, end = 16.dp)
                .clickable(onClick = onAccount)
                .padding(9.dp)
        )

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.125f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WaveMark()
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Open", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp)
                Text("Swim", color = Cyan, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp)
            }
            Spacer(Modifier.height(15.dp))
            Text(
                "Better swimming\nfor everyone.",
                color = Color(0xFFD7E4EE),
                fontSize = 19.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 21.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 37.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(if (index == 0) Cyan else Color(0xFF456C85))
                    )
                }
            }
            Spacer(Modifier.height(23.dp))
            Button(
                onClick = onStartSwim,
                modifier = Modifier.fillMaxWidth().height(59.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DeepNavy),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                SwimMark(DeepNavy)
                Spacer(Modifier.size(15.dp))
                Text("Start a Swim", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onGuidedWorkout,
                modifier = Modifier.fillMaxWidth().height(59.dp).border(1.dp, Cyan, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x6802182A), contentColor = Color.White)
            ) {
                ClipboardMark()
                Spacer(Modifier.size(15.dp))
                Text("Guided Workout", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WaveMark() {
    Canvas(Modifier.size(width = 66.dp, height = 43.dp).semantics { contentDescription = "OpenSwim" }) {
        val w = size.width
        val h = size.height
        val upper = Path().apply {
            moveTo(w * .08f, h * .27f)
            cubicTo(w * .27f, h * .09f, w * .42f, h * .06f, w * .57f, h * .27f)
            cubicTo(w * .72f, h * .44f, w * .82f, h * .42f, w * .94f, h * .27f)
        }
        val lower = Path().apply {
            moveTo(w * .08f, h * .69f)
            cubicTo(w * .28f, h * .50f, w * .42f, h * .53f, w * .57f, h * .70f)
            cubicTo(w * .72f, h * .89f, w * .82f, h * .82f, w * .94f, h * .67f)
        }
        drawPath(upper, Cyan, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
        drawPath(lower, Cyan, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun SwimMark(color: Color) {
    Canvas(Modifier.size(34.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * .10f, center = Offset(w * .69f, h * .19f))
        val arm = Path().apply {
            moveTo(w * .27f, h * .58f)
            cubicTo(w * .41f, h * .54f, w * .48f, h * .37f, w * .61f, h * .40f)
            lineTo(w * .81f, h * .58f)
        }
        drawPath(arm, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        val water = Path().apply {
            moveTo(w * .08f, h * .79f)
            cubicTo(w * .23f, h * .67f, w * .34f, h * .92f, w * .49f, h * .79f)
            cubicTo(w * .64f, h * .67f, w * .75f, h * .91f, w * .91f, h * .79f)
        }
        drawPath(water, color, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun ClipboardMark() {
    Canvas(Modifier.size(31.dp)) {
        val white = Color.White
        val stroke = 2.5.dp.toPx()
        drawRoundRect(
            color = white,
            topLeft = Offset(size.width * .19f, size.height * .18f),
            size = androidx.compose.ui.geometry.Size(size.width * .63f, size.height * .76f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke)
        )
        drawRoundRect(
            color = white,
            topLeft = Offset(size.width * .37f, size.height * .08f),
            size = androidx.compose.ui.geometry.Size(size.width * .27f, size.height * .17f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(stroke)
        )
        for (fraction in listOf(.47f, .62f, .77f)) {
            drawLine(white, Offset(size.width * .34f, size.height * fraction), Offset(size.width * .67f, size.height * fraction), stroke, cap = StrokeCap.Round)
        }
    }
}
