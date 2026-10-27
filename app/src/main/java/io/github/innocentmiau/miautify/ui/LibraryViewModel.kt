package io.github.innocentmiau.miautify.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.innocentmiau.miautify.data.MiautifyDatabase
import io.github.innocentmiau.miautify.library.MusicFolder
import io.github.innocentmiau.miautify.library.ScanState
import io.github.innocentmiau.miautify.library.SortOrder
import io.github.innocentmiau.miautify.library.scanLibrary
import io.github.innocentmiau.miautify.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The music folder and its scan. Lives longer than the screen: Android keeps it through
 * rotations and other redraws of the activity, so the scan doesn't start over each time.
 * Cleared only when the user really leaves the app (back out of it or the app is closed).
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = Settings(application)
    private var scanJob: Job? = null

    /** The chosen folder, or null when none was chosen or access to it was lost. */
    var folder by mutableStateOf(settings.musicFolder?.takeIf { MusicFolder.hasAccess(application, it) })
        private set

    /** The folder's name, or null when it can't be read. */
    var folderName by mutableStateOf<String?>(null)
        private set

    /** Null when there is no folder. */
    var scan by mutableStateOf<ScanState?>(null)
        private set

    var sortOrder by mutableStateOf(settings.sortOrder)
        private set

    var sortReversed by mutableStateOf(settings.sortReversed)
        private set

    /**
     * Play counts by document id, for sorting by most played. Read when the app opens and when
     * that sort is picked, not live, so the list doesn't reorder itself while a song plays.
     */
    var playCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /**
     * The search screen's text and chosen chip. Kept here and not in the screen, so they are
     * still there when you come back from an album or artist opened from the results.
     */
    var searchQuery by mutableStateOf("")
        private set

    var searchTab by mutableStateOf(SearchTab.SONGS)
        private set

    init {
        folder?.let(::startScan)
        loadPlayCounts()
    }

    fun search(query: String) {
        searchQuery = query
    }

    fun showSearchTab(tab: SearchTab) {
        searchTab = tab
    }

    /** Leaving search starts the next one fresh, on the same chip. */
    fun clearSearch() {
        searchQuery = ""
    }

    fun sortBy(order: SortOrder, reversed: Boolean) {
        sortOrder = order
        sortReversed = reversed
        settings.sortOrder = order
        settings.sortReversed = reversed
        if (order == SortOrder.MOST_PLAYED) loadPlayCounts()
    }

    private fun loadPlayCounts() {
        viewModelScope.launch {
            val plays = MiautifyDatabase.get(getApplication()).plays().all()
            playCounts = plays.associate { it.documentId to it.playCount }
        }
    }

    /** Uses [picked] from now on and remembers it. Scans it unless it is the current folder. */
    fun chooseFolder(picked: Uri) {
        val previous = folder
        MusicFolder.keepAccess(getApplication(), picked, previous)
        settings.musicFolder = picked
        folder = picked
        if (picked != previous) startScan(picked)
    }

    private fun startScan(folder: Uri) {
        // Stops the scan of the previous folder, if it is still running.
        scanJob?.cancel()
        folderName = MusicFolder.displayName(getApplication(), folder)
        // viewModelScope is cancelled when the ViewModel is cleared, which ends the scan too.
        scanJob = viewModelScope.launch {
            scanLibrary(getApplication(), folder).collect { scan = it }
        }
    }
}
