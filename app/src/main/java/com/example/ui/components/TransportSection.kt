package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjSkinColors

@Composable
fun TransportSection(
    deckNumber: Int,
    isPlaying: Boolean,
    isSync: Boolean,
    keyLock: Boolean,
    pitchPercent: Float,
    effectiveBpm: Float,
    deckColor: Color,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onPlayToggle: () -> Unit,
    onCuePress: () -> Unit,
    onSyncToggle: () -> Unit,
    onPitchPercentChange: (Float) -> Unit,
    onPitchNudge: (Float) -> Unit
) {
    Card(
        modifier = modifier
            .testTag("transport_section_deck_$deckNumber")
            .padding(horizontal = 2.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Main Transport Buttons: CUE, PLAY/PAUSE
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large CUE Button
                Box(
                    modifier = Modifier
                        .testTag("cue_btn_deck_$deckNumber")
                        .size(46.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isPlaying) skinColors.surfaceChassisAlt else Color(0xFFF59E0B))
                        .border(2.dp, Color(0xFFF59E0B), CircleShape)
                        .clickable { onCuePress() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CUE",
                        color = if (isPlaying) Color(0xFFF59E0B) else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Large PLAY / PAUSE Button
                Box(
                    modifier = Modifier
                        .testTag("play_btn_deck_$deckNumber")
                        .size(46.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF10B981) else skinColors.surfaceChassisAlt)
                        .border(2.dp, Color(0xFF10B981), CircleShape)
                        .clickable { onPlayToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Deck $deckNumber",
                        tint = if (isPlaying) Color.Black else Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Sync and Pitch Bend
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // SYNC Button
                Box(
                    modifier = Modifier
                        .testTag("sync_btn_deck_$deckNumber")
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSync) deckColor else skinColors.surfaceChassisAlt)
                        .border(1.dp, deckColor, RoundedCornerShape(4.dp))
                        .clickable { onSyncToggle() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SYNC",
                        color = if (isSync) Color.Black else deckColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Pitch Nudge Buttons - / +
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(skinColors.surfaceChassisAlt)
                            .clickable { onPitchNudge(-0.5f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = skinColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(skinColors.surfaceChassisAlt)
                            .clickable { onPitchNudge(0.5f) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = skinColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Pitch Slider with percentage display
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(80.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (pitchPercent >= 0) "+%.1f%%".format(pitchPercent) else "%.1f%%".format(pitchPercent),
                        color = if (pitchPercent == 0f) skinColors.textSecondary else deckColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Pitch slider control
                Slider(
                    value = pitchPercent,
                    onValueChange = onPitchPercentChange,
                    valueRange = -16f..16f,
                    modifier = Modifier
                        .height(28.dp)
                        .fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = deckColor,
                        activeTrackColor = deckColor,
                        inactiveTrackColor = skinColors.surfaceChassisAlt
                    )
                )

                // Double tap reset to 0%
                Text(
                    text = "RESET PITCH",
                    color = skinColors.textSecondary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onPitchPercentChange(0f) }
                        .padding(top = 1.dp)
                )
            }
        }
    }
}
