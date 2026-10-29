package io.github.innocentmiau.miautify.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import io.github.innocentmiau.miautify.library.GroupOrder
import io.github.innocentmiau.miautify.library.SortOrder

/** Small values the app remembers between launches. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** The folder picked in the system folder picker, or null if none was picked yet. */
    var musicFolder: Uri?
        get() = prefs.getString(KEY_MUSIC_FOLDER, null)?.toUri()
        set(value) = prefs.edit { putString(KEY_MUSIC_FOLDER, value?.toString()) }

    var shuffle: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE, false)
        set(value) = prefs.edit { putBoolean(KEY_SHUFFLE, value) }

    /** One of Media3's Player.REPEAT_MODE_ values: off (0), one (1) or all (2). */
    var repeatMode: Int
        get() = prefs.getInt(KEY_REPEAT_MODE, 0)
        set(value) = prefs.edit { putInt(KEY_REPEAT_MODE, value) }

    /** Saved by name, so reordering the enum can't change a saved choice. Unknown names read as folder order. */
    var sortOrder: SortOrder
        get() = SortOrder.entries.find { it.name == prefs.getString(KEY_SORT_ORDER, null) } ?: SortOrder.FOLDER
        set(value) = prefs.edit { putString(KEY_SORT_ORDER, value.name) }

    var sortReversed: Boolean
        get() = prefs.getBoolean(KEY_SORT_REVERSED, false)
        set(value) = prefs.edit { putBoolean(KEY_SORT_REVERSED, value) }

    /** How albums, artists and folders are browsed, most played first by default. Saved by name too. */
    var groupOrder: GroupOrder
        get() = GroupOrder.entries.find { it.name == prefs.getString(KEY_GROUP_ORDER, null) } ?: GroupOrder.MOST_PLAYED
        set(value) = prefs.edit { putString(KEY_GROUP_ORDER, value.name) }

    private companion object {
        const val KEY_MUSIC_FOLDER = "music_folder"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT_MODE = "repeat_mode"
        const val KEY_SORT_ORDER = "sort_order"
        const val KEY_SORT_REVERSED = "sort_reversed"
        const val KEY_GROUP_ORDER = "group_order"
    }
}
