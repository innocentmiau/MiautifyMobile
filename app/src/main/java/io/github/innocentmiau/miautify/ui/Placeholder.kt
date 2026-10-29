package io.github.innocentmiau.miautify.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A block that pulses while what goes there loads: the shape of a cover or a line of text.
 * The pulse is drawn by the main thread, so a block that stops pulsing means the app is stuck,
 * and one that keeps pulsing means that one thing never arrived.
 */
@Composable
fun Placeholder(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(4.dp)) {
    val transition = rememberInfiniteTransition(label = "placeholder")
    val alpha = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "placeholder alpha"
    )
    Box(
        modifier = modifier
            // Read while drawing only, so each step of the pulse redraws the block without
            // running any of the screen's code again.
            .graphicsLayer { this.alpha = alpha.value }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

/** Rows shaped like song rows, a cover and two lines, while a list is made. */
@Composable
fun SongRowsPlaceholder(modifier: Modifier = Modifier) {
    // More rows than any screen fits; the ones below the edge are cut off.
    Column(modifier = modifier.fillMaxSize()) {
        repeat(12) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Placeholder(Modifier.size(48.dp), RoundedCornerShape(6.dp))
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Placeholder(Modifier.fillMaxWidth(0.6f).height(14.dp))
                    Placeholder(Modifier.fillMaxWidth(0.4f).height(12.dp))
                }
            }
        }
    }
}

/** Tiles shaped like the album grid's, a cover and two lines, while it is made. */
@Composable
fun AlbumGridPlaceholder(modifier: Modifier = Modifier) {
    // The same columns and spacing as the real grid, so nothing jumps when it arrives.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(12) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Placeholder(Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(6.dp))
                Placeholder(Modifier.fillMaxWidth(0.8f).height(14.dp))
                Placeholder(Modifier.fillMaxWidth(0.5f).height(12.dp))
            }
        }
    }
}
