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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.Cover
import io.github.innocentmiau.miautify.library.Covers
import io.github.innocentmiau.miautify.library.SongFile

/**
 * A square cover with rounded corners: a pulsing block while it loads, a music note when there
 * is none. Decorative, so not read aloud. 48 dp by default; pass [size] as Dp.Unspecified to
 * size it with [modifier].
 */
@Composable
fun CoverImage(cover: Cover, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(6.dp)
    val sized = modifier.then(if (size != Dp.Unspecified) Modifier.size(size) else Modifier)
    if (cover == Cover.Loading) {
        Placeholder(sized, shape)
        return
    }
    Box(
        modifier = sized
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (cover is Cover.Loaded) {
            Image(
                bitmap = cover.image,
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
 * The cover of [file] in [folder], loaded in the background. Starts with what is known
 * already, if anything, so scrolling back shows it at once. Loading stops if the row scrolls
 * away first.
 */
@Composable
fun rememberCover(folder: Uri?, file: SongFile): Cover {
    val context = LocalContext.current
    val cover by produceState(Covers.cached(file) ?: Cover.Loading, folder, file) {
        // Set again here: the value is kept from before when the file changes.
        value = Covers.cached(file) ?: if (folder == null) Cover.None else Covers.load(context, folder, file)
    }
    return cover
}
