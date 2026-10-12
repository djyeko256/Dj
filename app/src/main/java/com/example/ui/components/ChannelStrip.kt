package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.VocalStemMode
import com.example.ui.theme.DjSkinColors
import kotlin.math.*

@Composable
fun ChannelStrip(
    deckNumber: Int,
    deckLabel: String,
    deckColor: Color,
    volume: Float,
    gain: Float,
    eqLow: Float,
    eqMid: Float,
    eqHigh: Float,
    eqLowKill: Boolean,
    eqMidKill: Boolean,
    eqHighKill: Boolean,
    filterValue: Float,
    vocalStemMode: VocalStemMode,
    crossfaderAssign: Int,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onVolumeChange: (Float) -> Unit,
    onGainChange: (Float) -> Unit,
    onEqChange: (band: String, Float) -> Unit,
    onEqKillToggle: (band: String) -> Unit,
    onFilterChange: (Float) -> Unit,
    onVocalCutToggle: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("channel_strip_deck_$deckNumber")
            .padding(horizontal = 2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(deckColor.copy(alpha = 0.35f)))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            // Deck Channel Label & Crossfader Assignment indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "CH $deckNumber ($deckLabel)",
                    color = deckColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                // Vocal Remover badge button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) Color(0xFFEF4444) else skinColors.surfaceChassisAlt)
                        .clickable { onVocalCutToggle() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) "VOCAL OFF" else "VOCAL ON",
                        color = if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) Color.White else skinColors.textSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Rotary EQ & Filter Section
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // High EQ with Kill
                DjKnobWithKill(
                    label = "HI",
                    value = eqHigh,
                    isKilled = eqHighKill,
                    range = 0f..2f,
                    centerValue = 1f,
                    activeColor = skinColors.waveHighColor,
                    skinColors = skinColors,
                    onValueChange = { onEqChange("HIGH", it) },
                    onKillToggle = { onEqKillToggle("HIGH") }
                )

                // Mid EQ with Kill
                DjKnobWithKill(
                    label = "MID",
                    value = eqMid,
                    isKilled = eqMidKill,
                    range = 0f..2f,
                    centerValue = 1f,
                    activeColor = skinColors.waveMidColor,
                    skinColors = skinColors,
                    onValueChange = { onEqChange("MID", it) },
                    onKillToggle = { onEqKillToggle("MID") }
                )

                // Low EQ with Kill
                DjKnobWithKill(
                    label = "LOW",
                    value = eqLow,
                    isKilled = eqLowKill,
                    range = 0f..2f,
                    centerValue = 1f,
                    activeColor = skinColors.waveLowColor,
                    skinColors = skinColors,
                    onValueChange = { onEqChange("LOW", it) },
                    onKillToggle = { onEqKillToggle("LOW") }
                )

                // Color Filter Knob (LPF / HPF)
                DjKnob(
                    label = "FILTER",
                    value = filterValue,
                    range = -1f..1f,
                    centerValue = 0f,
                    activeColor = if (filterValue < 0) Color(0xFFEF4444) else if (filterValue > 0) Color(0xFF06B6D4) else skinColors.textSecondary,
                    skinColors = skinColors,
                    onValueChange = onFilterChange
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Fader & VU Meter Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(horizontal = 4.dp)
            ) {
                // Dual LED VU Meter
                LedVuMeter(
                    level = volume * 0.95f,
                    skinColors = skinColors,
                    modifier = Modifier.width(10.dp).fillMaxHeight()
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Vertical Channel Volume Fader
                VerticalFader(
                    value = volume,
                    faderColor = deckColor,
                    skinColors = skinColors,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onValueChange = onVolumeChange
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Right channel VU meter
                LedVuMeter(
                    level = volume * 0.90f,
                    skinColors = skinColors,
                    modifier = Modifier.width(10.dp).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun DjKnobWithKill(
    label: String,
    value: Float,
    isKilled: Boolean,
    range: ClosedFloatingPointRange<Float>,
    centerValue: Float,
    activeColor: Color,
    skinColors: DjSkinColors,
    onValueChange: (Float) -> Unit,
    onKillToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(1.dp)
    ) {
        DjKnob(
            label = label,
            value = if (isKilled) 0f else value,
            range = range,
            centerValue = centerValue,
            activeColor = if (isKilled) Color.Gray else activeColor,
            skinColors = skinColors,
            onValueChange = onValueChange
        )
        // Kill button
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isKilled) Color(0xFFDC2626) else skinColors.surfaceChassisAlt)
                .clickable { onKillToggle() }
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "KILL",
                color = if (isKilled) Color.White else skinColors.textSecondary,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DjKnob(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    centerValue: Float,
    activeColor: Color,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    val normalized = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val angle = 135f + normalized * 270f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.pointerInput(label) {
            detectDragGestures { _, dragAmount ->
                val delta = -dragAmount.y * 0.01f
                val newValue = (value + delta * (range.endInclusive - range.start)).coerceIn(range.start, range.endInclusive)
                onValueChange(newValue)
            }
        }
    ) {
        Text(
            text = label,
            color = skinColors.textSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .padding(2.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(skinColors.eqKnobBase),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background arc
                drawArc(
                    color = skinColors.surfaceChassisAlt,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Active arc
                val sweep = (angle - 135f).coerceIn(0f, 270f)
                drawArc(
                    color = activeColor,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Pointer needle
                val rad = Math.toRadians(angle.toDouble())
                val needleEnd = Offset(
                    center.x + (radius * 0.75f * cos(rad)).toFloat(),
                    center.y + (radius * 0.75f * sin(rad)).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun VerticalFader(
    value: Float,
    faderColor: Color,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val ratio = 1.0f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(ratio)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f

            // Fader track slot
            drawRoundRect(
                color = skinColors.crossfaderTrack,
                topLeft = Offset(centerX - 3f, 4f),
                size = Size(6f, height - 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )

            // Tick marks on sides
            for (i in 0..6) {
                val y = 8f + i * ((height - 16f) / 6f)
                drawLine(
                    color = skinColors.textSecondary.copy(alpha = 0.4f),
                    start = Offset(centerX - 10f, y),
                    end = Offset(centerX - 5f, y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = skinColors.textSecondary.copy(alpha = 0.4f),
                    start = Offset(centerX + 5f, y),
                    end = Offset(centerX + 10f, y),
                    strokeWidth = 1.5f
                )
            }

            // Fader cap position
            val thumbY = (1.0f - value) * (height - 24f) + 12f
            // Fader cap shadow & body
            drawRoundRect(
                color = skinColors.surfaceChassisAlt,
                topLeft = Offset(centerX - 16f, thumbY - 8f),
                size = Size(32f, 16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
            // Accent stripe on cap
            drawLine(
                color = faderColor,
                start = Offset(centerX - 14f, thumbY),
                end = Offset(centerX + 14f, thumbY),
                strokeWidth = 2.5f
            )
        }
    }
}

@Composable
fun LedVuMeter(
    level: Float,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalSegments = 12
        val segHeight = size.height / totalSegments
        val activeSegments = (level * totalSegments).toInt().coerceIn(0, totalSegments)

        for (i in 0 until totalSegments) {
            val segIndexFromBottom = totalSegments - 1 - i
            val isActive = segIndexFromBottom < activeSegments
            val segY = i * segHeight

            val segColor = when {
                i < 2 -> skinColors.vuMeterClip
                i < 5 -> skinColors.vuMeterWarning
                else -> skinColors.vuMeterNormal
            }

            drawRoundRect(
                color = if (isActive) segColor else segColor.copy(alpha = 0.15f),
                topLeft = Offset(1f, segY + 1f),
                size = Size(size.width - 2f, segHeight - 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
            )
        }
    }
}
