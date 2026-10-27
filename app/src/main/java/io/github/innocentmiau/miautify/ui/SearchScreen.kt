package io.github.innocentmiau.miautify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.ScanState
import io.github.innocentmiau.miautify.library.SongSearch
import io.github.innocentmiau.miautify.library.sortSongs
import io.github.innocentmiau.miautify.playback.playSongs

/**
 * Searches the library's songs as you type. Results keep the library's sort order, and
 * tapping one plays the results from there: the list shown is the list played. The back
 * button or arrow returns to the library.
 */
@Composable
fun SearchScreen(
    player: Player?,
    playingSongId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // The same LibraryViewModel as the library screen: both belong to the activity.
    library: LibraryViewModel = viewModel()
) {
    // Saveable: the query survives a rotation, like the text in any text field should.
    var query by rememberSaveable { mutableStateOf("") }
    BackHandler(onBack = onClose)

    val allSongs = (library.scan as? ScanState.Done)?.songs.orEmpty()
    val sorted = remember(allSongs, library.sortOrder, library.sortReversed, library.playCounts) {
        sortSongs(allSongs, library.sortOrder, library.sortReversed, library.playCounts)
    }
    // Folds every song's text once, then each keystroke only compares.
    val search = remember(sorted) { SongSearch(sorted) }
    val results = remember(search, query) { search.search(query) }

    Column(modifier = modifier) {
        SearchField(query = query, onQueryChange = { query = it }, onClose = onClose)

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = stringResource(if (query.isBlank()) R.string.search_hint else R.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // imePadding: the last results can scroll up above the keyboard.
            LazyColumn(modifier = Modifier.fillMaxSize().imePadding()) {
                itemsIndexed(results, key = { _, song -> song.file.documentId }) { index, song ->
                    SongRow(
                        folder = library.folder,
                        song = song,
                        playCount = null,
                        isPlaying = song.file.documentId == playingSongId,
                        onClick = {
                            val folder = library.folder
                            if (player != null && folder != null) player.playSongs(folder, results, index)
                        }
                    )
                }
            }
        }
    }
}

/** The text field, with a back arrow and a clear button. Focused when the screen opens, so the keyboard is up. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
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
