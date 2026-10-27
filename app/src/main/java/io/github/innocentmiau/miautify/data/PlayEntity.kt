package io.github.innocentmiau.miautify.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How often and when a song was played. Kept apart from the scan cache, so it outlives
 * rescans and folder changes: the document id stays the same as long as the file isn't
 * renamed or moved, even if a parent folder is picked later.
 */
@Entity(tableName = "plays")
data class PlayEntity(
    @PrimaryKey val documentId: String,
    /** Times it was listened to for at least 30 seconds (see playThresholdMs). */
    val playCount: Int,
    /** When it last started playing, in milliseconds since 1970, or null if never. For shuffle. */
    val lastPlayedAt: Long?
)
