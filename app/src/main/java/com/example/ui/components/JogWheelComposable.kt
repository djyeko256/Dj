package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjSkinColors
import kotlin.math.*

@Composable
fun JogWheelComposable(
    deckNumber: Int,
    deckColor: Color,
    angleDegrees: Float,
    bpm: Float,
    keyCamelot: String,
    musicalKey: String,
    positionSeconds: Float,
    durationSeconds: Float,
    isPlaying: Boolean,
    isScratching: Boolean,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onScratchDelta: (Float) -> Unit = {}
) {
    var previousDragAngle by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .testTag("jog_wheel_deck_$deckNumber")
            .aspectRatio(1f)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(skinColors.jogRimColor)
            .pointerInput(deckNumber) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        previousDragAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val currentAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                        previousDragAngle?.let { prev ->
                            var delta = currentAngle - prev
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            onScratchDelta(delta)
                        }
                        previousDragAngle = currentAngle
                    },
                    onDragEnd = {
                        previousDragAngle = null
                    },
                    onDragCancel = {
                        previousDragAngle = null
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Canvas for outer strobe rim, vinyl grooves, spinning marker, and touch indicator
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f
            val platterRadius = outerRadius * 0.92f

            // Outer Strobe Dots Rim
            drawCircle(
                color = skinColors.jogRimColor,
                radius = outerRadius,
                center = center
            )

            // Outer bevel ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(skinColors.jogRimColor.copy(alpha = 0.5f), Color.Black),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = outerRadius * 0.08f)
            )

            // Vinyl Platter
            drawCircle(
                color = skinColors.jogPlatterColor,
                radius = platterRadius,
                center = center
            )

            // Vinyl Micro-grooves
            for (i in 1..8) {
                val grooveRadius = platterRadius * (0.50f + i * 0.05f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = grooveRadius,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }

            // Outer rotating cue marker / LED ring
            rotate(degrees = angleDegrees, pivot = center) {
                // Colored spinning cue line
                drawLine(
                    color = deckColor,
                    start = Offset(center.x, center.y - platterRadius),
                    end = Offset(center.x, center.y - platterRadius * 0.52f),
                    strokeWidth = 6f
                )

                // Neon glow strobe notch on outer rim
                drawCircle(
                    color = skinColors.jogAccentGlow,
                    radius = 5f,
                    center = Offset(center.x, center.y - platterRadius * 0.96f)
                )
            }

            // Center LCD Display Hub
            val hubRadius = platterRadius * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(skinColors.jogCenterColor, Color.Black),
                    center = center,
                    radius = hubRadius
                ),
                radius = hubRadius,
                center = center
            )

            // Glow ring around center hub
            drawCircle(
                color = if (isPlaying) deckColor else skinColors.jogAccentGlow.copy(alpha = 0.4f),
                radius = hubRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Center LCD Display details (BPM, Key, Time, Deck Label)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            // Deck Badge
            Text(
                text = "DECK ${if (deckNumber == 1) "A" else if (deckNumber == 2) "B" else if (deckNumber == 3) "C" else "D"}",
                color = deckColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            // BPM readout
            Text(
                text = String.format("%.1f", bpm),
                color = skinColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Time Remaining / Elapsed
            val totalSec = positionSeconds.toInt()
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                color = skinColors.textSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            // Camelot Key
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = keyCamelot,
                    color = deckColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "($musicalKey)",
                    color = skinColors.textSecondary,
                    fontSize = 9.sp
                )
            }
        }
    }
}
