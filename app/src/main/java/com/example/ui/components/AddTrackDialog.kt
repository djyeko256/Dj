package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LocalAudioHelper
import com.example.ui.theme.DjSkinColors
import kotlinx.coroutines.launch

@Composable
fun AddTrackDialog(
    skinColors: DjSkinColors,
    onAddTrack: (title: String, artist: String, bpm: Float, key: String, genre: String, isVideo: Boolean) -> Unit,
    onImportLocalAudio: (Uri) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var bpmText by remember { mutableStateOf("128") }
    var keyText by remember { mutableStateOf("8A") }
    var genreText by remember { mutableStateOf("Tech House") }
    var isVideo by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isLoadingFile by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            isLoadingFile = true
            scope.launch {
                val extracted = LocalAudioHelper.importAudioFromUri(context, uri)
                title = extracted.title
                artist = extracted.artist
                bpmText = extracted.bpm.toInt().toString()
                keyText = extracted.camelotKey
                genreText = extracted.genre
                isLoadingFile = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (selectedFileUri != null) {
                        onImportLocalAudio(selectedFileUri!!)
                        onDismiss()
                    } else if (title.isNotBlank()) {
                        val bpm = bpmText.toFloatOrNull() ?: 126f
                        onAddTrack(title, if (artist.isBlank()) "Unknown Artist" else artist, bpm, keyText, genreText, isVideo)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor),
                modifier = Modifier.testTag("confirm_add_track_button")
            ) {
                Text(if (selectedFileUri != null) "IMPORT LOCAL AUDIO" else "ADD TRACK", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = skinColors.textSecondary)
            }
        },
        title = {
            Text(
                text = "IMPORT LOCAL TRACK",
                color = skinColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // One-tap Local File Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color(0xFF10B981))
                            Text("SELECT AUDIO FILE FROM DEVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supports MP3, WAV, FLAC, M4A, OGG directly on device (No phone required)",
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { filePicker.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedFileUri != null) "CHANGE FILE" else "CHOOSE LOCAL FILE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        if (selectedFileUri != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Selected: ${title.ifBlank { "Local Audio" }}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 2.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Track Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist / Producer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bpmText,
                        onValueChange = { bpmText = it },
                        label = { Text("BPM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("Camelot Key") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = genreText,
                    onValueChange = { genreText = it },
                    label = { Text("Genre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = skinColors.surfaceChassis
    )
}
