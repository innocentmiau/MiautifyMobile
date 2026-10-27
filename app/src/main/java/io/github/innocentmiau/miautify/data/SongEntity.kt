package io.github.innocentmiau.miautify.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import io.github.innocentmiau.miautify.library.CachedSong
import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.library.SongFile
import io.github.innocentmiau.miautify.library.SongTags
import io.github.innocentmiau.miautify.library.TagReader

/**
 * One row of the scan cache: a song file and its tags. Rows are per music folder, so switching
 * back to a folder scanned before is quick too.
 */
@Entity(tableName = "songs", primaryKeys = ["folder", "documentId"])
data class SongEntity(
    /** The picked music folder's URI, as text. */
    val folder: String,
    val documentId: String,
    val name: String,
    /** The path of the file's folder inside the music folder, like "Album/Disc 1". */
    val path: String,
    val size: Long,
    val lastModified: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    /** Added in database version 2. Null in rows from version 1 until the next scan fills it. */
    val folderCoverId: String? = null,
    // Added in database version 4, with the TagReader version that read the tags. Rows from
    // before get 1, so the next scan reads their tags again and fills these in.
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    @ColumnInfo(defaultValue = "1")
    val tagsVersion: Int = TagReader.VERSION
) {
    fun toSong() = Song(SongFile(documentId, name, path, size, lastModified, folderCoverId), toTags())

    fun toCachedSong() = CachedSong(size, lastModified, toTags(), folderCoverId, tagsVersion)

    private fun toTags() = SongTags(title, artist, album, durationMs, albumArtist, trackNumber, discNumber)
}

/** The row for [song] in [folder]. A file whose tags couldn't be read is saved with empty tags, so it isn't read again until it changes. */
fun SongEntity(folder: String, song: Song) = SongEntity(
    folder = folder,
    documentId = song.file.documentId,
    name = song.file.name,
    path = song.file.folder,
    size = song.file.size,
    lastModified = song.file.lastModified,
    title = song.tags?.title,
    artist = song.tags?.artist,
    album = song.tags?.album,
    durationMs = song.tags?.durationMs,
    folderCoverId = song.file.folderCoverId,
    albumArtist = song.tags?.albumArtist,
    trackNumber = song.tags?.trackNumber,
    discNumber = song.tags?.discNumber,
    tagsVersion = TagReader.VERSION
)
