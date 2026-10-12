package com.example.engine

import com.example.data.model.MidiMapping

data class SoftwareActionDefinition(
    val actionType: String,
    val category: String, // "TRANSPORT", "EQ", "FADER", "EFFECTS_SEND", "DECK_SELECT"
    val displayName: String,
    val description: String,
    val defaultHardwareType: String, // "KNOB", "SLIDER", "BUTTON", "JOG_WHEEL"
    val defaultTargetDeck: Int, // 0 = Master, 1-4 = Decks
    val isDeckSpecific: Boolean = true
)

object MidiPresetProfiles {

    val ALL_ACTION_DEFINITIONS: List<SoftwareActionDefinition> = listOf(
        // Transport Controls
        SoftwareActionDefinition("PLAY_PAUSE", "TRANSPORT", "Play / Pause", "Toggle track playback", "BUTTON", 1),
        SoftwareActionDefinition("CUE", "TRANSPORT", "Cue", "Return to cue point or pause/set cue", "BUTTON", 1),
        SoftwareActionDefinition("SYNC", "TRANSPORT", "Beat Sync", "Match BPM and beatgrid with master deck", "BUTTON", 1),
        SoftwareActionDefinition("PITCH_NUDGE_UP", "TRANSPORT", "Pitch Bend +", "Momentarily speed up deck for beatmatching", "BUTTON", 1),
        SoftwareActionDefinition("PITCH_NUDGE_DOWN", "TRANSPORT", "Pitch Bend -", "Momentarily slow down deck for beatmatching", "BUTTON", 1),
        SoftwareActionDefinition("LOOP_TOGGLE", "TRANSPORT", "Auto Loop", "Engage or disengage beat loop (4 beats)", "BUTTON", 1),
        SoftwareActionDefinition("HOT_CUE_1", "TRANSPORT", "Hot Cue 1", "Jump to cue point 1 or set if empty", "BUTTON", 1),
        SoftwareActionDefinition("HOT_CUE_2", "TRANSPORT", "Hot Cue 2", "Jump to cue point 2 or set if empty", "BUTTON", 1),
        SoftwareActionDefinition("HOT_CUE_3", "TRANSPORT", "Hot Cue 3", "Jump to cue point 3 or set if empty", "BUTTON", 1),
        SoftwareActionDefinition("HOT_CUE_4", "TRANSPORT", "Hot Cue 4", "Jump to cue point 4 or set if empty", "BUTTON", 1),
        SoftwareActionDefinition("SCRATCH", "TRANSPORT", "Jog Wheel / Scratch", "Platter rotation and vinyl scratch emulation", "JOG_WHEEL", 1),

        // EQs & Tone Controls
        SoftwareActionDefinition("EQ_LOW", "EQ", "Low EQ (Bass)", "Cut or boost low frequencies (0 - 2.0x)", "KNOB", 1),
        SoftwareActionDefinition("EQ_MID", "EQ", "Mid EQ", "Cut or boost middle frequencies (0 - 2.0x)", "KNOB", 1),
        SoftwareActionDefinition("EQ_HIGH", "EQ", "High EQ (Treble)", "Cut or boost high frequencies (0 - 2.0x)", "KNOB", 1),
        SoftwareActionDefinition("EQ_LOW_KILL", "EQ", "Low EQ Kill", "Instant mute of bass frequencies", "BUTTON", 1),
        SoftwareActionDefinition("EQ_MID_KILL", "EQ", "Mid EQ Kill", "Instant mute of middle frequencies", "BUTTON", 1),
        SoftwareActionDefinition("EQ_HIGH_KILL", "EQ", "High EQ Kill", "Instant mute of treble frequencies", "BUTTON", 1),
        SoftwareActionDefinition("GAIN", "EQ", "Trim / Gain", "Pre-fader channel input level", "KNOB", 1),

        // Faders & Mixer Controls
        SoftwareActionDefinition("VOLUME", "FADER", "Channel Volume Fader", "Deck output level slider (0.0 to 1.0)", "SLIDER", 1),
        SoftwareActionDefinition("CROSSFADER", "FADER", "Master Crossfader", "Blend between Deck A/C and Deck B/D", "SLIDER", 0, isDeckSpecific = false),
        SoftwareActionDefinition("PITCH_FADER", "FADER", "Tempo / Pitch Slider", "Adjust deck playback speed (-50% to +50%)", "SLIDER", 1),
        SoftwareActionDefinition("MASTER_VOLUME", "FADER", "Master Output Volume", "Main console output level", "SLIDER", 0, isDeckSpecific = false),

        // Effects Sends & Sound Color
        SoftwareActionDefinition("FILTER", "EFFECTS_SEND", "Color Filter (LPF/HPF)", "Bipolar filter sweep (-1.0 Low Pass to +1.0 High Pass)", "KNOB", 1),
        SoftwareActionDefinition("FX_ECHO", "EFFECTS_SEND", "Echo FX Send", "Toggle or adjust wet/dry echo delay effect", "BUTTON", 1),
        SoftwareActionDefinition("FX_REVERB", "EFFECTS_SEND", "Reverb FX Send", "Toggle or adjust wet/dry hall reverb effect", "BUTTON", 1),
        SoftwareActionDefinition("FX_FLANGER", "EFFECTS_SEND", "Flanger FX Send", "Toggle jet sweep flanger modulation", "BUTTON", 1),
        SoftwareActionDefinition("FX_BITCRUSHER", "EFFECTS_SEND", "Bitcrusher FX Send", "Toggle lo-fi digital distortion", "BUTTON", 1),

        // Deck Selection & Routing
        SoftwareActionDefinition("DECK_FOCUS", "DECK_SELECT", "Deck Select / Focus", "Select active deck for focused controls", "BUTTON", 1),
        SoftwareActionDefinition("DECK_PAIR_TOGGLE", "DECK_SELECT", "Decks A/B vs C/D Toggle", "Toggle 4-deck display between pair 1-2 and 3-4", "BUTTON", 0, isDeckSpecific = false),
        SoftwareActionDefinition("DECK_NEXT", "DECK_SELECT", "Next Deck", "Cycle focus to the next deck", "BUTTON", 0, isDeckSpecific = false),
        SoftwareActionDefinition("DECK_PREV", "DECK_SELECT", "Previous Deck", "Cycle focus to previous deck", "BUTTON", 0, isDeckSpecific = false)
    )

    fun getActionDefinition(actionType: String): SoftwareActionDefinition? {
        return ALL_ACTION_DEFINITIONS.firstOrNull { it.actionType == actionType }
    }

    /**
     * Default Virtual DJ Controller Profile
     */
    fun createDefaultProfile(): List<MidiMapping> {
        val p = "Default"
        return listOf(
            // Crossfader
            MidiMapping(controlName = "Crossfader", targetDeck = 0, controlType = "CC", channel = 1, controlNumber = 8, description = "Master Crossfader Slider", actionCategory = "FADER", actionType = "CROSSFADER", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Master Volume", targetDeck = 0, controlType = "CC", channel = 1, controlNumber = 7, description = "Master Output Level", actionCategory = "FADER", actionType = "MASTER_VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Toggle Decks A/B vs C/D", targetDeck = 0, controlType = "NOTE", channel = 1, controlNumber = 80, description = "Switch 4-Deck Bank", actionCategory = "DECK_SELECT", actionType = "DECK_PAIR_TOGGLE", hardwareControlType = "BUTTON", profileName = p),

            // Deck 1 (A)
            MidiMapping(controlName = "Deck 1 Play", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 60, description = "Deck 1 Play/Pause", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Cue", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 61, description = "Deck 1 Cue Return", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Sync", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 62, description = "Deck 1 Beat Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Volume Fader", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 24, description = "Deck 1 Channel Fader", actionCategory = "FADER", actionType = "VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 1 Pitch Fader", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 26, description = "Deck 1 Pitch Slider", actionCategory = "FADER", actionType = "PITCH_FADER", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 1 High EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 18, description = "Deck 1 Treble EQ", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Mid EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 17, description = "Deck 1 Mid EQ", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Low EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 16, description = "Deck 1 Bass EQ", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Gain", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 14, description = "Deck 1 Trim Gain", actionCategory = "EQ", actionType = "GAIN", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Filter", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 19, description = "Deck 1 Sound Color Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Echo FX", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 70, description = "Deck 1 Echo Toggle", actionCategory = "EFFECTS_SEND", actionType = "FX_ECHO", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Reverb FX", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 71, description = "Deck 1 Reverb Toggle", actionCategory = "EFFECTS_SEND", actionType = "FX_REVERB", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Hot Cue 1", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 36, description = "Deck 1 Pad 1", actionCategory = "TRANSPORT", actionType = "HOT_CUE_1", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Hot Cue 2", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 37, description = "Deck 1 Pad 2", actionCategory = "TRANSPORT", actionType = "HOT_CUE_2", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Hot Cue 3", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 38, description = "Deck 1 Pad 3", actionCategory = "TRANSPORT", actionType = "HOT_CUE_3", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Hot Cue 4", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 39, description = "Deck 1 Pad 4", actionCategory = "TRANSPORT", actionType = "HOT_CUE_4", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Select", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 50, description = "Focus Deck 1", actionCategory = "DECK_SELECT", actionType = "DECK_FOCUS", hardwareControlType = "BUTTON", profileName = p),

            // Deck 2 (B)
            MidiMapping(controlName = "Deck 2 Play", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 64, description = "Deck 2 Play/Pause", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Cue", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 65, description = "Deck 2 Cue Return", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Sync", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 66, description = "Deck 2 Beat Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Volume Fader", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 25, description = "Deck 2 Channel Fader", actionCategory = "FADER", actionType = "VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 2 Pitch Fader", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 27, description = "Deck 2 Pitch Slider", actionCategory = "FADER", actionType = "PITCH_FADER", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 2 High EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 22, description = "Deck 2 Treble EQ", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Mid EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 21, description = "Deck 2 Mid EQ", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Low EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 20, description = "Deck 2 Bass EQ", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Gain", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 15, description = "Deck 2 Trim Gain", actionCategory = "EQ", actionType = "GAIN", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Filter", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 23, description = "Deck 2 Sound Color Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Echo FX", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 74, description = "Deck 2 Echo Toggle", actionCategory = "EFFECTS_SEND", actionType = "FX_ECHO", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Reverb FX", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 75, description = "Deck 2 Reverb Toggle", actionCategory = "EFFECTS_SEND", actionType = "FX_REVERB", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Hot Cue 1", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 40, description = "Deck 2 Pad 1", actionCategory = "TRANSPORT", actionType = "HOT_CUE_1", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Hot Cue 2", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 41, description = "Deck 2 Pad 2", actionCategory = "TRANSPORT", actionType = "HOT_CUE_2", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Hot Cue 3", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 42, description = "Deck 2 Pad 3", actionCategory = "TRANSPORT", actionType = "HOT_CUE_3", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Hot Cue 4", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 43, description = "Deck 2 Pad 4", actionCategory = "TRANSPORT", actionType = "HOT_CUE_4", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Select", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 51, description = "Focus Deck 2", actionCategory = "DECK_SELECT", actionType = "DECK_FOCUS", hardwareControlType = "BUTTON", profileName = p),

            // Deck 3 & 4 Deck Selection
            MidiMapping(controlName = "Deck 3 Select", targetDeck = 3, controlType = "NOTE", channel = 1, controlNumber = 52, description = "Focus Deck 3 (C)", actionCategory = "DECK_SELECT", actionType = "DECK_FOCUS", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 4 Select", targetDeck = 4, controlType = "NOTE", channel = 1, controlNumber = 53, description = "Focus Deck 4 (D)", actionCategory = "DECK_SELECT", actionType = "DECK_FOCUS", hardwareControlType = "BUTTON", profileName = p)
        )
    }

    /**
     * Pioneer DDJ-400 / DDJ-FLX4 Preset
     */
    fun createPioneerPreset(): List<MidiMapping> {
        val p = "Pioneer DDJ-400 / FLX4"
        return listOf(
            MidiMapping(controlName = "Crossfader", targetDeck = 0, controlType = "CC", channel = 1, controlNumber = 31, description = "Magvel Crossfader", actionCategory = "FADER", actionType = "CROSSFADER", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 1 Play", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 11, description = "Deck 1 Play", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Cue", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 12, description = "Deck 1 Cue", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Sync", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 88, description = "Deck 1 Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Volume", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 19, description = "Ch 1 Fader", actionCategory = "FADER", actionType = "VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 1 High EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 7, description = "Ch 1 High EQ", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Mid EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 11, description = "Ch 1 Mid EQ", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Low EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 15, description = "Ch 1 Low EQ", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Filter", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 23, description = "Ch 1 Color Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Beat FX Echo", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 71, description = "Beat FX On/Off", actionCategory = "EFFECTS_SEND", actionType = "FX_ECHO", hardwareControlType = "BUTTON", profileName = p),

            MidiMapping(controlName = "Deck 2 Play", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 13, description = "Deck 2 Play", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Cue", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 14, description = "Deck 2 Cue", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Sync", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 89, description = "Deck 2 Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Volume", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 20, description = "Ch 2 Fader", actionCategory = "FADER", actionType = "VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 2 High EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 8, description = "Ch 2 High EQ", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Mid EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 12, description = "Ch 2 Mid EQ", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Low EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 16, description = "Ch 2 Low EQ", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Filter", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 24, description = "Ch 2 Color Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Beat FX Reverb", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 72, description = "Beat FX Reverb", actionCategory = "EFFECTS_SEND", actionType = "FX_REVERB", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Focus", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 101, description = "Select Deck 1", actionCategory = "DECK_SELECT", actionType = "DECK_FOCUS", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Focus", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 102, description = "Select Deck 2", actionCategory = "DECK_SELECT", actionType = "DECK_FOCUS", hardwareControlType = "BUTTON", profileName = p)
        )
    }

    /**
     * Traktor Kontrol S2 / S4 Preset
     */
    fun createTraktorPreset(): List<MidiMapping> {
        val p = "Traktor Kontrol S2 / S4"
        return listOf(
            MidiMapping(controlName = "Crossfader", targetDeck = 0, controlType = "CC", channel = 2, controlNumber = 1, description = "Smooth Crossfader", actionCategory = "FADER", actionType = "CROSSFADER", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 1 Play", targetDeck = 1, controlType = "NOTE", channel = 2, controlNumber = 48, description = "Deck A Play", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Cue", targetDeck = 1, controlType = "NOTE", channel = 2, controlNumber = 49, description = "Deck A Cue", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Sync", targetDeck = 1, controlType = "NOTE", channel = 2, controlNumber = 50, description = "Deck A Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Volume", targetDeck = 1, controlType = "CC", channel = 2, controlNumber = 2, description = "Deck A Volume", actionCategory = "FADER", actionType = "VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 1 High EQ", targetDeck = 2, controlType = "CC", channel = 2, controlNumber = 4, description = "Deck A High", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Mid EQ", targetDeck = 1, controlType = "CC", channel = 2, controlNumber = 5, description = "Deck A Mid", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Low EQ", targetDeck = 1, controlType = "CC", channel = 2, controlNumber = 6, description = "Deck A Low", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Filter", targetDeck = 1, controlType = "CC", channel = 2, controlNumber = 7, description = "Deck A Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 FX Unit 1", targetDeck = 1, controlType = "NOTE", channel = 2, controlNumber = 55, description = "Deck A FX Send", actionCategory = "EFFECTS_SEND", actionType = "FX_ECHO", hardwareControlType = "BUTTON", profileName = p),

            MidiMapping(controlName = "Deck 2 Play", targetDeck = 2, controlType = "NOTE", channel = 2, controlNumber = 52, description = "Deck B Play", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Cue", targetDeck = 2, controlType = "NOTE", channel = 2, controlNumber = 53, description = "Deck B Cue", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Sync", targetDeck = 2, controlType = "NOTE", channel = 2, controlNumber = 54, description = "Deck B Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Volume", targetDeck = 2, controlType = "CC", channel = 2, controlNumber = 3, description = "Deck B Volume", actionCategory = "FADER", actionType = "VOLUME", hardwareControlType = "SLIDER", profileName = p),
            MidiMapping(controlName = "Deck 2 High EQ", targetDeck = 2, controlType = "CC", channel = 2, controlNumber = 8, description = "Deck B High", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Mid EQ", targetDeck = 2, controlType = "CC", channel = 2, controlNumber = 9, description = "Deck B Mid", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Low EQ", targetDeck = 2, controlType = "CC", channel = 2, controlNumber = 10, description = "Deck B Low", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Filter", targetDeck = 2, controlType = "CC", channel = 2, controlNumber = 11, description = "Deck B Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 FX Unit 2", targetDeck = 2, controlType = "NOTE", channel = 2, controlNumber = 56, description = "Deck B FX Send", actionCategory = "EFFECTS_SEND", actionType = "FX_REVERB", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck C/D Toggle", targetDeck = 0, controlType = "NOTE", channel = 2, controlNumber = 60, description = "Deck Flavor Switch", actionCategory = "DECK_SELECT", actionType = "DECK_PAIR_TOGGLE", hardwareControlType = "BUTTON", profileName = p)
        )
    }

    /**
     * Akai MPK Mini & APC Grid Preset
     */
    fun createAkaiPreset(): List<MidiMapping> {
        val p = "Akai MPK Mini / APC"
        return listOf(
            MidiMapping(controlName = "Deck 1 Low EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 1, description = "K1: Low EQ", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Mid EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 2, description = "K2: Mid EQ", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 High EQ", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 3, description = "K3: High EQ", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 1 Filter", targetDeck = 1, controlType = "CC", channel = 1, controlNumber = 4, description = "K4: Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Low EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 5, description = "K5: Low EQ", actionCategory = "EQ", actionType = "EQ_LOW", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Mid EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 6, description = "K6: Mid EQ", actionCategory = "EQ", actionType = "EQ_MID", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 High EQ", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 7, description = "K7: High EQ", actionCategory = "EQ", actionType = "EQ_HIGH", hardwareControlType = "KNOB", profileName = p),
            MidiMapping(controlName = "Deck 2 Filter", targetDeck = 2, controlType = "CC", channel = 1, controlNumber = 8, description = "K8: Filter", actionCategory = "EFFECTS_SEND", actionType = "FILTER", hardwareControlType = "KNOB", profileName = p),

            MidiMapping(controlName = "Deck 1 Play", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 48, description = "Pad 1: D1 Play", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Cue", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 49, description = "Pad 2: D1 Cue", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Sync", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 50, description = "Pad 3: D1 Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 1 Echo", targetDeck = 1, controlType = "NOTE", channel = 1, controlNumber = 51, description = "Pad 4: D1 Echo", actionCategory = "EFFECTS_SEND", actionType = "FX_ECHO", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Play", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 52, description = "Pad 5: D2 Play", actionCategory = "TRANSPORT", actionType = "PLAY_PAUSE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Cue", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 53, description = "Pad 2: D2 Cue", actionCategory = "TRANSPORT", actionType = "CUE", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Sync", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 54, description = "Pad 7: D2 Sync", actionCategory = "TRANSPORT", actionType = "SYNC", hardwareControlType = "BUTTON", profileName = p),
            MidiMapping(controlName = "Deck 2 Reverb", targetDeck = 2, controlType = "NOTE", channel = 1, controlNumber = 55, description = "Pad 8: D2 Reverb", actionCategory = "EFFECTS_SEND", actionType = "FX_REVERB", hardwareControlType = "BUTTON", profileName = p)
        )
    }

    fun getAllBuiltInProfiles(): Map<String, List<MidiMapping>> {
        return mapOf(
            "Default" to createDefaultProfile(),
            "Pioneer DDJ-400 / FLX4" to createPioneerPreset(),
            "Traktor Kontrol S2 / S4" to createTraktorPreset(),
            "Akai MPK Mini / APC" to createAkaiPreset()
        )
    }
}
