package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DjDatabase
import com.example.data.model.MidiMapping
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.repository.CloudSyncStatus
import com.example.data.repository.DjLibraryRepository
import com.example.engine.*
import com.example.ui.theme.DjSkinType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DeckUiState(
    val index: Int,
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionSeconds: Float = 0f,
    val durationSeconds: Float = 200f,
    val baseBpm: Float = 126f,
    val pitchPercent: Float = 0f, // -50% to +50%
    val keyLock: Boolean = true,
    val isSync: Boolean = false,
    val isMaster: Boolean = false,
    val volume: Float = 0.85f,
    val gain: Float = 1.0f,
    val eqLow: Float = 1.0f,
    val eqMid: Float = 1.0f,
    val eqHigh: Float = 1.0f,
    val eqLowKill: Boolean = false,
    val eqMidKill: Boolean = false,
    val eqHighKill: Boolean = false,
    val filterValue: Float = 0.0f, // -1.0 to +1.0
    val vocalStemMode: VocalStemMode = VocalStemMode.FULL_MIX,
    val echoActive: Boolean = false,
    val reverbActive: Boolean = false,
    val flangerActive: Boolean = false,
    val bitcrusherActive: Boolean = false,
    val loopActive: Boolean = false,
    val loopBeats: Float = 4f,
    val hotCues: Map<Int, Float> = emptyMap(), // Cue 1-4 -> seconds
    val platterAngleDegrees: Float = 0f,
    val isVideo: Boolean = false,
    val crossfaderAssign: Int = -1 // -1 = Left (A/C), 1 = Right (B/D), 0 = Thru
) {
    val effectiveBpm: Float
        get() = baseBpm * (1.0f + pitchPercent / 100f)

    val camelotKey: String
        get() = track?.camelotKey ?: "8A"

    val musicalKey: String
        get() = track?.musicalKey ?: "Am"
}

enum class ActiveDialog {
    NONE,
    MIDI_MAPPING,
    CLOUD_SYNC,
    SKIN_PICKER,
    LIBRARY,
    ADD_TRACK,
    VIDEO_MONITOR
}

class DjViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DjDatabase.getDatabase(application, viewModelScope)
    val repository = DjLibraryRepository(database.trackDao())
    val audioEngine = DjAudioEngine(viewModelScope)
    val midiService = DjMidiService(application)
    val localDeckPlayers = Array(4) { index -> LocalDeckPlayer(application, index) }

    private val _deckCount = MutableStateFlow(2) // 2 or 4 decks
    val deckCount = _deckCount.asStateFlow()

    private val _decks = MutableStateFlow(
        List(4) { idx ->
            DeckUiState(
                index = idx,
                crossfaderAssign = if (idx % 2 == 0) -1 else 1
            )
        }
    )
    val decks = _decks.asStateFlow()

    private val _crossfaderPosition = MutableStateFlow(0.0f) // -1.0 (A) to +1.0 (B)
    val crossfaderPosition = _crossfaderPosition.asStateFlow()

    private val _crossfaderCurve = MutableStateFlow(CrossfaderCurve.SMOOTH)
    val crossfaderCurve = _crossfaderCurve.asStateFlow()

    private val _isHamsterReversed = MutableStateFlow(false)
    val isHamsterReversed = _isHamsterReversed.asStateFlow()

    private val _visualizerBands = MutableStateFlow(FloatArray(32))
    val visualizerBands = _visualizerBands.asStateFlow()

    private val _beatPulse = MutableStateFlow(0f)
    val beatPulse = _beatPulse.asStateFlow()

    private val _activeSkin = MutableStateFlow(DjSkinType.CLASSIC_DARK)
    val activeSkin = _activeSkin.asStateFlow()

    private val _activeDialog = MutableStateFlow(ActiveDialog.NONE)
    val activeDialog = _activeDialog.asStateFlow()

    private val _masterVideoTransition = MutableStateFlow("Crossfade") // "Crossfade", "Cut", "Wipe", "Glitch"
    val masterVideoTransition = _masterVideoTransition.asStateFlow()

    private val _selectedCrate = MutableStateFlow("All Tracks")
    val selectedCrate = _selectedCrate.asStateFlow()

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery = _librarySearchQuery.asStateFlow()

    val allPlaylists = repository.getAllPlaylists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val libraryTracks = combine(_selectedCrate, _librarySearchQuery) { crate, query ->
        Pair(crate, query)
    }.flatMapLatest { (crate, query) ->
        if (query.isNotBlank()) {
            repository.searchTracks(query)
        } else {
            repository.getTracksByCrate(crate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudSyncStatus = repository.syncStatus

    private val _focusedDeck = MutableStateFlow(0) // 0=A, 1=B, 2=C, 3=D
    val focusedDeck = _focusedDeck.asStateFlow()

    private val _activeDeckTabPair = MutableStateFlow(0) // 0 = Decks A&B, 1 = Decks C&D
    val activeDeckTabPair = _activeDeckTabPair.asStateFlow()

    private val _selectedMidiProfile = MutableStateFlow("Default")
    val selectedMidiProfile = _selectedMidiProfile.asStateFlow()

    val distinctMidiProfiles = repository.getDistinctProfileNames().map { list ->
        val standard = listOf("Default", "Pioneer DDJ-400 / FLX4", "Traktor Kontrol S2 / S4", "Akai MPK Mini / APC")
        (standard + list).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Default"))

    val currentMidiMappings = _selectedMidiProfile.flatMapLatest { profile ->
        repository.getMidiMappingsByProfile(profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var playbackTickerJob: Job? = null
    private var autoTransitionJob: Job? = null

    init {
        audioEngine.start()
        midiService.initialize()

        // Set visualizer callback from low latency audio engine
        audioEngine.onSpectrumUpdated = { bands, energy ->
            _visualizerBands.value = bands
            _beatPulse.value = energy
        }

        // MIDI controller binding callback
        midiService.onMidiActionTriggered = { deckIdx, action, value ->
            handleMidiAction(deckIdx, action, value)
        }

        // MIDI learn callback
        midiService.onMidiLearned = { newMapping ->
            viewModelScope.launch {
                repository.insertMidiMapping(newMapping)
            }
        }

        // Observe current MIDI mappings and keep midiService in sync
        viewModelScope.launch {
            currentMidiMappings.collectLatest { mappings ->
                if (mappings.isEmpty()) {
                    // Seed from built-in preset if available
                    val preset = MidiPresetProfiles.getAllBuiltInProfiles()[_selectedMidiProfile.value]
                        ?: MidiPresetProfiles.createDefaultProfile()
                    val mappedPreset = preset.map { it.copy(profileName = _selectedMidiProfile.value) }
                    repository.insertMidiMappings(mappedPreset)
                } else {
                    midiService.updateActiveMappings(mappings)
                }
            }
        }

        // Auto load initial tracks once DB is ready
        viewModelScope.launch {
            repository.getAllTracks().collectLatest { tracks ->
                if (tracks.isNotEmpty()) {
                    if (_decks.value[0].track == null) {
                        loadTrackToDeck(0, tracks[0])
                    }
                    if (tracks.size > 1 && _decks.value[1].track == null) {
                        loadTrackToDeck(1, tracks[1])
                    }
                    if (tracks.size > 2 && _decks.value[2].track == null) {
                        loadTrackToDeck(2, tracks[2])
                    }
                    if (tracks.size > 3 && _decks.value[3].track == null) {
                        loadTrackToDeck(3, tracks[3])
                    }
                }
            }
        }

        startPlaybackTicker()
    }

    private fun startPlaybackTicker() {
        playbackTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(33) // ~30 FPS state update ticker for platters & progress
                val currentDecks = _decks.value.toMutableList()
                var updated = false

                for (i in 0 until 4) {
                    val deck = currentDecks[i]
                    if (deck.isPlaying) {
                        val dt = 0.033f * (deck.effectiveBpm / deck.baseBpm)
                        var newPos = deck.positionSeconds + dt

                        // Check loop
                        if (deck.loopActive) {
                            val loopLengthSec = (60f / deck.effectiveBpm) * deck.loopBeats
                            val loopStart = deck.hotCues[1] ?: 0f
                            val loopEnd = loopStart + loopLengthSec
                            if (newPos >= loopEnd) {
                                newPos = loopStart
                            }
                        }

                        if (newPos > deck.durationSeconds) {
                            newPos = 0f
                        }

                        val dAngle = 360f * (deck.effectiveBpm / 60f) * 0.033f * 0.5f
                        val newAngle = (deck.platterAngleDegrees + dAngle) % 360f

                        currentDecks[i] = deck.copy(
                            positionSeconds = newPos,
                            platterAngleDegrees = newAngle
                        )
                        updated = true
                    }
                }

                if (updated) {
                    _decks.value = currentDecks
                }
            }
        }
    }

    fun setDeckCount(count: Int) {
        _deckCount.value = count.coerceIn(2, 4)
    }

    fun loadTrackToDeck(deckIndex: Int, track: Track) {
        if (track.localFileUri != null) {
            localDeckPlayers[deckIndex].loadUri(track.localFileUri)
        }

        updateDeck(deckIndex) { deck ->
            audioEngine.deckStates[deckIndex].bpm = track.bpm
            audioEngine.deckStates[deckIndex].soundPreset = track.soundPreset
            audioEngine.deckStates[deckIndex].playbackPositionSamples = 0L

            deck.copy(
                track = track,
                baseBpm = track.bpm,
                durationSeconds = track.durationSeconds.toFloat(),
                positionSeconds = 0f,
                isVideo = track.isVideo,
                hotCues = mapOf(1 to 0f, 2 to 16f * (60f / track.bpm), 3 to 32f * (60f / track.bpm), 4 to 64f * (60f / track.bpm))
            )
        }
        updateDeckPlayerVolume(deckIndex)
    }

    fun togglePlay(deckIndex: Int) {
        updateDeck(deckIndex) { deck ->
            val nextPlaying = !deck.isPlaying
            audioEngine.deckStates[deckIndex].isPlaying = nextPlaying
            if (nextPlaying) {
                localDeckPlayers[deckIndex].play()
            } else {
                localDeckPlayers[deckIndex].pause()
            }
            deck.copy(isPlaying = nextPlaying)
        }
    }

    fun pressCue(deckIndex: Int) {
        updateDeck(deckIndex) { deck ->
            if (deck.isPlaying) {
                // Return to cue position and pause
                val cuePos = deck.hotCues[1] ?: 0f
                audioEngine.deckStates[deckIndex].isPlaying = false
                audioEngine.deckStates[deckIndex].playbackPositionSamples = (cuePos * DjAudioEngine.SAMPLE_RATE).toLong()
                localDeckPlayers[deckIndex].pause()
                localDeckPlayers[deckIndex].seekTo(cuePos)
                deck.copy(isPlaying = false, positionSeconds = cuePos)
            } else {
                // Set Cue point to current position
                val newCues = deck.hotCues.toMutableMap()
                newCues[1] = deck.positionSeconds
                deck.copy(hotCues = newCues)
            }
        }
    }

    fun toggleSync(deckIndex: Int) {
        val masterDeck = _decks.value.firstOrNull { it.isMaster && it.index != deckIndex }
            ?: _decks.value.firstOrNull { it.isPlaying && it.index != deckIndex }
            ?: _decks.value.firstOrNull { it.index != deckIndex }

        val targetBpm = masterDeck?.effectiveBpm ?: 126f

        updateDeck(deckIndex) { deck ->
            val neededPitch = KeyBpmHelper.calculateSyncPitch(deck.baseBpm, targetBpm)
            val pitchRate = 1.0f + (neededPitch / 100f)
            audioEngine.deckStates[deckIndex].pitchRate = pitchRate
            localDeckPlayers[deckIndex].setPlaybackRate(pitchRate)
            deck.copy(
                isSync = !deck.isSync,
                pitchPercent = neededPitch
            )
        }
    }

    fun setPitchPercent(deckIndex: Int, pitch: Float) {
        updateDeck(deckIndex) { deck ->
            val clamped = pitch.coerceIn(-50f, 50f)
            val pitchRate = 1.0f + (clamped / 100f)
            audioEngine.deckStates[deckIndex].pitchRate = pitchRate
            localDeckPlayers[deckIndex].setPlaybackRate(pitchRate)
            deck.copy(pitchPercent = clamped)
        }
    }

    fun nudgePitch(deckIndex: Int, amount: Float) {
        updateDeck(deckIndex) { deck ->
            val newPitch = (deck.pitchPercent + amount).coerceIn(-50f, 50f)
            val pitchRate = 1.0f + (newPitch / 100f)
            audioEngine.deckStates[deckIndex].pitchRate = pitchRate
            localDeckPlayers[deckIndex].setPlaybackRate(pitchRate)
            deck.copy(pitchPercent = newPitch)
        }
    }

    fun setVolume(deckIndex: Int, volume: Float) {
        updateDeck(deckIndex) { deck ->
            val clamped = volume.coerceIn(0f, 1f)
            audioEngine.deckStates[deckIndex].volume = clamped
            deck.copy(volume = clamped)
        }
        updateDeckPlayerVolume(deckIndex)
    }

    fun setGain(deckIndex: Int, gain: Float) {
        updateDeck(deckIndex) { deck ->
            val clamped = gain.coerceIn(0f, 2f)
            audioEngine.deckStates[deckIndex].gain = clamped
            deck.copy(gain = clamped)
        }
        updateDeckPlayerVolume(deckIndex)
    }

    private fun updateDeckPlayerVolume(deckIndex: Int) {
        if (deckIndex in _decks.value.indices) {
            val deck = _decks.value[deckIndex]
            val crossGain = audioEngine.calculateCrossfaderGain(deck.crossfaderAssign, _crossfaderPosition.value, _crossfaderCurve.value)
            val finalVol = (deck.volume * deck.gain * crossGain * audioEngine.masterVolume).coerceIn(0f, 1f)
            localDeckPlayers[deckIndex].setVolume(finalVol)
        }
    }

    fun setEq(deckIndex: Int, band: String, value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        updateDeck(deckIndex) { deck ->
            when (band) {
                "LOW" -> {
                    audioEngine.deckStates[deckIndex].eqLow = clamped
                    deck.copy(eqLow = clamped)
                }
                "MID" -> {
                    audioEngine.deckStates[deckIndex].eqMid = clamped
                    deck.copy(eqMid = clamped)
                }
                "HIGH" -> {
                    audioEngine.deckStates[deckIndex].eqHigh = clamped
                    deck.copy(eqHigh = clamped)
                }
                else -> deck
            }
        }
    }

    fun toggleEqKill(deckIndex: Int, band: String) {
        updateDeck(deckIndex) { deck ->
            when (band) {
                "LOW" -> {
                    val next = !deck.eqLowKill
                    audioEngine.deckStates[deckIndex].eqLowKill = next
                    deck.copy(eqLowKill = next)
                }
                "MID" -> {
                    val next = !deck.eqMidKill
                    audioEngine.deckStates[deckIndex].eqMidKill = next
                    deck.copy(eqMidKill = next)
                }
                "HIGH" -> {
                    val next = !deck.eqHighKill
                    audioEngine.deckStates[deckIndex].eqHighKill = next
                    deck.copy(eqHighKill = next)
                }
                else -> deck
            }
        }
    }

    fun setFilter(deckIndex: Int, value: Float) {
        val clamped = value.coerceIn(-1.0f, 1.0f)
        updateDeck(deckIndex) { deck ->
            audioEngine.deckStates[deckIndex].filterValue = clamped
            deck.copy(filterValue = clamped)
        }
    }

    fun setVocalStemMode(deckIndex: Int, mode: VocalStemMode) {
        updateDeck(deckIndex) { deck ->
            audioEngine.deckStates[deckIndex].vocalStemMode = mode
            deck.copy(vocalStemMode = mode)
        }
    }

    fun toggleVocalCut(deckIndex: Int) {
        updateDeck(deckIndex) { deck ->
            val nextMode = if (deck.vocalStemMode == VocalStemMode.REMOVE_VOCALS) {
                VocalStemMode.FULL_MIX
            } else {
                VocalStemMode.REMOVE_VOCALS
            }
            audioEngine.deckStates[deckIndex].vocalStemMode = nextMode
            deck.copy(vocalStemMode = nextMode)
        }
    }

    fun toggleFx(deckIndex: Int, fxName: String) {
        updateDeck(deckIndex) { deck ->
            when (fxName) {
                "ECHO" -> {
                    val next = !deck.echoActive
                    audioEngine.deckStates[deckIndex].echoEnabled = next
                    deck.copy(echoActive = next)
                }
                "REVERB" -> {
                    val next = !deck.reverbActive
                    audioEngine.deckStates[deckIndex].reverbEnabled = next
                    deck.copy(reverbActive = next)
                }
                "FLANGER" -> {
                    val next = !deck.flangerActive
                    audioEngine.deckStates[deckIndex].flangerEnabled = next
                    deck.copy(flangerActive = next)
                }
                "BITCRUSHER" -> {
                    val next = !deck.bitcrusherActive
                    audioEngine.deckStates[deckIndex].bitcrusherEnabled = next
                    deck.copy(bitcrusherActive = next)
                }
                else -> deck
            }
        }
    }

    fun toggleLoop(deckIndex: Int, beats: Float = 4f) {
        updateDeck(deckIndex) { deck ->
            val next = !deck.loopActive
            audioEngine.deckStates[deckIndex].loopActive = next
            if (next) {
                val startSec = deck.positionSeconds
                val loopSec = (60f / deck.effectiveBpm) * beats
                audioEngine.deckStates[deckIndex].loopStartSample = (startSec * DjAudioEngine.SAMPLE_RATE).toLong()
                audioEngine.deckStates[deckIndex].loopEndSample = ((startSec + loopSec) * DjAudioEngine.SAMPLE_RATE).toLong()
            }
            deck.copy(loopActive = next, loopBeats = beats)
        }
    }

    fun triggerHotCue(deckIndex: Int, cueNumber: Int) {
        updateDeck(deckIndex) { deck ->
            val cuePos = deck.hotCues[cueNumber]
            if (cuePos != null) {
                audioEngine.deckStates[deckIndex].playbackPositionSamples = (cuePos * DjAudioEngine.SAMPLE_RATE).toLong()
                deck.copy(positionSeconds = cuePos)
            } else {
                // Set hot cue at current pos
                val newCues = deck.hotCues.toMutableMap()
                newCues[cueNumber] = deck.positionSeconds
                deck.copy(hotCues = newCues)
            }
        }
    }

    fun onPlatterScratch(deckIndex: Int, deltaDegrees: Float) {
        updateDeck(deckIndex) { deck ->
            val newAngle = (deck.platterAngleDegrees + deltaDegrees) % 360f
            val scratchSpeed = (deltaDegrees / 360f) * 15f
            audioEngine.onScratchMove(deckIndex, scratchSpeed)
            val posDelta = (deltaDegrees / 360f) * (60f / deck.effectiveBpm)
            val newPos = (deck.positionSeconds + posDelta).coerceIn(0f, deck.durationSeconds)
            deck.copy(platterAngleDegrees = newAngle, positionSeconds = newPos)
        }
    }

    fun setCrossfaderPosition(pos: Float) {
        val clamped = pos.coerceIn(-1.0f, 1.0f)
        val effectivePos = if (_isHamsterReversed.value) -clamped else clamped
        _crossfaderPosition.value = clamped
        audioEngine.crossfaderPosition = effectivePos
        for (i in 0 until 4) {
            updateDeckPlayerVolume(i)
        }
    }

    fun setCrossfaderCurve(curve: CrossfaderCurve) {
        _crossfaderCurve.value = curve
        audioEngine.crossfaderCurve = curve
    }

    fun toggleHamster() {
        _isHamsterReversed.value = !_isHamsterReversed.value
        setCrossfaderPosition(_crossfaderPosition.value)
    }

    fun autoTransition(toDeckB: Boolean) {
        autoTransitionJob?.cancel()
        autoTransitionJob = viewModelScope.launch {
            val start = _crossfaderPosition.value
            val target = if (toDeckB) 1.0f else -1.0f
            val steps = 30
            for (s in 1..steps) {
                delay(35)
                val progress = s.toFloat() / steps
                val current = start + (target - start) * progress
                setCrossfaderPosition(current)
            }
        }
    }

    fun setSkin(skin: DjSkinType) {
        _activeSkin.value = skin
    }

    fun setActiveDialog(dialog: ActiveDialog) {
        _activeDialog.value = dialog
    }

    fun setMasterVideoTransition(transition: String) {
        _masterVideoTransition.value = transition
    }

    fun selectCrate(crate: String) {
        _selectedCrate.value = crate
    }

    fun searchLibrary(query: String) {
        _librarySearchQuery.value = query
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.triggerCloudSync()
        }
    }

    fun addNewTrack(title: String, artist: String, bpm: Float, key: String, genre: String, isVideo: Boolean) {
        viewModelScope.launch {
            val newTrack = Track(
                title = title,
                artist = artist,
                durationSeconds = 210,
                bpm = bpm,
                camelotKey = key,
                musicalKey = "Am",
                genre = genre,
                isVideo = isVideo,
                videoAssetUrl = if (isVideo) "custom_visual" else "",
                soundPreset = "custom_beat",
                crate = _selectedCrate.value
            )
            repository.insertTrack(newTrack)
        }
    }

    fun importLocalAudioUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val track = LocalAudioHelper.importAudioFromUri(getApplication(), uri)
            repository.insertTrack(track)
            _selectedCrate.value = "Local Device Audio"
        }
    }

    fun importMultipleAudioUris(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            for (uri in uris) {
                val track = LocalAudioHelper.importAudioFromUri(getApplication(), uri)
                repository.insertTrack(track)
            }
            _selectedCrate.value = "Local Device Audio"
        }
    }

    fun scanDeviceMusic() {
        viewModelScope.launch {
            val discovered = LocalAudioHelper.scanDeviceMediaStore(getApplication())
            if (discovered.isNotEmpty()) {
                repository.insertTracks(discovered)
            }
            _selectedCrate.value = "Local Device Audio"
        }
    }

    fun setFocusedDeck(deckIdx: Int) {
        _focusedDeck.value = deckIdx.coerceIn(0, 3)
    }

    fun setActiveDeckTabPair(pair: Int) {
        _activeDeckTabPair.value = pair.coerceIn(0, 1)
    }

    fun toggleDeckPair() {
        _activeDeckTabPair.value = if (_activeDeckTabPair.value == 0) 1 else 0
    }

    fun cycleNextDeck() {
        val next = (_focusedDeck.value + 1) % _deckCount.value
        _focusedDeck.value = next
    }

    fun cyclePrevDeck() {
        val prev = if (_focusedDeck.value - 1 < 0) _deckCount.value - 1 else _focusedDeck.value - 1
        _focusedDeck.value = prev
    }

    fun selectMidiProfile(profileName: String) {
        _selectedMidiProfile.value = profileName
    }

    fun saveMidiMapping(mapping: MidiMapping) {
        viewModelScope.launch {
            repository.insertMidiMapping(mapping.copy(profileName = _selectedMidiProfile.value))
        }
    }

    fun deleteMidiMapping(mapping: MidiMapping) {
        viewModelScope.launch {
            repository.deleteMidiMapping(mapping)
        }
    }

    fun loadPresetProfile(presetName: String) {
        viewModelScope.launch {
            val preset = MidiPresetProfiles.getAllBuiltInProfiles()[presetName]
            if (preset != null) {
                _selectedMidiProfile.value = presetName
                repository.deleteMidiMappingsByProfile(presetName)
                val mapped = preset.map { it.copy(profileName = presetName) }
                repository.insertMidiMappings(mapped)
            }
        }
    }

    fun saveCurrentProfileAs(newProfileName: String) {
        viewModelScope.launch {
            val currentList = currentMidiMappings.value
            val duplicated = currentList.map { it.copy(id = 0, profileName = newProfileName) }
            repository.insertMidiMappings(duplicated)
            _selectedMidiProfile.value = newProfileName
        }
    }

    fun deleteMidiProfile(profileName: String) {
        if (profileName == "Default") return // Don't delete Default
        viewModelScope.launch {
            repository.deleteMidiMappingsByProfile(profileName)
            _selectedMidiProfile.value = "Default"
        }
    }

    fun resetCurrentProfileToDefaults() {
        viewModelScope.launch {
            val currentProf = _selectedMidiProfile.value
            repository.deleteMidiMappingsByProfile(currentProf)
            val template = MidiPresetProfiles.getAllBuiltInProfiles()[currentProf]
                ?: MidiPresetProfiles.createDefaultProfile()
            val mapped = template.map { it.copy(id = 0, profileName = currentProf) }
            repository.insertMidiMappings(mapped)
        }
    }

    private fun handleMidiAction(targetDeck: Int, action: String, value: Float) {
        val effectiveDeckIdx = if (targetDeck in 1..4) {
            targetDeck - 1
        } else {
            _focusedDeck.value
        }
        val dIdx = effectiveDeckIdx.coerceIn(0, 3)

        when (action) {
            // Faders & Global
            "CROSSFADER" -> setCrossfaderPosition(value)
            "MASTER_VOLUME" -> audioEngine.masterVolume = value.coerceIn(0f, 1f)
            "VOLUME" -> setVolume(dIdx, value)
            "PITCH_FADER" -> setPitchPercent(dIdx, value)

            // Transport Controls
            "PLAY_PAUSE" -> togglePlay(dIdx)
            "CUE" -> pressCue(dIdx)
            "SYNC" -> toggleSync(dIdx)
            "PITCH_NUDGE_UP" -> nudgePitch(dIdx, 0.5f)
            "PITCH_NUDGE_DOWN" -> nudgePitch(dIdx, -0.5f)
            "LOOP_TOGGLE" -> toggleLoop(dIdx)
            "HOT_CUE_1" -> triggerHotCue(dIdx, 1)
            "HOT_CUE_2" -> triggerHotCue(dIdx, 2)
            "HOT_CUE_3" -> triggerHotCue(dIdx, 3)
            "HOT_CUE_4" -> triggerHotCue(dIdx, 4)
            "SCRATCH" -> onPlatterScratch(dIdx, value * 10f)

            // EQs & Tone
            "EQ_LOW" -> setEq(dIdx, "LOW", value)
            "EQ_MID" -> setEq(dIdx, "MID", value)
            "EQ_HIGH" -> setEq(dIdx, "HIGH", value)
            "EQ_LOW_KILL" -> toggleEqKill(dIdx, "LOW")
            "EQ_MID_KILL" -> toggleEqKill(dIdx, "MID")
            "EQ_HIGH_KILL" -> toggleEqKill(dIdx, "HIGH")
            "GAIN" -> setGain(dIdx, value)

            // Effects Sends & Sound Color
            "FILTER" -> setFilter(dIdx, value)
            "FX_ECHO" -> toggleFx(dIdx, "ECHO")
            "FX_REVERB" -> toggleFx(dIdx, "REVERB")
            "FX_FLANGER" -> toggleFx(dIdx, "FLANGER")
            "FX_BITCRUSHER" -> toggleFx(dIdx, "BITCRUSHER")

            // Deck Selection
            "DECK_FOCUS" -> {
                val chosenDeck = if (targetDeck in 1..4) targetDeck - 1 else (value.toInt() - 1).coerceIn(0, 3)
                setFocusedDeck(chosenDeck)
                // If deck is in bank 2 (C or D), switch tab view
                if (chosenDeck >= 2) {
                    _activeDeckTabPair.value = 1
                } else {
                    _activeDeckTabPair.value = 0
                }
            }
            "DECK_PAIR_TOGGLE" -> toggleDeckPair()
            "DECK_NEXT" -> cycleNextDeck()
            "DECK_PREV" -> cyclePrevDeck()
        }
    }

    private inline fun updateDeck(deckIndex: Int, transform: (DeckUiState) -> DeckUiState) {
        val current = _decks.value.toMutableList()
        if (deckIndex in current.indices) {
            current[deckIndex] = transform(current[deckIndex])
            _decks.value = current
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        midiService.close()
        for (i in 0 until 4) {
            localDeckPlayers[i].release()
        }
    }
}
