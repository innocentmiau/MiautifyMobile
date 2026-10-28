package io.github.innocentmiau.miautify.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
 * @param broken The file failed to play: a red title, and "Can't play this file" first in the details.
 */
@Composable
fun SongRow(folder: Uri?, song: Song, playCount: Int?, isPlaying: Boolean, broken: Boolean, onClick: () -> Unit) {
    // "Artist · Album", or the folder path until the tags are read or when there are none.
    // Starts with "Can't play this file" or "12 plays" when those apply.
    val warning = if (broken) stringResource(R.string.song_broken) else null
    val plays = playCount?.let { pluralStringResource(R.plurals.library_play_count, it, it) }
    val details = listOfNotNull(song.tags?.artist, song.tags?.album)
        .joinToString(" · ")
        .ifEmpty { song.file.folder }
        .let { tags -> listOfNotNull(warning, plays, tags.takeIf { it.isNotEmpty() }).joinToString(" · ") }
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
            Text(
                text = song.title,
                color = if (broken) MaterialTheme.colorScheme.error else Color.Unspecified,
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
