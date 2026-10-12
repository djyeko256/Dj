package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Storage
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CloudSyncDialog(
    status: CloudSyncStatus,
    skinColors: DjSkinColors,
    onTriggerSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(status.lastSyncTimestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onTriggerSync,
                colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor)
            ) {
                Text(
                    text = if (status.isSyncing) "SYNCING..." else "SYNC NOW",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = skinColors.textSecondary)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Cloud Sync",
                    tint = skinColors.deckAColor
                )
                Text(
                    text = "CLOUD LIBRARY SYNCHRONIZATION",
                    color = skinColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status card
                Card(
                    colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassisAlt),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (status.isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (status.isSyncing) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                            Text(
                                text = if (status.isSyncing) "Synchronizing In Progress" else "Active & Connected",
                                color = if (status.isSyncing) Color(0xFFF59E0B) else Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = status.cloudStatusMessage,
                            color = skinColors.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Last synchronized: $dateStr",
                            color = skinColors.textSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Synced nodes info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassisAlt),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = skinColors.deckAColor, modifier = Modifier.size(18.dp))
                            Text("3 Devices", color = skinColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Tablet, Laptop, Mobile", color = skinColors.textSecondary, fontSize = 9.sp)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = skinColors.surfaceChassisAlt),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = skinColors.deckBColor, modifier = Modifier.size(18.dp))
                            Text("Real-Time", color = skinColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Crates, Cues, BPM tags", color = skinColors.textSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }
        },
        containerColor = skinColors.surfaceChassis
    )
}
