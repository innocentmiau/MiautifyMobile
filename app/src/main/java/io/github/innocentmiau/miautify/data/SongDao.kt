package io.github.innocentmiau.miautify.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/** The queries on the scan cache. Room writes the code for each one at build time. */
@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE folder = :folder")
    suspend fun songsIn(folder: String): List<SongEntity>

    /** Adds the rows, or replaces those with the same folder and id. */
    @Upsert
    suspend fun save(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE folder = :folder AND documentId IN (:ids)")
    suspend fun deleteSome(folder: String, ids: List<String>)

    /** Deletes the rows of files that are gone, in chunks, since SQLite limits how many values one query takes. */
    @Transaction
    suspend fun delete(folder: String, ids: List<String>) {
        ids.chunked(500).forEach { deleteSome(folder, it) }
    }
}
