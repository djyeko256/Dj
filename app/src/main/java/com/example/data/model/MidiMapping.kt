package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "midi_mappings")
data class MidiMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val controlName: String, // e.g. "Deck 1 Low EQ", "Deck 1 Play", "Crossfader"
    val targetDeck: Int = 0, // 0 for Master/Global, 1-4 for Decks
    val controlType: String = "CC", // "CC", "NOTE", "PITCH"
    val channel: Int = 1, // 1-16 (0 = Any/Omni)
    val controlNumber: Int = 0, // CC number or Note number (0-127)
    val description: String = "",
    val actionCategory: String = "TRANSPORT", // "TRANSPORT", "EQ", "FADER", "EFFECTS_SEND", "DECK_SELECT"
    val actionType: String = "PLAY_PAUSE", // PLAY_PAUSE, CUE, SYNC, EQ_LOW, EQ_MID, EQ_HIGH, EQ_LOW_KILL, EQ_MID_KILL, EQ_HIGH_KILL, GAIN, VOLUME, CROSSFADER, PITCH_FADER, PITCH_NUDGE_UP, PITCH_NUDGE_DOWN, FILTER, FX_ECHO, FX_REVERB, FX_FLANGER, FX_BITCRUSHER, LOOP_TOGGLE, HOT_CUE_1, HOT_CUE_2, HOT_CUE_3, HOT_CUE_4, DECK_FOCUS, DECK_PAIR_TOGGLE, SCRATCH
    val hardwareControlType: String = "BUTTON", // "KNOB", "SLIDER", "BUTTON", "JOG_WHEEL"
    val isInverted: Boolean = false,
    val behavior: String = "ABSOLUTE", // "ABSOLUTE", "TOGGLE", "MOMENTARY", "RELATIVE"
    val profileName: String = "Default"
)
