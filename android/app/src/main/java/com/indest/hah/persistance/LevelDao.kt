package com.indest.hah.persistance

import androidx.room.*
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(levelInfo: LevelData)

    @Transaction
    @Query("delete from letter_info_table where owner_id = :levelNr")
    suspend fun deleteLetterInfo(levelNr: Int)

    @Transaction
    @Query("select * from level_info_table where level_id = :nr")
    suspend fun getLevel(nr: Int): Level

    @Transaction
    @Query("select *, max(time_modified) from level_info_table")
    fun getLastLevel(): Flow<Level>

    @Query("update level_info_table set puzzle = :newPuzzle where level_id = :level")
    suspend fun updateLevelPuzzle(level: Int, newPuzzle: String)

    @Query("insert into letter_info_table (owner_id, home_index, friend_index, letter, hint_used) values(:level, :homeIndex, :friendIndex, :letter, :hintUsed)")
    suspend fun insertLetterData(
        level: Int,
        homeIndex: Int,
        friendIndex: Int,
        letter: Char,
        hintUsed: Boolean
    )

    @Query("update letter_info_table set friend_index = :newFriendIndex where home_index = :homeIndex and owner_id = :level")
    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int)

    @Query("update letter_info_table set letter = :letter where home_index = :homeIndex and owner_id = :level")
    suspend fun updateLetterQuery(homeIndex: Int, letter: Char, level: Int)

    @Transaction
    suspend fun updateLetter(homeIndex: Int, letter: Char, level: Int) {
        updateLetterQuery(homeIndex, letter, level)
    }

    @Query("select letter from letter_info_table where owner_id=:levelNr and friend_index!=-666 order by friend_index asc")
    suspend fun getCurrentSolution(levelNr: Int): List<Char>

    @Query("select count(*) from letter_info_table where owner_id=:levelNr")
    suspend fun areLettersInitialized(levelNr: Int): Int

    @Query("update level_info_table set time_modified = (select time_modified + 1 from level_info_table where level_id=:currentLevelNr) where level_id=:nextLevelNr")
    suspend fun updateNextLevelTimestamp(currentLevelNr: Int, nextLevelNr: Int)

    @Transaction
    suspend fun switchHomeIndexes(index1: Int, index2: Int, level: Int) {
        updateHomeIndex(index1, -420, level)
        updateHomeIndex(index2, index1, level)
        updateHomeIndex(-420, index2, level)
    }

    @Query("update letter_info_table set home_index = :newIndex where home_index = :oldIndex and owner_id = :level")
    suspend fun updateHomeIndex(oldIndex: Int, newIndex: Int, level: Int)

    @Update
    suspend fun updateLevelData(levelInfo: LevelData)

    @Update
    suspend fun updateLettersProperties(level: List<LetterProperties>)

    @Transaction
    suspend fun updateLevels(levels: List<Level>) {
        levels.forEach {
            updateLevelData(it.levelInfo)
            updateLettersProperties(it.lettersProperties)
        }
    }

    @Query("select * from level_info_table")
    suspend fun getAllLevels(): List<Level>

    @Query("select count(*) from level_info_table")
    suspend fun rowCount(): Int

    @Query("update level_info_table set score=:newScore where level_id=:level")
    suspend fun updateScore(newScore:Int, level: Int)

    //SELECT * FROM table WHERE id IN (SELECT id FROM table ORDER BY RANDOM() LIMIT x) TODO:Get random hint

}