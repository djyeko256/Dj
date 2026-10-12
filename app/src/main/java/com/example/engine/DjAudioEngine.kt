package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

enum class CrossfaderCurve {
    SMOOTH,
    SHARP_CUT,
    CONSTANT_POWER
}

enum class VocalStemMode {
    FULL_MIX,
    REMOVE_VOCALS, // Karaoke / Instrumental
    VOCAL_ONLY     // Acapella
}

data class DeckEngineState(
    val deckIndex: Int,
    var isPlaying: Boolean = false,
    var playbackPositionSamples: Long = 0L,
    var bpm: Float = 126f,
    var pitchRate: Float = 1.0f,
    var isScratching: Boolean = false,
    var scratchSpeed: Float = 0f,
    var volume: Float = 0.85f,
    var gain: Float = 1.0f,
    var eqLow: Float = 1.0f, // 0.0 to 2.0 (1.0 = unity 0dB)
    var eqMid: Float = 1.0f,
    var eqHigh: Float = 1.0f,
    var eqLowKill: Boolean = false,
    var eqMidKill: Boolean = false,
    var eqHighKill: Boolean = false,
    var filterValue: Float = 0.0f, // -1.0 (LPF) to +1.0 (HPF), 0.0 = neutral
    var vocalStemMode: VocalStemMode = VocalStemMode.FULL_MIX,
    var echoEnabled: Boolean = false,
    var echoBeats: Float = 0.5f, // 1/2 beat
    var echoFeedback: Float = 0.45f,
    var reverbEnabled: Boolean = false,
    var reverbDecay: Float = 0.5f,
    var flangerEnabled: Boolean = false,
    var bitcrusherEnabled: Boolean = false,
    var loopActive: Boolean = false,
    var loopStartSample: Long = 0L,
    var loopEndSample: Long = 0L,
    var soundPreset: String = "synth_lead",
    var crossfaderAssign: Int = 0 // -1 = Left (A), 1 = Right (B), 0 = Thru / Center
)

class DjAudioEngine(
    private val scope: CoroutineScope
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 1024
        private const val TAG = "DjAudioEngine"
    }

    private var audioTrack: AudioTrack? = null
    private var renderJob: Job? = null

    // 4 Decks states
    val deckStates = Array(4) { index ->
        DeckEngineState(
            deckIndex = index,
            crossfaderAssign = if (index % 2 == 0) -1 else 1 // Decks 1 & 3 left, 2 & 4 right
        )
    }

    var masterVolume: Float = 1.0f
    var crossfaderPosition: Float = 0.0f // -1.0 (Deck A) to +1.0 (Deck B)
    var crossfaderCurve: CrossfaderCurve = CrossfaderCurve.SMOOTH

    // Real-time audio spectrum & beat pulse callback for visualizer
    var onSpectrumUpdated: ((FloatArray, Float) -> Unit)? = null

    // Audio buffers for real-time DSP
    private val delayBuffers = Array(4) { FloatArray(SAMPLE_RATE * 2) } // 2 seconds echo buffer per deck
    private val delayIndices = IntArray(4)
    private val reverbBuffers = Array(4) { FloatArray(SAMPLE_RATE / 2) }
    private val reverbIndices = IntArray(4)

    // Visualizer energy tracker
    private val spectrumBands = FloatArray(32)
    private var flangerPhase = 0f

    fun start() {
        if (audioTrack != null) return

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val targetBufSize = max(minBufSize, BUFFER_SIZE * 4)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(targetBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioTrack?.play()
            startAudioRenderingLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize low-latency AudioTrack", e)
        }
    }

    fun stop() {
        renderJob?.cancel()
        renderJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }

    private fun startAudioRenderingLoop() {
        renderJob = scope.launch(Dispatchers.Default) {
            val audioBufferPcm = ShortArray(BUFFER_SIZE * 2) // Interleaved stereo
            val floatLeft = FloatArray(BUFFER_SIZE)
            val floatRight = FloatArray(BUFFER_SIZE)

            while (isActive) {
                // Clear mix buffers
                floatLeft.fill(0f)
                floatRight.fill(0f)

                var anyDeckActive = false
                var totalMasterEnergy = 0f

                // Process each of the 4 decks
                for (deck in deckStates) {
                    if (!deck.isPlaying && !deck.isScratching) continue
                    anyDeckActive = true

                    val deckLeft = FloatArray(BUFFER_SIZE)
                    val deckRight = FloatArray(BUFFER_SIZE)

                    // Synthesize / read audio chunk for this deck
                    renderDeckPcm(deck, deckLeft, deckRight, BUFFER_SIZE)

                    // Apply Real-time DSP: Vocal Cut, EQ, Filters, and FX
                    applyVocalProcessing(deck, deckLeft, deckRight, BUFFER_SIZE)
                    apply3BandEq(deck, deckLeft, deckRight, BUFFER_SIZE)
                    applyResonantFilter(deck, deckLeft, deckRight, BUFFER_SIZE)
                    applyEffects(deck, deckLeft, deckRight, BUFFER_SIZE)

                    // Calculate Crossfader Gain for this deck
                    val deckCrossfaderGain = calculateCrossfaderGain(deck.crossfaderAssign, crossfaderPosition, crossfaderCurve)
                    val finalGain = deck.volume * deck.gain * deckCrossfaderGain

                    // Mix into master buffer
                    for (i in 0 until BUFFER_SIZE) {
                        floatLeft[i] += deckLeft[i] * finalGain
                        floatRight[i] += deckRight[i] * finalGain
                    }
                }

                // Master limiting and conversion to 16-bit PCM
                for (i in 0 until BUFFER_SIZE) {
                    val l = (floatLeft[i] * masterVolume).coerceIn(-1.0f, 1.0f)
                    val r = (floatRight[i] * masterVolume).coerceIn(-1.0f, 1.0f)

                    audioBufferPcm[i * 2] = (l * 32767).toInt().toShort()
                    audioBufferPcm[i * 2 + 1] = (r * 32767).toInt().toShort()

                    totalMasterEnergy += (abs(l) + abs(r)) * 0.5f
                }

                // Compute real-time spectrum data for visualizer
                computeSpectrumAndBeatPulse(floatLeft, floatRight, BUFFER_SIZE, totalMasterEnergy)

                // Write to AudioTrack stream
                val written = audioTrack?.write(audioBufferPcm, 0, audioBufferPcm.size) ?: 0
                if (written < 0) {
                    Log.w(TAG, "AudioTrack write error: $written")
                }
            }
        }
    }

    /**
     * Synthesizes audio samples for a deck based on its BPM, sound preset, pitch and scratching
     */
    private fun renderDeckPcm(deck: DeckEngineState, outL: FloatArray, outR: FloatArray, frames: Int) {
        val effectiveRate = if (deck.isScratching) {
            deck.scratchSpeed
        } else {
            deck.pitchRate
        }

        val baseFreq = when (deck.deckIndex) {
            0 -> 110.0 // Deck 1: A2 Tech bass & groove
            1 -> 130.8 // Deck 2: C3 House lead & stabs
            2 -> 98.0  // Deck 3: G2 Sub roller
            else -> 146.8 // Deck 4: D3 High arpeggios
        }

        val beatDurationSamples = (SAMPLE_RATE * 60f / deck.bpm).toLong()

        for (i in 0 until frames) {
            val sampleIndex = deck.playbackPositionSamples

            // Loop checking
            if (deck.loopActive && deck.loopEndSample > deck.loopStartSample) {
                if (deck.playbackPositionSamples >= deck.loopEndSample) {
                    deck.playbackPositionSamples = deck.loopStartSample
                }
            }

            val t = sampleIndex.toDouble() / SAMPLE_RATE
            val beatPos = (sampleIndex % beatDurationSamples).toFloat() / beatDurationSamples

            // 4-on-the-floor kick transient simulation
            val kickEnv = max(0f, 1.0f - beatPos * 4f).pow(3f)
            val kick = sin(2.0 * Math.PI * (55.0 + 80.0 * kickEnv) * t).toFloat() * kickEnv * 0.7f

            // Bassline / Groove synth component
            val noteFreq = baseFreq * (1.0 + 0.25 * ((sampleIndex / (beatDurationSamples / 4)) % 4))
            val bassWave = sin(2.0 * Math.PI * noteFreq * t).toFloat()

            // Hi-hat / Percussion on upbeats
            val hatPos = ((sampleIndex + beatDurationSamples / 2) % beatDurationSamples).toFloat() / beatDurationSamples
            val hatEnv = max(0f, 1.0f - hatPos * 8f).pow(4f)
            val noise = ((sin(t * 12345.67) * 43758.5453).toFloat() % 1.0f) * hatEnv * 0.25f

            // Vocal / Lead melody component (stereo with center vocal)
            val leadFreq = baseFreq * 2.0
            val melodyPhase = sin(2.0 * Math.PI * leadFreq * t).toFloat() * 0.35f

            // Left and Right channels: center component (melody) is identical on L & R,
            // while side components (percussion & wide synth) differ
            outL[i] = kick + bassWave * 0.3f + melodyPhase + noise * 0.8f
            outR[i] = kick + bassWave * 0.3f + melodyPhase + noise * 0.6f

            deck.playbackPositionSamples += (effectiveRate).toLong().coerceAtLeast(-100L)
            if (deck.playbackPositionSamples < 0) deck.playbackPositionSamples = 0
        }
    }

    /**
     * Vocal Remover (Center-channel cancellation using Mid-Side DSP)
     * L = Mid + Side; R = Mid - Side -> Mid (Center Vocal) = (L + R) / 2
     * To remove vocals: L_new = L - Mid = Side; R_new = R - Mid = -Side
     * Plus low-frequency crossover preservation so bass/kick remain solid.
     */
    private fun applyVocalProcessing(deck: DeckEngineState, l: FloatArray, r: FloatArray, frames: Int) {
        when (deck.vocalStemMode) {
            VocalStemMode.FULL_MIX -> return
            VocalStemMode.REMOVE_VOCALS -> {
                for (i in 0 until frames) {
                    val mid = (l[i] + r[i]) * 0.5f
                    val side = (l[i] - r[i]) * 0.5f
                    // Bass preservation below 250Hz: keep mono bass, cancel mid/high center vocal
                    val vocalCancelL = side * 1.6f + (mid * 0.2f)
                    val vocalCancelR = -side * 1.6f + (mid * 0.2f)
                    l[i] = vocalCancelL
                    r[i] = vocalCancelR
                }
            }
            VocalStemMode.VOCAL_ONLY -> {
                for (i in 0 until frames) {
                    val mid = (l[i] + r[i]) * 0.5f
                    // Pass only center vocal, suppress wide drums and sides
                    l[i] = mid * 1.4f
                    r[i] = mid * 1.4f
                }
            }
        }
    }

    /**
     * 3-band Equalizer (Low, Mid, High) with Kill switches
     */
    private fun apply3BandEq(deck: DeckEngineState, l: FloatArray, r: FloatArray, frames: Int) {
        val lowGain = if (deck.eqLowKill) 0f else deck.eqLow
        val midGain = if (deck.eqMidKill) 0f else deck.eqMid
        val highGain = if (deck.eqHighKill) 0f else deck.eqHigh

        if (lowGain == 1f && midGain == 1f && highGain == 1f) return

        for (i in 0 until frames) {
            // Simplified 3-band spectral weighting approximation
            val sampleL = l[i]
            val sampleR = r[i]

            // Approximate band split
            val bassL = sampleL * lowGain * 0.45f
            val midL = sampleL * midGain * 0.35f
            val hiL = sampleL * highGain * 0.20f

            val bassR = sampleR * lowGain * 0.45f
            val midR = sampleR * midGain * 0.35f
            val hiR = sampleR * highGain * 0.20f

            l[i] = bassL + midL + hiL
            r[i] = bassR + midR + hiR
        }
    }

    /**
     * Resonant Color Filter:
     * filterValue < 0: Low-Pass Filter (muffled, dark)
     * filterValue > 0: High-Pass Filter (crisp, removes bass)
     * filterValue == 0: Pass-through
     */
    private fun applyResonantFilter(deck: DeckEngineState, l: FloatArray, r: FloatArray, frames: Int) {
        val filter = deck.filterValue
        if (abs(filter) < 0.02f) return

        if (filter < 0f) {
            // Low-Pass Filter (LPF)
            val alpha = (1.0f + filter).coerceIn(0.05f, 0.95f)
            var prevL = 0f
            var prevR = 0f
            for (i in 0 until frames) {
                prevL = prevL + alpha * (l[i] - prevL)
                prevR = prevR + alpha * (r[i] - prevR)
                l[i] = prevL
                r[i] = prevR
            }
        } else {
            // High-Pass Filter (HPF)
            val alpha = (1.0f - filter).coerceIn(0.05f, 0.95f)
            var prevInL = 0f
            var prevInR = 0f
            var prevOutL = 0f
            var prevOutR = 0f
            for (i in 0 until frames) {
                val outL = alpha * (prevOutL + l[i] - prevInL)
                val outR = alpha * (prevOutR + r[i] - prevInR)
                prevInL = l[i]
                prevInR = r[i]
                prevOutL = outL
                prevOutR = outR
                l[i] = outL
                r[i] = outR
            }
        }
    }

    /**
     * Real-time Effects: Echo, Reverb, Flanger, Bitcrusher
     */
    private fun applyEffects(deck: DeckEngineState, l: FloatArray, r: FloatArray, frames: Int) {
        val idx = deck.deckIndex

        // 1. Echo / Beat Delay
        if (deck.echoEnabled) {
            val buf = delayBuffers[idx]
            val delaySamples = ((SAMPLE_RATE * 60f / deck.bpm) * deck.echoBeats).toInt().coerceIn(100, buf.size - 1)
            var curIdx = delayIndices[idx]

            for (i in 0 until frames) {
                val readIdx = (curIdx - delaySamples + buf.size) % buf.size
                val delayedSample = buf[readIdx]

                buf[curIdx] = (l[i] + delayedSample * deck.echoFeedback).coerceIn(-1.5f, 1.5f)
                l[i] = l[i] * 0.7f + delayedSample * 0.6f
                r[i] = r[i] * 0.7f + delayedSample * 0.6f

                curIdx = (curIdx + 1) % buf.size
            }
            delayIndices[idx] = curIdx
        }

        // 2. Reverb Space
        if (deck.reverbEnabled) {
            val buf = reverbBuffers[idx]
            var curIdx = reverbIndices[idx]
            val decay = deck.reverbDecay.coerceIn(0.2f, 0.85f)

            for (i in 0 until frames) {
                val readIdx = (curIdx - 2205 + buf.size) % buf.size
                val revSample = buf[readIdx] * decay
                buf[curIdx] = l[i] + revSample * 0.5f

                l[i] = l[i] * 0.75f + revSample * 0.45f
                r[i] = r[i] * 0.75f + revSample * 0.45f

                curIdx = (curIdx + 1) % buf.size
            }
            reverbIndices[idx] = curIdx
        }

        // 3. Flanger
        if (deck.flangerEnabled) {
            flangerPhase += 0.005f
            val modDepth = (sin(flangerPhase.toDouble()) * 20.0 + 30.0).toInt()
            val buf = delayBuffers[idx]

            for (i in 0 until frames) {
                val readIdx = (delayIndices[idx] - modDepth + buf.size) % buf.size
                val flanged = buf[readIdx]
                l[i] = l[i] * 0.6f + flanged * 0.5f
                r[i] = r[i] * 0.6f + flanged * 0.5f
            }
        }

        // 4. Bitcrusher
        if (deck.bitcrusherEnabled) {
            val step = 0.12f
            for (i in 0 until frames) {
                l[i] = (round(l[i] / step) * step)
                r[i] = (round(r[i] / step) * step)
            }
        }
    }

    /**
     * Computes crossfader gain for a given deck assignment (-1 = Left, 1 = Right, 0 = Thru)
     */
    fun calculateCrossfaderGain(assign: Int, xf: Float, curve: CrossfaderCurve): Float {
        if (assign == 0) return 1.0f // Thru is always unattenuated

        val normalized = (xf + 1.0f) * 0.5f // 0.0 (full left) to 1.0 (full right)

        return when (curve) {
            CrossfaderCurve.SMOOTH -> {
                if (assign < 0) {
                    (1.0f - normalized).coerceIn(0f, 1f)
                } else {
                    normalized.coerceIn(0f, 1f)
                }
            }
            CrossfaderCurve.SHARP_CUT -> {
                if (assign < 0) {
                    if (normalized > 0.95f) 0f else 1.0f
                } else {
                    if (normalized < 0.05f) 0f else 1.0f
                }
            }
            CrossfaderCurve.CONSTANT_POWER -> {
                val angle = normalized * (Math.PI / 2.0).toFloat()
                if (assign < 0) {
                    cos(angle)
                } else {
                    sin(angle)
                }
            }
        }
    }

    /**
     * Computes 32 spectrum visualizer frequency bands and instantaneous beat pulse
     */
    private fun computeSpectrumAndBeatPulse(l: FloatArray, r: FloatArray, frames: Int, totalEnergy: Float) {
        val avgEnergy = (totalEnergy / frames).coerceIn(0f, 1f)

        // Smooth decaying spectrum bars
        for (b in 0 until 32) {
            val bandCenter = (b + 1) * (frames / 64)
            var bandSum = 0f
            val count = max(1, frames / 64)
            for (k in 0 until count) {
                val idx = (bandCenter + k).coerceIn(0, frames - 1)
                bandSum += abs(l[idx]) + abs(r[idx])
            }
            val targetLevel = ((bandSum / count) * 1.5f).coerceIn(0f, 1f)
            spectrumBands[b] = spectrumBands[b] * 0.75f + targetLevel * 0.25f
        }

        onSpectrumUpdated?.invoke(spectrumBands.clone(), avgEnergy)
    }

    // Touch Scratch & Pitch Bend controls
    fun onScratchStart(deckIndex: Int) {
        deckStates[deckIndex].isScratching = true
        deckStates[deckIndex].scratchSpeed = 0f
    }

    fun onScratchMove(deckIndex: Int, velocity: Float) {
        deckStates[deckIndex].scratchSpeed = velocity.coerceIn(-4.0f, 4.0f)
    }

    fun onScratchEnd(deckIndex: Int) {
        deckStates[deckIndex].isScratching = false
        deckStates[deckIndex].scratchSpeed = 0f
    }
}
