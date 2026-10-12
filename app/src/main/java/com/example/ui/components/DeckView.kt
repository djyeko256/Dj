package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DjSkinColors
import com.example.ui.viewmodel.DeckUiState

@Composable
fun DeckView(
    deckState: DeckUiState,
    deckColor: Color,
    skinColors: DjSkinColors,
    modifier: Modifier = Modifier,
    onLoadTrackClick: () -> Unit,
    onPlayToggle: () -> Unit,
    onCuePress: () -> Unit,
    onSyncToggle: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchNudge: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onGainChange: (Float) -> Unit,
    onEqChange: (String, Float) -> Unit,
    onEqKillToggle: (String) -> Unit,
    onFilterChange: (Float) -> Unit,
    onVocalCutToggle: () -> Unit,
    onHotCueTrigger: (Int) -> Unit,
    onLoopToggle: (Float) -> Unit,
    onFxToggle: (String) -> Unit,
    onScratchDelta: (Float) -> Unit,
    onSeekSeconds: (Float) -> Unit
) {
    val deckNumber = deckState.index + 1
    val deckLabel = when (deckState.index) {
        0 -> "A"
        1 -> "B"
        2 -> "C"
        else -> "D"
    }

    Card(
        modifier = modifier
            .testTag("deck_view_$deckNumber")
            .padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassis),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(deckColor.copy(alpha = 0.4f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Deck Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(skinColors.surfaceChassisAlt)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Deck Badge & Track details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(deckColor)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DECK $deckLabel",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = deckState.track?.title ?: "No Track Loaded",
                            color = skinColors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${deckState.track?.artist ?: "Tap Eject to load"} • ${deckState.camelotKey} (${deckState.musicalKey})",
                            color = skinColors.textSecondary,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }

                // Load / Eject Button
                Button(
                    onClick = onLoadTrackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = skinColors.surfaceChassis),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Load Track",
                        tint = deckColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LOAD",
                        color = deckColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Scrolling Waveform Visualizer
            WaveformVisualizer(
                deckNumber = deckNumber,
                deckColor = deckColor,
                bpm = deckState.effectiveBpm,
                positionSeconds = deckState.positionSeconds,
                durationSeconds = deckState.durationSeconds,
                hotCues = deckState.hotCues,
                loopActive = deckState.loopActive,
                skinColors = skinColors,
                onSeekSeconds = onSeekSeconds
            )

            // Platter & Mixer Section side-by-side or stacked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Tactile Jog Wheel
                JogWheelComposable(
                    deckNumber = deckNumber,
                    deckColor = deckColor,
                    angleDegrees = deckState.platterAngleDegrees,
                    bpm = deckState.effectiveBpm,
                    keyCamelot = deckState.camelotKey,
                    musicalKey = deckState.musicalKey,
                    positionSeconds = deckState.positionSeconds,
                    durationSeconds = deckState.durationSeconds,
                    isPlaying = deckState.isPlaying,
                    isScratching = false,
                    skinColors = skinColors,
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(2.dp),
                    onScratchDelta = onScratchDelta
                )

                // Channel Strip (EQs, Filter, Volume Fader, VU Meters)
                ChannelStrip(
                    deckNumber = deckNumber,
                    deckLabel = deckLabel,
                    deckColor = deckColor,
                    volume = deckState.volume,
                    gain = deckState.gain,
                    eqLow = deckState.eqLow,
                    eqMid = deckState.eqMid,
                    eqHigh = deckState.eqHigh,
                    eqLowKill = deckState.eqLowKill,
                    eqMidKill = deckState.eqMidKill,
                    eqHighKill = deckState.eqHighKill,
                    filterValue = deckState.filterValue,
                    vocalStemMode = deckState.vocalStemMode,
                    crossfaderAssign = deckState.crossfaderAssign,
                    skinColors = skinColors,
                    modifier = Modifier.weight(1.4f),
                    onVolumeChange = onVolumeChange,
                    onGainChange = onGainChange,
                    onEqChange = onEqChange,
                    onEqKillToggle = onEqKillToggle,
                    onFilterChange = onFilterChange,
                    onVocalCutToggle = onVocalCutToggle
                )
            }

            // Performance RGB Pads (Hot Cues, Loops, FX, Vocal Cut)
            PerformancePads(
                deckNumber = deckNumber,
                hotCues = deckState.hotCues,
                loopActive = deckState.loopActive,
                currentLoopBeats = deckState.loopBeats,
                echoActive = deckState.echoActive,
                reverbActive = deckState.reverbActive,
                flangerActive = deckState.flangerActive,
                bitcrusherActive = deckState.bitcrusherActive,
                vocalStemMode = deckState.vocalStemMode,
                skinColors = skinColors,
                onHotCueTrigger = onHotCueTrigger,
                onLoopToggle = onLoopToggle,
                onFxToggle = onFxToggle,
                onVocalStemToggle = onVocalCutToggle
            )

            // Transport Controls: CUE, PLAY, SYNC, PITCH
            TransportSection(
                deckNumber = deckNumber,
                isPlaying = deckState.isPlaying,
                isSync = deckState.isSync,
                keyLock = deckState.keyLock,
                pitchPercent = deckState.pitchPercent,
                effectiveBpm = deckState.effectiveBpm,
                deckColor = deckColor,
                skinColors = skinColors,
                onPlayToggle = onPlayToggle,
                onCuePress = onCuePress,
                onSyncToggle = onSyncToggle,
                onPitchPercentChange = onPitchChange,
                onPitchNudge = onPitchNudge
            )
        }
    }
}
