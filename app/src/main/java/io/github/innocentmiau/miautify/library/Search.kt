package io.github.innocentmiau.miautify.library

import java.text.Normalizer
import java.util.Locale

/*
 * Search matching, with the same rules as the desktop app: a song matches when every word of
 * the query appears somewhere in its title (or file name, without a title), artist, album or
 * album artist, in any order: "clank ratchet" finds "Ratchet & Clank", "ac dc" finds
 * "AC/DC", "beyonce" finds "Beyoncé". Not fuzzy on purpose: no typo tolerance.
 */

private val marks = Regex("\\p{M}+")
private val notLetterOrDigit = Regex("[^\\p{L}\\p{N}]+")

/**
 * [text] in its searchable form: lowercase, accents removed, and anything that isn't a letter
 * or a digit (in any script) turned into a space.
 *
 * NFKD splits "é" into "e" plus an accent mark (then removed) and folds look-alikes: the
 * full-width "Ｍ" becomes "M", the ligature "ﬁ" becomes "fi". \p{L} and \p{N} are any letter
 * and any digit in any script, so Japanese, Cyrillic or Greek titles stay searchable.
 */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFKD)
        .replace(marks, "")
        // Locale.ROOT: the same result on every phone (a Turkish phone would otherwise turn "I" into "ı").
        .lowercase(Locale.ROOT)
        .replace(notLetterOrDigit, " ")
        .trim()

/** The words of a query, folded. Empty means "nothing to search for". */
fun searchWords(query: String): List<String> =
    foldForSearch(query).split(' ').filter { it.isNotEmpty() }

/**
 * Searches a list of songs. Each song's text is folded once, here, so a keystroke only folds
 * the query and compares: fast even over thousands of songs.
 */
class SongSearch(private val songs: List<Song>) {
    private val haystacks = songs.map { song ->
        // The file name only stands in without a title tag, like in the list: for tagged songs
        // it's mostly noise ("07 - ...") that would match number searches.
        foldForSearch(
            listOfNotNull(song.title, song.tags?.artist, song.tags?.album, song.tags?.albumArtist)
                .joinToString(" ")
        )
    }

    /** The matching songs, in the list's order. No words means no results. */
    fun search(query: String): List<Song> {
        val words = searchWords(query)
        if (words.isEmpty()) return emptyList()
        return songs.filterIndexed { index, _ -> words.all { it in haystacks[index] } }
    }
}
