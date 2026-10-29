package io.github.innocentmiau.miautify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.GroupKind
import io.github.innocentmiau.miautify.library.ScanState
import io.github.innocentmiau.miautify.library.isBroken
import io.github.innocentmiau.miautify.library.durationMs
import io.github.innocentmiau.miautify.library.groupSongs
import io.github.innocentmiau.miautify.library.hoursAndMinutes
import io.github.innocentmiau.miautify.library.playCount
import io.github.innocentmiau.miautify.playback.playSongs

/**
 * One album, artist or folder opened from search: only its songs, in its own order (track
 * order for an album). Playing from here keeps the queue, and so shuffle and repeat, inside it.
 * The group is found again by [id] in the current scan, so it stays right after a rescan.
 */
@Composable
fun GroupScreen(
    kind: GroupKind,
    id: String,
    player: Player?,
    playingSongId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    library: LibraryViewModel = viewModel()
) {
    BackHandler(onBack = onBack)
    val allSongs = (library.scan as? ScanState.Done)?.songs.orEmpty()
    val group = remember(allSongs, kind, id) { groupSongs(allSongs, kind).find { it.id == id } }

    // Read again whenever the song changes, so the plays count up while you listen here: the
    // song before has had its chance to reach 30 seconds by then.
    LaunchedEffect(playingSongId) { library.loadPlayCounts() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
            }
            if (group != null) {
                CoverImage(rememberCover(library.folder, group.songs.first().file), size = 64.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = groupTitle(group, library.folderName),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val detail = if (kind == GroupKind.ALBUM) {
                        group.detail ?: stringResource(R.string.various_artists)
                    } else {
                        group.detail
                    }
                    if (detail != null) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // "12 songs · 47 min · 23 plays". The length is left out when no song's is known.
                    val songs = pluralStringResource(R.plurals.library_song_count, group.songs.size, group.songs.size)
                    val (hours, minutes) = hoursAndMinutes(group.durationMs)
                    val length = when {
                        group.durationMs == 0L -> null
                        hours > 0 -> stringResource(R.string.length_hours, hours, minutes)
                        else -> stringResource(R.string.length_minutes, minutes)
                    }
                    val playCount = group.playCount(library.playCounts)
                    val plays = pluralStringResource(R.plurals.library_play_count, playCount, playCount)
                    Text(
                        text = listOfNotNull(songs, length, plays).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (group == null) {
            // Its songs were removed or retagged since it was opened.
            Text(
                text = stringResource(R.string.group_gone),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(group.songs, key = { _, song -> song.file.documentId }) { index, song ->
                SongRow(
                    folder = library.folder,
                    song = song,
                    playCount = null,
                    isPlaying = song.file.documentId == playingSongId,
                    broken = song.isBroken(library.brokenFiles),
                    onClick = {
                        val folder = library.folder
                        if (player != null && folder != null) player.playSongs(folder, group.songs, index)
                    }
                )
            }
        }
    }
}
