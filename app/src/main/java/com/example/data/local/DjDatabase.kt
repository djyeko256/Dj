package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MidiMapping
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Track::class, Playlist::class, MidiMapping::class],
    version = 3,
    exportSchema = false
)
abstract class DjDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: DjDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DjDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DjDatabase::class.java,
                    "virtual_dj_pro.db"
                ).addCallback(DjDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DjDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.trackDao())
                }
            }
        }

        suspend fun populateInitialData(dao: TrackDao) {
            val defaultPlaylists = listOf(
                Playlist(name = "All Tracks", iconName = "library_music", trackCount = 8, description = "Full library collection", isSystemCrate = true),
                Playlist(name = "Local Device Audio", iconName = "folder", trackCount = 0, description = "Offline music loaded directly from device storage (no phone/cloud required)", isSystemCrate = false),
                Playlist(name = "Club Tech-House", iconName = "album", trackCount = 3, description = "126-128 BPM peak time weapons", isSystemCrate = false),
                Playlist(name = "Festival EDM & Electro", iconName = "bolt", trackCount = 2, description = "High energy festival anthems", isSystemCrate = false),
                Playlist(name = "DnB & Bassline", iconName = "graphic_eq", trackCount = 2, description = "174 BPM rolling basslines", isSystemCrate = false),
                Playlist(name = "Video DJ Set (AV)", iconName = "videocam", trackCount = 3, description = "Synchronized audio + visual video tracks", isSystemCrate = false)
            )
            dao.insertPlaylists(defaultPlaylists)

            val seedTracks = listOf(
                Track(
                    title = "Midnight Strobe (Club Edit)",
                    artist = "Kinetics & Neon",
                    album = "Resonance EP",
                    durationSeconds = 224,
                    bpm = 126.0f,
                    camelotKey = "8A",
                    musicalKey = "Am",
                    genre = "Tech House",
                    energyLevel = 5,
                    isVideo = true,
                    videoAssetUrl = "cyber_grid",
                    soundPreset = "pulse_lead",
                    isVocalCutSupported = true,
                    crate = "Club Tech-House",
                    isFavorite = true
                ),
                Track(
                    title = "Hyperdrive Odyssey",
                    artist = "Solaria",
                    album = "Future Horizon",
                    durationSeconds = 196,
                    bpm = 128.0f,
                    camelotKey = "9A",
                    musicalKey = "Em",
                    genre = "Melodic Techno",
                    energyLevel = 4,
                    isVideo = true,
                    videoAssetUrl = "neon_tunnel",
                    soundPreset = "arp_synth",
                    isVocalCutSupported = true,
                    crate = "Club Tech-House",
                    isFavorite = true
                ),
                Track(
                    title = "Vocal Euphoria (Acapella In)",
                    artist = "Aria Cruz feat. Lynx",
                    album = "Vocal Sessions Vol 1",
                    durationSeconds = 240,
                    bpm = 126.0f,
                    camelotKey = "8B",
                    musicalKey = "C",
                    genre = "House",
                    energyLevel = 4,
                    isVideo = false,
                    soundPreset = "vocal_lead",
                    isVocalCutSupported = true,
                    crate = "Club Tech-House",
                    isFavorite = false
                ),
                Track(
                    title = "Sub-Zero Drop",
                    artist = "Apex Velocity",
                    album = "Overdrive",
                    durationSeconds = 210,
                    bpm = 130.0f,
                    camelotKey = "7A",
                    musicalKey = "Dm",
                    genre = "Electro House",
                    energyLevel = 5,
                    isVideo = false,
                    soundPreset = "saw_bass",
                    isVocalCutSupported = true,
                    crate = "Festival EDM & Electro",
                    isFavorite = false
                ),
                Track(
                    title = "Neon Skyline Video Mix",
                    artist = "Tokyo Drift Collective",
                    album = "Night City",
                    durationSeconds = 250,
                    bpm = 124.0f,
                    camelotKey = "11B",
                    musicalKey = "A",
                    genre = "Synthwave / AV",
                    energyLevel = 3,
                    isVideo = true,
                    videoAssetUrl = "synthwave_city",
                    soundPreset = "retro_synth",
                    isVocalCutSupported = true,
                    crate = "Video DJ Set (AV)",
                    isFavorite = true
                ),
                Track(
                    title = "Quantum Roller 174",
                    artist = "Disruptor",
                    album = "Bass Protocol",
                    durationSeconds = 185,
                    bpm = 174.0f,
                    camelotKey = "4A",
                    musicalKey = "Fm",
                    genre = "Drum & Bass",
                    energyLevel = 5,
                    isVideo = false,
                    soundPreset = "reese_bass",
                    isVocalCutSupported = true,
                    crate = "DnB & Bassline",
                    isFavorite = false
                ),
                Track(
                    title = "Solaris Liquid Dawn",
                    artist = "Etherium",
                    album = "Atmospheres",
                    durationSeconds = 270,
                    bpm = 172.0f,
                    camelotKey = "5A",
                    musicalKey = "Cm",
                    genre = "Liquid DnB",
                    energyLevel = 4,
                    isVideo = false,
                    soundPreset = "liquid_pad",
                    isVocalCutSupported = true,
                    crate = "DnB & Bassline",
                    isFavorite = true
                ),
                Track(
                    title = "Bassline Hustle",
                    artist = "DJ Rogue",
                    album = "Urban Underground",
                    durationSeconds = 178,
                    bpm = 128.0f,
                    camelotKey = "8A",
                    musicalKey = "Am",
                    genre = "UK Bass",
                    energyLevel = 4,
                    isVideo = false,
                    soundPreset = "sub_punch",
                    isVocalCutSupported = true,
                    crate = "Festival EDM & Electro",
                    isFavorite = false
                )
            )
            dao.insertTracks(seedTracks)

            // Seed standard MIDI controller mapping presets (Default, Pioneer, Traktor, Akai)
            val defaultMidi = com.example.engine.MidiPresetProfiles.createDefaultProfile()
            dao.insertMidiMappings(defaultMidi)
        }
    }
}
