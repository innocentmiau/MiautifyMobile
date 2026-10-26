package io.github.innocentmiau.miautify.library

import org.junit.Assert.assertEquals
import org.junit.Test

class FindSongsTest {
    /** A fake folder tree: folder id to what is directly inside it. */
    private fun tree(vararg folders: Pair<String, List<DocumentEntry>>): (String) -> List<DocumentEntry> {
        val map = folders.toMap()
        return { id -> map[id].orEmpty() }
    }

    private fun file(id: String, name: String = id) = DocumentEntry(id, name, isFolder = false)
    private fun folder(id: String, name: String = id) = DocumentEntry(id, name, isFolder = true)

    @Test
    fun findsSongsInSubfoldersWithTheirPath() {
        val children = tree(
            "root" to listOf(file("a.mp3"), folder("album", "Album")),
            "album" to listOf(file("b.mp3"), folder("disc2", "Disc 2")),
            "disc2" to listOf(file("c.mp3"))
        )

        val songs = findSongs("root", children)

        assertEquals(
            listOf(
                SongFile("a.mp3", "a.mp3", ""),
                SongFile("b.mp3", "b.mp3", "Album"),
                SongFile("c.mp3", "c.mp3", "Album/Disc 2")
            ),
            songs
        )
    }

    @Test
    fun keepsOnlyMp3FilesInAnyCase() {
        val children = tree(
            "root" to listOf(file("song.mp3"), file("LOUD.MP3"), file("cover.jpg"), file("notes.txt"), file("mp3"))
        )

        val names = findSongs("root", children).map { it.name }

        assertEquals(listOf("LOUD.MP3", "song.mp3"), names)
    }

    @Test
    fun sortsByFolderThenNameIgnoringCase() {
        val children = tree(
            "root" to listOf(folder("b", "beta"), folder("a", "Alpha"), file("z.mp3"), file("Y.mp3")),
            "a" to listOf(file("2.mp3"), file("1.mp3")),
            "b" to listOf(file("x.mp3"))
        )

        val paths = findSongs("root", children).map { "${it.folder}/${it.name}" }

        assertEquals(listOf("/Y.mp3", "/z.mp3", "Alpha/1.mp3", "Alpha/2.mp3", "beta/x.mp3"), paths)
    }

    @Test
    fun emptyFolderHasNoSongs() {
        assertEquals(emptyList<SongFile>(), findSongs("root", tree("root" to emptyList())))
    }

    @Test
    fun songsGetTheCoverImageOfTheirOwnFolder() {
        val children = tree(
            "root" to listOf(file("top.mp3"), folder("album", "Album")),
            // The image comes after the song in the listing: it still counts.
            "album" to listOf(file("song.mp3"), file("img", "Cover.JPG"))
        )

        val covers = findSongs("root", children).associate { it.name to it.folderCoverId }

        assertEquals(mapOf("top.mp3" to null, "song.mp3" to "img"), covers)
    }

    @Test
    fun theBestNamedImageWins() {
        val children = tree(
            "root" to listOf(file("song.mp3"), file("album", "album.png"), file("cover", "cover.jpg"), file("front", "front.jpeg"))
        )

        assertEquals("cover", findSongs("root", children).single().folderCoverId)
    }

    @Test
    fun otherImagesAreNotCovers() {
        assertEquals(null, folderImageRank("photo.jpg"))
        assertEquals(null, folderImageRank("cover.gif"))
        assertEquals(null, folderImageRank("cover"))
        assertEquals(0, folderImageRank("COVER.jpeg"))
        assertEquals(1, folderImageRank("folder.png"))
    }
}
