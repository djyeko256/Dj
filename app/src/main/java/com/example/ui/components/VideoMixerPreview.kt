package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjSkinColors
import kotlin.math.*

@Composable
fun VideoMixerPreview(
    deckAIsPlaying: Boolean,
    deckBIsPlaying: Boolean,
    deckAPosition: Float,
    deckBPosition: Float,
    crossfaderPos: Float, // -1.0 to 1.0
    transitionType: String, // "Crossfade", "Cut", "Wipe", "Glitch"
    beatPulse: Float,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onExpandVideo: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .testTag("video_mixer_preview")
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            // Header: Video DJ Engine status & Transition type
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video DJ",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VIDEO DJ MIXER",
                        color = skinColors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color(0xFFEF4444),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "FX: $transitionType",
                        color = skinColors.textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = onExpandVideo,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen Video",
                            tint = skinColors.deckAColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Dual Deck Preview + Master Center Output
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Deck A Video Monitor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black)
                ) {
                    VideoRendererCanvas(
                        isPlaying = deckAIsPlaying,
                        timeSeconds = deckAPosition,
                        accentColor = skinColors.deckAColor,
                        beatPulse = beatPulse,
                        label = "DECK A (AV)"
                    )
                }

                // Master Output Mix Monitor (Blended by Crossfader)
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black)
                ) {
                    MasterBlendedVideoCanvas(
                        deckAPos = deckAPosition,
                        deckBPos = deckBPosition,
                        deckAPlaying = deckAIsPlaying,
                        deckBPlaying = deckBIsPlaying,
                        crossfader = crossfaderPos,
                        transitionType = transitionType,
                        colorA = skinColors.deckAColor,
                        colorB = skinColors.deckBColor,
                        beatPulse = beatPulse
                    )
                }

                // Deck B Video Monitor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black)
                ) {
                    VideoRendererCanvas(
                        isPlaying = deckBIsPlaying,
                        timeSeconds = deckBPosition,
                        accentColor = skinColors.deckBColor,
                        beatPulse = beatPulse,
                        label = "DECK B (AV)"
                    )
                }
            }
        }
    }
}

@Composable
fun VideoRendererCanvas(
    isPlaying: Boolean,
    timeSeconds: Float,
    accentColor: Color,
    beatPulse: Float,
    label: String
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Dark digital background
        drawRect(color = Color(0xFF07090E))

        if (isPlaying) {
            // Animated Cyberpunk 3D perspective grid lines
            val t = timeSeconds * 2f
            for (i in 0..6) {
                val lineX = (i / 6f) * w
                drawLine(
                    color = accentColor.copy(alpha = 0.35f),
                    start = Offset(cx, cy * 0.3f),
                    end = Offset(lineX, h),
                    strokeWidth = 1.2f
                )
            }
            // Moving horizontal horizon rings
            for (j in 1..4) {
                val y = cy * 0.3f + (h - cy * 0.3f) * ((j.toFloat() + (t % 1f)) / 5f)
                drawLine(
                    color = accentColor.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.2f
                )
            }

            // Audio reactive neon sun/orb on the horizon
            val pulseRadius = 14f + beatPulse * 8f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor, Color.Transparent),
                    center = Offset(cx, cy * 0.35f),
                    radius = pulseRadius * 2f
                ),
                radius = pulseRadius * 1.5f,
                center = Offset(cx, cy * 0.35f)
            )
            drawCircle(
                color = Color.White,
                radius = pulseRadius * 0.5f,
                center = Offset(cx, cy * 0.35f)
            )
        } else {
            // Standby pattern
            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, cy),
                end = Offset(w, cy),
                strokeWidth = 1f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MasterBlendedVideoCanvas(
    deckAPos: Float,
    deckBPos: Float,
    deckAPlaying: Boolean,
    deckBPlaying: Boolean,
    crossfader: Float, // -1.0 to +1.0
    transitionType: String,
    colorA: Color,
    colorB: Color,
    beatPulse: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val norm = (crossfader + 1f) * 0.5f // 0.0 (Deck A) to 1.0 (Deck B)

        // Background
        drawRect(color = Color.Black)

        when (transitionType) {
            "Wipe" -> {
                val wipeX = norm * w
                // Deck A on left
                drawRect(
                    brush = Brush.verticalGradient(listOf(colorA.copy(alpha = 0.8f), Color.Black)),
                    topLeft = Offset(0f, 0f),
                    size = Size(wipeX, h)
                )
                // Deck B on right
                drawRect(
                    brush = Brush.verticalGradient(listOf(colorB.copy(alpha = 0.8f), Color.Black)),
                    topLeft = Offset(wipeX, 0f),
                    size = Size(w - wipeX, h)
                )
                // Wipe seam laser line
                drawLine(
                    color = Color.White,
                    start = Offset(wipeX, 0f),
                    end = Offset(wipeX, h),
                    strokeWidth = 2f
                )
            }
            "Cut" -> {
                val activeColor = if (norm < 0.5f) colorA else colorB
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = 0.7f), Color.Black),
                        center = Offset(cx, cy),
                        radius = w * 0.7f
                    )
                )
            }
            else -> { // Crossfade & Glitch
                // Blend Deck A and Deck B
                val alphaA = (1f - norm).coerceIn(0f, 1f)
                val alphaB = norm.coerceIn(0f, 1f)

                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(colorA.copy(alpha = alphaA * 0.6f), Color.Black)
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(colorB.copy(alpha = alphaB * 0.6f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.8f
                    )
                )

                // Beat-reactive center audio-visual waveform ring
                val ringRadius = (20f + beatPulse * 16f).coerceIn(15f, h * 0.45f)
                drawCircle(
                    color = if (norm < 0.5f) colorA else colorB,
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "PROGRAM OUT (PGM)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}
