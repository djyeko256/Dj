package com.example.ui.components.midi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.MidiMapping
import com.example.engine.DjMidiService
import com.example.engine.MidiPresetProfiles
import com.example.engine.SoftwareActionDefinition
import com.example.ui.theme.DjSkinColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMappingDialog(
    initialMapping: MidiMapping?,
    midiService: DjMidiService,
    activeProfileName: String,
    skinColors: DjSkinColors,
    onSave: (MidiMapping) -> Unit,
    onDismiss: () -> Unit
) {
    val allActions = MidiPresetProfiles.ALL_ACTION_DEFINITIONS

    var selectedCategory by remember {
        mutableStateOf(initialMapping?.actionCategory ?: "TRANSPORT")
    }
    var selectedActionType by remember {
        mutableStateOf(initialMapping?.actionType ?: "PLAY_PAUSE")
    }
    var targetDeck by remember {
        mutableStateOf(initialMapping?.targetDeck ?: 1)
    }
    var hardwareType by remember {
        mutableStateOf(initialMapping?.hardwareControlType ?: "BUTTON")
    }
    var controlType by remember {
        mutableStateOf(initialMapping?.controlType ?: "NOTE")
    }
    var channel by remember {
        mutableStateOf(initialMapping?.channel ?: 1)
    }
    var controlNumber by remember {
        mutableStateOf(initialMapping?.controlNumber ?: 60)
    }
    var isInverted by remember {
        mutableStateOf(initialMapping?.isInverted ?: false)
    }
    var behavior by remember {
        mutableStateOf(initialMapping?.behavior ?: "ABSOLUTE")
    }
    var customLabel by remember {
        mutableStateOf(initialMapping?.controlName ?: "")
    }

    val learnSession by midiService.learnSession.collectAsState()
    val isLearning = learnSession != null
    val lastEvent by midiService.lastMidiEvent.collectAsState()

    // Listen for MIDI learn in-dialog
    LaunchedEffect(lastEvent) {
        if (isLearning && lastEvent != null) {
            val ev = lastEvent!!
            channel = ev.channel
            controlNumber = ev.controlOrNote
            controlType = if (ev.type.startsWith("NOTE")) "NOTE" else "CC"
            if (controlType == "NOTE") hardwareType = "BUTTON"
            midiService.cancelMidiLearn()
        }
    }

    val filteredActions = remember(selectedCategory) {
        allActions.filter { it.category == selectedCategory }
    }

    val currentDef = remember(selectedActionType) {
        MidiPresetProfiles.getActionDefinition(selectedActionType)
    }

    AlertDialog(
        onDismissRequest = {
            if (isLearning) midiService.cancelMidiLearn()
            onDismiss()
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = if (customLabel.isNotBlank()) {
                        customLabel
                    } else {
                        val deckName = if (targetDeck == 0) "Master" else "Deck $targetDeck"
                        val defName = currentDef?.displayName ?: selectedActionType
                        "$deckName $defName"
                    }
                    val mapping = MidiMapping(
                        id = initialMapping?.id ?: 0L,
                        controlName = label,
                        targetDeck = targetDeck,
                        controlType = controlType,
                        channel = channel,
                        controlNumber = controlNumber,
                        description = "Hardware ${hardwareType} (${controlType} #${controlNumber}) on Ch ${channel}",
                        actionCategory = selectedCategory,
                        actionType = selectedActionType,
                        hardwareControlType = hardwareType,
                        isInverted = isInverted,
                        behavior = behavior,
                        profileName = activeProfileName
                    )
                    onSave(mapping)
                },
                colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor),
                modifier = Modifier.testTag("save_mapping_button")
            ) {
                Text(if (initialMapping == null) "ADD ASSIGNMENT" else "SAVE CHANGES", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (isLearning) midiService.cancelMidiLearn()
                onDismiss()
            }) {
                Text("CANCEL", color = skinColors.textSecondary)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = skinColors.deckAColor
                )
                Text(
                    text = if (initialMapping == null) "ASSIGN MIDI CONTROL" else "EDIT MIDI ASSIGNMENT",
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
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // MIDI LEARN CARD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLearning) Color(0xFF1E293B) else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isLearning) skinColors.deckAColor else Color(0xFF334155),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLearning) "LISTENING FOR HARDWARE..." else "MIDI AUTO-LEARN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLearning) skinColors.deckAColor else skinColors.textPrimary
                            )
                            Text(
                                text = if (isLearning) "Move any knob, slider, or press pad now" else "Auto-detects Channel, CC/Note # and Type",
                                fontSize = 9.sp,
                                color = skinColors.textSecondary
                            )
                        }

                        Button(
                            onClick = {
                                if (isLearning) {
                                    midiService.cancelMidiLearn()
                                } else {
                                    midiService.startMidiLearn(
                                        actionType = selectedActionType,
                                        targetDeck = targetDeck,
                                        controlName = currentDef?.displayName ?: selectedActionType,
                                        actionCategory = selectedCategory,
                                        hardwareControlType = hardwareType,
                                        profileName = activeProfileName
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLearning) Color(0xFFEF4444) else skinColors.deckAColor
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (isLearning) "CANCEL LEARN" else "TAP TO LEARN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLearning) Color.White else Color.Black
                            )
                        }
                    }
                }

                // 1. Action Category Selector
                Text(
                    text = "1. FUNCTION CATEGORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.textSecondary
                )
                val categories = listOf(
                    "TRANSPORT" to "Transport",
                    "EQ" to "EQs & Gain",
                    "FADER" to "Faders",
                    "EFFECTS_SEND" to "Effects Sends",
                    "DECK_SELECT" to "Deck Selection"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = selectedCategory == catKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) skinColors.deckAColor else Color(0xFF1E293B))
                                .clickable {
                                    selectedCategory = catKey
                                    val firstAction = allActions.firstOrNull { it.category == catKey }
                                    if (firstAction != null) {
                                        selectedActionType = firstAction.actionType
                                        hardwareType = firstAction.defaultHardwareType
                                        controlType = if (hardwareType == "BUTTON") "NOTE" else "CC"
                                        targetDeck = firstAction.defaultTargetDeck
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = catLabel,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color(0xFFCBD5E1),
                                maxLines = 1
                            )
                        }
                    }
                }

                // 2. Software Action Selection
                Text(
                    text = "2. SOFTWARE FUNCTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.textSecondary
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredActions.chunked(2).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowActions.forEach { act ->
                                val isSelected = selectedActionType == act.actionType
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
                                        .border(
                                            1.dp,
                                            if (isSelected) skinColors.deckAColor else Color(0xFF334155),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedActionType = act.actionType
                                            hardwareType = act.defaultHardwareType
                                            controlType = if (act.defaultHardwareType == "BUTTON") "NOTE" else "CC"
                                            if (act.defaultTargetDeck == 0) targetDeck = 0
                                        }
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = act.displayName,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) skinColors.deckAColor else Color.White
                                        )
                                        Text(
                                            text = act.description,
                                            fontSize = 8.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowActions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 3. Target Deck Picker
                Text(
                    text = "3. TARGET DECK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val deckOptions = listOf(
                        0 to "Master / Global",
                        1 to "Deck 1 (A)",
                        2 to "Deck 2 (B)",
                        3 to "Deck 3 (C)",
                        4 to "Deck 4 (D)"
                    )
                    deckOptions.forEach { (dNum, dLabel) ->
                        val isSelected = targetDeck == dNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) skinColors.deckBColor else Color(0xFF1E293B))
                                .clickable { targetDeck = dNum }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (dNum == 0) "Master" else "Deck $dNum",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                // 4. Hardware Control Type
                Text(
                    text = "4. HARDWARE CONTROL TYPE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("KNOB", "SLIDER", "BUTTON", "JOG_WHEEL").forEach { hw ->
                        val isSelected = hardwareType == hw
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B))
                                .clickable {
                                    hardwareType = hw
                                    controlType = if (hw == "BUTTON") "NOTE" else "CC"
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = hw.replace("_", " "),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                // 5. MIDI Signal Configuration (Channel, Control Number, Message Type)
                Text(
                    text = "5. MIDI SIGNAL BINDING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = skinColors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type (CC vs NOTE)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Type", fontSize = 9.sp, color = Color(0xFF94A3B8))
                        Row {
                            listOf("CC", "NOTE").forEach { t ->
                                val sel = controlType == t
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (sel) skinColors.deckAColor else Color(0xFF1E293B))
                                        .clickable { controlType = t }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(t, fontSize = 9.sp, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Channel (1-16)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Channel (1-16)", fontSize = 9.sp, color = Color(0xFF94A3B8))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { channel = (channel - 1).coerceAtLeast(1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("Ch $channel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            IconButton(
                                onClick = { channel = (channel + 1).coerceAtMost(16) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Number (0-127)
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("$controlType # (0-127)", fontSize = 9.sp, color = Color(0xFF94A3B8))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = { controlNumber = (controlNumber - 1).coerceAtLeast(0) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text("#$controlNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            IconButton(
                                onClick = { controlNumber = (controlNumber + 1).coerceAtMost(127) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Invert Polarity Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invert Polarity", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Reverse 0-127 range (useful for inverted faders)", fontSize = 8.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = isInverted,
                        onCheckedChange = { isInverted = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = skinColors.deckAColor)
                    )
                }
            }
        },
        containerColor = skinColors.surfaceChassis
    )
}
