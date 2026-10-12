package com.example.engine

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.MidiMapping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MidiEvent(
    val type: String, // "CC", "NOTE_ON", "NOTE_OFF", "PITCH"
    val channel: Int,
    val controlOrNote: Int,
    val value: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class MidiDeviceItem(
    val id: Int,
    val name: String,
    val manufacturer: String,
    val isConnected: Boolean = false
)

data class TriggeredMidiAction(
    val mapping: MidiMapping,
    val event: MidiEvent,
    val normalizedValue: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class MidiLearnSession(
    val actionType: String,
    val targetDeck: Int,
    val controlName: String,
    val actionCategory: String,
    val hardwareControlType: String,
    val profileName: String
)

class DjMidiService(private val context: Context) {

    companion object {
        private const val TAG = "DjMidiService"
    }

    private var midiManager: MidiManager? = null
    private var openDevice: MidiDevice? = null
    private var openPort: MidiOutputPort? = null

    private val _connectedDevices = MutableStateFlow<List<MidiDeviceItem>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()

    private val _lastMidiEvent = MutableStateFlow<MidiEvent?>(null)
    val lastMidiEvent = _lastMidiEvent.asStateFlow()

    private val _isMidiSupported = MutableStateFlow(false)
    val isMidiSupported = _isMidiSupported.asStateFlow()

    private val _activeMappings = MutableStateFlow<List<MidiMapping>>(emptyList())
    val activeMappings = _activeMappings.asStateFlow()

    private val _lastTriggeredAction = MutableStateFlow<TriggeredMidiAction?>(null)
    val lastTriggeredAction = _lastTriggeredAction.asStateFlow()

    private val _midiActivityCount = MutableStateFlow(0)
    val midiActivityCount = _midiActivityCount.asStateFlow()

    private val _learnSession = MutableStateFlow<MidiLearnSession?>(null)
    val learnSession = _learnSession.asStateFlow()

    val isMidiLearnActive: Boolean
        get() = _learnSession.value != null

    // Callback when mapped DJ action is triggered from MIDI
    var onMidiActionTriggered: ((targetDeck: Int, action: String, normalizedValue: Float) -> Unit)? = null

    // Callback when MIDI Learn successfully captures an event
    var onMidiLearned: ((MidiMapping) -> Unit)? = null

    fun initialize() {
        val hasMidiFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)
        _isMidiSupported.value = hasMidiFeature

        if (!hasMidiFeature) {
            Log.i(TAG, "Android MIDI feature not available or simulated.")
            return
        }

        try {
            midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
            scanDevices()

            midiManager?.registerDeviceCallback(object : MidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo?) {
                    scanDevices()
                }

                override fun onDeviceRemoved(device: MidiDeviceInfo?) {
                    scanDevices()
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MidiManager", e)
        }
    }

    fun scanDevices() {
        val manager = midiManager ?: return
        try {
            val infos = manager.devices
            val list = infos.map { info ->
                val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "MIDI Device #${info.id}"
                val manufacturer = info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: "Generic Hardware"
                MidiDeviceItem(
                    id = info.id,
                    name = name,
                    manufacturer = manufacturer,
                    isConnected = openDevice?.info?.id == info.id
                )
            }
            _connectedDevices.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MIDI devices", e)
        }
    }

    fun connectDevice(deviceInfoItem: MidiDeviceItem) {
        val manager = midiManager ?: return
        val info = manager.devices.firstOrNull { it.id == deviceInfoItem.id } ?: return

        manager.openDevice(info, { device ->
            if (device != null) {
                openDevice = device
                try {
                    openPort = device.openOutputPort(0)
                    openPort?.connect(object : MidiReceiver() {
                        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
                            if (msg != null && count >= 3) {
                                processMidiBytes(msg, offset, count)
                            }
                        }
                    })
                    _connectedDevices.value = _connectedDevices.value.map {
                        if (it.id == deviceInfoItem.id) it.copy(isConnected = true) else it.copy(isConnected = false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect MIDI output port", e)
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun disconnectCurrentDevice() {
        try {
            openPort?.close()
            openPort = null
            openDevice?.close()
            openDevice = null
            _connectedDevices.value = _connectedDevices.value.map { it.copy(isConnected = false) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect MIDI device", e)
        }
    }

    fun updateActiveMappings(mappings: List<MidiMapping>) {
        _activeMappings.value = mappings
    }

    fun startMidiLearn(
        actionType: String,
        targetDeck: Int,
        controlName: String,
        actionCategory: String,
        hardwareControlType: String,
        profileName: String
    ) {
        _learnSession.value = MidiLearnSession(
            actionType = actionType,
            targetDeck = targetDeck,
            controlName = controlName,
            actionCategory = actionCategory,
            hardwareControlType = hardwareControlType,
            profileName = profileName
        )
    }

    fun cancelMidiLearn() {
        _learnSession.value = null
    }

    private fun processMidiBytes(bytes: ByteArray, offset: Int, count: Int) {
        val status = bytes[offset].toInt() and 0xFF
        val typeNibble = (status and 0xF0) ushr 4
        val channel = (status and 0x0F) + 1
        val data1 = bytes[offset + 1].toInt() and 0x7F
        val data2 = if (count > 2) bytes[offset + 2].toInt() and 0x7F else 0

        val event = when (typeNibble) {
            0x8 -> MidiEvent("NOTE_OFF", channel, data1, data2)
            0x9 -> MidiEvent(if (data2 > 0) "NOTE_ON" else "NOTE_OFF", channel, data1, data2)
            0xB -> MidiEvent("CC", channel, data1, data2)
            0xE -> MidiEvent("PITCH", channel, data1, data2)
            else -> null
        }

        if (event != null) {
            handleMidiEvent(event)
        }
    }

    fun handleMidiEvent(event: MidiEvent) {
        _lastMidiEvent.value = event
        _midiActivityCount.value = (_midiActivityCount.value + 1) % 10000

        // 1. If MIDI Learn mode is active, bind this control
        val currentLearn = _learnSession.value
        if (currentLearn != null) {
            // Determine mapped type
            val type = when (event.type) {
                "NOTE_ON", "NOTE_OFF" -> "NOTE"
                "CC" -> "CC"
                "PITCH" -> "PITCH"
                else -> event.type
            }

            // Determine hardware type if auto-detecting
            val detectedHw = if (type == "NOTE") {
                "BUTTON"
            } else {
                if (currentLearn.hardwareControlType == "BUTTON") "KNOB" else currentLearn.hardwareControlType
            }

            val newMapping = MidiMapping(
                controlName = currentLearn.controlName,
                targetDeck = currentLearn.targetDeck,
                controlType = type,
                channel = event.channel,
                controlNumber = event.controlOrNote,
                description = "Hardware ${type} #${event.controlOrNote} on Ch ${event.channel}",
                actionCategory = currentLearn.actionCategory,
                actionType = currentLearn.actionType,
                hardwareControlType = detectedHw,
                profileName = currentLearn.profileName
            )

            _learnSession.value = null
            onMidiLearned?.invoke(newMapping)
            return
        }

        // 2. Dynamic mapping lookup
        val mappings = _activeMappings.value
        val eventTypeStr = if (event.type == "NOTE_ON" || event.type == "NOTE_OFF") "NOTE" else event.type

        val matched = mappings.filter { mapping ->
            val typeMatches = mapping.controlType.equals(eventTypeStr, ignoreCase = true)
            val channelMatches = mapping.channel == 0 || mapping.channel == event.channel
            val numMatches = mapping.controlNumber == event.controlOrNote
            typeMatches && channelMatches && numMatches
        }

        if (matched.isNotEmpty()) {
            for (m in matched) {
                val normalizedValue = calculateNormalizedValue(m, event)
                // Filter out NOTE_OFF for momentary button triggers if desired
                if (eventTypeStr == "NOTE" && event.type == "NOTE_OFF" && m.hardwareControlType == "BUTTON") {
                    // For buttons, note off can be ignored or sent as 0
                    continue
                }

                _lastTriggeredAction.value = TriggeredMidiAction(
                    mapping = m,
                    event = event,
                    normalizedValue = normalizedValue
                )

                onMidiActionTriggered?.invoke(m.targetDeck, m.actionType, normalizedValue)
            }
            return
        }

        // 3. Fallback default routing if no custom mapping is in place
        executeFallbackMapping(event)
    }

    private fun calculateNormalizedValue(mapping: MidiMapping, event: MidiEvent): Float {
        var raw = event.value
        if (mapping.isInverted && mapping.controlType == "CC") {
            raw = 127 - raw
        }

        return when (mapping.actionType) {
            "CROSSFADER", "FILTER" -> {
                val norm = raw / 127.0f
                norm * 2.0f - 1.0f // -1.0 to +1.0
            }
            "EQ_LOW", "EQ_MID", "EQ_HIGH", "GAIN" -> {
                val norm = raw / 127.0f
                norm * 2.0f // 0.0 to 2.0
            }
            "PITCH_FADER" -> {
                val norm = raw / 127.0f
                (norm * 2.0f - 1.0f) * 50.0f // -50% to +50%
            }
            "VOLUME", "MASTER_VOLUME", "FX_ECHO", "FX_REVERB", "FX_FLANGER", "FX_BITCRUSHER" -> {
                raw / 127.0f
            }
            "SCRATCH" -> {
                // Jog wheel delta
                val centerOffset = raw - 64
                centerOffset * 0.5f
            }
            else -> {
                // Buttons / Toggles
                if (event.value > 0) 1.0f else 0.0f
            }
        }
    }

    private fun executeFallbackMapping(event: MidiEvent) {
        when (event.type) {
            "CC" -> {
                val normalized = event.value / 127.0f
                when (event.controlOrNote) {
                    8 -> onMidiActionTriggered?.invoke(0, "CROSSFADER", normalized * 2.0f - 1.0f)
                    16 -> onMidiActionTriggered?.invoke(1, "EQ_LOW", normalized * 2.0f)
                    17 -> onMidiActionTriggered?.invoke(1, "EQ_MID", normalized * 2.0f)
                    18 -> onMidiActionTriggered?.invoke(1, "EQ_HIGH", normalized * 2.0f)
                    19 -> onMidiActionTriggered?.invoke(1, "FILTER", normalized * 2.0f - 1.0f)
                    20 -> onMidiActionTriggered?.invoke(2, "EQ_LOW", normalized * 2.0f)
                    21 -> onMidiActionTriggered?.invoke(2, "EQ_MID", normalized * 2.0f)
                    22 -> onMidiActionTriggered?.invoke(2, "EQ_HIGH", normalized * 2.0f)
                    23 -> onMidiActionTriggered?.invoke(2, "FILTER", normalized * 2.0f - 1.0f)
                    24 -> onMidiActionTriggered?.invoke(1, "VOLUME", normalized)
                    25 -> onMidiActionTriggered?.invoke(2, "VOLUME", normalized)
                }
            }
            "NOTE_ON" -> {
                if (event.value > 0) {
                    when (event.controlOrNote) {
                        60 -> onMidiActionTriggered?.invoke(1, "PLAY_PAUSE", 1.0f)
                        61 -> onMidiActionTriggered?.invoke(1, "CUE", 1.0f)
                        62 -> onMidiActionTriggered?.invoke(1, "SYNC", 1.0f)
                        64 -> onMidiActionTriggered?.invoke(2, "PLAY_PAUSE", 1.0f)
                        65 -> onMidiActionTriggered?.invoke(2, "CUE", 1.0f)
                        66 -> onMidiActionTriggered?.invoke(2, "SYNC", 1.0f)
                    }
                }
            }
        }
    }

    /**
     * Send simulated MIDI message from Virtual Hardware Controller or test panels
     */
    fun sendVirtualMidiMessage(type: String, channel: Int, controlOrNote: Int, value: Int) {
        val event = MidiEvent(type, channel, controlOrNote, value)
        handleMidiEvent(event)
    }

    fun close() {
        try {
            openPort?.close()
            openDevice?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing MIDI device", e)
        }
    }
}
