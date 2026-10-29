package io.github.innocentmiau.miautify.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.GroupKind
import io.github.innocentmiau.miautify.library.ScanState
import io.github.innocentmiau.miautify.library.isBroken
import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.library.SongGroup
import io.github.innocentmiau.miautify.library.SongSearch
import io.github.innocentmiau.miautify.library.groupSongs
import io.github.innocentmiau.miautify.library.searchGroups
import io.github.innocentmiau.miautify.library.sortGroups
import io.github.innocentmiau.miautify.library.sortSongs
import io.github.innocentmiau.miautify.playback.playSongs

/** What the search looks through: songs, or groups of songs (null for songs). */
// The chips show in this order.
enum class SearchTab(val kind: GroupKind?, val label: Int, val placeholder: Int) {
    ALBUMS(GroupKind.ALBUM, R.string.search_tab_albums, R.string.search_placeholder_albums),
    ARTISTS(GroupKind.ARTIST, R.string.search_tab_artists, R.string.search_placeholder_artists),
    FOLDERS(GroupKind.FOLDER, R.string.search_tab_folders, R.string.search_placeholder_folders),
    SONGS(null, R.string.search_tab_songs, R.string.search_placeholder_songs)
}

/**
 * Browses the library: albums, artists or folders, picked with the chips, all of them shown
 * right away. The field at the top narrows down the chosen chip as you type; the Songs chip
 * searches songs. Tapping a song plays the results from there; tapping a group opens it.
 */
@Composable
fun SearchScreen(
    player: Player?,
    playingSongId: String?,
    onOpenGroup: (GroupKind, id: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // The same LibraryViewModel as the library screen: both belong to the activity.
    library: LibraryViewModel = viewModel()
) {
    val close = {
        library.clearSearch()
        onClose()
    }
    BackHandler(onBack = close)

    val tab = library.searchTab
    val query = library.searchQuery
    val allSongs = (library.scan as? ScanState.Done)?.songs.orEmpty()

    // Fresh play counts and times each time browsing opens, so they include what was just heard.
    LaunchedEffect(Unit) { library.loadPlayCounts() }

    Column(modifier = modifier) {
        SearchField(query, stringResource(tab.placeholder), onQueryChange = library::search, onClose = close)
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sorts what the chip shows: groups have their own orders, songs use the song list's.
            if (tab.kind == null) {
                SortMenu(library.sortOrder, library.sortReversed, library::sortBy)
            } else {
                GroupSortMenu(library.groupOrder, library::sortGroupsBy)
            }
            TabChips(tab, onSelect = library::showSearchTab)
        }

        // Each chip keeps its own scroll position, so switching from artists to folders and
        // back returns to the same artist, and two lists never share a position.
        val chips = rememberSaveableStateHolder()
        chips.SaveableStateProvider(tab.name) {
            val kind = tab.kind
            if (kind == null) {
                SongResults(allSongs, query, library, player, playingSongId)
            } else {
                // Grouped and sorted once per scan update or sort change, not on every keystroke.
                val groups = remember(allSongs, kind, library.groupOrder, library.playCounts, library.lastPlayed) {
                    sortGroups(groupSongs(allSongs, kind), library.groupOrder, library.playCounts, library.lastPlayed)
                }
                val results = remember(groups, query) { searchGroups(groups, query) }
                when {
                    results.isEmpty() -> Hint(stringResource(R.string.search_nothing))
                    kind == GroupKind.ALBUM -> AlbumGrid(results, library.folder) { onOpenGroup(kind, it.id) }
                    else -> GroupList(results, library.folder, library.folderName, kind) { onOpenGroup(kind, it.id) }
                }
            }
        }
    }
}

@Composable
private fun SongResults(
    allSongs: List<Song>,
    query: String,
    library: LibraryViewModel,
    player: Player?,
    playingSongId: String?
) {
    val sorted = remember(allSongs, library.sortOrder, library.sortReversed, library.playCounts) {
        sortSongs(allSongs, library.sortOrder, library.sortReversed, library.playCounts)
    }
    // Folds every song's text once, then each keystroke only compares.
    val search = remember(sorted) { SongSearch(sorted) }
    val results = remember(search, query) { search.search(query) }

    if (results.isEmpty()) {
        Hint(stringResource(if (query.isBlank()) R.string.search_hint else R.string.search_no_results))
        return
    }
    // imePadding: the last results can scroll up above the keyboard.
    LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
        itemsIndexed(results, key = { _, song -> song.file.documentId }) { index, song ->
            SongRow(
                folder = library.folder,
                song = song,
                playCount = null,
                isPlaying = song.file.documentId == playingSongId,
                    broken = song.isBroken(library.brokenFiles),
                onClick = {
                    val folder = library.folder
                    if (player != null && folder != null) player.playSongs(folder, results, index)
                }
            )
        }
    }
}

/** Albums as a grid of covers: as many columns as fit, each at least 110 dp wide. */
@Composable
private fun AlbumGrid(albums: List<SongGroup>, folder: Uri?, onOpen: (SongGroup) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            Column(modifier = Modifier.clickable { onOpen(album) }) {
                CoverImage(
                    cover = rememberCover(folder, album.songs.first().file),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    size = Dp.Unspecified
                )
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = album.detail ?: stringResource(R.string.various_artists),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Artists or folders as rows: a cover from their first song, the name, and the song count. */
@Composable
private fun GroupList(
    groups: List<SongGroup>,
    folder: Uri?,
    folderName: String?,
    kind: GroupKind,
    onOpen: (SongGroup) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
        items(groups, key = { it.id }) { group ->
            val count = pluralStringResource(R.plurals.library_song_count, group.songs.size, group.songs.size)
            ListItem(
                modifier = Modifier.clickable { onOpen(group) },
                leadingContent = { CoverImage(rememberCover(folder, group.songs.first().file)) },
                headlineContent = {
                    Text(groupTitle(group, folderName), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    // A folder shows where it is: "Rock · 12 songs".
                    val details = if (kind == GroupKind.FOLDER) listOfNotNull(group.detail, count) else listOf(count)
                    Text(details.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            )
        }
    }
}

/** A group's name, with the music folder's own name for its top level (whose path is ""). */
fun groupTitle(group: SongGroup, folderName: String?): String =
    group.name.ifEmpty { folderName.orEmpty() }

@Composable
private fun TabChips(selected: SearchTab, onSelect: (SearchTab) -> Unit) {
    // Scrolls sideways if a translation makes the chips wider than the screen.
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (tab in SearchTab.entries) {
            FilterChip(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(stringResource(tab.label)) }
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** The text field, with a back arrow and a clear button. The keyboard only opens when it is tapped. */
@Composable
private fun SearchField(query: String, placeholder: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        // No underline: the field is the screen's top bar.
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
