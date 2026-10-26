package io.github.innocentmiau.miautify.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri

/** Small values the app remembers between launches. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** The folder picked in the system folder picker, or null if none was picked yet. */
    var musicFolder: Uri?
        get() = prefs.getString(KEY_MUSIC_FOLDER, null)?.toUri()
        set(value) = prefs.edit { putString(KEY_MUSIC_FOLDER, value?.toString()) }

    private companion object {
        const val KEY_MUSIC_FOLDER = "music_folder"
    }
}
