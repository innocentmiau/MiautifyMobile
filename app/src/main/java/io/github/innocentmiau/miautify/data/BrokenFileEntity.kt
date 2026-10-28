package io.github.innocentmiau.miautify.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import io.github.innocentmiau.miautify.library.BrokenFile
import kotlinx.coroutines.flow.Flow

/** A file that failed to play, with its size and date then (see Song.isBroken). By document id, like the plays table. */
@Entity(tableName = "broken_files")
data class BrokenFileEntity(
    @PrimaryKey val documentId: String,
    val size: Long,
    val lastModified: Long
) {
    fun toBrokenFile() = BrokenFile(size, lastModified)
}

@Dao
interface BrokenFileDao {
    /** Every broken file, sent again whenever the table changes, so the list turns red live. */
    @Query("SELECT * FROM broken_files")
    fun observeAll(): Flow<List<BrokenFileEntity>>

    /** Room's upsert is an insert that falls back to an update, so it works on Android 8's older SQLite too. */
    @Upsert
    suspend fun save(file: BrokenFileEntity)

    /** It played after all (it was fixed, or the failure was a fluke). */
    @Query("DELETE FROM broken_files WHERE documentId = :documentId")
    suspend fun forget(documentId: String)
}
