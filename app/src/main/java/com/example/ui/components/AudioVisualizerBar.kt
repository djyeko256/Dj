package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import com.example.engine.HarmonicCompatibility
import com.example.engine.KeyBpmHelper
import com.example.ui.theme.DjSkinColors

@Composable
fun AudioVisualizerBar(
    spectrumBands: FloatArray,
    beatPulse: Float,
    masterKeyCamelot: String,
    deckBKeyCamelot: String,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier
) {
    val harmonicInfo = KeyBpmHelper.checkCompatibility(masterKeyCamelot, deckBKeyCamelot)

    Card(
        modifier = modifier
            .testTag("audio_visualizer_bar")
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Harmonic Camelot Mixing badge
            Column(
                modifier = Modifier.width(100.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HARMONIC KEY",
                    color = skinColors.textSecondary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(harmonicInfo.hexColor).copy(alpha = 0.25f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${masterKeyCamelot} ⟷ ${deckBKeyCamelot}",
                        color = Color(harmonicInfo.hexColor),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = harmonicInfo.description,
                    color = Color(harmonicInfo.hexColor),
                    fontSize = 7.sp,
                    maxLines = 1
                )
            }

            // Center: 32-Band Real-Time Audio Spectrum Analyzer Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF090D14))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barCount = 32
                    val barSpacing = 1.5f
                    val barWidth = (w - (barCount * barSpacing)) / barCount

                    for (i in 0 until barCount) {
                        val bandLevel = if (i < spectrumBands.size) spectrumBands[i] else 0f
                        val barHeight = (bandLevel * h).coerceIn(2f, h)
                        val x = i * (barWidth + barSpacing)

                        // Color gradient: Cyan -> Green -> Yellow -> Red at top
                        val barColor = when {
                            i < 8 -> skinColors.waveLowColor   // Bass
                            i < 20 -> skinColors.waveMidColor  // Mids
                            else -> skinColors.waveHighColor   // Highs
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, h - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
                        )

                        // Peak hold dot
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(x, (h - barHeight - 2f).coerceAtLeast(0f)),
                            size = Size(barWidth, 1.5f)
                        )
                    }
                }
            }

            // Right: Beat Phase Meter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(68.dp)
            ) {
                Text(
                    text = "BEAT SYNC",
                    color = skinColors.textSecondary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
                // 4-beat Phase LED lights
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val currentBeatIndex = ((beatPulse * 4f).toInt()) % 4
                    for (b in 0..3) {
                        val isLit = b == currentBeatIndex
                        Box(
                            modifier = Modifier
                                .size(width = 11.dp, height = 14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isLit) (if (b == 0) Color(0xFFEF4444) else Color(0xFF10B981)) else Color(0xFF1E293B))
                        )
                    }
                }
            }
        }
    }
}
