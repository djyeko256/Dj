package com.example.ui.components.midi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DjMidiService
import com.example.ui.theme.DjSkinColors
import kotlin.math.*

/**
 * Tactile Rotary Knob component for MIDI hardware simulation
 */
@Composable
fun HardwareRotaryKnob(
    label: String,
    value: Float, // 0.0 to 1.0
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    bipolar: Boolean = false, // If true, center is neutral (e.g. EQ / Filter)
    subLabel: String = ""
) {
    var currentValue by remember(value) { mutableStateOf(value) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .size(46.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Drag up/right increases, drag down/left decreases
                        val delta = (-dragAmount.y + dragAmount.x) / 120f
                        val next = (currentValue + delta).coerceIn(0f, 1f)
                        currentValue = next
                        onValueChange(next)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 4.dp.toPx()

                // Background track arc (from 135 deg to 405 deg)
                val startAngle = 135f
                val sweepTotal = 270f
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active value arc
                if (bipolar) {
                    val midAngle = 135f + sweepTotal / 2f // 270 deg (top center)
                    val offsetSweep = (currentValue - 0.5f) * sweepTotal
                    val arcStart = if (offsetSweep >= 0) midAngle else midAngle + offsetSweep
                    drawArc(
                        color = accentColor,
                        startAngle = arcStart,
                        sweepAngle = abs(offsetSweep),
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    drawArc(
                        color = accentColor,
                        startAngle = startAngle,
                        sweepAngle = sweepTotal * currentValue,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Metallic Knob Body
                val knobRadius = radius - 5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                        center = center,
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF475569),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Indicator pointer notch
                val angleRad = Math.toRadians((startAngle + sweepTotal * currentValue).toDouble())
                val notchStart = Offset(
                    (center.x + (knobRadius * 0.35f) * cos(angleRad)).toFloat(),
                    (center.y + (knobRadius * 0.35f) * sin(angleRad)).toFloat()
                )
                val notchEnd = Offset(
                    (center.x + (knobRadius * 0.9f) * cos(angleRad)).toFloat(),
                    (center.y + (knobRadius * 0.9f) * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (bipolar && abs(currentValue - 0.5f) < 0.03f) Color.White else accentColor,
                    start = notchStart,
                    end = notchEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Value text readout
        val displayStr = if (bipolar) {
            val db = ((currentValue - 0.5f) * 24f).roundToInt()
            if (db > 0) "+$db" else "$db"
        } else {
            "${(currentValue * 127).roundToInt()}"
        }
        Text(
            text = if (subLabel.isNotEmpty()) subLabel else displayStr,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Tactile Vertical Fader component for hardware slider simulation
 */
@Composable
fun HardwareVerticalFader(
    label: String,
    value: Float, // 0.0 to 1.0 (0=bottom, 1=top)
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    height: Int = 110
) {
    var currentValue by remember(value) { mutableStateOf(value) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(height.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging upwards increases value
                        val delta = -dragAmount.y / height
                        val next = (currentValue + delta).coerceIn(0f, 1f)
                        currentValue = next
                        onValueChange(next)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track slot
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF0F172A))
                    .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(3.dp))
            )

            // Graduated ticks along track
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tickSteps = 8
                for (i in 0..tickSteps) {
                    val y = size.height * (i.toFloat() / tickSteps)
                    val tickLen = if (i == 0 || i == tickSteps / 2 || i == tickSteps) 7.dp.toPx() else 4.dp.toPx()
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(2.dp.toPx(), y),
                        end = Offset(2.dp.toPx() + tickLen, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(size.width - 2.dp.toPx() - tickLen, y),
                        end = Offset(size.width - 2.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // Fader Knob Handle
            val knobHeight = 24.dp
            val availableHeight = height.dp - knobHeight
            val offsetY = availableHeight * (1f - currentValue)

            Box(
                modifier = Modifier
                    .offset(y = offsetY - (availableHeight / 2f))
                    .width(32.dp)
                    .height(knobHeight)
                    .shadow(4.dp, RoundedCornerShape(3.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .border(1.dp, Color(0xFF64748B), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                // White center marker on fader cap
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.dp)
                        .background(accentColor)
                )
            }
        }

        Text(
            text = "${(currentValue * 127).roundToInt()}",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Tactile Horizontal Crossfader for hardware simulation
 */
@Composable
fun HardwareHorizontalCrossfader(
    value: Float, // -1.0 to +1.0
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPos by remember(value) { mutableStateOf(value) }
    val norm = (currentPos + 1.0f) / 2.0f // 0.0 to 1.0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DECK A / C", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
            Text("CROSSFADER", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF94A3B8))
            Text("DECK B / D", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0F1D))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = dragAmount.x / 240f
                        val next = (currentPos + delta).coerceIn(-1.0f, 1.0f)
                        currentPos = next
                        onValueChange(next)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track slot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF0F172A))
            )

            // Center detent mark
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(18.dp)
                    .background(Color(0xFF475569))
            )

            // Crossfader handle
            val handleWidth = 32.dp
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxOffset = maxWidth - handleWidth
                val xPos = maxOffset * norm

                Box(
                    modifier = Modifier
                        .offset(x = xPos)
                        .width(handleWidth)
                        .height(30.dp)
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(1.dp, Color(0xFF64748B), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(18.dp)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

/**
 * Backlit illuminated hardware button/pad
 */
@Composable
fun HardwareIlluminatedButton(
    label: String,
    subLabel: String = "",
    activeColor: Color,
    isLit: Boolean = false,
    onPress: () -> Unit,
    onRelease: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = when {
            isPressed -> activeColor.copy(alpha = 0.85f)
            isLit -> activeColor.copy(alpha = 0.35f)
            else -> Color(0xFF131D2E)
        },
        label = "btnColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isLit -> activeColor
            else -> Color(0xFF334155)
        },
        label = "btnBorder"
    )

    Box(
        modifier = modifier
            .shadow(if (isLit || isPressed) 6.dp else 1.dp, RoundedCornerShape(6.dp), spotColor = activeColor)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease?.invoke()
                    }
                )
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isPressed || isLit) Color.White else activeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed || isLit) Color.White else Color(0xFFE2E8F0),
                maxLines = 1
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPressed || isLit) Color(0xFFF1F5F9) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

/**
 * Interactive hardware jog wheel
 */
@Composable
fun HardwareJogWheel(
    deckLabel: String,
    accentColor: Color,
    onScratchMove: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Text(
            text = "JOG WHEEL ($deckLabel)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(1.5.dp, Color(0xFF334155), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount.x - dragAmount.y) * 1.5f
                        rotationAngle = (rotationAngle + delta) % 360f
                        onScratchMove(delta)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // Outer silver rim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Vinyl Grooves
                for (i in 1..4) {
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = radius * (0.35f + i * 0.12f),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Center Label Disc
                val centerDiscRadius = radius * 0.38f
                drawCircle(
                    color = accentColor.copy(alpha = 0.2f),
                    radius = centerDiscRadius,
                    center = center
                )
                drawCircle(
                    color = accentColor,
                    radius = centerDiscRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Vinyl position marker line
                val angleRad = Math.toRadians(rotationAngle.toDouble())
                val markerEnd = Offset(
                    (center.x + (radius * 0.88f) * cos(angleRad)).toFloat(),
                    (center.y + (radius * 0.88f) * sin(angleRad)).toFloat()
                )
                val markerStart = Offset(
                    (center.x + (centerDiscRadius * 0.9f) * cos(angleRad)).toFloat(),
                    (center.y + (centerDiscRadius * 0.9f) * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = accentColor,
                    start = markerStart,
                    end = markerEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center spindle hole
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
        }
    }
}
