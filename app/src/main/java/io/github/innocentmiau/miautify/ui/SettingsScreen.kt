package io.github.innocentmiau.miautify.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.innocentmiau.miautify.R

/** The app's settings, one row each. Changes apply and are saved at once. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    library: LibraryViewModel = viewModel()
) {
    BackHandler(onBack = onBack)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
        }
        SwitchSetting(
            title = stringResource(R.string.settings_full_names),
            detail = stringResource(R.string.settings_full_names_detail),
            checked = library.fullNames,
            onCheckedChange = library::showFullNames
        )
    }
}

/**
 * A setting that is on or off. The whole row is the switch, so it can be tapped anywhere and
 * is read out as one switch with its title.
 */
@Composable
private fun SwitchSetting(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) },
        // Null: the row handles the tap, the switch only shows the state.
        trailingContent = { Switch(checked = checked, onCheckedChange = null) }
    )
}
