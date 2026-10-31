package io.github.innocentmiau.miautify.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.Cover
import io.github.innocentmiau.miautify.library.GroupKind
import io.github.innocentmiau.miautify.library.SongGroup
import io.github.innocentmiau.miautify.library.formatDuration
import io.github.innocentmiau.miautify.playback.NowPlaying
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

/**
 * The bar at the bottom: shuffle, the seek bar with times and repeat on top, then the playing
 * song with previous, play or pause, and next. Tapping the song opens a menu to go to the
 * album, artist or folder it is in ([groups]); with none, it can't be tapped.
 */
@Composable
fun PlayerBar(
    nowPlaying: NowPlaying,
    cover: Cover,
    positionMs: Long,
    onSeek: (positionMs: Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onShuffle: (enabled: Boolean) -> Unit,
    onRepeat: () -> Unit,
    groups: List<Pair<GroupKind, SongGroup>>,
    folderName: String?,
    onOpenGroup: (kind: GroupKind, id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        // Keeps the bar above the phone's navigation bar.
        Column(modifier = Modifier.navigationBarsPadding()) {
            // Shuffle and repeat sit at the ends of the seek bar's row, so the row below
            // leaves the song's title room to be read.
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A toggle button: tinted in the accent color when on, and read out as on or off.
                IconToggleButton(checked = nowPlaying.shuffle, onCheckedChange = onShuffle) {
                    Icon(painterResource(R.drawable.ic_shuffle), stringResource(R.string.player_shuffle))
                }
                SeekBar(
                    positionMs = positionMs,
                    durationMs = nowPlaying.durationMs,
                    onSeek = onSeek,
                    modifier = Modifier.weight(1f)
                )
                RepeatButton(repeatMode = nowPlaying.repeatMode, onClick = onRepeat)
            }
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The menu is placed from this box, so it lines up with the song.
                Box(modifier = Modifier.weight(1f)) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.clickable(
                            enabled = groups.isNotEmpty(),
                            onClickLabel = stringResource(R.string.player_song_menu),
                            onClick = { menuOpen = true }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverImage(cover)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = nowPlaying.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            nowPlaying.artist?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Opens upwards on its own: it goes where it fits, and the bar is at the bottom.
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        for ((kind, group) in groups) {
                            GoToItem(kind, group, folderName) {
                                menuOpen = false
                                onOpenGroup(kind, group.id)
                            }
                        }
                    }
                }
                IconButton(onClick = onPrevious) {
                    Icon(painterResource(R.drawable.ic_skip_previous), stringResource(R.string.player_previous))
                }
                FilledIconButton(onClick = onPlayPause) {
                    if (nowPlaying.isPlaying) {
                        Icon(painterResource(R.drawable.ic_pause), stringResource(R.string.player_pause))
                    } else {
                        Icon(painterResource(R.drawable.ic_play), stringResource(R.string.player_play))
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.ic_skip_next), stringResource(R.string.player_next))
                }
            }
        }
    }
}

/** "Go to album" and the like, with the group's name below. */
@Composable
private fun GoToItem(kind: GroupKind, group: SongGroup, folderName: String?, onClick: () -> Unit) {
    val (icon, label) = when (kind) {
        GroupKind.ALBUM -> R.drawable.ic_album to R.string.player_go_to_album
        GroupKind.ARTIST -> R.drawable.ic_person to R.string.player_go_to_artist
        GroupKind.FOLDER -> R.drawable.ic_folder to R.string.player_go_to_folder
    }
    DropdownMenuItem(
        text = {
            Column {
                Text(stringResource(label))
                Text(
                    text = groupTitle(group, folderName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
        onClick = onClick
    )
}

/** Three states, so not a toggle: a plain button whose icon, color and description show the mode. */
@Composable
private fun RepeatButton(repeatMode: Int, onClick: () -> Unit) {
    val (icon, description) = when (repeatMode) {
        Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat to R.string.player_repeat_all
        Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one to R.string.player_repeat_one
        else -> R.drawable.ic_repeat to R.string.player_repeat_off
    }
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = if (repeatMode == Player.REPEAT_MODE_OFF) LocalContentColor.current else MaterialTheme.colorScheme.primary
        )
    }
}

/** Elapsed time, the slider, and the song's length. Disabled until the length is known. */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long?, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    // While a finger is on the slider it shows the finger's place, not the player's, and
    // only seeks once on release, so dragging doesn't seek dozens of times.
    var draggingMs by remember { mutableStateOf<Long?>(null) }
    val duration = durationMs ?: 0L
    val shownMs = (draggingMs ?: positionMs).coerceIn(0L, duration)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDuration(shownMs),
            style = MaterialTheme.typography.labelMedium
        )
        Slider(
            value = shownMs.toFloat(),
            onValueChange = { draggingMs = it.toLong() },
            onValueChangeFinished = {
                draggingMs?.let(onSeek)
                draggingMs = null
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            enabled = duration > 0,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = durationMs?.let(::formatDuration).orEmpty(),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Preview
@Composable
private fun PlayerBarPreview() {
    MiautifyTheme {
        PlayerBar(
            nowPlaying = NowPlaying(
                "1", "First Song", "Some Artist", isPlaying = true, durationMs = 198_000,
                shuffle = true, repeatMode = Player.REPEAT_MODE_ONE
            ),
            cover = Cover.None,
            positionMs = 67_000,
            onSeek = {},
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
            onShuffle = {},
            onRepeat = {},
            groups = emptyList(),
            folderName = null,
            onOpenGroup = { _, _ -> }
        )
    }
}
