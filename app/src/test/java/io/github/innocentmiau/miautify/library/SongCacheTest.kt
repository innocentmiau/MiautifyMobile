package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SongCacheTest {
    private val tags = SongTags("Title", "Artist", "Album", 180_000)

    private fun file(id: String, size: Long = 100, lastModified: Long = 1) =
        SongFile(id, "$id.mp3", "", size, lastModified)

    @Test
    fun unchangedFilesUseTheCachedTags() {
        val match = matchCache(listOf(file("a")), mapOf("a" to CachedSong(100, 1, tags)))

        assertEquals(tags, match.songs.single().tags)
        assertEquals(emptyList<Int>(), match.toRead)
    }

    @Test
    fun newFilesAreRead() {
        val match = matchCache(listOf(file("a"), file("b")), mapOf("a" to CachedSong(100, 1, tags)))

        assertEquals(listOf(1), match.toRead)
        assertEquals(null, match.songs[1].tags)
    }

    @Test
    fun filesWithAnotherSizeOrDateAreReadAgain() {
        val cached = mapOf(
            "size" to CachedSong(100, 1, tags),
            "date" to CachedSong(100, 1, tags)
        )

        val match = matchCache(listOf(file("size", size = 200), file("date", lastModified = 2)), cached)

        assertEquals(listOf(0, 1), match.toRead)
    }

    @Test
    fun cachedFilesNotFoundAnymoreAreRemoved() {
        val cached = mapOf("a" to CachedSong(100, 1, tags), "gone" to CachedSong(100, 1, tags))

        assertEquals(listOf("gone"), matchCache(listOf(file("a")), cached).removed)
    }

    @Test
    fun aNewFolderCoverUpdatesTheRowWithoutReadingTagsAgain() {
        val withCover = file("a").copy(folderCoverId = "cover")

        val match = matchCache(listOf(withCover), mapOf("a" to CachedSong(100, 1, tags, folderCoverId = null)))

        assertEquals(emptyList<Int>(), match.toRead)
        assertEquals(listOf(0), match.toUpdate)
        assertEquals(tags, match.songs.single().tags)
    }

    @Test
    fun anUnchangedCoverNeedsNoUpdate() {
        val withCover = file("a").copy(folderCoverId = "cover")

        val match = matchCache(listOf(withCover), mapOf("a" to CachedSong(100, 1, tags, folderCoverId = "cover")))

        assertEquals(emptyList<Int>(), match.toUpdate)
    }

    @Test
    fun theOrderOfTheFilesIsKept() {
        val files = listOf(file("c"), file("a"), file("b"))

        val ids = matchCache(files, emptyMap()).songs.map { it.file.documentId }

        assertEquals(listOf("c", "a", "b"), ids)
    }
}
