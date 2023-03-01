package com.indest.hah.persistance.level

import androidx.room.*
import com.indest.hah.game.creation.Shuffler
import com.indest.hah.persistance.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


@Dao
interface LevelDao {

    @Query("delete from letter_info_table where owner_id = :levelNr")
    suspend fun deleteLetterInfo(levelNr: Int)

    @Query("select letter from letter_info_table where owner_id=:levelNr and friend_index!=-666 order by friend_index asc")
    suspend fun getCurrentSolution(levelNr: Int): List<Char>

    @Query("select count(*) from letter_info_table where owner_id=:levelNr")
    suspend fun levelLettersCount(levelNr: Int): Int

    @Query("update level_info_table set time_modified = (select time_modified + 1 from level_info_table where level_id=:currentLevelNr) where level_id=:chosenLevel")
    suspend fun updateChosenLevelTimestamp(currentLevelNr: Int, chosenLevel: Int)

    @Transaction
    suspend fun nextLevelActions(currentLevelNr: Int, chosenLevel: Int) {
        persistInitialLettersInfo(chosenLevel)
        updateChosenLevelTimestamp(currentLevelNr, chosenLevel)
    }

    @Query("update level_info_table set best_score = score where best_score < score and level_id = :level")
    suspend fun updateBestScoreQuery(level: Int)

    @Query("select sum(best_score) from level_info_table where level_id <= 100")
    suspend fun getTotalScore(): Int

    @Query("select sum(best_score) from level_info_table where level_id > 1000")
    suspend fun getTotalPlayerLevelsScore(): Int?

    @Insert
    suspend fun insertLevelInfo(levels: LevelData)

    @Insert
    suspend fun insertSolutions(solutions: List<Solution>)

    @Insert
    suspend fun insertSolution(solution: Solution)

    @Query("select max(solution_id) from solution_table")
    suspend fun getMaxSolutionId(): Int

    @Query("select * from level_info_table where level_id = (select max(level_id) from level_info_table)")
    suspend fun getMaxLevel(): LevelData

    @Query("select * from level_info_table where level_id = :id")
    suspend fun getSpecificLevel(id:Long): LevelData

    @Query("update level_info_table set column_count = :newColumnCount where level_id = :level")
    suspend fun changeColumnCount(level: Int, newColumnCount: Int)

    @Transaction
    suspend fun changeFriendIndexesBasedOnColumnCount(
        level: Int,
        columnCount: Int,
        letters: List<LetterProperties>,
        homeIndexNewFriendIndexes: List<Pair<Int, Int>>
    ) {
        homeIndexNewFriendIndexes.forEach {
            updateFriendIndexSub(it.first, it.second, level)

        }
        changeColumnCount(level, columnCount)
    }


    @Query("select exists (select 1 from level_info_table where level_id = :id)")
    suspend fun isLevelAdded(id: Long): Boolean

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
                        progress = LevelProgress.TODO,
                        score = puzzle.length * 100,
                        best_score = 0,
                        timeModified = 0,
                        author = level.player,
                        columnCount = -1
                    )
                )
                level.solutionToCategory.entries.forEach { solutionToCategory ->
                    val maxSolutionId = getMaxSolutionId()
                    insertSolution(
                        Solution(
                            solutionId = if (maxSolutionId == 100) 1001 else maxSolutionId + 1,
                            parentId = level.levelId.toInt(),
                            palindrome = solutionToCategory.key,
                            hint = solutionToCategory.value,
                            solved = false
                        )
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

    @Query("select category_rewarded from level_info_table where level_id=:level")
    suspend fun isCategoryRewarded(level: Int): Boolean

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
    @Query("select *, max(time_modified) from level_info_table")
    fun getLastLevel(): Flow<Level>

    @Query("update or ignore letter_info_table set friend_index = :newFriendIndex where home_index = :homeIndex and owner_id = :level")
    suspend fun updateFriendIndexSub(homeIndex: Int, newFriendIndex: Int, level: Int)

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

    @Update
    suspend fun updateLevelData(levelInfo: LevelData)

    @Query("update level_info_table set progress = :progress where level_id = :level")
    suspend fun updateProgress(level: Int, progress: LevelProgress)

    @Query("update level_info_table set puzzle = :puzzle where level_id = :level")
    suspend fun updatePuzzle(level: Int, puzzle: String)

    @Transaction
    suspend fun levelCompletedActions(currentLevel: Int, hintInterval: String) {
        // reset hint interval
        resetCount(hintInterval)
        // delete current level letters info
        deleteLetterInfo(currentLevel)
        // update current level progress
        updateProgress(currentLevel, LevelProgress.REPLAY)
        // update current level best score
        updatePuzzle(currentLevel, "")
        // Reset and make old level info
        persistInitialLettersInfo(currentLevel)
        // unlock next level (change progress and increase timestamp)
        updateProgress(currentLevel + 1, LevelProgress.IN_PROGRESS)
        // update next level default letters and shuffled puzzle
        persistInitialLettersInfo(currentLevel + 1)
        // update next level timestamp
        updateChosenLevelTimestamp(currentLevel, currentLevel + 1)
    }

    @Transaction
    suspend fun levelCompletedActionsFinal(currentLevel: Int, hintInterval: String) {
        // reset hint interval
        resetCount(hintInterval)
        // delete current level letters info
        deleteLetterInfo(currentLevel)
        // update current level progress
        updateProgress(currentLevel, LevelProgress.REPLAY)
        // update current level best score
        updatePuzzle(currentLevel, "")
        // Reset and make old level info
        persistInitialLettersInfo(currentLevel)
    }


    @Query("select palindrome from solution_table where parent_id=:level")
    suspend fun getLevelSolutions(level: Int): List<String>

    @Query("select progress from level_info_table where level_id=:level")
    suspend fun getLevelProgress(level: Int): LevelProgress

    @Transaction
    suspend fun persistInitialLettersInfo(levelNr: Int) {
        if (levelNr != 0) {
            val lettersCount = levelLettersCount(levelNr)
            val isLevelFinished = getLevelProgress(levelNr)
            if (lettersCount <= 0 && isLevelFinished != LevelProgress.INTERMEDIATE) {
                val solution = getLevelSolutions(levelNr).first()
                val puzzle = Shuffler.getShuffledSolution(solution)
                val newScore = if (levelNr <= 100) levelNr * 100 else puzzle.trim().length * 100
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
                updateScore(newScore, levelNr)
                updateProgress(levelNr, LevelProgress.IN_PROGRESS)
            }
        }
    }

    @Transaction
    suspend fun init() {
        val firstLevelProgress = getLevelProgress(1)
        if (firstLevelProgress == LevelProgress.TODO) persistInitialLettersInfo(1)
    }

    @Transaction
    suspend fun getLocalSaveGame(): SaveGame {
        val levels = getAllLevels()
        val intervals = getAllIntervals()
        return SaveGame(levels, intervals)
    }

    @Transaction
    suspend fun updateLocalGame(saveGame: SaveGame) {
        val localMaxTimestamp = getMaximumTimestamp() ?: 0
        val cloudMaxTimestamp = saveGame.game.maxOfOrNull { it.levelInfo.timeModified } ?: -1
        if (cloudMaxTimestamp > localMaxTimestamp)
        {
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

    @Query("update level_info_table set author='Migrated' where level_id = :level")
    suspend fun updateToMigrated(level: Int)

    @Transaction
    suspend fun updateAuthorToMigrated(
        level: Int, columnCount: Int,
        letters: List<LetterProperties>,
        homeIndexNewFriendIndexes: List<Pair<Int, Int>>
    ) {
        updateToMigrated(level)
        changeColumnCount(level, columnCount)
        changeFriendIndexesBasedOnColumnCount(
            level,
            columnCount,
            letters,
            homeIndexNewFriendIndexes
        )
    }

    @Query("select max(time_modified) from level_info_table")
    suspend fun getMaximumTimestamp(): Long?

}