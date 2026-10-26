package io.github.innocentmiau.miautify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's SQLite database (miautify.db in the app's private storage). Raise [version] and
 * add a migration whenever a table changes; app/schemas keeps each version's layout.
 */
@Database(entities = [SongEntity::class], version = 1)
abstract class MiautifyDatabase : RoomDatabase() {
    abstract fun songs(): SongDao

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
