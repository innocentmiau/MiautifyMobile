package io.github.innocentmiau.miautify.library

import android.content.Context
import android.net.Uri
import io.github.innocentmiau.miautify.data.MiautifyDatabase
import io.github.innocentmiau.miautify.data.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException

/** Where the scan of the music folder is. */
sealed interface ScanState {
    /** No list to show yet: the folder was never scanned before. */
    data object Scanning : ScanState

    /**
     * A list to show.
     *
     * @property checking True while the folder is walked to find new, changed or removed files.
     *   The list is the cached one until then.
     * @property tagsToRead How many new or changed files need their tags read.
     * @property tagsRead How many of those are done.
     */
    data class Done(
        val songs: List<Song>,
        val checking: Boolean = false,
        val tagsToRead: Int = 0,
        val tagsRead: Int = 0
    ) : ScanState {
        val readingTags: Boolean
            get() = tagsRead < tagsToRead
    }

    data object Failed : ScanState
}

/** How many tags to read between two updates of the list and the cache. */
private const val TAGS_PER_UPDATE = 20

/**
 * Scans [folder] using the cache, sending a new state after each step:
 * 1. The cached list, if the folder was scanned before (instant).
 * 2. The files found now, with cached tags for unchanged files.
 * 3. Every [TAGS_PER_UPDATE] songs, the tags read for new or changed files, also saved to the cache.
 * Stops when the collector stops, for example on a new folder.
 */
fun scanLibrary(context: Context, folder: Uri): Flow<ScanState> = flow {
    val dao = MiautifyDatabase.get(context).songs()
    val key = folder.toString()

    val cachedRows = dao.songsIn(key)
    if (cachedRows.isEmpty()) {
        emit(ScanState.Scanning)
    } else {
        val cachedSongs = cachedRows.map { it.toSong() }.sortedWith(compareBy(songFileOrder) { it.file })
        emit(ScanState.Done(cachedSongs, checking = true))
    }

    val files = try {
        MusicFolder.scan(context, folder)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
    if (files == null) {
        emit(ScanState.Failed)
        return@flow
    }

    val match = matchCache(files, cachedRows.associate { it.documentId to it.toCachedSong() })
    dao.delete(key, match.removed)
    val songs = match.songs.toMutableList()
    emit(ScanState.Done(songs.toList(), tagsToRead = match.toRead.size))

    var read = 0
    for (batch in match.toRead.chunked(TAGS_PER_UPDATE)) {
        for (index in batch) {
            val file = songs[index].file
            songs[index] = Song(file, TagReader.read(context, MusicFolder.fileUri(folder, file)))
        }
        dao.save(batch.map { SongEntity(key, songs[it]) })
        read += batch.size
        // A copy, so the screen gets a list that won't change under it.
        emit(ScanState.Done(songs.toList(), tagsToRead = match.toRead.size, tagsRead = read))
    }
}.flowOn(Dispatchers.IO)
