package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.VocalStemMode
import com.example.ui.theme.DjSkinColors

enum class PadMode {
    HOT_CUE,
    AUTO_LOOP,
    FX_DROPS
}

@Composable
fun PerformancePads(
    deckNumber: Int,
    hotCues: Map<Int, Float>,
    loopActive: Boolean,
    currentLoopBeats: Float,
    echoActive: Boolean,
    reverbActive: Boolean,
    flangerActive: Boolean,
    bitcrusherActive: Boolean,
    vocalStemMode: VocalStemMode,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onHotCueTrigger: (Int) -> Unit,
    onLoopToggle: (Float) -> Unit,
    onFxToggle: (String) -> Unit,
    onVocalStemToggle: () -> Unit
) {
    var activeMode by remember { mutableStateOf(PadMode.HOT_CUE) }

    Card(
        modifier = modifier
            .testTag("performance_pads_deck_$deckNumber")
            .padding(horizontal = 2.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // Mode Select Tabs
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        PadMode.HOT_CUE to "HOT CUE",
                        PadMode.AUTO_LOOP to "LOOP",
                        PadMode.FX_DROPS to "FX / STEMS"
                    ).forEach { (mode, label) ->
                        val isSelected = activeMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) skinColors.deckAColor else skinColors.surfaceChassisAlt)
                                .clickable { activeMode = mode }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else skinColors.textSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Vocal Stem Quick Toggle (Instrumental vs Full Mix)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) Color(0xFFEF4444) else skinColors.surfaceChassisAlt)
                        .border(1.dp, if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) Color(0xFFEF4444) else Color.Transparent, RoundedCornerShape(3.dp))
                        .clickable { onVocalStemToggle() }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) "VOCAL REMOVED" else "REMOVE VOCALS",
                        color = if (vocalStemMode == VocalStemMode.REMOVE_VOCALS) Color.White else skinColors.textSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4 Pads depending on mode
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (activeMode) {
                    PadMode.HOT_CUE -> {
                        val cueColors = listOf(Color(0xFFEF4444), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFF59E0B))
                        for (cue in 1..4) {
                            val hasCue = hotCues.containsKey(cue)
                            val padColor = cueColors[cue - 1]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (hasCue) padColor.copy(alpha = 0.85f) else skinColors.surfaceChassisAlt)
                                    .border(1.5.dp, padColor, RoundedCornerShape(4.dp))
                                    .clickable { onHotCueTrigger(cue) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "CUE $cue",
                                        color = if (hasCue) Color.White else skinColors.textSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    if (hasCue) {
                                        val sec = hotCues[cue] ?: 0f
                                        Text(
                                            text = "${sec.toInt()}s",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 7.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PadMode.AUTO_LOOP -> {
                        val loopSizes = listOf(0.5f to "1/2", 1.0f to "1", 2.0f to "2", 4.0f to "4")
                        loopSizes.forEach { (beats, label) ->
                            val isThisLoop = loopActive && currentLoopBeats == beats
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isThisLoop) Color(0xFF06B6D4) else skinColors.surfaceChassisAlt)
                                    .border(1.5.dp, Color(0xFF06B6D4), RoundedCornerShape(4.dp))
                                    .clickable { onLoopToggle(beats) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$label BEAT",
                                    color = if (isThisLoop) Color.Black else Color(0xFF06B6D4),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    PadMode.FX_DROPS -> {
                        val fxList = listOf(
                            Triple("ECHO", echoActive, Color(0xFF38BDF8)),
                            Triple("REVERB", reverbActive, Color(0xFFA78BFA)),
                            Triple("FLANGER", flangerActive, Color(0xFFF472B6)),
                            Triple("CRUSH", bitcrusherActive, Color(0xFFFBBF24))
                        )
                        fxList.forEach { (name, isActive, fxColor) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isActive) fxColor else skinColors.surfaceChassisAlt)
                                    .border(1.5.dp, fxColor, RoundedCornerShape(4.dp))
                                    .clickable { onFxToggle(if (name == "CRUSH") "BITCRUSHER" else name) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isActive) Color.Black else fxColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
