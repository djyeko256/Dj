package com.example.engine

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalAudioHelper {
    private const val TAG = "LocalAudioHelper"

    /**
     * Extracts metadata from a user-selected audio file URI and converts it into a Track entity
     */
    suspend fun importAudioFromUri(context: Context, uri: Uri): Track = withContext(Dispatchers.IO) {
        // Attempt to persist URI permission so app can read file later
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Some file providers do not support persistable permissions
        }

        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs: Long = 180000L
        var genre: String? = null

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durStr != null) {
                durationMs = durStr.toLongOrNull() ?: 180000L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting metadata from uri: $uri", e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // Fallback to display name from Document / Content Resolver
        if (title.isNullOrBlank()) {
            title = getDisplayName(context, uri) ?: "Local Track ${System.currentTimeMillis() % 1000}"
        }

        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(30)
        // Default energetic DJ BPM estimation
        val estimatedBpm = 126.0f

        Track(
            title = title,
            artist = if (!artist.isNullOrBlank()) artist else "Local Artist",
            album = if (!album.isNullOrBlank()) album else "Device Storage",
            durationSeconds = durationSec,
            bpm = estimatedBpm,
            camelotKey = "8A",
            musicalKey = "Am",
            genre = if (!genre.isNullOrBlank()) genre else "Local Audio",
            energyLevel = 4,
            isVideo = false,
            soundPreset = "local_audio",
            isVocalCutSupported = true,
            crate = "Local Device Audio",
            isFavorite = true,
            localFileUri = uri.toString(),
            isLocalFile = true
        )
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get display name from cursor", e)
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name?.substringBeforeLast('.')
    }

    /**
     * Scans MediaStore for audio files physically on this device (downloads, music, storage)
     */
    suspend fun scanDeviceMediaStore(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val title = it.getString(titleCol) ?: "Unknown"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Local Music"
                    val durationMs = it.getLong(durationCol)

                    tracks.add(
                        Track(
                            title = title,
                            artist = artist,
                            album = album,
                            durationSeconds = (durationMs / 1000).toInt().coerceAtLeast(30),
                            bpm = 126.0f,
                            camelotKey = "8A",
                            musicalKey = "Am",
                            genre = "Local Storage",
                            energyLevel = 4,
                            isVideo = false,
                            soundPreset = "local_audio",
                            isVocalCutSupported = true,
                            crate = "Local Device Audio",
                            isFavorite = false,
                            localFileUri = contentUri.toString(),
                            isLocalFile = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore scan error (permission may not be granted yet): ${e.message}")
        }
        tracks
    }
}

/**
 * Dedicated player for a deck playing real local audio files
 */
class LocalDeckPlayer(
    private val context: Context,
    val deckIndex: Int
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUriString: String? = null
    private var isPrepared = false

    fun loadUri(uriString: String) {
        if (currentUriString == uriString && mediaPlayer != null) return

        release()
        currentUriString = uriString

        try {
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener {
                    isPrepared = true
                }
                setOnCompletionListener {
                    // Loop or pause
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("LocalDeckPlayer", "Failed to load URI on deck $deckIndex: $uriString", e)
        }
    }

    fun play() {
        try {
            if (isPrepared && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("LocalDeckPlayer", "Error starting playback on deck $deckIndex", e)
        }
    }

    fun pause() {
        try {
            if (isPrepared && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e("LocalDeckPlayer", "Error pausing playback on deck $deckIndex", e)
        }
    }

    fun seekTo(seconds: Float) {
        try {
            if (isPrepared) {
                val ms = (seconds * 1000).toInt()
                mediaPlayer?.seekTo(ms)
            }
        } catch (e: Exception) {
            Log.e("LocalDeckPlayer", "Error seeking playback on deck $deckIndex", e)
        }
    }

    fun setPlaybackRate(rate: Float) {
        try {
            if (isPrepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val clamped = rate.coerceIn(0.5f, 2.0f)
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                mediaPlayer?.playbackParams = params.setSpeed(clamped)
            }
        } catch (e: Exception) {
            // Speed change error
        }
    }

    fun setVolume(volume: Float) {
        try {
            val clamped = volume.coerceIn(0f, 1f)
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (e: Exception) {
            // Volume error
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPrepared = false
        currentUriString = null
    }
}
