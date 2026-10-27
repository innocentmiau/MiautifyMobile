package io.github.innocentmiau.miautify.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's SQLite database (miautify.db in the app's private storage). Raise [version] and
 * add a migration whenever a table changes; app/schemas keeps each version's layout.
 *
 * Versions:
 * 1. The songs table (scan cache).
 * 2. songs.folderCoverId. Room writes this migration itself, by comparing the saved layouts
 *    of versions 1 and 2: it only adds a column, so nothing is lost.
 * 3. The plays table (play counts and last played times), also an auto-migration.
 */
@Database(
    entities = [SongEntity::class, PlayEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class MiautifyDatabase : RoomDatabase() {
    abstract fun songs(): SongDao

    abstract fun plays(): PlayDao

    companion object {
        @Volatile
        private var instance: MiautifyDatabase? = null

        /** The one shared database. Opening it is slow, so the app keeps a single instance. */
        fun get(context: Context): MiautifyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, MiautifyDatabase::class.java, "miautify.db")
                    .build()
                    .also { instance = it }
            }
    }
}
