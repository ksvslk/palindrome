package com.indest.hah.persistance.level

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.Keep
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.indest.hah.game.creation.Shuffler
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.C
import com.indest.hah.other.getLevelScore
import com.indest.hah.persistance.Converters
import com.indest.hah.persistance.dto.Interval
import com.indest.hah.persistance.dto.LanguageLevel
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.RegularLevel
import com.indest.hah.persistance.dto.Solution
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement


@Keep
@Serializable
data class LevelInfo(val nr: Int, val solutionToCategory: Map<String, String>)

@Database(
    entities = [LevelData::class, LetterProperties::class, Interval::class, Solution::class],
    version = 4,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = LevelDatabase.MyAutoMigration::class
        ),
        AutoMigration(
            from = 3,
            to = 4,
            spec = LevelDatabase.AutoMigrationFrom3to4::class
        )
    ],
    exportSchema = true
)

@TypeConverters(Converters::class)
@RewriteQueriesToDropUnusedColumns
abstract class LevelDatabase : RoomDatabase() {

    abstract val levelDao: LevelDao

    @DeleteColumn(tableName = "level_info_table", columnName = "solution")
    @DeleteColumn(tableName = "level_info_table", columnName = "category")
    @DeleteColumn(tableName = "level_info_table", columnName = "level_pack")
    @ProvidedAutoMigrationSpec
    class MyAutoMigration(private val context: Context) : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            val jsonInstance = Json { isLenient = true }
            val json =
                context.applicationContext.assets.open("level_pack_1_modified.json")
                    .bufferedReader().use {
                        it.readText()
                    }

            val levels = jsonInstance
                .decodeFromString<List<JsonElement>>(json)
                .map { jsonInstance.decodeFromJsonElement(it) as LevelInfo }

            for (i in levels.indices) {
                for (solutionToCategory in levels[i].solutionToCategory.entries) {
                    val solutionContentValues = ContentValues()
                    solutionContentValues.put("parent_id", i + 1)
                    solutionContentValues.put("palindrome", solutionToCategory.key)
                    solutionContentValues.put("hint", solutionToCategory.value)
                    solutionContentValues.put("solved", false)
                    db.insert(
                        "solution_table",
                        OnConflictStrategy.IGNORE,
                        solutionContentValues
                    )
                }
            }
        }
    }

    @ProvidedAutoMigrationSpec
    class AutoMigrationFrom3to4(private val context: Context) : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)

            val levelMap = mutableMapOf<Int, LevelData>()
            val query = "SELECT * FROM level_info_table"
            val cursorResult = db.query(query)

            cursorResult.use { cursor ->
                while (cursor.moveToNext()) {
                    val levelId = cursor.getInt(cursor.getColumnIndexOrThrow("level_id"))
                    val puzzle = cursor.getString(cursor.getColumnIndexOrThrow("puzzle"))
                    val categoryRewarded = cursor.getInt(cursor.getColumnIndexOrThrow("category_rewarded")) == 1
                    val progress = cursor.getInt(cursor.getColumnIndexOrThrow("progress"))
                    val score = cursor.getInt(cursor.getColumnIndexOrThrow("score"))
                    val bestScore = cursor.getInt(cursor.getColumnIndexOrThrow("best_score"))
                    val timeModified = cursor.getLong(cursor.getColumnIndexOrThrow("time_modified"))
                    val author = cursor.getString(cursor.getColumnIndexOrThrow("author"))
                    val columnCount = cursor.getInt(cursor.getColumnIndexOrThrow("column_count"))

                    val levelData = LevelData(
                        levelId = levelId,
                        puzzle = puzzle,
                        categoryRewarded = categoryRewarded,
                        progress = LevelProgress.values()[progress],
                        score = score,
                        best_score = bestScore,
                        timeModified = timeModified,
                        author = author,
                        columnCount = columnCount
                    )

                    levelMap[levelId] = levelData
                }
            }
            cursorResult.close()

            // get all levels info for levels 1-100 and 1001-1080

            db.delete("letter_info_table", null, null)
            db.delete("solution_table", null, null)
            db.execSQL("UPDATE level_info_table SET progress = 2 where progress > 0")

            val jsonInstance = Json { isLenient = true }
            val json = context.applicationContext.assets.open("level_pack_1_modified.json")
                .bufferedReader().use {
                    it.readText()
                }
            val json2 =
                context.applicationContext.assets.open("player_levels_until_1080.json")
                    .bufferedReader().use {
                        it.readText()
                    }
            val playerLevels = jsonInstance.decodeFromString<List<RegularLevel>>(json2)
            val regularLevels = jsonInstance.decodeFromString<List<RegularLevel>>(json)
            for (level in regularLevels.plus(playerLevels)) {
                val puzzle = level.solutionToCategory.keys.firstOrNull() ?: ""
                val score = level.nr.toInt().getLevelScore(puzzle = puzzle)
                val shuffled = Shuffler.getShuffledSolution(level.solutionToCategory.keys.first()).replace("-","")

                // ignore these as when migrating they are already set to correct values
                // best_score
                // progress
                // category_rewarded
                // time_modified

                val contentValues = ContentValues()
                contentValues.put("puzzle", shuffled)
                contentValues.put("score", score)
                if (levelMap.contains(level.nr.toInt()))
                {
                    val oldLevelInfo = levelMap[level.nr.toInt()]!!
                    contentValues.put("best_score", oldLevelInfo.best_score)
                    contentValues.put("progress", oldLevelInfo.progress.ordinal)
                    contentValues.put("category_rewarded", oldLevelInfo.categoryRewarded)
                    contentValues.put("time_modified", oldLevelInfo.timeModified)
                    contentValues.put("author", oldLevelInfo.author)
                }

                for (solutionToCategory in level.solutionToCategory.entries) {
                    val solutionContentValues = ContentValues()
                    solutionContentValues.put("parent_id", level.nr)
                    solutionContentValues.put("palindrome", solutionToCategory.key)
                    solutionContentValues.put("hint", solutionToCategory.value["hint"])
                    solutionContentValues.put("hint_cn", solutionToCategory.value["hint_cn"])
                    solutionContentValues.put("hint_de", solutionToCategory.value["hint_de"])
                    solutionContentValues.put("hint_es", solutionToCategory.value["hint_es"])
                    solutionContentValues.put("hint_et", solutionToCategory.value["hint_et"])
                    solutionContentValues.put("hint_fi", solutionToCategory.value["hint_fi"])
                    solutionContentValues.put("hint_fr", solutionToCategory.value["hint_fr"])
                    solutionContentValues.put("hint_id", solutionToCategory.value["hint_id"])
                    solutionContentValues.put("hint_it", solutionToCategory.value["hint_it"])
                    solutionContentValues.put("hint_ja", solutionToCategory.value["hint_ja"])
                    solutionContentValues.put("hint_ko", solutionToCategory.value["hint_ko"])
                    solutionContentValues.put("hint_nl", solutionToCategory.value["hint_nl"])
                    solutionContentValues.put("hint_pl", solutionToCategory.value["hint_pl"])
                    solutionContentValues.put("hint_pt", solutionToCategory.value["hint_pt"])
                    solutionContentValues.put("hint_ro", solutionToCategory.value["hint_ro"])
                    solutionContentValues.put("hint_sv", solutionToCategory.value["hint_sv"])
                    solutionContentValues.put("hint_tr", solutionToCategory.value["hint_tr"])
                    solutionContentValues.put("hint_uk", solutionToCategory.value["hint_uk"])
                    solutionContentValues.put("solved", false)

                    db.insert(
                        "solution_table",
                        OnConflictStrategy.REPLACE,
                        solutionContentValues
                    )
                }

                db.update(
                    "level_info_table",
                    OnConflictStrategy.REPLACE,
                    contentValues,
                    "level_id = ?",
                    arrayOf(level.nr)
                )

                shuffled.forEachIndexed { index, char ->
                    val letterContentValue = ContentValues()
                    letterContentValue.put("owner_id", level.nr)
                    letterContentValue.put("home_index", index)
                    letterContentValue.put("friend_index", -666)
                    letterContentValue.put("letter", char.toString())
                    letterContentValue.put("locked", false)
                    db.insert(
                        "letter_info_table",
                        OnConflictStrategy.REPLACE,
                        letterContentValue)
                }
            }

            var currentLevelId = 10000
            var first: Boolean
            C.languages.entries.forEach { entry ->
                first = true
                val cc = entry.key
                val langJson =
                    context.applicationContext.assets.open("language_pack_$cc.json")
                        .bufferedReader().use {
                            it.readText()
                        }
                val languageLevels = jsonInstance.decodeFromString<List<LanguageLevel>>(langJson)

                languageLevels.forEach { level ->
                    currentLevelId += 1
                    val puzzle = level.solutionToCategory.keys.firstOrNull() ?: ""
                    val score = currentLevelId.getLevelScore(puzzle = puzzle)
                    val progress = if (first) 1 else 0
                    first = false
                    val shuffled = Shuffler.getShuffledSolution(level.solutionToCategory.keys.first()).replace("-","")

                    val contentValues = ContentValues()
                    contentValues.put("level_id", currentLevelId)
                    contentValues.put("puzzle", shuffled)
                    contentValues.put("category_rewarded", false)
                    contentValues.put("picture_rewarded", false)
                    contentValues.put("progress", progress)
                    contentValues.put("score", score)
                    contentValues.put("best_score", 0)
                    contentValues.put("time_modified", 0)
                    contentValues.put("author", "")
                    contentValues.put("column_count", -1)
                    contentValues.put("country_code", cc)

                    for (solutionToCategory in level.solutionToCategory.entries) {
                        val solutionContentValues = ContentValues()
                        solutionContentValues.put("parent_id", currentLevelId)
                        solutionContentValues.put("palindrome", solutionToCategory.key)
                        solutionContentValues.put("hint", solutionToCategory.value["hint"])
                        solutionContentValues.put("hint_cn", solutionToCategory.value["hint_cn"])
                        solutionContentValues.put("hint_de", solutionToCategory.value["hint_de"])
                        solutionContentValues.put("hint_es", solutionToCategory.value["hint_es"])
                        solutionContentValues.put("hint_et", solutionToCategory.value["hint_et"])
                        solutionContentValues.put("hint_fi", solutionToCategory.value["hint_fi"])
                        solutionContentValues.put("hint_fr", solutionToCategory.value["hint_fr"])
                        solutionContentValues.put("hint_id", solutionToCategory.value["hint_id"])
                        solutionContentValues.put("hint_it", solutionToCategory.value["hint_it"])
                        solutionContentValues.put("hint_ja", solutionToCategory.value["hint_ja"])
                        solutionContentValues.put("hint_ko", solutionToCategory.value["hint_ko"])
                        solutionContentValues.put("hint_nl", solutionToCategory.value["hint_nl"])
                        solutionContentValues.put("hint_pl", solutionToCategory.value["hint_pl"])
                        solutionContentValues.put("hint_pt", solutionToCategory.value["hint_pt"])
                        solutionContentValues.put("hint_ro", solutionToCategory.value["hint_ro"])
                        solutionContentValues.put("hint_sv", solutionToCategory.value["hint_sv"])
                        solutionContentValues.put("hint_tr", solutionToCategory.value["hint_tr"])
                        solutionContentValues.put("hint_uk", solutionToCategory.value["hint_uk"])
                        solutionContentValues.put("solved", false)

                        db.insert(
                            "solution_table",
                            OnConflictStrategy.REPLACE,
                            solutionContentValues,
                        )
                    }
                    db.insert(
                        "level_info_table",
                        OnConflictStrategy.REPLACE,
                        contentValues,
                    )
                    shuffled.forEachIndexed { index, char ->
                        val letterContentValue = ContentValues()
                        letterContentValue.put("owner_id", currentLevelId)
                        letterContentValue.put("home_index", index)
                        letterContentValue.put("friend_index", -666)
                        letterContentValue.put("letter", char.toString())
                        letterContentValue.put("locked", false)
                        db.insert(
                            "letter_info_table",
                            OnConflictStrategy.REPLACE,
                            contentValues)
                    }
                }
            }

        }
    }


    companion object {

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE level_info_table ADD COLUMN author TEXT NOT NULL DEFAULT 'NewVersion'")
                    database.execSQL("ALTER TABLE level_info_table ADD COLUMN column_count INTEGER NOT NULL DEFAULT 7")
                    database.execSQL("ALTER TABLE interval ADD COLUMN timestamp INTEGER NOT NULL DEFAULT -1")
                } catch (ex: SQLiteException) {
                    Log.e(
                        "LevelDatabase",
                        "Altering database to migrate from version 2 to 3: " + ex.message
                    )
                }
            }
        }


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
                        .addMigrations(MIGRATION_2_3)
                        .addAutoMigrationSpec(MyAutoMigration(context))
                        .addAutoMigrationSpec(AutoMigrationFrom3to4(context))
                        .createFromAsset("dbd.db")
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

                      val json =
                          context.applicationContext.assets.open("level_pack_1_modified.json")
                              .bufferedReader().use {
                              it.readText()
                          }

                      val levels = jsonInstance
                          .decodeFromString<List<JsonElement>>(json)
                          .map { jsonInstance.decodeFromJsonElement(it) as LevelInfo }

                      for (i in levels.indices) {
                          val puzzle = ""
                          //val category = levels[i].category
                          val progress = LevelProgress.TODO
                          val score = (i + 1) * 100

                          val contentValues = ContentValues()
                          contentValues.put("level_id", i + 1)
                          //contentValues.put("solution", levels[i].solution)
                          contentValues.put("puzzle", puzzle)
                          //contentValues.put("category", category)
                          contentValues.put("category_rewarded", false)
                          contentValues.put("progress", progress.ordinal)
                          contentValues.put("score", score)
                          contentValues.put("best_score", 0)
                          contentValues.put("time_modified", 0)
                          contentValues.put("author", "")
                          contentValues.put("column_count", 7)

                          for (solutionToCategory in levels[i].solutionToCategory.entries)
                          {
                              val solutionContentValues = ContentValues()
                              solutionContentValues.put("parent_id", i + 1)
                              solutionContentValues.put("palindrome", solutionToCategory.key)
                              solutionContentValues.put("hint", solutionToCategory.value)
                              solutionContentValues.put("solved", false)
                              db.insert(
                                  "solution_table",
                                  OnConflictStrategy.IGNORE,
                                  solutionContentValues
                              )
                          }

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
                      intervals.add(context.getString(com.indest.hah.R.string.last_submission))

                      intervals.forEach {
                          val interval = ContentValues()
                          val limit = when (it) {
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
                          interval.put("timestamp", -1L)

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