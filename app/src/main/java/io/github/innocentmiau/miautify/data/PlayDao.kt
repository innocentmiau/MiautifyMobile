package io.github.innocentmiau.miautify.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/**
 * The queries on the play history. Each change is "make sure the row exists, then update it".
 * SQLite's one-statement upsert (ON CONFLICT DO UPDATE) is missing on Android 8 to 10, which
 * ship an older SQLite, so it is done in two statements inside one transaction.
 */
@Dao
interface PlayDao {
    @Query("SELECT * FROM plays")
    suspend fun all(): List<PlayEntity>

    @Query("INSERT OR IGNORE INTO plays (documentId, playCount, lastPlayedAt) VALUES (:documentId, 0, NULL)")
    suspend fun ensureRow(documentId: String)

    @Query("UPDATE plays SET lastPlayedAt = :at WHERE documentId = :documentId")
    suspend fun setLastPlayed(documentId: String, at: Long)

    @Query("UPDATE plays SET playCount = playCount + 1 WHERE documentId = :documentId")
    suspend fun incrementCount(documentId: String)

    /** The song started playing at [at]. */
    @Transaction
    suspend fun markStarted(documentId: String, at: Long) {
        ensureRow(documentId)
        setLastPlayed(documentId, at)
    }

    /** The song was listened to long enough to count. */
    @Transaction
    suspend fun addPlay(documentId: String) {
        ensureRow(documentId)
        incrementCount(documentId)
    }
}
