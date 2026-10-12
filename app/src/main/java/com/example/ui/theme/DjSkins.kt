package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class DjSkinType(val displayName: String, val description: String) {
    CLASSIC_DARK("Classic Pro DJ", "Pioneer DJM style dark graphite with amber cues and cyan deck accents"),
    CYBERPUNK_NEON("Cyberpunk Neon", "Futuristic midnight violet with electric magenta and cyan laser glow"),
    MINIMAL_STUDIO("Minimal Studio", "Clean matte slate console with crisp emerald green accents"),
    VINYL_GOLD("Vinyl Gold Edition", "Luxury obsidian chassis with brushed champagne gold turntable platters")
}

data class DjSkinColors(
    val type: DjSkinType,
    val background: Color,
    val surfaceChassis: Color,
    val surfaceChassisAlt: Color,
    val deckAColor: Color,
    val deckBColor: Color,
    val deckCColor: Color,
    val deckDColor: Color,
    val jogRimColor: Color,
    val jogPlatterColor: Color,
    val jogCenterColor: Color,
    val jogAccentGlow: Color,
    val crossfaderTrack: Color,
    val crossfaderThumb: Color,
    val eqKnobBase: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val vuMeterNormal: Color,
    val vuMeterWarning: Color,
    val vuMeterClip: Color,
    val waveHighColor: Color,
    val waveMidColor: Color,
    val waveLowColor: Color,
    val chassisGradient: Brush
)

object DjSkins {

    val ClassicDark = DjSkinColors(
        type = DjSkinType.CLASSIC_DARK,
        background = Color(0xFF0D1117),
        surfaceChassis = Color(0xFF161B22),
        surfaceChassisAlt = Color(0xFF1F2937),
        deckAColor = Color(0xFF06B6D4), // Cyan
        deckBColor = Color(0xFFF59E0B), // Amber / Orange
        deckCColor = Color(0xFF3B82F6), // Blue
        deckDColor = Color(0xFF10B981), // Emerald
        jogRimColor = Color(0xFF374151),
        jogPlatterColor = Color(0xFF111827),
        jogCenterColor = Color(0xFF1F2937),
        jogAccentGlow = Color(0xFF06B6D4),
        crossfaderTrack = Color(0xFF0F172A),
        crossfaderThumb = Color(0xFFE2E8F0),
        eqKnobBase = Color(0xFF27272A),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        vuMeterNormal = Color(0xFF10B981),
        vuMeterWarning = Color(0xFFF59E0B),
        vuMeterClip = Color(0xFFEF4444),
        waveHighColor = Color(0xFF38BDF8),
        waveMidColor = Color(0xFF34D399),
        waveLowColor = Color(0xFFF87171),
        chassisGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF161B22), Color(0xFF0F141C))
        )
    )

    val CyberpunkNeon = DjSkinColors(
        type = DjSkinType.CYBERPUNK_NEON,
        background = Color(0xFF090614),
        surfaceChassis = Color(0xFF130F26),
        surfaceChassisAlt = Color(0xFF1F183D),
        deckAColor = Color(0xFF00F0FF), // Cyber Cyan
        deckBColor = Color(0xFFFF007F), // Neon Magenta
        deckCColor = Color(0xFF9D00FF), // Electric Purple
        deckDColor = Color(0xFF00FF66), // Acid Green
        jogRimColor = Color(0xFF2D2353),
        jogPlatterColor = Color(0xFF0F0B1E),
        jogCenterColor = Color(0xFF1C1338),
        jogAccentGlow = Color(0xFFFF007F),
        crossfaderTrack = Color(0xFF0A0717),
        crossfaderThumb = Color(0xFF00F0FF),
        eqKnobBase = Color(0xFF2D1B4E),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFC4B5FD),
        vuMeterNormal = Color(0xFF00F0FF),
        vuMeterWarning = Color(0xFFFFB800),
        vuMeterClip = Color(0xFFFF0055),
        waveHighColor = Color(0xFF00F0FF),
        waveMidColor = Color(0xFFFF007F),
        waveLowColor = Color(0xFFFF4081),
        chassisGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF181133), Color(0xFF0D0820))
        )
    )

    val MinimalStudio = DjSkinColors(
        type = DjSkinType.MINIMAL_STUDIO,
        background = Color(0xFF121417),
        surfaceChassis = Color(0xFF1C2026),
        surfaceChassisAlt = Color(0xFF282E37),
        deckAColor = Color(0xFF10B981), // Emerald
        deckBColor = Color(0xFF6366F1), // Indigo
        deckCColor = Color(0xFF14B8A6), // Teal
        deckDColor = Color(0xFF8B5CF6), // Violet
        jogRimColor = Color(0xFF333B47),
        jogPlatterColor = Color(0xFF161A20),
        jogCenterColor = Color(0xFF222832),
        jogAccentGlow = Color(0xFF10B981),
        crossfaderTrack = Color(0xFF14181E),
        crossfaderThumb = Color(0xFFE5E7EB),
        eqKnobBase = Color(0xFF2B323D),
        textPrimary = Color(0xFFF9FAFB),
        textSecondary = Color(0xFF9CA3AF),
        vuMeterNormal = Color(0xFF10B981),
        vuMeterWarning = Color(0xFFFBBF24),
        vuMeterClip = Color(0xFFF87171),
        waveHighColor = Color(0xFF6EE7B7),
        waveMidColor = Color(0xFF818CF8),
        waveLowColor = Color(0xFFF472B6),
        chassisGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E242C), Color(0xFF15191E))
        )
    )

    val VinylGold = DjSkinColors(
        type = DjSkinType.VINYL_GOLD,
        background = Color(0xFF0B0907),
        surfaceChassis = Color(0xFF17130F),
        surfaceChassisAlt = Color(0xFF241D17),
        deckAColor = Color(0xFFF59E0B), // Champagne Gold
        deckBColor = Color(0xFFD97706), // Amber Bronze
        deckCColor = Color(0xFFFBBF24), // Bright Gold
        deckDColor = Color(0xFFB45309), // Deep Copper
        jogRimColor = Color(0xFF3D3226),
        jogPlatterColor = Color(0xFF14100C),
        jogCenterColor = Color(0xFF271F17),
        jogAccentGlow = Color(0xFFF59E0B),
        crossfaderTrack = Color(0xFF0F0C08),
        crossfaderThumb = Color(0xFFFDE68A),
        eqKnobBase = Color(0xFF362B1E),
        textPrimary = Color(0xFFFEF3C7),
        textSecondary = Color(0xFFD4AF37),
        vuMeterNormal = Color(0xFFEAB308),
        vuMeterWarning = Color(0xFFF97316),
        vuMeterClip = Color(0xFFEF4444),
        waveHighColor = Color(0xFFFDE047),
        waveMidColor = Color(0xFFF59E0B),
        waveLowColor = Color(0xFFDC2626),
        chassisGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF1D1712), Color(0xFF110D09))
        )
    )

    fun getSkin(type: DjSkinType): DjSkinColors = when (type) {
        DjSkinType.CLASSIC_DARK -> ClassicDark
        DjSkinType.CYBERPUNK_NEON -> CyberpunkNeon
        DjSkinType.MINIMAL_STUDIO -> MinimalStudio
        DjSkinType.VINYL_GOLD -> VinylGold
    }
}
