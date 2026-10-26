package io.github.innocentmiau.miautify.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.playback.NowPlaying
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

/** The bar at the bottom: the playing song, previous, play or pause, and next. */
@Composable
fun PlayerBar(
    nowPlaying: NowPlaying,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // BottomAppBar also keeps its content above the phone's navigation bar.
    BottomAppBar(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
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

@Preview
@Composable
private fun PlayerBarPreview() {
    MiautifyTheme {
        PlayerBar(
            nowPlaying = NowPlaying("1", "First Song", "Some Artist", isPlaying = true),
            onPrevious = {},
            onPlayPause = {},
            onNext = {}
        )
    }
}
