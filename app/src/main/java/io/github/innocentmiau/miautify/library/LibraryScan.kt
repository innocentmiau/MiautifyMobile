package io.github.innocentmiau.miautify.library

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException

/** Where the scan of the music folder is. */
sealed interface ScanState {
    data object Scanning : ScanState

    /** The files are listed. Tags are read for the first [tagsRead] songs; all of them when it equals the size. */
    data class Done(val songs: List<Song>, val tagsRead: Int) : ScanState {
        val readingTags: Boolean
            get() = tagsRead < songs.size
    }

    data object Failed : ScanState
}

/** How many tags to read between two updates of the list, so the screen isn't redrawn for every file. */
private const val TAGS_PER_UPDATE = 20

/**
 * Scans [folder] in two steps: first lists the mp3 files (fast), then reads their tags one by
 * one (slow). Sends a new state after each step and every [TAGS_PER_UPDATE] songs, so the list
 * shows up quickly and fills in. Stops when the collector stops, for example on a new folder.
 */
fun scanLibrary(context: Context, folder: Uri): Flow<ScanState> = flow {
    emit(ScanState.Scanning)

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

    val songs = files.map { Song(it, tags = null) }.toMutableList()
    emit(ScanState.Done(songs.toList(), tagsRead = 0))

    for ((index, file) in files.withIndex()) {
        songs[index] = Song(file, TagReader.read(context, MusicFolder.fileUri(folder, file)))
        val read = index + 1
        if (read % TAGS_PER_UPDATE == 0 || read == files.size) {
            // A copy, so the screen gets a list that won't change under it.
            emit(ScanState.Done(songs.toList(), tagsRead = read))
        }
    }
}.flowOn(Dispatchers.IO)
