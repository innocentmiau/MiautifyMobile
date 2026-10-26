package io.github.innocentmiau.miautify.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.MusicFolder
import io.github.innocentmiau.miautify.library.SongFile
import io.github.innocentmiau.miautify.settings.Settings
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme
import kotlin.coroutines.cancellation.CancellationException

/** Where the scan of the music folder is. */
private sealed interface ScanState {
    data object Scanning : ScanState
    data class Done(val songs: List<SongFile>) : ScanState
    data object Failed : ScanState
}

/** Shows the songs in the chosen music folder and lets the user pick one, remembered between launches. */
@Composable
fun MusicFolderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var folder by remember {
        mutableStateOf(settings.musicFolder?.takeIf { MusicFolder.hasAccess(context, it) })
    }
    val folderName = remember(folder) { folder?.let { MusicFolder.displayName(context, it) } }

    // Starts a new scan whenever the folder changes, and cancels the old one.
    val scan by produceState<ScanState?>(initialValue = null, folder) {
        val current = folder ?: return@produceState
        value = ScanState.Scanning
        value = try {
            ScanState.Done(MusicFolder.scan(context, current))
        } catch (e: CancellationException) {
            // Not a failure: the scan is no longer needed. Let it stop.
            throw e
        } catch (_: Exception) {
            ScanState.Failed
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        // Null when the user backs out of the picker: keep the current folder.
        if (picked != null) {
            MusicFolder.keepAccess(context, picked, previous = folder)
            settings.musicFolder = picked
            folder = picked
        }
    }

    MusicFolderContent(
        folderName = folderName,
        scan = scan,
        // Opens the picker inside the current folder, so changing it is quicker.
        onChooseFolder = { picker.launch(folder) },
        modifier = modifier
    )
}

/** [scan] is null when no folder was chosen yet. */
@Composable
private fun MusicFolderContent(
    folderName: String?,
    scan: ScanState?,
    onChooseFolder: () -> Unit,
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
                SongList(folderName, scan.songs, onChooseFolder, modifier)
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
    folderName: String?,
    songs: List<SongFile>,
    onChooseFolder: () -> Unit,
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
                    text = pluralStringResource(R.plurals.library_song_count, songs.size, songs.size),
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
            items(songs, key = { it.documentId }) { song ->
                ListItem(
                    headlineContent = {
                        Text(song.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = song.folder.takeIf { it.isNotEmpty() }?.let { folder ->
                        { Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoFolderPreview() {
    MiautifyTheme {
        MusicFolderContent(folderName = null, scan = null, onChooseFolder = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningPreview() {
    MiautifyTheme {
        MusicFolderContent(folderName = "Music", scan = ScanState.Scanning, onChooseFolder = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun SongListPreview() {
    val songs = listOf(
        SongFile("1", "Intro.mp3", ""),
        SongFile("2", "First Song.mp3", "Some Album"),
        SongFile("3", "Second Song.mp3", "Some Album/Disc 2")
    )
    MiautifyTheme {
        MusicFolderContent(folderName = "Music", scan = ScanState.Done(songs), onChooseFolder = {}, modifier = Modifier.fillMaxSize())
    }
}
