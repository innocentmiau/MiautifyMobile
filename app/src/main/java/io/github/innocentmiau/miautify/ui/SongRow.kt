package io.github.innocentmiau.miautify.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.library.formatDuration
import io.github.innocentmiau.miautify.ui.theme.TextBright

/**
 * One song in a list: cover, title, "Artist · Album" (or its folder path) and length. Used by
 * the library and the search results.
 *
 * @param playCount Shown first in the details when given, like "12 plays".
 */
@Composable
fun SongRow(folder: Uri?, song: Song, playCount: Int?, isPlaying: Boolean, onClick: () -> Unit) {
    // "Artist · Album", or the folder path until the tags are read or when there are none.
    // Starts with "12 plays" when [playCount] is given.
    val plays = playCount?.let { pluralStringResource(R.plurals.library_play_count, it, it) }
    val details = listOfNotNull(song.tags?.artist, song.tags?.album)
        .joinToString(" · ")
        .ifEmpty { song.file.folder }
        .let { tags -> listOfNotNull(plays, tags.takeIf { it.isNotEmpty() }).joinToString(" · ") }
    val duration = song.tags?.durationMs

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        // The playing song stands out like Discord's selected channel: a lighter row and a brighter title.
        colors = if (isPlaying) {
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                headlineColor = TextBright
            )
        } else {
            ListItemDefaults.colors()
        },
        leadingContent = { CoverImage(rememberCover(folder, song.file)) },
        headlineContent = {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = details.takeIf { it.isNotEmpty() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = duration?.let {
            { Text(formatDuration(it), style = MaterialTheme.typography.bodySmall) }
        }
    )
}
