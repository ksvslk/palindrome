package com.indest.hah.persistance

import android.content.ContentValues
import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.Sku
import java.util.ArrayList

@Database(
    entities = [LevelData::class, LetterProperties::class, Sku::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
@RewriteQueriesToDropUnusedColumns
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
                        //.createFromAsset("uus.db")
                        .fallbackToDestructiveMigration()
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                val trigger = "create trigger level_updated\n" +
                                        "after update on letter_info_table\n" +
                                        "begin\n" +
                                        "update level_info_table set time_modified = (CAST((julianday('now') - 2440587.5)*86400000 AS INTEGER)) WHERE level_id = NEW.owner_id;\n" +
                                        "end;"
                                db.execSQL(trigger)
                                val json = context.applicationContext.assets.open("puzzles.json").bufferedReader().use {
                                    it.readText()
                                }
                                val gson = Gson()
                                val levels = gson.fromJson(json, Array<LevelData>::class.java)

                                for (i in levels.indices) {
                                    //val sol = levels[i].solution.replace(" ", "")
                                    //val shuffledChars = sol.toCharArray()
                                    //while (shuffledChars.concatToString() == sol) shuffledChars.shuffle()
                                    val puzzle = ""
                                    val category = levels[i].category
                                    val progress = LevelProgress.TODO
                                    val score = (i + 1) * 100

                                    val contentValues = ContentValues()
                                    contentValues.put("level_id", i + 1)
                                    contentValues.put("solution", levels[i].solution)
                                    contentValues.put("puzzle", puzzle)
                                    contentValues.put("category", category)
                                    contentValues.put("category_rewarded", false)
                                    contentValues.put("progress", progress.ordinal)
                                    contentValues.put("score", score)
                                    contentValues.put("best_score", 0)
                                    contentValues.put("time_modified", 0)

                                    db.insert(
                                        "level_info_table",
                                        OnConflictStrategy.IGNORE,
                                        contentValues
                                    )
                                }
                                val skuList = ArrayList<String>()
                                skuList.add(context.getString(com.indest.hah.R.string.full_no_ads))
                                skuList.add(context.getString(com.indest.hah.R.string.instant_clues))
                                skuList.add(context.getString(com.indest.hah.R.string.no_intermediate_ads))
                                skuList.add(context.getString(com.indest.hah.R.string.instant_letters))

                                skuList.forEach {
                                    val skuValue = ContentValues()
                                    skuValue.put("sku", it)
                                    skuValue.put("purchased", false)
                                    db.insert(
                                        "sku",
                                        OnConflictStrategy.IGNORE,
                                        skuValue
                                    )
                                }
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