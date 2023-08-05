package com.indest.hah.persistance.level

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.indest.hah.game.creation.Shuffler
import com.indest.hah.other.getLevelScore
import com.indest.hah.persistance.dto.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Dao
interface LevelDao {
    private val databaseIdentityQuery: SimpleSQLiteQuery
        get() = SimpleSQLiteQuery(
            "SELECT identity_hash FROM room_master_table"
        )
    @RawQuery
    suspend fun getDatabaseIdentityViaQuery(query: SupportSQLiteQuery = databaseIdentityQuery): String

    @Query("select distinct country_code from level_info_table where country_code not in('gb')")
    suspend fun getUniqueCountries():List<String>

    @MapInfo(keyColumn = "language", valueColumn = "count")
    @Query("select country_code as language, count(*) AS count from level_info_table where country_code not in('gb') group by country_code order by count desc")
    suspend fun getUniqueCountriesWithLevelCount():Map<String, Int>

    @Transaction
    @Query("select * from level_info_table where country_code = :cc")
    suspend fun getLevelsForCountry(cc:String):List<Level>

    @Query("delete from letter_info_table where owner_id = :levelNr")
    suspend fun deleteLetterInfo(levelNr: Int)

    @Query("update letter_info_table set friend_index = -666, locked = 0 where owner_id = :levelNr")
    suspend fun resetLetterInfo(levelNr: Int)

    @Query("select letter from letter_info_table where owner_id=:levelNr and friend_index!=-666 order by friend_index asc")
    suspend fun getCurrentSolution(levelNr: Int): List<Char>

    @Query("update level_info_table set time_modified = (select time_modified + 1 from level_info_table where level_id=:currentLevelNr) where level_id=:chosenLevel")
    suspend fun updateChosenLevelTimestamp(currentLevelNr: Int, chosenLevel: Int)

    @Transaction
    suspend fun nextLevelActions(currentLevelNr: Int, chosenLevel: Int) {
        resetLevel(chosenLevel)
        updateChosenLevelTimestamp(currentLevelNr, chosenLevel)
    }

    @Query("update level_info_table set best_score = score where best_score < score and level_id = :level")
    suspend fun updateBestScoreQuery(level: Int)

    @Query("select sum(best_score) from level_info_table where level_id <= 100")
    suspend fun getTotalScore(): Int

    @Query("select sum(best_score) from level_info_table where level_id > 1000 and level_id < 10000")
    suspend fun getTotalPlayerLevelsScore(): Int?

    @Query("select sum(best_score) from level_info_table where level_id > 10000 and country_code =:countryCode")
    suspend fun getTotalScoreForCountry(countryCode:String): Int

    @Insert
    suspend fun insertLevelInfo(levels: LevelData)

    @Insert
    suspend fun insertSolutions(solutions: List<Solution>)

    @Insert
    suspend fun insertSolution(solution: Solution)

    @Query("select max(solution_id) from solution_table")
    suspend fun getMaxSolutionId(): Int

    @Query("select * from level_info_table where level_id = :id")
    suspend fun getSpecificLevel(id: Long): LevelData

    @Query("update level_info_table set column_count = :newColumnCount where level_id = :level")
    suspend fun changeColumnCount(level: Int, newColumnCount: Int)

    @Query("select exists (select 1 from level_info_table where level_id = :id)")
    suspend fun isLevelAdded(id: Long): Boolean

    @Transaction
    suspend fun insertLanguagePackLevel(levels:List<LanguageLevel>, countryCode:String)
    {
        var maxLevelId = getMaxLevelId()
        if (maxLevelId < 10000) maxLevelId = 10000
        var nextLevelId = maxLevelId
        levels.forEach { level ->
            nextLevelId += 1
            if (!isLevelAdded(nextLevelId.toLong())) {
                val puzzle = level.solutionToCategory.keys.firstOrNull() ?: ""
                insertLevelInfo(
                    LevelData(
                        levelId = nextLevelId,
                        puzzle = puzzle,
                        categoryRewarded = false,
                        pictureRewarded = false,
                        progress = LevelProgress.TODO,
                        score = nextLevelId.getLevelScore(puzzle = puzzle),
                        best_score = 0,
                        timeModified = 0,
                        author = "",
                        columnCount = -1,
                        countryCode = countryCode
                    )
                )
                level.solutionToCategory.entries.forEach { solutionToCategory ->
                    val maxSolutionId = getMaxSolutionId()
                    insertSolution(
                        Solution(
                            solutionId = if (maxSolutionId < 10000) 10001 else maxSolutionId + 1,
                            parentId = nextLevelId,
                            palindrome = solutionToCategory.key,
                            hint = solutionToCategory.value["hint"]!!,
                            hint_cn = solutionToCategory.value["hint_cn"]!!,
                            hint_de = solutionToCategory.value["hint_de"]!!,
                            hint_es = solutionToCategory.value["hint_es"]!!,
                            hint_et = solutionToCategory.value["hint_et"]!!,
                            hint_fi = solutionToCategory.value["hint_fi"]!!,
                            hint_fr = solutionToCategory.value["hint_fr"]!!,
                            hint_id = solutionToCategory.value["hint_id"]!!,
                            hint_it = solutionToCategory.value["hint_it"]!!,
                            hint_ja = solutionToCategory.value["hint_ja"]!!,
                            hint_ko = solutionToCategory.value["hint_ko"]!!,
                            hint_nl = solutionToCategory.value["hint_nl"]!!,
                            hint_pl = solutionToCategory.value["hint_pl"]!!,
                            hint_pt = solutionToCategory.value["hint_pt"]!!,
                            hint_ro = solutionToCategory.value["hint_ro"]!!,
                            hint_sv = solutionToCategory.value["hint_sv"]!!,
                            hint_tr = solutionToCategory.value["hint_tr"]!!,
                            hint_uk = solutionToCategory.value["hint_uk"]!!,
                            solved = false
                        )
                    )
                }
                val solution = level.solutionToCategory.keys.first()
                val puzzleShuffled = Shuffler.getShuffledSolution(solution).replace("-","")
                updatePuzzle(nextLevelId, puzzleShuffled)
                puzzleShuffled.forEachIndexed { index, char ->
                    insertLetterData(
                        ownerId = nextLevelId,
                        homeIndex = index,
                        friendIndex = -666,
                        letter = char,
                        locked = false,
                    )
                }
            }
        }
    }

    @Transaction
    suspend fun insertRegularLevels(playerLevel: List<RegularLevel>) {
        playerLevel.forEach { level ->
            if (!isLevelAdded(level.nr)) {
                val puzzle = level.solutionToCategory.keys.firstOrNull() ?: ""
                insertLevelInfo(
                    LevelData(
                        levelId = level.nr.toInt(),
                        puzzle = puzzle,
                        categoryRewarded = false,
                        pictureRewarded = false,
                        progress = LevelProgress.TODO,
                        score = level.nr.toInt().getLevelScore(puzzle = puzzle),
                        best_score = 0,
                        timeModified = 0,
                        columnCount = -1,
                    )
                )
                level.solutionToCategory.entries.forEach { solutionToCategory ->
                    var maxSolutionId = 0
                    if (level.nr > 1) {
                        maxSolutionId = getMaxSolutionId()
                    }
                    insertSolution(
                        Solution(
                            solutionId = if (maxSolutionId == 100) 1001 else maxSolutionId + 1,
                            parentId = level.nr.toInt(),
                            palindrome = solutionToCategory.key,
                            hint = solutionToCategory.value["hint"] ?: "",
                            hint_cn = solutionToCategory.value["hint_cn"] ?: "",
                            hint_de = solutionToCategory.value["hint_de"] ?: "",
                            hint_es = solutionToCategory.value["hint_es"] ?: "",
                            hint_et = solutionToCategory.value["hint_et"] ?: "",
                            hint_fi = solutionToCategory.value["hint_fi"] ?: "",
                            hint_fr = solutionToCategory.value["hint_fr"] ?: "",
                            hint_id = solutionToCategory.value["hint_id"] ?: "",
                            hint_it = solutionToCategory.value["hint_it"] ?: "",
                            hint_ja = solutionToCategory.value["hint_ja"] ?: "",
                            hint_ko = solutionToCategory.value["hint_ko"] ?: "",
                            hint_nl = solutionToCategory.value["hint_nl"] ?: "",
                            hint_pl = solutionToCategory.value["hint_pl"] ?: "",
                            hint_pt = solutionToCategory.value["hint_pt"] ?: "",
                            hint_ro = solutionToCategory.value["hint_ro"] ?: "",
                            hint_sv = solutionToCategory.value["hint_sv"] ?: "",
                            hint_tr = solutionToCategory.value["hint_tr"] ?: "",
                            hint_uk = solutionToCategory.value["hint_uk"] ?: "",
                            solved = false
                        )
                    )
                }
                val solution = level.solutionToCategory.keys.first()
                val puzzleShuffled = Shuffler.getShuffledSolution(solution).replace("-","")
                updatePuzzle(level.nr.toInt(), puzzleShuffled)
                puzzleShuffled.forEachIndexed { index, char ->
                    insertLetterData(
                        ownerId = level.nr.toInt(),
                        homeIndex = index,
                        friendIndex = -666,
                        letter = char,
                        locked = false,
                    )
                }
            }
        }
    }


    @Transaction
    suspend fun insertPlayerLevels(playerLevel: List<PlayerLevel>): Pair<LevelData, Int> {
        var newLevelsCount = 0
        var oneLevelId = 1L
        playerLevel.forEach { level ->
            if (!isLevelAdded(level.levelId)) {
                val puzzle = level.solutionToCategory.keys.firstOrNull() ?: ""
                insertLevelInfo(
                    LevelData(
                        levelId = level.levelId.toInt(),
                        puzzle = puzzle,
                        categoryRewarded = false,
                        pictureRewarded = false,
                        progress = LevelProgress.TODO,
                        score = level.levelId.toInt().getLevelScore(puzzle = puzzle),
                        best_score = 0,
                        timeModified = 0,
                        author = level.player,
                        columnCount = -1,
                        countryCode = level.countryCode
                    )
                )
                level.solutionToCategory.entries.forEach { solutionToCategory ->
                    val maxSolutionId = getMaxSolutionId()
                    insertSolution(
                        Solution(
                            solutionId = if (maxSolutionId == 100) 1001 else maxSolutionId + 1,
                            parentId = level.levelId.toInt(),
                            palindrome = solutionToCategory.key,
                            hint = solutionToCategory.value["hint"] ?: "",
                            hint_cn = solutionToCategory.value["hint_cn"] ?: "",
                            hint_de = solutionToCategory.value["hint_de"] ?: "",
                            hint_es = solutionToCategory.value["hint_es"] ?: "",
                            hint_et = solutionToCategory.value["hint_et"] ?: "",
                            hint_fi = solutionToCategory.value["hint_fi"] ?: "",
                            hint_fr = solutionToCategory.value["hint_fr"] ?: "",
                            hint_id = solutionToCategory.value["hint_id"] ?: "",
                            hint_it = solutionToCategory.value["hint_it"] ?: "",
                            hint_ja = solutionToCategory.value["hint_ja"] ?: "",
                            hint_ko = solutionToCategory.value["hint_ko"] ?: "",
                            hint_nl = solutionToCategory.value["hint_nl"] ?: "",
                            hint_pl = solutionToCategory.value["hint_pl"] ?: "",
                            hint_pt = solutionToCategory.value["hint_pt"] ?: "",
                            hint_ro = solutionToCategory.value["hint_ro"] ?: "",
                            hint_sv = solutionToCategory.value["hint_sv"] ?: "",
                            hint_tr = solutionToCategory.value["hint_tr"] ?: "",
                            hint_uk = solutionToCategory.value["hint_uk"] ?: "",
                            solved = false
                        )
                    )
                }
                val solution = level.solutionToCategory.keys.first()
                val puzzleShuffled = Shuffler.getShuffledSolution(solution).replace("-","")
                updatePuzzle(level.levelId.toInt(), puzzleShuffled)
                puzzleShuffled.forEachIndexed { index, char ->
                    insertLetterData(
                        ownerId = level.levelId.toInt(),
                        homeIndex = index,
                        friendIndex = -666,
                        letter = char,
                        locked = false,
                    )
                }
                newLevelsCount++
                oneLevelId = level.levelId
            }
        }
        return Pair(getSpecificLevel(oneLevelId), newLevelsCount)
    }

    @Query("select max(level_id) from level_info_table")
    suspend fun getMaxLevelId(): Int

    @Query("select max(level_id) from level_info_table where level_id > 1000 and level_id < 10000")
    suspend fun getMaxPlayerLevelId(): Int?
    @Query("select min(level_id) from level_info_table where country_code = :cc")
    suspend fun getMinLevelIdForCountry(cc:String): Int

    @Query("select max(level_id) from level_info_table where country_code = :cc")
    suspend fun getMaxLevelIdForCountry(cc:String): Int

    @Query("select max(level_id) from level_info_table where level_id < 1000")
    suspend fun getMaxGameLevels(): Int


    @Transaction
    suspend fun updateLevels(levels: List<Level>) {
        deleteSolutions()
        deleteLettersInfo()
        deleteLevels()
        levels.forEach { level ->
            insertLevelInfo(level.levelInfo)
            insertSolutions(level.solutions)
            level.lettersProperties.forEach { lp ->
                insertLetterData(
                    ownerId = lp.ownerId,
                    homeIndex = lp.homeIndex,
                    friendIndex = lp.friendIndex,
                    letter = lp.letter,
                    locked = lp.locked
                )
            }
        }
    }

    @Transaction
    suspend fun updateIntervals(intervals: List<Interval>) {
        deleteIntervals()
        intervals.forEach { interval ->
            val special = "player_centered_scores"
            if (interval.name == special) {
                if (!isLimitAttained(special)) {
                    val modifiedInterval = Interval(
                        name = special,
                        currentCount = 0,
                        limitCount = interval.limitCount,
                        -1L
                    )
                    insertInterval(modifiedInterval)
                }
            } else {
                insertInterval(interval)
            }
        }
    }

    @Insert
    suspend fun insertInterval(interval: Interval)

    @Query("DELETE FROM interval")
    suspend fun deleteIntervals()

    @Query("DELETE FROM solution_table")
    suspend fun deleteSolutions()

    @Query("DELETE FROM letter_info_table")
    suspend fun deleteLettersInfo()

    @Query("DELETE FROM level_info_table")
    suspend fun deleteLevels()

    @Transaction
    @Query("select * from level_info_table")
    suspend fun getAllLevels(): List<Level>

    @Query("select * from interval")
    suspend fun getAllIntervals(): List<Interval>

    @Query("select palindrome from solution_table")
    suspend fun getAllPalindromes(): List<String>

    @Query("update level_info_table set category_rewarded=:value where level_id=:level")
    suspend fun updateCategoryRewarded(value: Boolean, level: Int)

    @Query("update level_info_table set picture_rewarded=:value where level_id=:level")
    suspend fun updatePictureRewarded(value: Boolean, level: Int)

    @Query("update interval set current_count = (select current_count + 1 from interval where name=:name) where name=:name")
    suspend fun increaseCurrentCount(name: String)

    @Query("update interval set current_count = 0 where name=:name and current_count = (select limit_count from interval where name=:name)")
    suspend fun resetCount(name: String)

    @Query("select current_count from interval where name = :name")
    suspend fun getIntervalCount(name: String): Int

    @Query("update interval set current_count = :count where name = :name")
    suspend fun setCountTo(name: String, count: Int)

    @Query("select exists (select 1 from interval where name = :name and current_count >= limit_count)")
    suspend fun isLimitAttained(name: String): Boolean

    @Query("select exists (select 1 from interval where name = :name and current_count = limit_count)")
    fun sadModeIntervalFlow(name: String): Flow<Boolean>

    @Transaction
    suspend fun increaseCountTransaction(name: String): Boolean {
        increaseCurrentCount(name)
        val isLimit = isLimitAttained(name)
        if (isLimit) resetCount(name)
        return isLimit
    }

    @Transaction
    @Query("select *, max(time_modified) from level_info_table where puzzle != ''")
    fun getLastLevel(): Flow<Level>

    @Query("update or ignore letter_info_table set friend_index = :newFriendIndex where home_index = :homeIndex and owner_id = :level")
    fun updateFriendIndexSub(homeIndex: Int, newFriendIndex: Int, level: Int)

    @Transaction
    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int): String {
        updateFriendIndexSub(homeIndex, newFriendIndex, level)
        return getCurrentSolution(level).joinToString("")
    }

    @Query("update letter_info_table set locked = 1 where home_index = :homeIndex and owner_id = :level")
    suspend fun setLetterLocked(homeIndex: Int, level: Int)

    @Query("update or ignore letter_info_table set letter = :letter where home_index = :homeIndex and owner_id = :level")
    suspend fun updateLetterQuery(homeIndex: Int, letter: Char, level: Int)

    @Transaction
    suspend fun updateLetters(
        homeIndex1: Int,
        homeIndex2: Int,
        letter1: Char,
        letter2: Char,
        level: Int
    ): String {
        updateLetterQuery(homeIndex1, letter1, level)
        updateLetterQuery(homeIndex2, letter2, level)
        return getCurrentSolution(level).joinToString("")
    }

    @Query("update level_info_table set score=:newScore where level_id=:level")
    suspend fun updateScore(newScore: Int, level: Int)

    @Query("insert or ignore into letter_info_table (owner_id, home_index, friend_index, letter, locked) values(:ownerId, :homeIndex, :friendIndex, :letter, :locked)")
    suspend fun insertLetterData(
        ownerId: Int,
        homeIndex: Int,
        friendIndex: Int,
        letter: Char,
        locked: Boolean
    )

    @Query("update level_info_table set progress = :progress where level_id = :level")
    suspend fun updateProgress(level: Int, progress: LevelProgress)

    @Query("update level_info_table set puzzle = :puzzle where level_id = :level")
    suspend fun updatePuzzle(level: Int, puzzle: String)

    @Transaction
    suspend fun levelCompletedActions(currentLevel: Int, hintInterval: String, chosenLevel: Int) {
        // reset hint interval
        resetCount(hintInterval)
        // delete current level letters info
        resetLevel(currentLevel, LevelProgress.REPLAY)
        // update chosen level default letters and shuffled puzzle (when level was chosen from level select)
        if (getLevelProgress(chosenLevel) != LevelProgress.IN_PROGRESS) resetLevel(chosenLevel)
        // reset next level if it exists
        val nextLevel = currentLevel + 1
        if (isLevelAdded(nextLevel.toLong())) resetLevel(nextLevel)
        // update next level timestamp
        updateChosenLevelTimestamp(currentLevel, chosenLevel)
    }

    @Transaction
    suspend fun chooseLevelActions(currentLevel: Int, chosenLevel: Int, hintInterval: String)
    {
        // reset hint interval
        resetCount(hintInterval)
        // update chosen level timestamp
        updateChosenLevelTimestamp(currentLevelNr = currentLevel, chosenLevel = chosenLevel)
    }


    @Transaction
    suspend fun levelCompletedActionsFinal(currentLevel: Int, hintInterval: String) {
        // reset hint interval
        resetCount(hintInterval)
        // Reset and make old level info
        resetLevel(currentLevel, LevelProgress.REPLAY)
    }


    @Query("select palindrome from solution_table where parent_id=:level")
    suspend fun getLevelSolutions(level: Int): List<String>

    @Query("select progress from level_info_table where level_id=:level")
    suspend fun getLevelProgress(level: Int): LevelProgress


    /** Steps taken to reset the level:
            1. Delete letters info
            2. Update puzzle (shuffle and set)
            3. Update letters info to initial blank field (set friend to -666)
            4. Update initial score to the max you can get from the level
            5. Update progress to IN_PROGRESS or REPLAY (if just finished)
     *
     * @param levelNr level nr to be set to initial state.
     * @param progress progress that should be set to this level after reset.
     *
     */
    suspend fun resetLevel(levelNr: Int, progress: LevelProgress = LevelProgress.IN_PROGRESS) {
        deleteLetterInfo(levelNr)
        val levelSolutions = getLevelSolutions(levelNr)
        val solution = levelSolutions.first()
        val puzzle = Shuffler.getShuffledSolution(solution).replace("-","")
        updatePuzzle(levelNr, puzzle)
        puzzle.forEachIndexed { index, char ->
            insertLetterData(
                ownerId = levelNr,
                homeIndex = index,
                friendIndex = -666,
                letter = char,
                locked = false,
            )
        }
        val newScore = levelNr.getLevelScore(puzzle = puzzle)
        updateScore(newScore, levelNr)
        updateProgress(levelNr, progress)
    }

    @Query("select level_id from level_info_table where progress = 4")
    suspend fun getAllMigratedLevelIds():List<Int>


    @Transaction
    suspend fun resetAllMigratedLevels()
    {
        val allLevelsToBeReset = getAllMigratedLevelIds()
        allLevelsToBeReset.forEach { levelNr ->
            resetLevel(levelNr, LevelProgress.REPLAY)
        }
    }

    @Transaction
    suspend fun init() {
        val firstLevelProgress = getLevelProgress(1)
        if (firstLevelProgress == LevelProgress.TODO) resetLevel(1)
        resetAllMigratedLevels()
    }

    @Transaction
    suspend fun getLocalSaveGame(): SaveGame {
        val levels = getAllLevels()
        val intervals = getAllIntervals()
        val dbIdentityHash = getDatabaseIdentityViaQuery()
        return SaveGame(levels, intervals, dbIdentityHash)
    }

    @Transaction
    suspend fun updateLocalGame(saveGame: SaveGame) {
        val localMaxTimestamp = getMaximumTimestamp() ?: 0
        val cloudMaxTimestamp = saveGame.game.maxOfOrNull { it.levelInfo.timeModified } ?: -1
        if (cloudMaxTimestamp > localMaxTimestamp) {
            updateLevels(saveGame.game)
            updateIntervals(saveGame.interval)
        }
    }

    @Query("select max(level_id) = :currentLevel from level_info_table where level_id >= :minLimit and level_id <= :maxLimit")
    suspend fun isFinalLevel(currentLevel: Int, minLimit: Int, maxLimit: Int): Int?

    @Query("select timestamp from interval where name = 'last_submission'")
    suspend fun getLastSubmissionTimestamp(): Long?

    @Query("insert or replace into interval (name,current_count,limit_count,timestamp) VALUES ('last_submission',-1,-1,:time)")
    suspend fun updateSubmissionTimestamp(time: Long)

    @Query("select max(time_modified) from level_info_table")
    suspend fun getMaximumTimestamp(): Long?


}