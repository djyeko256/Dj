package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjSkinColors
import kotlin.math.*

@Composable
fun WaveformVisualizer(
    deckNumber: Int,
    deckColor: Color,
    bpm: Float,
    positionSeconds: Float,
    durationSeconds: Float,
    hotCues: Map<Int, Float>,
    loopActive: Boolean,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onSeekSeconds: (Float) -> Unit = {}
) {
    Box(
        modifier = modifier
            .testTag("waveform_visualizer_deck_$deckNumber")
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF090D14))
            .pointerInput(deckNumber) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    // Seek relative to window or needle position
                    val seekSec = ratio * durationSeconds
                    onSeekSeconds(seekSec)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Beat length in seconds
            val beatSec = 60f / max(1f, bpm)
            val windowSeconds = 12f // Visible audio window in seconds
            val startSec = positionSeconds - (windowSeconds * 0.4f)
            val endSec = positionSeconds + (windowSeconds * 0.6f)

            // Center playhead X coordinate
            val playheadX = width * 0.4f

            // Draw scrolling frequency-colored waveform bars
            val numBars = 80
            val barWidth = width / numBars

            for (i in 0 until numBars) {
                val barSec = startSec + (i.toFloat() / numBars) * windowSeconds
                val x = i * barWidth

                // Procedural multi-band amplitude simulation (Low, Mid, High)
                val beatPhase = (barSec / beatSec)
                val isBeat = abs(beatPhase - round(beatPhase)) < 0.08f

                val bassAmp = if (isBeat) 0.92f else 0.35f * (0.5f + 0.5f * sin(barSec * 3.5).toFloat())
                val midAmp = 0.45f * (0.6f + 0.4f * cos(barSec * 7.2).toFloat())
                val highAmp = 0.25f * (0.5f + 0.5f * sin(barSec * 14.1).toFloat())

                val totalHeight = ((bassAmp + midAmp + highAmp) * (height * 0.42f)).coerceIn(4f, height * 0.48f)

                // High frequencies (top/bottom tips - Cyan/Blue)
                drawRect(
                    color = skinColors.waveHighColor,
                    topLeft = Offset(x + 1f, centerY - totalHeight),
                    size = Size(max(1f, barWidth - 1.5f), totalHeight * 2f)
                )

                // Mid frequencies (body - Green/Magenta)
                val midHeight = totalHeight * 0.75f
                drawRect(
                    color = skinColors.waveMidColor,
                    topLeft = Offset(x + 1f, centerY - midHeight),
                    size = Size(max(1f, barWidth - 1.5f), midHeight * 2f)
                )

                // Low frequencies (core bass punch - Red/Gold)
                val bassHeight = totalHeight * 0.45f
                drawRect(
                    color = skinColors.waveLowColor,
                    topLeft = Offset(x + 1f, centerY - bassHeight),
                    size = Size(max(1f, barWidth - 1.5f), bassHeight * 2f)
                )

                // Beat Grid vertical line
                if (isBeat) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                }
            }

            // Draw Hot Cue Markers
            hotCues.forEach { (cueNum, cueSec) ->
                if (cueSec in startSec..endSec) {
                    val cueProgress = (cueSec - startSec) / windowSeconds
                    val cueX = cueProgress * width

                    val cueColor = when (cueNum) {
                        1 -> Color(0xFFEF4444) // Red
                        2 -> Color(0xFF10B981) // Green
                        3 -> Color(0xFF3B82F6) // Blue
                        else -> Color(0xFFF59E0B) // Amber
                    }

                    // Cue line
                    drawLine(
                        color = cueColor,
                        start = Offset(cueX, 0f),
                        end = Offset(cueX, height),
                        strokeWidth = 2f
                    )

                    // Cue flag at top
                    drawCircle(
                        color = cueColor,
                        radius = 4f,
                        center = Offset(cueX, 6f)
                    )
                }
            }

            // Active Loop shaded region
            if (loopActive) {
                drawRect(
                    color = deckColor.copy(alpha = 0.15f),
                    topLeft = Offset(playheadX, 0f),
                    size = Size(width * 0.25f, height)
                )
            }

            // Fixed Center Playhead
            drawLine(
                color = Color.White,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2.5f
            )

            // Playhead indicator triangle at top
            drawCircle(
                color = deckColor,
                radius = 4.5f,
                center = Offset(playheadX, 4f)
            )
        }

        // Mini overview needle search bar at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            val progress = (positionSeconds / max(1f, durationSeconds)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(deckColor)
            )
        }
    }
}
