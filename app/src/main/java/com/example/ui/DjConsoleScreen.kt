package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.DjSkins
import com.example.ui.viewmodel.ActiveDialog
import com.example.ui.viewmodel.DjViewModel

@Composable
fun DjConsoleScreen(
    viewModel: DjViewModel,
    modifier: Modifier = Modifier
) {
    val deckCount by viewModel.deckCount.collectAsStateWithLifecycle()
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val crossfaderPos by viewModel.crossfaderPosition.collectAsStateWithLifecycle()
    val crossfaderCurve by viewModel.crossfaderCurve.collectAsStateWithLifecycle()
    val isHamster by viewModel.isHamsterReversed.collectAsStateWithLifecycle()
    val visualizerBands by viewModel.visualizerBands.collectAsStateWithLifecycle()
    val beatPulse by viewModel.beatPulse.collectAsStateWithLifecycle()
    val activeSkin by viewModel.activeSkin.collectAsStateWithLifecycle()
    val activeDialog by viewModel.activeDialog.collectAsStateWithLifecycle()
    val masterVideoTransition by viewModel.masterVideoTransition.collectAsStateWithLifecycle()

    val libraryTracks by viewModel.libraryTracks.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val selectedCrate by viewModel.selectedCrate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.librarySearchQuery.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    var isVideoVisible by remember { mutableStateOf(false) }
    var targetDeckForLoad by remember { mutableStateOf(0) }
    val activeDeckTabPair by viewModel.activeDeckTabPair.collectAsStateWithLifecycle()

    val skinColors = remember(activeSkin) { DjSkins.getSkin(activeSkin) }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier
            .testTag("dj_console_screen")
            .fillMaxSize(),
        topBar = {
            DjTopBar(
                deckCount = deckCount,
                isVideoVisible = isVideoVisible,
                cloudSyncStatus = cloudSyncStatus,
                skinColors = skinColors,
                onDeckCountToggle = {
                    val nextCount = if (deckCount == 2) 4 else 2
                    viewModel.setDeckCount(nextCount)
                },
                onVideoToggle = { isVideoVisible = !isVideoVisible },
                onOpenLibrary = {
                    viewModel.setActiveDialog(ActiveDialog.LIBRARY)
                },
                onOpenMidi = {
                    viewModel.setActiveDialog(ActiveDialog.MIDI_MAPPING)
                },
                onOpenCloudSync = {
                    viewModel.setActiveDialog(ActiveDialog.CLOUD_SYNC)
                },
                onOpenSkins = {
                    viewModel.setActiveDialog(ActiveDialog.SKIN_PICKER)
                }
            )
        },
        bottomBar = {
            // Persistent Bottom Bar: Visualizer & Crossfader
            Surface(
                color = skinColors.surfaceChassis,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    // Audio Visualizer Bar with Harmonic Key status
                    AudioVisualizerBar(
                        spectrumBands = visualizerBands,
                        beatPulse = beatPulse,
                        masterKeyCamelot = decks[0].camelotKey,
                        deckBKeyCamelot = decks[1].camelotKey,
                        skinColors = skinColors
                    )

                    // Master Crossfader
                    CrossfaderSection(
                        crossfaderPosition = crossfaderPos,
                        crossfaderCurve = crossfaderCurve,
                        isHamster = isHamster,
                        skinColors = skinColors,
                        onPositionChange = { viewModel.setCrossfaderPosition(it) },
                        onCurveChange = { viewModel.setCrossfaderCurve(it) },
                        onToggleHamster = { viewModel.toggleHamster() },
                        onAutoTransition = { viewModel.autoTransition(it) }
                    )
                }
            }
        },
        containerColor = skinColors.background
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(skinColors.chassisGradient),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            // Optional Video Mixer Preview
            if (isVideoVisible) {
                item(key = "video_mixer_preview") {
                    VideoMixerPreview(
                        deckAIsPlaying = decks[0].isPlaying,
                        deckBIsPlaying = decks[1].isPlaying,
                        deckAPosition = decks[0].positionSeconds,
                        deckBPosition = decks[1].positionSeconds,
                        crossfaderPos = crossfaderPos,
                        transitionType = masterVideoTransition,
                        beatPulse = beatPulse,
                        skinColors = skinColors,
                        onExpandVideo = {
                            val nextTrans = when (masterVideoTransition) {
                                "Crossfade" -> "Wipe"
                                "Wipe" -> "Cut"
                                "Cut" -> "Glitch"
                                else -> "Crossfade"
                            }
                            viewModel.setMasterVideoTransition(nextTrans)
                        }
                    )
                }
            }

            // 4-Deck Sub-selector Switcher if 4-Deck mode is active
            if (deckCount == 4) {
                item(key = "deck_pair_tabs") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TabRow(
                            selectedTabIndex = activeDeckTabPair,
                            containerColor = skinColors.surfaceChassis,
                            contentColor = skinColors.deckAColor,
                            modifier = Modifier
                                .width(280.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Tab(
                                selected = activeDeckTabPair == 0,
                                onClick = { viewModel.setActiveDeckTabPair(0) },
                                text = {
                                    Text(
                                        text = "DECKS A & B (CH 1 & 2)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeDeckTabPair == 0) skinColors.deckAColor else skinColors.textSecondary
                                    )
                                }
                            )
                            Tab(
                                selected = activeDeckTabPair == 1,
                                onClick = { viewModel.setActiveDeckTabPair(1) },
                                text = {
                                    Text(
                                        text = "DECKS C & D (CH 3 & 4)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeDeckTabPair == 1) skinColors.deckBColor else skinColors.textSecondary
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Determine which decks to display based on deckCount and active tab
            val activeDecksToDisplay = if (deckCount == 2) {
                listOf(0, 1)
            } else {
                if (activeDeckTabPair == 0) listOf(0, 1) else listOf(2, 3)
            }

            // Deck Views
            items(activeDecksToDisplay.size) { i ->
                val deckIdx = activeDecksToDisplay[i]
                val deckState = decks[deckIdx]
                val deckColor = when (deckIdx) {
                    0 -> skinColors.deckAColor
                    1 -> skinColors.deckBColor
                    2 -> skinColors.deckCColor
                    else -> skinColors.deckDColor
                }

                DeckView(
                    deckState = deckState,
                    deckColor = deckColor,
                    skinColors = skinColors,
                    onLoadTrackClick = {
                        targetDeckForLoad = deckIdx
                        viewModel.setActiveDialog(ActiveDialog.LIBRARY)
                    },
                    onPlayToggle = { viewModel.togglePlay(deckIdx) },
                    onCuePress = { viewModel.pressCue(deckIdx) },
                    onSyncToggle = { viewModel.toggleSync(deckIdx) },
                    onPitchChange = { viewModel.setPitchPercent(deckIdx, it) },
                    onPitchNudge = { viewModel.nudgePitch(deckIdx, it) },
                    onVolumeChange = { viewModel.setVolume(deckIdx, it) },
                    onGainChange = { viewModel.setGain(deckIdx, it) },
                    onEqChange = { band, valGain -> viewModel.setEq(deckIdx, band, valGain) },
                    onEqKillToggle = { band -> viewModel.toggleEqKill(deckIdx, band) },
                    onFilterChange = { viewModel.setFilter(deckIdx, it) },
                    onVocalCutToggle = { viewModel.toggleVocalCut(deckIdx) },
                    onHotCueTrigger = { cueNum -> viewModel.triggerHotCue(deckIdx, cueNum) },
                    onLoopToggle = { beats -> viewModel.toggleLoop(deckIdx, beats) },
                    onFxToggle = { fxName -> viewModel.toggleFx(deckIdx, fxName) },
                    onScratchDelta = { delta -> viewModel.onPlatterScratch(deckIdx, delta) },
                    onSeekSeconds = { sec ->
                        // Seek deck
                        viewModel.triggerHotCue(deckIdx, 1)
                    }
                )
            }
        }
    }

    // Modal Dialogs
    when (activeDialog) {
        ActiveDialog.LIBRARY -> {
            LibraryDialog(
                tracks = libraryTracks,
                playlists = allPlaylists,
                selectedCrate = selectedCrate,
                searchQuery = searchQuery,
                cloudSyncStatus = cloudSyncStatus,
                deckCount = deckCount,
                skinColors = skinColors,
                onSelectCrate = { viewModel.selectCrate(it) },
                onSearchChange = { viewModel.searchLibrary(it) },
                onLoadTrack = { deckIdx, track ->
                    viewModel.loadTrackToDeck(deckIdx, track)
                    viewModel.setActiveDialog(ActiveDialog.NONE)
                },
                onOpenAddTrack = {
                    viewModel.setActiveDialog(ActiveDialog.ADD_TRACK)
                },
                onTriggerCloudSync = { viewModel.triggerCloudSync() },
                onImportLocalAudio = { uris -> viewModel.importMultipleAudioUris(uris) },
                onScanLocalStorage = { viewModel.scanDeviceMusic() },
                onDismiss = { viewModel.setActiveDialog(ActiveDialog.NONE) }
            )
        }

        ActiveDialog.MIDI_MAPPING -> {
            MidiSettingsDialog(
                viewModel = viewModel,
                skinColors = skinColors,
                onDismiss = { viewModel.setActiveDialog(ActiveDialog.NONE) }
            )
        }

        ActiveDialog.CLOUD_SYNC -> {
            CloudSyncDialog(
                status = cloudSyncStatus,
                skinColors = skinColors,
                onTriggerSync = { viewModel.triggerCloudSync() },
                onDismiss = { viewModel.setActiveDialog(ActiveDialog.NONE) }
            )
        }

        ActiveDialog.SKIN_PICKER -> {
            SkinSelectorSheet(
                activeSkin = activeSkin,
                skinColors = skinColors,
                onSelectSkin = { viewModel.setSkin(it) },
                onDismiss = { viewModel.setActiveDialog(ActiveDialog.NONE) }
            )
        }

        ActiveDialog.ADD_TRACK -> {
            AddTrackDialog(
                skinColors = skinColors,
                onAddTrack = { title, artist, bpm, key, genre, isVideo ->
                    viewModel.addNewTrack(title, artist, bpm, key, genre, isVideo)
                },
                onImportLocalAudio = { uri -> viewModel.importLocalAudioUri(uri) },
                onDismiss = { viewModel.setActiveDialog(ActiveDialog.NONE) }
            )
        }

        else -> { /* No active dialog */ }
    }
}
