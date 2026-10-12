package com.example.ui.components.midi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MidiMapping
import com.example.engine.DjMidiService
import com.example.engine.MidiDeviceItem
import com.example.engine.MidiEvent
import com.example.engine.MidiPresetProfiles
import com.example.ui.theme.DjSkinColors
import com.example.ui.viewmodel.DjViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidiMappingScreen(
    viewModel: DjViewModel,
    skinColors: DjSkinColors,
    onDismiss: () -> Unit
) {
    val midiService = viewModel.midiService
    val activeMappings by viewModel.currentMidiMappings.collectAsStateWithLifecycle()
    val activeProfile by viewModel.selectedMidiProfile.collectAsStateWithLifecycle()
    val allProfiles by viewModel.distinctMidiProfiles.collectAsStateWithLifecycle()
    val connectedDevices by midiService.connectedDevices.collectAsStateWithLifecycle()
    val isMidiSupported by midiService.isMidiSupported.collectAsStateWithLifecycle()
    val lastEvent by midiService.lastMidiEvent.collectAsStateWithLifecycle()
    val lastAction by midiService.lastTriggeredAction.collectAsStateWithLifecycle()
    val learnSession by midiService.learnSession.collectAsStateWithLifecycle()
    val isLearning = learnSession != null
    val midiActivityCount by midiService.midiActivityCount.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0=Mappings, 1=Virtual Hardware, 2=Monitor, 3=Profiles
    var categoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var mappingToEdit by remember { mutableStateOf<MidiMapping?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileNameInput by remember { mutableStateOf("") }

    // Live activity indicator flash
    val isActivityActive = remember(midiActivityCount) { midiActivityCount > 0 }
    val activityLedColor by animateColorAsState(
        targetValue = if (isLearning) Color(0xFFEF4444) else if (isActivityActive) Color(0xFF10B981) else Color(0xFF334155),
        label = "ledColor"
    )

    Surface(
        color = skinColors.surfaceChassis,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .testTag("midi_mapping_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B14))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Activity LED
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activityLedColor)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "MIDI CONTROLLER SYSTEM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = skinColors.textPrimary
                            )
                            // Profile pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(skinColors.deckAColor.copy(alpha = 0.2f))
                                    .border(0.5.dp, skinColors.deckAColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeProfile,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skinColors.deckAColor
                                )
                            }
                        }

                        Text(
                            text = if (isMidiSupported) "Android MIDI Core Active • ${connectedDevices.count { it.isConnected }} connected" else "USB OTG / BLE MIDI Ready (Virtual Host Active)",
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = skinColors.textSecondary)
                }
            }

            // Learning banner
            AnimatedVisibility(visible = isLearning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7F1D1D))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = "MIDI LEARN: Move hardware knob/slider or press pad to assign '${learnSession?.controlName}'",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        TextButton(
                            onClick = { midiService.cancelMidiLearn() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("CANCEL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF0A0F1D),
                contentColor = skinColors.deckAColor,
                divider = { HorizontalDivider(color = Color(0xFF1E293B)) }
            ) {
                val tabs = listOf(
                    "ASSIGNMENTS" to Icons.Default.Tune,
                    "VIRTUAL RACK" to Icons.Default.Equalizer,
                    "LIVE MONITOR" to Icons.Default.Cable,
                    "PRESETS" to Icons.Default.SettingsSuggest
                )
                tabs.forEachIndexed { idx, (title, icon) ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        icon = { Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(16.dp)) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = if (activeTab == idx) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Contents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF070B14))
            ) {
                when (activeTab) {
                    0 -> MappingsListTab(
                        mappings = activeMappings,
                        categoryFilter = categoryFilter,
                        searchQuery = searchQuery,
                        lastTriggered = lastAction,
                        skinColors = skinColors,
                        onCategorySelect = { categoryFilter = it },
                        onSearchChange = { searchQuery = it },
                        onAddClick = {
                            mappingToEdit = null
                            showEditDialog = true
                        },
                        onEditClick = { mapping ->
                            mappingToEdit = mapping
                            showEditDialog = true
                        },
                        onLearnClick = { mapping ->
                            midiService.startMidiLearn(
                                actionType = mapping.actionType,
                                targetDeck = mapping.targetDeck,
                                controlName = mapping.controlName,
                                actionCategory = mapping.actionCategory,
                                hardwareControlType = mapping.hardwareControlType,
                                profileName = activeProfile
                            )
                        },
                        onToggleInvert = { mapping ->
                            viewModel.saveMidiMapping(mapping.copy(isInverted = !mapping.isInverted))
                        },
                        onDeleteClick = { mapping ->
                            viewModel.deleteMidiMapping(mapping)
                        }
                    )

                    1 -> VirtualHardwareControllerTab(
                        midiService = midiService,
                        viewModel = viewModel,
                        skinColors = skinColors
                    )

                    2 -> LiveMidiMonitorTab(
                        midiService = midiService,
                        connectedDevices = connectedDevices,
                        lastEvent = lastEvent,
                        lastAction = lastAction,
                        skinColors = skinColors
                    )

                    3 -> PresetsAndProfilesTab(
                        activeProfile = activeProfile,
                        allProfiles = allProfiles,
                        skinColors = skinColors,
                        onSelectProfile = { viewModel.selectMidiProfile(it) },
                        onLoadBuiltInPreset = { viewModel.loadPresetProfile(it) },
                        onSaveAsNew = { showNewProfileDialog = true },
                        onResetDefaults = { viewModel.resetCurrentProfileToDefaults() },
                        onDeleteProfile = { viewModel.deleteMidiProfile(it) }
                    )
                }
            }
        }
    }

    // Modal: Edit or Add Mapping Dialog
    if (showEditDialog) {
        EditMappingDialog(
            initialMapping = mappingToEdit,
            midiService = midiService,
            activeProfileName = activeProfile,
            skinColors = skinColors,
            onSave = { updated ->
                viewModel.saveMidiMapping(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // Modal: Save Profile As Dialog
    if (showNewProfileDialog) {
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            title = { Text("SAVE MAPPING PROFILE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = skinColors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newProfileNameInput,
                    onValueChange = { newProfileNameInput = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. My Studio Controller") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileNameInput.isNotBlank()) {
                            viewModel.saveCurrentProfileAs(newProfileNameInput.trim())
                            showNewProfileDialog = false
                            newProfileNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor)
                ) {
                    Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) {
                    Text("CANCEL", color = skinColors.textSecondary)
                }
            },
            containerColor = skinColors.surfaceChassis
        )
    }
}

/**
 * Tab 1: Mappings List with Search, Filter by Category, and CRUD actions
 */
@Composable
private fun MappingsListTab(
    mappings: List<MidiMapping>,
    categoryFilter: String,
    searchQuery: String,
    lastTriggered: com.example.engine.TriggeredMidiAction?,
    skinColors: DjSkinColors,
    onCategorySelect: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (MidiMapping) -> Unit,
    onLearnClick: (MidiMapping) -> Unit,
    onToggleInvert: (MidiMapping) -> Unit,
    onDeleteClick: (MidiMapping) -> Unit
) {
    val filtered = remember(mappings, categoryFilter, searchQuery) {
        mappings.filter { m ->
            val matchesCategory = categoryFilter == "ALL" || m.actionCategory == categoryFilter
            val matchesSearch = searchQuery.isBlank() ||
                m.controlName.contains(searchQuery, ignoreCase = true) ||
                m.actionType.contains(searchQuery, ignoreCase = true) ||
                m.controlNumber.toString().contains(searchQuery)
            matchesCategory && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Category Filter Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search hardware control, CC, note...", fontSize = 10.sp, color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skinColors.deckAColor,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("add_mapping_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val chips = listOf(
                    "ALL" to "All (${mappings.size})",
                    "TRANSPORT" to "Transport",
                    "EQ" to "EQs",
                    "FADER" to "Faders",
                    "EFFECTS_SEND" to "Effects",
                    "DECK_SELECT" to "Decks"
                )
                chips.forEach { (catKey, catLabel) ->
                    val isSel = categoryFilter == catKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) skinColors.deckAColor else Color(0xFF1E293B))
                            .clickable { onCategorySelect(catKey) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = catLabel,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Mapping List
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No MIDI mappings found", color = skinColors.textSecondary, fontSize = 12.sp)
                    Text("Tap 'ADD' to assign a hardware control or choose a preset", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { mapping ->
                    val isJustTriggered = lastTriggered?.mapping?.id == mapping.id &&
                        (System.currentTimeMillis() - (lastTriggered?.timestamp ?: 0)) < 400

                    val cardBorderColor by animateColorAsState(
                        targetValue = if (isJustTriggered) skinColors.deckAColor else Color(0xFF1E293B),
                        label = "cardBorder"
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isJustTriggered) Color(0xFF132338) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorderColor, RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Hardware Control Icon badge
                                    val (hwIcon, hwColor) = when (mapping.hardwareControlType) {
                                        "KNOB" -> Icons.Default.Adjust to Color(0xFF38BDF8)
                                        "SLIDER" -> Icons.Default.LinearScale to Color(0xFFF59E0B)
                                        "BUTTON" -> Icons.Default.RadioButtonChecked to Color(0xFF10B981)
                                        else -> Icons.Default.RotateRight to Color(0xFFA855F7)
                                    }
                                    Icon(imageVector = hwIcon, contentDescription = null, tint = hwColor, modifier = Modifier.size(13.dp))

                                    Text(
                                        text = mapping.controlName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    if (mapping.targetDeck > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF334155))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("D${mapping.targetDeck}", fontSize = 7.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF334155))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("MST", fontSize = 7.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${mapping.controlType} #${mapping.controlNumber} • Ch ${mapping.channel}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "• ${mapping.actionCategory}",
                                        fontSize = 8.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    if (mapping.isInverted) {
                                        Text(
                                            text = "[INV]",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF43F5E)
                                        )
                                    }
                                }
                            }

                            // Action buttons
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Learn
                                IconButton(
                                    onClick = { onLearnClick(mapping) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Sensors, contentDescription = "Learn", tint = skinColors.deckAColor, modifier = Modifier.size(15.dp))
                                }

                                // Edit
                                IconButton(
                                    onClick = { onEditClick(mapping) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                                }

                                // Delete
                                IconButton(
                                    onClick = { onDeleteClick(mapping) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Tactile Virtual Hardware Controller Simulator (Test Rack)
 */
@Composable
private fun VirtualHardwareControllerTab(
    midiService: DjMidiService,
    viewModel: DjViewModel,
    skinColors: DjSkinColors
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val crossfaderPos by viewModel.crossfaderPosition.collectAsStateWithLifecycle()
    val focusedDeck by viewModel.focusedDeck.collectAsStateWithLifecycle()

    var ch1Vol by remember { mutableStateOf(0.85f) }
    var ch2Vol by remember { mutableStateOf(0.85f) }
    var ch1High by remember { mutableStateOf(0.5f) }
    var ch1Mid by remember { mutableStateOf(0.5f) }
    var ch1Low by remember { mutableStateOf(0.5f) }
    var ch1Filter by remember { mutableStateOf(0.5f) }
    var ch1Gain by remember { mutableStateOf(0.5f) }

    var ch2High by remember { mutableStateOf(0.5f) }
    var ch2Mid by remember { mutableStateOf(0.5f) }
    var ch2Low by remember { mutableStateOf(0.5f) }
    var ch2Filter by remember { mutableStateOf(0.5f) }
    var ch2Gain by remember { mutableStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Deck Selection Row
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "DECK SELECTION & ROUTING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val deckButtons = listOf(
                        Triple(1, "DECK 1 (A)", Color(0xFF06B6D4)),
                        Triple(2, "DECK 2 (B)", Color(0xFFEC4899)),
                        Triple(3, "DECK 3 (C)", Color(0xFFF59E0B)),
                        Triple(4, "DECK 4 (D)", Color(0xFFA855F7))
                    )
                    deckButtons.forEach { (dNum, label, color) ->
                        HardwareIlluminatedButton(
                            label = label,
                            subLabel = "Note ${49 + dNum}",
                            activeColor = color,
                            isLit = focusedDeck == (dNum - 1),
                            onPress = {
                                midiService.sendVirtualMidiMessage("NOTE_ON", 1, 49 + dNum, 127)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HardwareIlluminatedButton(
                        label = "A/B ↔ C/D",
                        subLabel = "Note 80",
                        activeColor = Color(0xFF10B981),
                        isLit = false,
                        onPress = {
                            midiService.sendVirtualMidiMessage("NOTE_ON", 1, 80, 127)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Mixer & Decks Strip
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "HARDWARE CONSOLE MIXER (CH 1 & CH 2)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // CHANNEL 1 STRIP
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CHANNEL 1", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF06B6D4))

                        // Knobs
                        HardwareRotaryKnob(
                            label = "TRIM",
                            value = ch1Gain,
                            accentColor = Color(0xFF06B6D4),
                            onValueChange = {
                                ch1Gain = it
                                midiService.sendVirtualMidiMessage("CC", 1, 14, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "HIGH",
                            value = ch1High,
                            bipolar = true,
                            accentColor = Color(0xFF38BDF8),
                            onValueChange = {
                                ch1High = it
                                midiService.sendVirtualMidiMessage("CC", 1, 18, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "MID",
                            value = ch1Mid,
                            bipolar = true,
                            accentColor = Color(0xFF38BDF8),
                            onValueChange = {
                                ch1Mid = it
                                midiService.sendVirtualMidiMessage("CC", 1, 17, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "LOW",
                            value = ch1Low,
                            bipolar = true,
                            accentColor = Color(0xFF38BDF8),
                            onValueChange = {
                                ch1Low = it
                                midiService.sendVirtualMidiMessage("CC", 1, 16, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "FILTER",
                            value = ch1Filter,
                            bipolar = true,
                            accentColor = Color(0xFFF59E0B),
                            onValueChange = {
                                ch1Filter = it
                                midiService.sendVirtualMidiMessage("CC", 1, 19, (it * 127).roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // FX Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HardwareIlluminatedButton(
                                label = "ECHO",
                                activeColor = Color(0xFF06B6D4),
                                isLit = decks[0].echoActive,
                                onPress = {
                                    midiService.sendVirtualMidiMessage("NOTE_ON", 1, 70, 127)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HardwareIlluminatedButton(
                                label = "REVERB",
                                activeColor = Color(0xFF06B6D4),
                                isLit = decks[0].reverbActive,
                                onPress = {
                                    midiService.sendVirtualMidiMessage("NOTE_ON", 1, 71, 127)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Vertical Fader Ch 1
                        HardwareVerticalFader(
                            label = "CH 1 FADER",
                            value = ch1Vol,
                            accentColor = Color(0xFF06B6D4),
                            onValueChange = {
                                ch1Vol = it
                                midiService.sendVirtualMidiMessage("CC", 1, 24, (it * 127).roundToInt())
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // CENTER MASTER SECTION
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MASTER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF94A3B8))

                        var masterVol by remember { mutableStateOf(0.9f) }
                        HardwareVerticalFader(
                            label = "MASTER VOL",
                            value = masterVol,
                            accentColor = Color(0xFF10B981),
                            onValueChange = {
                                masterVol = it
                                midiService.sendVirtualMidiMessage("CC", 1, 7, (it * 127).roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("STATUS", fontSize = 8.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (decks[0].isPlaying || decks[1].isPlaying) "ON AIR" else "STANDBY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (decks[0].isPlaying || decks[1].isPlaying) Color(0xFF10B981) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // CHANNEL 2 STRIP
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CHANNEL 2", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFEC4899))

                        // Knobs
                        HardwareRotaryKnob(
                            label = "TRIM",
                            value = ch2Gain,
                            accentColor = Color(0xFFEC4899),
                            onValueChange = {
                                ch2Gain = it
                                midiService.sendVirtualMidiMessage("CC", 1, 15, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "HIGH",
                            value = ch2High,
                            bipolar = true,
                            accentColor = Color(0xFFEC4899),
                            onValueChange = {
                                ch2High = it
                                midiService.sendVirtualMidiMessage("CC", 1, 22, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "MID",
                            value = ch2Mid,
                            bipolar = true,
                            accentColor = Color(0xFFEC4899),
                            onValueChange = {
                                ch2Mid = it
                                midiService.sendVirtualMidiMessage("CC", 1, 21, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "LOW",
                            value = ch2Low,
                            bipolar = true,
                            accentColor = Color(0xFFEC4899),
                            onValueChange = {
                                ch2Low = it
                                midiService.sendVirtualMidiMessage("CC", 1, 20, (it * 127).roundToInt())
                            }
                        )
                        HardwareRotaryKnob(
                            label = "FILTER",
                            value = ch2Filter,
                            bipolar = true,
                            accentColor = Color(0xFFF59E0B),
                            onValueChange = {
                                ch2Filter = it
                                midiService.sendVirtualMidiMessage("CC", 1, 23, (it * 127).roundToInt())
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // FX Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HardwareIlluminatedButton(
                                label = "ECHO",
                                activeColor = Color(0xFFEC4899),
                                isLit = decks[1].echoActive,
                                onPress = {
                                    midiService.sendVirtualMidiMessage("NOTE_ON", 1, 74, 127)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HardwareIlluminatedButton(
                                label = "REVERB",
                                activeColor = Color(0xFFEC4899),
                                isLit = decks[1].reverbActive,
                                onPress = {
                                    midiService.sendVirtualMidiMessage("NOTE_ON", 1, 75, 127)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Vertical Fader Ch 2
                        HardwareVerticalFader(
                            label = "CH 2 FADER",
                            value = ch2Vol,
                            accentColor = Color(0xFFEC4899),
                            onValueChange = {
                                ch2Vol = it
                                midiService.sendVirtualMidiMessage("CC", 1, 25, (it * 127).roundToInt())
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Master Crossfader
                HardwareHorizontalCrossfader(
                    value = crossfaderPos,
                    onValueChange = {
                        val ccVal = (((it + 1.0f) / 2.0f) * 127f).roundToInt()
                        midiService.sendVirtualMidiMessage("CC", 1, 8, ccVal)
                    }
                )
            }
        }

        // Hardware Transport & Performance Pads Row
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "TRANSPORT & PERFORMANCE PADS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Deck 1 Transport
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DECK 1 TRANSPORT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HardwareIlluminatedButton(
                                label = "PLAY",
                                subLabel = "N#60",
                                icon = Icons.Default.PlayArrow,
                                activeColor = Color(0xFF10B981),
                                isLit = decks[0].isPlaying,
                                onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 60, 127) },
                                modifier = Modifier.weight(1f)
                            )
                            HardwareIlluminatedButton(
                                label = "CUE",
                                subLabel = "N#61",
                                activeColor = Color(0xFFF59E0B),
                                isLit = false,
                                onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 61, 127) },
                                modifier = Modifier.weight(1f)
                            )
                            HardwareIlluminatedButton(
                                label = "SYNC",
                                subLabel = "N#62",
                                activeColor = Color(0xFF06B6D4),
                                isLit = decks[0].isSync,
                                onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 62, 127) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Hot Cues
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..4).forEach { cueIdx ->
                                HardwareIlluminatedButton(
                                    label = "CUE $cueIdx",
                                    subLabel = "N#${35 + cueIdx}",
                                    activeColor = Color(0xFF06B6D4),
                                    isLit = decks[0].hotCues.containsKey(cueIdx),
                                    onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 35 + cueIdx, 127) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Jog Wheel
                        HardwareJogWheel(
                            deckLabel = "1",
                            accentColor = Color(0xFF06B6D4),
                            onScratchMove = { delta ->
                                val ccVal = (64 + delta.coerceIn(-30f, 30f)).roundToInt()
                                midiService.sendVirtualMidiMessage("CC", 1, 32, ccVal)
                            }
                        )
                    }

                    // Deck 2 Transport
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DECK 2 TRANSPORT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HardwareIlluminatedButton(
                                label = "PLAY",
                                subLabel = "N#64",
                                icon = Icons.Default.PlayArrow,
                                activeColor = Color(0xFF10B981),
                                isLit = decks[1].isPlaying,
                                onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 64, 127) },
                                modifier = Modifier.weight(1f)
                            )
                            HardwareIlluminatedButton(
                                label = "CUE",
                                subLabel = "N#65",
                                activeColor = Color(0xFFF59E0B),
                                isLit = false,
                                onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 65, 127) },
                                modifier = Modifier.weight(1f)
                            )
                            HardwareIlluminatedButton(
                                label = "SYNC",
                                subLabel = "N#66",
                                activeColor = Color(0xFFEC4899),
                                isLit = decks[1].isSync,
                                onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 66, 127) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Hot Cues
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..4).forEach { cueIdx ->
                                HardwareIlluminatedButton(
                                    label = "CUE $cueIdx",
                                    subLabel = "N#${39 + cueIdx}",
                                    activeColor = Color(0xFFEC4899),
                                    isLit = decks[1].hotCues.containsKey(cueIdx),
                                    onPress = { midiService.sendVirtualMidiMessage("NOTE_ON", 1, 39 + cueIdx, 127) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Jog Wheel
                        HardwareJogWheel(
                            deckLabel = "2",
                            accentColor = Color(0xFFEC4899),
                            onScratchMove = { delta ->
                                val ccVal = (64 + delta.coerceIn(-30f, 30f)).roundToInt()
                                midiService.sendVirtualMidiMessage("CC", 1, 33, ccVal)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Live MIDI Packet Monitor & Hardware Device discovery
 */
@Composable
private fun LiveMidiMonitorTab(
    midiService: DjMidiService,
    connectedDevices: List<MidiDeviceItem>,
    lastEvent: MidiEvent?,
    lastAction: com.example.engine.TriggeredMidiAction?,
    skinColors: DjSkinColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Physical Controllers List
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DETECTED USB & BLUETOOTH MIDI CONTROLLERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    IconButton(onClick = { midiService.scanDevices() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = skinColors.deckAColor, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (connectedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "No external physical MIDI controller currently attached.\nConnect your DJ controller via USB OTG cable or Bluetooth MIDI. You can also use the Virtual Hardware Rack above.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 14.sp
                        )
                    }
                } else {
                    connectedDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(dev.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(dev.manufacturer, fontSize = 9.sp, color = Color(0xFF94A3B8))
                            }
                            if (dev.isConnected) {
                                Button(
                                    onClick = { midiService.disconnectCurrentDevice() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("DISCONNECT", fontSize = 8.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { midiService.connectDevice(dev) },
                                    colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("CONNECT", fontSize = 8.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live MIDI Event Stream
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LIVE INCOMING MIDI PACKETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text("STATUS: LISTENING", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF050811))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    if (lastEvent != null) {
                        val ev = lastEvent
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("LATEST PACKET:", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                Text("${ev.timestamp}", fontSize = 8.sp, color = Color(0xFF475569))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column {
                                    Text("TYPE", fontSize = 8.sp, color = Color(0xFF64748B))
                                    Text(ev.type, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("CHANNEL", fontSize = 8.sp, color = Color(0xFF64748B))
                                    Text("Ch ${ev.channel}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("CTRL / NOTE", fontSize = 8.sp, color = Color(0xFF64748B))
                                    Text("#${ev.controlOrNote}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("RAW VALUE", fontSize = 8.sp, color = Color(0xFF64748B))
                                    Text("${ev.value}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontFamily = FontFamily.Monospace)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 4.dp))

                            Text("RESOLVED SOFTWARE DISPATCH:", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            if (lastAction != null) {
                                val act = lastAction
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Action: ${act.mapping.controlName} (${act.mapping.actionType})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = skinColors.deckAColor
                                        )
                                        Text(
                                            text = "Target: ${if (act.mapping.targetDeck == 0) "Master" else "Deck ${act.mapping.targetDeck}"} • Output: ${act.normalizedValue}",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            } else {
                                Text("No software action matched for this hardware control.", fontSize = 9.sp, color = Color(0xFFEF4444))
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Waiting for incoming MIDI packets...\nMove a physical controller knob or tap the virtual rack.",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: Controller Presets & Custom Profiles Management
 */
@Composable
private fun PresetsAndProfilesTab(
    activeProfile: String,
    allProfiles: List<String>,
    skinColors: DjSkinColors,
    onSelectProfile: (String) -> Unit,
    onLoadBuiltInPreset: (String) -> Unit,
    onSaveAsNew: () -> Unit,
    onResetDefaults: () -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    val builtInPresets = MidiPresetProfiles.getAllBuiltInProfiles().keys.toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Built-in Hardware Templates
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "HARDWARE CONTROLLER PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Load pre-configured mappings for popular DJ hardware",
                    fontSize = 8.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                builtInPresets.forEach { presetName ->
                    val isCurrent = activeProfile == presetName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrent) Color(0xFF1E293B) else Color(0xFF0A0F1D))
                            .border(1.dp, if (isCurrent) skinColors.deckAColor else Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = presetName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) skinColors.deckAColor else Color.White
                            )
                            val desc = when (presetName) {
                                "Default" -> "Standard Virtual DJ 2-4 deck layout"
                                "Pioneer DDJ-400 / FLX4" -> "Official Pioneer DDJ CC & Note assignments"
                                "Traktor Kontrol S2 / S4" -> "Native Instruments Kontrol 2-channel matrix"
                                else -> "8 Rotary knobs + 8 velocity pads matrix"
                            }
                            Text(desc, fontSize = 8.5.sp, color = Color(0xFF94A3B8))
                        }

                        Button(
                            onClick = { onLoadBuiltInPreset(presetName) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrent) skinColors.deckAColor else Color(0xFF334155)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isCurrent) "LOADED" else "LOAD",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.Black else Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Active Profile Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "PROFILE MANAGEMENT ('$activeProfile')",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onSaveAsNew,
                        colors = ButtonDefaults.buttonColors(containerColor = skinColors.deckAColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE AS NEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = onResetDefaults,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (activeProfile != "Default") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onDeleteProfile(activeProfile) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DELETE THIS PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
