package com.indest.hah.persistance.level

import android.content.Context
import androidx.room.*
import com.indest.hah.persistance.Converters
import com.indest.hah.persistance.dto.Interval
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.LevelData


/*@Serializable
data class LevelInfo(val nr:Int, val category: String, val solution:String)*/

@Database(
    entities = [LevelData::class, LetterProperties::class, Interval::class],
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
                        "dbd.db"
                    )
                        .createFromAsset("dbd.db")
                        .fallbackToDestructiveMigration()
                         /*
                        .addCallback(object : RoomDatabase.Callback() {

                            private val jsonInstance = Json { isLenient = true }

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

                                val levels = jsonInstance
                                    .decodeFromString<List<JsonElement>>(json)
                                    .map { jsonInstance.decodeFromJsonElement(it) as LevelInfo}

                               for (i in levels.indices) {
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

                               val intervals = ArrayList<String>()
                               intervals.add(context.getString(com.indest.hah.R.string.sad_mode_interval))
                               intervals.add(context.getString(com.indest.hah.R.string.player_centered_scores))
                               intervals.add(context.getString(com.indest.hah.R.string.hint_suggestion))
                               intervals.add(context.getString(com.indest.hah.R.string.achievement_flawless_5))
                               intervals.add(context.getString(com.indest.hah.R.string.achievement_flawless_15))

                               intervals.forEach {
                                   val interval = ContentValues()
                                   val limit = when(it)
                                   {
                                       context.getString(com.indest.hah.R.string.sad_mode_interval) -> 10
                                       context.getString(com.indest.hah.R.string.player_centered_scores) -> 6
                                       context.getString(com.indest.hah.R.string.hint_suggestion) -> 3
                                       context.getString(com.indest.hah.R.string.achievement_flawless_5) -> 5
                                       context.getString(com.indest.hah.R.string.achievement_flawless_15) -> 15
                                       else -> 7
                                   }
                                   interval.put("name", it)
                                   interval.put("current_count", 0)
                                   interval.put("limit_count", limit)

                                   db.insert(
                                       "interval",
                                       OnConflictStrategy.IGNORE,
                                       interval
                                   )
                               }

                            }
                        }
                        )*/.build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}