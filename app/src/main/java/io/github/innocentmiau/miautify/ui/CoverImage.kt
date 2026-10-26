package io.github.innocentmiau.miautify.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.Covers
import io.github.innocentmiau.miautify.library.SongFile

/** A square cover with rounded corners, or a music note when there is none. Decorative, so not read aloud. */
@Composable
fun CoverImage(cover: ImageBitmap?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                // Fills the square, cutting the sides of a cover that isn't square.
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The cover of [file] in [folder], loaded in the background. Starts with the one in memory,
 * if any, so scrolling back shows it at once. Loading stops if the row scrolls away first.
 */
@Composable
fun rememberCover(folder: Uri?, file: SongFile): ImageBitmap? {
    val context = LocalContext.current
    val cover by produceState(Covers.cached(file), folder, file) {
        if (value == null && folder != null) value = Covers.load(context, folder, file)
    }
    return cover
}
