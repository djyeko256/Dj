package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CloudSyncStatus
import com.example.ui.theme.DjSkinColors

@Composable
fun DjTopBar(
    deckCount: Int,
    isVideoVisible: Boolean,
    cloudSyncStatus: CloudSyncStatus,
    skinColors: DjSkinColors,
    onDeckCountToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenMidi: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onOpenSkins: () -> Unit
) {
    Surface(
        color = skinColors.surfaceChassis,
        modifier = Modifier
            .testTag("dj_top_bar")
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Live Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = skinColors.deckAColor,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "VIRTUAL DJ PRO",
                        color = skinColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "LOCAL OFFLINE",
                            color = Color(0xFF10B981),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Central Deck Toggle & Video Mode Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2 / 4 Deck Mode Toggle
                Box(
                    modifier = Modifier
                        .testTag("deck_count_toggle_btn")
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (deckCount == 4) skinColors.deckBColor else skinColors.surfaceChassisAlt)
                        .clickable { onDeckCountToggle() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (deckCount == 4) "4 DECKS" else "2 DECKS",
                        color = if (deckCount == 4) Color.Black else skinColors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Video AV Toggle
                Box(
                    modifier = Modifier
                        .testTag("video_toggle_btn")
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isVideoVisible) Color(0xFFEF4444) else skinColors.surfaceChassisAlt)
                        .clickable { onVideoToggle() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isVideoVisible) Color.White else skinColors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "VIDEO",
                            color = if (isVideoVisible) Color.White else skinColors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Action Tools: Library, MIDI, Cloud, Skins
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crates / Library
                IconButton(onClick = onOpenLibrary, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Open Library",
                        tint = skinColors.deckAColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // MIDI Controller
                Box(
                    modifier = Modifier
                        .testTag("open_midi_button")
                        .clip(RoundedCornerShape(6.dp))
                        .background(skinColors.deckAColor.copy(alpha = 0.15f))
                        .border(1.dp, skinColors.deckAColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable { onOpenMidi() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = "MIDI Settings",
                            tint = skinColors.deckAColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "MIDI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = skinColors.deckAColor
                        )
                    }
                }

                // Cloud Sync
                IconButton(onClick = onOpenCloudSync, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync",
                        tint = if (cloudSyncStatus.isSyncing) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Skins
                IconButton(onClick = onOpenSkins, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Skins",
                        tint = skinColors.deckBColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
