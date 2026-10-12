package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.CloudSyncStatus
import com.example.ui.theme.DjSkinColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDialog(
    tracks: List<Track>,
    playlists: List<Playlist>,
    selectedCrate: String,
    searchQuery: String,
    cloudSyncStatus: CloudSyncStatus,
    deckCount: Int,
    skinColors: DjSkinColors,
    onSelectCrate: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onLoadTrack: (deckIndex: Int, track: Track) -> Unit,
    onOpenAddTrack: () -> Unit,
    onTriggerCloudSync: () -> Unit,
    onImportLocalAudio: (List<Uri>) -> Unit = {},
    onScanLocalStorage: () -> Unit = {},
    onDismiss: () -> Unit
) {
    // Zero-permission Android audio file picker for local music
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onImportLocalAudio(uris)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = skinColors.surfaceChassis,
        contentColor = skinColors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("library_dialog")
        ) {
            // Header: Library Title, Local Storage & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Music Library",
                        tint = skinColors.deckAColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "LOCAL MUSIC & TRACK CRATES",
                            color = skinColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "No Phone or External Connection Required • 100% On-Device",
                            color = Color(0xFF10B981),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = skinColors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dedicated Local Audio Import Actions Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text(
                                text = "DEVICE STORAGE MUSIC (OFFLINE)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("NO PHONE NEEDED", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Import Audio Files button
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(32.dp)
                                .testTag("import_local_audio_button")
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IMPORT AUDIO FILES", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }

                        // Scan Device Storage button
                        OutlinedButton(
                            onClick = onScanLocalStorage,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = skinColors.deckAColor),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("scan_device_storage_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("SCAN STORAGE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Add Custom Track
                        IconButton(
                            onClick = onOpenAddTrack,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Track", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search local title, artist, genre, or key...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = skinColors.textSecondary, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = skinColors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = skinColors.deckAColor,
                    unfocusedBorderColor = skinColors.surfaceChassisAlt,
                    focusedTextColor = skinColors.textPrimary,
                    unfocusedTextColor = skinColors.textPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Crates / Category Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val crateList = listOf("All Tracks", "Local Device Audio") + playlists.map { it.name }.filter { it != "All Tracks" && it != "Local Device Audio" }.distinct()
                items(crateList) { crate ->
                    val isSelected = selectedCrate == crate
                    val isLocalCrate = crate == "Local Device Audio"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isSelected -> skinColors.deckAColor
                                    isLocalCrate -> Color(0xFF065F46)
                                    else -> skinColors.surfaceChassisAlt
                                }
                            )
                            .clickable { onSelectCrate(crate) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isLocalCrate) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = if (isSelected) Color.Black else Color(0xFF34D399), modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = crate,
                                color = if (isSelected) Color.Black else if (isLocalCrate) Color(0xFF34D399) else skinColors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Track List
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedCrate == "Local Device Audio") "No local device music files added yet" else "No tracks found in '$selectedCrate'",
                            color = skinColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'IMPORT AUDIO FILES' above to load your MP3/WAV tracks directly from phone/tablet storage with zero internet required.",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            deckCount = deckCount,
                            skinColors = skinColors,
                            onLoadToDeck = { deckIdx ->
                                onLoadTrack(deckIdx, track)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: Track,
    deckCount: Int,
    skinColors: DjSkinColors,
    onLoadToDeck: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassisAlt),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Info: Title, Artist, Genre, Camelot Key, BPM
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = track.title,
                        color = skinColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (track.isLocalFile) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF059669))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "LOCAL", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    if (track.isVideo) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(text = "VIDEO", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = track.artist,
                        color = skinColors.textSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        color = skinColors.textSecondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "${track.bpm.toInt()} BPM",
                        color = skinColors.deckAColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        color = skinColors.textSecondary,
                        fontSize = 9.sp
                    )
                    // Camelot Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.3f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${track.camelotKey} (${track.musicalKey})",
                            color = Color(0xFF60A5FA),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Load to Deck buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onLoadToDeck(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("DECK 1", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }

                Button(
                    onClick = { onLoadToDeck(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckBColor),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("DECK 2", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }

                if (deckCount >= 4) {
                    Button(
                        onClick = { onLoadToDeck(2) },
                        colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckCColor),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("3", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    Button(
                        onClick = { onLoadToDeck(3) },
                        colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckDColor),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("4", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
