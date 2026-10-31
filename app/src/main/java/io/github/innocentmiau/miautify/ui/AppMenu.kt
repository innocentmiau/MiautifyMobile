package io.github.innocentmiau.miautify.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

/**
 * The gear in the library's header: things done now and then, kept out of the header so it
 * stays tidy. Settings go here too once there are some.
 */
@Composable
fun AppMenu(onChooseFolder: () -> Unit, onRefresh: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    // The box anchors the menu to the gear.
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(R.drawable.ic_folder, R.string.library_change_folder) {
                open = false
                onChooseFolder()
            }
            MenuItem(R.drawable.ic_refresh, R.string.library_refresh) {
                open = false
                onRefresh()
            }
        }
    }
}

@Composable
private fun MenuItem(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
        onClick = onClick
    )
}
