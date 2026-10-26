package io.github.innocentmiau.miautify.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.formatDuration
import io.github.innocentmiau.miautify.playback.NowPlaying
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

/** The bar at the bottom: a seek bar with times, the playing song, previous, play or pause, and next. */
@Composable
fun PlayerBar(
    nowPlaying: NowPlaying,
    positionMs: Long,
    onSeek: (positionMs: Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        // Keeps the bar above the phone's navigation bar.
        Column(modifier = Modifier.navigationBarsPadding()) {
            SeekBar(
                positionMs = positionMs,
                durationMs = nowPlaying.durationMs,
                onSeek = onSeek
            )
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

/** Elapsed time, the slider, and the song's length. Disabled until the length is known. */
@Composable
private fun SeekBar(positionMs: Long, durationMs: Long?, onSeek: (Long) -> Unit) {
    // While a finger is on the slider it shows the finger's place, not the player's, and
    // only seeks once on release, so dragging doesn't seek dozens of times.
    var draggingMs by remember { mutableStateOf<Long?>(null) }
    val duration = durationMs ?: 0L
    val shownMs = (draggingMs ?: positionMs).coerceIn(0L, duration)

    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
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
                .padding(horizontal = 12.dp)
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
            nowPlaying = NowPlaying("1", "First Song", "Some Artist", isPlaying = true, durationMs = 198_000),
            positionMs = 67_000,
            onSeek = {},
            onPrevious = {},
            onPlayPause = {},
            onNext = {}
        )
    }
}
