package io.github.innocentmiau.miautify.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.ScanState
import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.library.SongFile
import io.github.innocentmiau.miautify.library.SongTags
import io.github.innocentmiau.miautify.library.formatDuration
import io.github.innocentmiau.miautify.playback.playSongs
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

/**
 * Shows the songs in the chosen music folder and lets the user pick one, remembered between
 * launches. Tapping a song plays the whole list from there on [player].
 */
@Composable
fun MusicFolderScreen(
    player: Player?,
    playingSongId: String?,
    modifier: Modifier = Modifier,
    // Gives the same LibraryViewModel back after a rotation, instead of a new one.
    library: LibraryViewModel = viewModel()
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        // Null when the user backs out of the picker: keep the current folder.
        if (picked != null) {
            if (picked != library.folder) {
                // The queue holds songs from the old folder, which the app can't read once
                // chooseFolder gives that folder back, so the next song would fail. Clearing
                // it also stops playback and deletes the saved queue.
                player?.clearMediaItems()
            }
            library.chooseFolder(picked)
        }
    }

    MusicFolderContent(
        folder = library.folder,
        folderName = library.folderName,
        scan = library.scan,
        playingSongId = playingSongId,
        // Opens the picker inside the current folder, so changing it is quicker.
        onChooseFolder = { picker.launch(library.folder) },
        onPlaySong = { songs, index ->
            val folder = library.folder
            if (player != null && folder != null) player.playSongs(folder, songs, index)
        },
        modifier = modifier
    )
}

/** [scan] is null when no folder was chosen yet. */
@Composable
private fun MusicFolderContent(
    folder: Uri?,
    folderName: String?,
    scan: ScanState?,
    playingSongId: String?,
    onChooseFolder: () -> Unit,
    onPlaySong: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (scan) {
        null -> Message(stringResource(R.string.library_empty), onChooseFolder, modifier)
        ScanState.Failed -> Message(stringResource(R.string.library_scan_failed), onChooseFolder, modifier)
        ScanState.Scanning -> Scanning(modifier)
        is ScanState.Done ->
            if (scan.songs.isEmpty()) {
                Message(stringResource(R.string.library_no_songs), onChooseFolder, modifier)
            } else {
                SongList(folder, folderName, scan, playingSongId, onChooseFolder, onPlaySong, modifier)
            }
    }
}

@Composable
private fun Message(text: String, onChooseFolder: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onChooseFolder) {
            Text(stringResource(R.string.library_choose_folder))
        }
    }
}

@Composable
private fun Scanning(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.library_scanning))
    }
}

@Composable
private fun SongList(
    folder: Uri?,
    folderName: String?,
    scan: ScanState.Done,
    playingSongId: String?,
    onChooseFolder: () -> Unit,
    onPlaySong: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs = scan.songs
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        scan.checking -> stringResource(R.string.library_checking)
                        scan.readingTags -> stringResource(R.string.library_reading_tags, scan.tagsRead, scan.tagsToRead)
                        else -> pluralStringResource(R.plurals.library_song_count, songs.size, songs.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onChooseFolder) {
                Text(stringResource(R.string.library_change_folder))
            }
        }

        // Only the rows on screen are built, so a library of thousands of songs scrolls smoothly.
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(songs, key = { _, song -> song.file.documentId }) { index, song ->
                SongRow(
                    folder = folder,
                    song = song,
                    isPlaying = song.file.documentId == playingSongId,
                    onClick = { onPlaySong(songs, index) }
                )
            }
        }
    }
}

@Composable
private fun SongRow(folder: Uri?, song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    // "Artist · Album", or the folder path until the tags are read or when there are none.
    val details = listOfNotNull(song.tags?.artist, song.tags?.album)
        .joinToString(" · ")
        .ifEmpty { song.file.folder }
    val duration = song.tags?.durationMs

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { CoverImage(rememberCover(folder, song.file)) },
        headlineContent = {
            Text(
                text = song.title,
                // The playing song stands out in the theme's accent color.
                color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = details.takeIf { it.isNotEmpty() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = duration?.let {
            { Text(formatDuration(it), style = MaterialTheme.typography.bodySmall) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun NoFolderPreview() {
    MiautifyTheme {
        MusicFolderContent(folder = null, folderName = null, scan = null, playingSongId = null, onChooseFolder = {}, onPlaySong = { _, _ -> }, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningPreview() {
    MiautifyTheme {
        MusicFolderContent(folder = null, folderName = "Music", scan = ScanState.Scanning, playingSongId = null, onChooseFolder = {}, onPlaySong = { _, _ -> }, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun SongListPreview() {
    val songs = listOf(
        Song(SongFile("1", "01 intro.mp3", "Some Album"), SongTags("Intro", "Some Artist", "Some Album", 95_000)),
        Song(SongFile("2", "02 first.mp3", "Some Album"), SongTags("First Song", "Some Artist", "Some Album", 241_000)),
        Song(SongFile("3", "Untagged Song.mp3", "Some Album/Disc 2"), tags = null)
    )
    MiautifyTheme {
        MusicFolderContent(
            folder = null,
            folderName = "Music",
            scan = ScanState.Done(songs, tagsToRead = 3, tagsRead = 2),
            playingSongId = "2",
            onChooseFolder = {},
            onPlaySong = { _, _ -> },
            modifier = Modifier.fillMaxSize()
        )
    }
}
