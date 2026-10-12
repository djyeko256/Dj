package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Int,
    val bpm: Float,
    val camelotKey: String, // e.g. "8A", "8B", "11A"
    val musicalKey: String, // e.g. "Am", "C", "F#m"
    val genre: String, // e.g. "Techno", "House", "Hip Hop", "DnB"
    val energyLevel: Int = 4, // 1 to 5
    val isVideo: Boolean = false,
    val videoAssetUrl: String = "",
    val soundPreset: String = "synth_lead", // For procedural audio engine
    val isVocalCutSupported: Boolean = true,
    val crate: String = "All Tracks",
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val localFileUri: String? = null,
    val isLocalFile: Boolean = false
)
