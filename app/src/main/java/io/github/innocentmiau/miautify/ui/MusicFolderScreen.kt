package io.github.innocentmiau.miautify.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.BrokenFile
import io.github.innocentmiau.miautify.library.ScanState
import io.github.innocentmiau.miautify.library.isBroken
import io.github.innocentmiau.miautify.library.SortOrder
import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.library.SongFile
import io.github.innocentmiau.miautify.library.SongTags
import io.github.innocentmiau.miautify.library.formatDuration
import io.github.innocentmiau.miautify.playback.playSongs
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme
import io.github.innocentmiau.miautify.ui.theme.TextBright

/**
 * Shows the songs in the chosen music folder and lets the user pick one, remembered between
 * launches. Tapping a song plays the whole list from there on [player].
 */
@Composable
fun MusicFolderScreen(
    player: Player?,
    playingSongId: String?,
    onBrowse: () -> Unit,
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
        player = player,
        folder = library.folder,
        folderName = library.folderName,
        scan = library.scan,
        sorting = Sorting(library.sortOrder, library.sortReversed, library.playCounts),
        sortedSongs = library.lists?.songs,
        brokenFiles = library.brokenFiles,
        playingSongId = playingSongId,
        refreshing = library.refreshing,
        actions = LibraryActions(
            sort = library::sortBy,
            browse = onBrowse,
            // Opens the picker inside the current folder, so changing it is quicker.
            chooseFolder = { picker.launch(library.folder) },
            refresh = library::refresh,
            playSong = { songs, index ->
                val folder = library.folder
                if (player != null && folder != null) player.playSongs(folder, songs, index)
            }
        ),
        modifier = modifier
    )
}

/** What the library screen's buttons and rows do, in one value instead of five parameters. */
private data class LibraryActions(
    val sort: (SortOrder, reversed: Boolean) -> Unit = { _, _ -> },
    val browse: () -> Unit = {},
    val chooseFolder: () -> Unit = {},
    val refresh: () -> Unit = {},
    val playSong: (songs: List<Song>, index: Int) -> Unit = { _, _ -> }
)

/**
 * [scan] is null when no folder was chosen yet, or for a moment at launch until the cached list
 * is read. [sortedSongs] are its songs in [sorting], null while they are sorted.
 */
@Composable
private fun MusicFolderContent(
    player: Player?,
    folder: Uri?,
    folderName: String?,
    scan: ScanState?,
    sorting: Sorting,
    sortedSongs: List<Song>?,
    brokenFiles: Map<String, BrokenFile>,
    playingSongId: String?,
    refreshing: Boolean,
    actions: LibraryActions,
    modifier: Modifier = Modifier
) {
    when (scan) {
        // With a folder, the cached list is on its way: a loading screen instead of "Nothing
        // to play yet", which would be wrong for the half second it shows.
        null -> if (folder != null) {
            Loading(modifier)
        } else {
            Message(stringResource(R.string.library_empty), actions.chooseFolder, modifier = modifier)
        }
        ScanState.Failed -> Message(stringResource(R.string.library_scan_failed), actions.chooseFolder, modifier = modifier)
        ScanState.Scanning -> Scanning(modifier)
        is ScanState.Done ->
            if (scan.songs.isEmpty()) {
                // No list to pull down here, so the message gets its own Refresh button.
                Message(stringResource(R.string.library_no_songs), actions.chooseFolder, modifier, onRefresh = actions.refresh)
            } else {
                SongList(player, folder, folderName, scan, sorting, sortedSongs, brokenFiles, playingSongId, refreshing, actions, modifier)
            }
    }
}

@Composable
private fun Message(
    text: String,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null
) {
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
        if (onRefresh != null) {
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.library_refresh))
            }
        }
    }
}

/** The app icon over a progress bar, while the list is read at launch. */
@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The launcher icon is two layers, which Compose can't draw as one: stacked here,
        // cut into a circle like on the home screen. Decorative, so not read aloud.
        Box(modifier = Modifier.size(96.dp).clip(CircleShape)) {
            Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        LinearProgressIndicator(modifier = Modifier.width(120.dp))
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
    player: Player?,
    folder: Uri?,
    folderName: String?,
    scan: ScanState.Done,
    sorting: Sorting,
    sortedSongs: List<Song>?,
    brokenFiles: Map<String, BrokenFile>,
    playingSongId: String?,
    refreshing: Boolean,
    actions: LibraryActions,
    modifier: Modifier = Modifier
) {
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
                        else -> pluralStringResource(R.plurals.library_song_count, scan.songs.size, scan.songs.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // The whole library, in the order shown, after the playing song.
            if (sortedSongs != null) {
                ContinueQueueButton(player, playingSongId, folder, sortedSongs, folderName.orEmpty())
            }
            IconButton(onClick = actions.browse) {
                Icon(painterResource(R.drawable.ic_library_music), stringResource(R.string.browse))
            }
            SortMenu(sorting.order, sorting.reversed, actions.sort)
            TextButton(onClick = actions.chooseFolder) {
                Text(stringResource(R.string.library_change_folder))
            }
        }

        // Pull the list down from the top to look for new, changed or removed files.
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = actions.refresh, modifier = Modifier.fillMaxSize()) {
            // The songs play in the order shown: the list shown is the list played.
            val songs = sortedSongs
            if (songs == null) {
                // Being sorted in the background, a moment for a big library.
                SongRowsPlaceholder()
            } else {
                // Only the rows on screen are built, so a library of thousands of songs scrolls smoothly.
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(songs, key = { _, song -> song.file.documentId }) { index, song ->
                        SongRow(
                            folder = folder,
                            song = song,
                            // Only shown when sorted by it, where it explains the order.
                            playCount = if (sorting.order == SortOrder.MOST_PLAYED) {
                                sorting.playCounts[song.file.documentId] ?: 0
                            } else {
                                null
                            },
                            isPlaying = song.file.documentId == playingSongId,
                            broken = song.isBroken(brokenFiles),
                            onClick = { actions.playSong(songs, index) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoFolderPreview() {
    MiautifyTheme {
        MusicFolderContent(player = null, folder = null, folderName = null, scan = null, sorting = Sorting(), sortedSongs = null, brokenFiles = emptyMap(), playingSongId = null, refreshing = false, actions = LibraryActions(), modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() {
    MiautifyTheme {
        MusicFolderContent(player = null, folder = Uri.EMPTY, folderName = null, scan = null, sorting = Sorting(), sortedSongs = null, brokenFiles = emptyMap(), playingSongId = null, refreshing = false, actions = LibraryActions(), modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningPreview() {
    MiautifyTheme {
        MusicFolderContent(player = null, folder = null, folderName = "Music", scan = ScanState.Scanning, sorting = Sorting(), sortedSongs = null, brokenFiles = emptyMap(), playingSongId = null, refreshing = false, actions = LibraryActions(), modifier = Modifier.fillMaxSize())
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
            player = null,
            folder = null,
            folderName = "Music",
            scan = ScanState.Done(songs, tagsToRead = 3, tagsRead = 2),
            sorting = Sorting(),
            sortedSongs = songs,
            brokenFiles = emptyMap(),
            playingSongId = "2",
            refreshing = false,
            actions = LibraryActions(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
