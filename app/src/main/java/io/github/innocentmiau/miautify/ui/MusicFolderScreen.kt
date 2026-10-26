package io.github.innocentmiau.miautify.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.innocentmiau.miautify.R
import io.github.innocentmiau.miautify.library.MusicFolder
import io.github.innocentmiau.miautify.settings.Settings
import io.github.innocentmiau.miautify.ui.theme.MiautifyTheme

/** Shows the chosen music folder and lets the user pick one, remembered between launches. */
@Composable
fun MusicFolderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var folder by remember {
        mutableStateOf(settings.musicFolder?.takeIf { MusicFolder.hasAccess(context, it) })
    }
    val folderName = remember(folder) { folder?.let { MusicFolder.displayName(context, it) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        // Null when the user backs out of the picker: keep the current folder.
        if (picked != null) {
            MusicFolder.keepAccess(context, picked, previous = folder)
            settings.musicFolder = picked
            folder = picked
        }
    }

    MusicFolderContent(
        hasFolder = folder != null,
        folderName = folderName,
        // Opens the picker inside the current folder, so changing it is quicker.
        onChooseFolder = { picker.launch(folder) },
        modifier = modifier
    )
}

@Composable
private fun MusicFolderContent(
    hasFolder: Boolean,
    folderName: String?,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when {
        !hasFolder -> stringResource(R.string.library_empty)
        folderName == null -> stringResource(R.string.library_scan_failed)
        else -> folderName
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onChooseFolder) {
            Text(stringResource(R.string.library_choose_folder))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoFolderPreview() {
    MiautifyTheme {
        MusicFolderContent(hasFolder = false, folderName = null, onChooseFolder = {}, modifier = Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderPreview() {
    MiautifyTheme {
        MusicFolderContent(hasFolder = true, folderName = "Music", onChooseFolder = {}, modifier = Modifier.fillMaxSize())
    }
}
