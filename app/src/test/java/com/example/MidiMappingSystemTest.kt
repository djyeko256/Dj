package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MidiMapping
import com.example.engine.DjMidiService
import com.example.engine.MidiPresetProfiles
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MidiMappingSystemTest {

    private lateinit var context: Context
    private lateinit var midiService: DjMidiService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        midiService = DjMidiService(context)
        midiService.initialize()
    }

    @Test
    fun `default presets contain transport eq fader effects and deck selection`() {
        val defaultProfile = MidiPresetProfiles.createDefaultProfile()
        assertTrue("Profile should contain mappings", defaultProfile.isNotEmpty())

        val categories = defaultProfile.map { it.actionCategory }.toSet()
        assertTrue("Must include TRANSPORT category", categories.contains("TRANSPORT"))
        assertTrue("Must include EQ category", categories.contains("EQ"))
        assertTrue("Must include FADER category", categories.contains("FADER"))
        assertTrue("Must include EFFECTS_SEND category", categories.contains("EFFECTS_SEND"))
        assertTrue("Must include DECK_SELECT category", categories.contains("DECK_SELECT"))

        val hwTypes = defaultProfile.map { it.hardwareControlType }.toSet()
        assertTrue("Must support KNOB hardware controls", hwTypes.contains("KNOB"))
        assertTrue("Must support SLIDER hardware controls", hwTypes.contains("SLIDER"))
        assertTrue("Must support BUTTON hardware controls", hwTypes.contains("BUTTON"))
    }

    @Test
    fun `midi learn captures hardware signal and creates mapping`() {
        var learnedMapping: MidiMapping? = null
        midiService.onMidiLearned = { mapping ->
            learnedMapping = mapping
        }

        // Start learn session for High EQ on Deck 1
        midiService.startMidiLearn(
            actionType = "EQ_HIGH",
            targetDeck = 1,
            controlName = "Deck 1 High EQ",
            actionCategory = "EQ",
            hardwareControlType = "KNOB",
            profileName = "Default"
        )

        assertTrue(midiService.isMidiLearnActive)

        // Simulate incoming hardware knob movement (CC 45 on Channel 1)
        midiService.sendVirtualMidiMessage("CC", 1, 45, 96)

        assertFalse("MIDI Learn should conclude after capturing", midiService.isMidiLearnActive)
        assertNotNull("Mapping should be created", learnedMapping)
        assertEquals("EQ_HIGH", learnedMapping?.actionType)
        assertEquals(1, learnedMapping?.targetDeck)
        assertEquals(1, learnedMapping?.channel)
        assertEquals(45, learnedMapping?.controlNumber)
        assertEquals("CC", learnedMapping?.controlType)
    }

    @Test
    fun `mapped midi events trigger corresponding actions`() {
        val mappings = listOf(
            MidiMapping(
                controlName = "Deck 1 Play",
                targetDeck = 1,
                controlType = "NOTE",
                channel = 1,
                controlNumber = 60,
                actionCategory = "TRANSPORT",
                actionType = "PLAY_PAUSE",
                hardwareControlType = "BUTTON"
            ),
            MidiMapping(
                controlName = "Crossfader",
                targetDeck = 0,
                controlType = "CC",
                channel = 1,
                controlNumber = 8,
                actionCategory = "FADER",
                actionType = "CROSSFADER",
                hardwareControlType = "SLIDER"
            )
        )

        midiService.updateActiveMappings(mappings)

        var triggeredDeck = -1
        var triggeredAction = ""
        var triggeredValue = 0f

        midiService.onMidiActionTriggered = { deck, action, value ->
            triggeredDeck = deck
            triggeredAction = action
            triggeredValue = value
        }

        // Press Deck 1 Play Note 60
        midiService.sendVirtualMidiMessage("NOTE_ON", 1, 60, 127)
        assertEquals(1, triggeredDeck)
        assertEquals("PLAY_PAUSE", triggeredAction)
        assertEquals(1.0f, triggeredValue, 0.001f)

        // Move Crossfader CC 8 to center (64)
        midiService.sendVirtualMidiMessage("CC", 1, 8, 64)
        assertEquals(0, triggeredDeck)
        assertEquals("CROSSFADER", triggeredAction)
        // Center crossfader: (64 / 127) * 2 - 1 is approx 0.0
        assertTrue("Crossfader value should be close to center", kotlin.math.abs(triggeredValue) < 0.05f)
    }

    @Test
    fun `local audio track creates valid track entity with offline flags`() {
        val track = com.example.data.model.Track(
            title = "My Local Beat",
            artist = "Local Artist",
            durationSeconds = 180,
            bpm = 126f,
            camelotKey = "8A",
            musicalKey = "Am",
            genre = "Local Storage",
            crate = "Local Device Audio",
            localFileUri = "content://media/external/audio/media/123",
            isLocalFile = true
        )

        assertTrue(track.isLocalFile)
        assertNotNull(track.localFileUri)
        assertEquals("Local Device Audio", track.crate)
    }
}
