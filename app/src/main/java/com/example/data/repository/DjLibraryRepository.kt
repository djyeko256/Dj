package com.example.data.repository

import com.example.data.local.TrackDao
import com.example.data.model.MidiMapping
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncedDevicesCount: Int = 3,
    val cloudStatusMessage: String = "Cloud Library Synced (Europe-West2 Node)"
)

class DjLibraryRepository(private val trackDao: TrackDao) {

    private val _syncStatus = MutableStateFlow(CloudSyncStatus())
    val syncStatus = _syncStatus.asStateFlow()

    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    fun getTracksByCrate(crateName: String): Flow<List<Track>> {
        return if (crateName == "All Tracks") {
            trackDao.getAllTracks()
        } else {
            trackDao.getTracksByCrate(crateName)
        }
    }

    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)

    fun getFavoriteTracks(): Flow<List<Track>> = trackDao.getFavoriteTracks()

    suspend fun insertTrack(track: Track): Long = trackDao.insertTrack(track)

    suspend fun insertTracks(tracks: List<Track>) = trackDao.insertTracks(tracks)

    suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)

    suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)

    fun getAllPlaylists(): Flow<List<Playlist>> = trackDao.getAllPlaylists()

    suspend fun insertPlaylist(playlist: Playlist): Long = trackDao.insertPlaylist(playlist)

    fun getAllMidiMappings(): Flow<List<MidiMapping>> = trackDao.getAllMidiMappings()

    fun getMidiMappingsByProfile(profileName: String): Flow<List<MidiMapping>> = trackDao.getMidiMappingsByProfile(profileName)

    fun getDistinctProfileNames(): Flow<List<String>> = trackDao.getDistinctProfileNames()

    suspend fun insertMidiMapping(mapping: MidiMapping): Long = trackDao.insertMidiMapping(mapping)

    suspend fun insertMidiMappings(mappings: List<MidiMapping>) = trackDao.insertMidiMappings(mappings)

    suspend fun deleteMidiMapping(mapping: MidiMapping) = trackDao.deleteMidiMapping(mapping)

    suspend fun deleteMidiMappingsByProfile(profileName: String) = trackDao.deleteMidiMappingsByProfile(profileName)

    suspend fun clearMidiMappings() = trackDao.clearAllMidiMappings()

    suspend fun triggerCloudSync(): Boolean {
        _syncStatus.value = _syncStatus.value.copy(
            isSyncing = true,
            cloudStatusMessage = "Syncing crates & cue points with Cloud Storage..."
        )
        kotlinx.coroutines.delay(1200) // Simulated fast cloud sync
        _syncStatus.value = _syncStatus.value.copy(
            isSyncing = false,
            lastSyncTimestamp = System.currentTimeMillis(),
            syncedDevicesCount = 3,
            cloudStatusMessage = "Library fully synchronized with 3 connected DJ consoles"
        )
        return true
    }
}
