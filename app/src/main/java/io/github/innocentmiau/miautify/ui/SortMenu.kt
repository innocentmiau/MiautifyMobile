package io.github.innocentmiau.miautify.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.SortOrder

/** How the list is sorted, with the play counts that "most played" needs. */
data class Sorting(
    val order: SortOrder = SortOrder.FOLDER,
    val reversed: Boolean = false,
    val playCounts: Map<String, Int> = emptyMap()
)

/** The menu's label for each order. */
private val SortOrder.label: Int
    get() = when (this) {
        SortOrder.FOLDER -> R.string.sort_folder
        SortOrder.TITLE -> R.string.sort_title
        SortOrder.ARTIST -> R.string.sort_artist
        SortOrder.ALBUM -> R.string.sort_album
        SortOrder.MOST_PLAYED -> R.string.sort_most_played
    }

/** A sort button that opens the list of orders, with a check on the current one, and "Reverse order". */
@Composable
fun SortMenu(order: SortOrder, reversed: Boolean, onSort: (SortOrder, reversed: Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }

    // The Box is the anchor: the menu opens next to the button inside it.
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_sort), stringResource(R.string.sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in SortOrder.entries) {
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    onClick = {
                        onSort(option, reversed)
                        open = false
                    },
                    leadingIcon = { CheckMark(visible = option == order) }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_reverse)) },
                onClick = {
                    onSort(order, !reversed)
                    open = false
                },
                leadingIcon = { CheckMark(visible = reversed) }
            )
        }
    }
}

/** A check, or an empty space of the same size so the labels stay lined up. */
@Composable
private fun CheckMark(visible: Boolean) {
    if (visible) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null)
    } else {
        Box {}
    }
}
