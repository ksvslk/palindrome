package com.indest.hah.persistance.level

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.Keep
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.indest.hah.persistance.Converters
import com.indest.hah.persistance.dto.Interval
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.Solution
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement


@Keep
@Serializable
data class LevelInfo(val nr: Int, val solutionToCategory:Map<String,String>)

@Database(
    entities = [LevelData::class, LetterProperties::class, Interval::class, Solution::class],
    version = 4,
    autoMigrations = [
        AutoMigration (
            from = 1,
            to = 2,
            spec = LevelDatabase.MyAutoMigration::class
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
            }
        }
    }

    companion object {

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try{
                    database.execSQL("ALTER TABLE level_info_table ADD COLUMN author TEXT NOT NULL DEFAULT 'NewVersion'")
                    database.execSQL("ALTER TABLE level_info_table ADD COLUMN column_count INTEGER NOT NULL DEFAULT 7")
                    database.execSQL("ALTER TABLE interval ADD COLUMN timestamp INTEGER NOT NULL DEFAULT -1")
                } catch (ex: SQLiteException) {
                    Log.e("LevelDatabase", "Altering database to migrate from version 2 to 3: " + ex.message)
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try{
                    database.execSQL("ALTER TABLE level_info_table ADD COLUMN picture_rewarded INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE level_info_table ADD COLUMN country_code TEXT NOT NULL DEFAULT 'gb'")
                    database.delete("letter_info_table", null, null)
                    database.execSQL("UPDATE level_info_table SET progress = 1 WHERE level_id > 1000 and level_id < 10000")
                    database.execSQL("UPDATE level_info_table SET progress = 4 WHERE progress > 0")
                } catch (ex: SQLiteException) {
                    Log.e("LevelDatabase", "Migrating from version 3 to 4: " + ex.message)
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
                        .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                        .addAutoMigrationSpec(MyAutoMigration(context))
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