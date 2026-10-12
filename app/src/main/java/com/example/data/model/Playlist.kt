package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "library_music",
    val trackCount: Int = 0,
    val description: String = "",
    val isSystemCrate: Boolean = false
)
