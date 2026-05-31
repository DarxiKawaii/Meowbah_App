package com.kawaii.meowbah.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kawaii.meowbah.data.model.RssPost

@Database(entities = [RssPost::class, VideoEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rssPostDao(): RssPostDao
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meowbah_app_database"
                )
                .fallbackToDestructiveMigration() // Not ideal for production, but okay for now
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
