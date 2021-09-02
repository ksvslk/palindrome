package com.indest.oc.persistance

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Level::class, LetterInfo::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LevelDatabase : RoomDatabase() {

    abstract val levelDao: LevelDao

    companion object {

        @Volatile
        private var INSTANCE: LevelDatabase? = null

        fun getInstance(context: Context): LevelDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        LevelDatabase::class.java,
                        "level_info_database.db"
                    )
                        //.createFromAsset("level_info_database.db")
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}