package com.indest.heh.persistance

import android.content.ContentValues
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.indest.heh.persistance.dto.LetterProperties
import com.indest.heh.persistance.dto.LevelData
import com.indest.heh.persistance.dto.MetaData

@Database(entities = [LevelData::class, LetterProperties::class, MetaData::class], version = 1, exportSchema = true)
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
                        "uus.db"
                    )
                        .fallbackToDestructiveMigration()
                        .addCallback(object: RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                               val trigger = "create trigger level_updated\n" +
                                        "after update on letter_info_table\n" +
                                        "begin\n" +
                                        "update meta_data set last_modified_timestamp = (CAST((julianday('now') - 2440587.5)*86400000 AS INTEGER));\n" +
                                        "end;"
                                db.execSQL(trigger)
                                val contentValues = ContentValues()
                                contentValues.put("last_modified_timestamp", 0)
                                db.insert("meta_data", OnConflictStrategy.IGNORE, contentValues)
                            }
                        })
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}