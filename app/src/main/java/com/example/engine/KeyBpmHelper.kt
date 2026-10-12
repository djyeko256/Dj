package com.example.engine

enum class KeyMatchLevel {
    PERFECT,     // Same key
    COMPATIBLE,  // +1 or -1 on Camelot wheel, or relative major/minor
    ENERGY_JUMP, // +2 on Camelot wheel
    CLASH        // Not harmonically compatible
}

data class HarmonicCompatibility(
    val level: KeyMatchLevel,
    val description: String,
    val hexColor: Long
)

object KeyBpmHelper {

    fun parseCamelot(camelot: String): Pair<Int, Char>? {
        if (camelot.length < 2) return null
        val numberPart = camelot.dropLast(1).toIntOrNull() ?: return null
        val letterPart = camelot.last().uppercaseChar()
        if (letterPart != 'A' && letterPart != 'B') return null
        if (numberPart !in 1..12) return null
        return Pair(numberPart, letterPart)
    }

    /**
     * Checks harmonic compatibility between reference deck (Master) and target deck.
     */
    fun checkCompatibility(masterCamelot: String, targetCamelot: String): HarmonicCompatibility {
        if (masterCamelot.isBlank() || targetCamelot.isBlank()) {
            return HarmonicCompatibility(KeyMatchLevel.COMPATIBLE, "No Key Set", 0xFF94A3B8)
        }
        val master = parseCamelot(masterCamelot)
        val target = parseCamelot(targetCamelot)
        if (master == null || target == null) {
            return HarmonicCompatibility(KeyMatchLevel.COMPATIBLE, "Unknown Key", 0xFF94A3B8)
        }

        if (master == target) {
            return HarmonicCompatibility(
                KeyMatchLevel.PERFECT,
                "Perfect Match (${target.first}${target.second})",
                0xFF10B981 // Emerald Green
            )
        }

        // Relative major/minor (e.g. 8A <-> 8B)
        if (master.first == target.first && master.second != target.second) {
            return HarmonicCompatibility(
                KeyMatchLevel.COMPATIBLE,
                "Relative ${if (target.second == 'B') "Major" else "Minor"}",
                0xFF06B6D4 // Cyan
            )
        }

        // Adjacent numbers on the wheel (wrap 12 <-> 1)
        val diff = (target.first - master.first + 12) % 12
        if (master.second == target.second) {
            if (diff == 1) {
                return HarmonicCompatibility(
                    KeyMatchLevel.COMPATIBLE,
                    "+1 Energy Lift",
                    0xFF3B82F6 // Blue
                )
            }
            if (diff == 11) { // which is -1
                return HarmonicCompatibility(
                    KeyMatchLevel.COMPATIBLE,
                    "-1 Deep Groove",
                    0xFF3B82F6 // Blue
                )
            }
            if (diff == 2) {
                return HarmonicCompatibility(
                    KeyMatchLevel.ENERGY_JUMP,
                    "+2 Energy Jump",
                    0xFFF59E0B // Amber
                )
            }
        }

        return HarmonicCompatibility(
            KeyMatchLevel.CLASH,
            "Key Clash (Dissonance Risk)",
            0xFFEF4444 // Red
        )
    }

    /**
     * Calculates pitch adjust percentage to match master BPM
     */
    fun calculateSyncPitch(currentBpm: Float, targetBpm: Float): Float {
        if (currentBpm <= 0f || targetBpm <= 0f) return 0f
        return ((targetBpm - currentBpm) / currentBpm) * 100f
    }
}
