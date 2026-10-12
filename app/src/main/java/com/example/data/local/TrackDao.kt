package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MidiMapping
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY id ASC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE crate = :crateName ORDER BY id ASC")
    fun getTracksByCrate(crateName: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%'")
    fun searchTracks(query: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    // Playlists / Crates
    @Query("SELECT * FROM playlists ORDER BY id ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    // MIDI Mappings
    @Query("SELECT * FROM midi_mappings ORDER BY id ASC")
    fun getAllMidiMappings(): Flow<List<MidiMapping>>

    @Query("SELECT * FROM midi_mappings WHERE profileName = :profileName ORDER BY id ASC")
    fun getMidiMappingsByProfile(profileName: String): Flow<List<MidiMapping>>

    @Query("SELECT DISTINCT profileName FROM midi_mappings ORDER BY profileName ASC")
    fun getDistinctProfileNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMidiMapping(mapping: MidiMapping): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMidiMappings(mappings: List<MidiMapping>)

    @Delete
    suspend fun deleteMidiMapping(mapping: MidiMapping)

    @Query("DELETE FROM midi_mappings WHERE controlName = :controlName")
    suspend fun deleteMidiMappingByControl(controlName: String)

    @Query("DELETE FROM midi_mappings WHERE profileName = :profileName")
    suspend fun deleteMidiMappingsByProfile(profileName: String)

    @Query("DELETE FROM midi_mappings")
    suspend fun clearAllMidiMappings()
}
