package io.github.innocentmiau.miautify.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.Song
import io.github.innocentmiau.miautify.playback.continueWith

/**
 * Makes [songs] the rest of the queue while the playing song plays on, and says so with a short
 * message naming [listName]. Shows nothing while nothing plays: there is nothing to continue.
 */
@Composable
fun ContinueQueueButton(player: Player?, playingSongId: String?, folder: Uri?, songs: List<Song>, listName: String) {
    if (player == null || playingSongId == null || folder == null) return
    val context = LocalContext.current
    val message = stringResource(R.string.continue_queue_done, listName)
    IconButton(
        onClick = {
            player.continueWith(folder, songs)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    ) {
        Icon(painterResource(R.drawable.ic_playlist_play), stringResource(R.string.continue_queue))
    }
}
