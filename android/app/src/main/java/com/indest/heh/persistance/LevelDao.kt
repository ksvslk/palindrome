package com.indest.heh.persistance

import androidx.room.*
import com.indest.heh.persistance.dto.LetterProperties
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData

@Dao
interface LevelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(levelInfo: LevelData)

    @Transaction
    @Query("select * from level_info_table where level_id = :nr")
    suspend fun getLevel(nr: Int): Level

    // TODO: change progress to 1
    @Transaction
    @Query("select * from level_info_table where progress = 0 order by level_id asc limit 1")
    suspend fun getLastLevel(): Level

    @Query("update level_info_table set puzzle = :newPuzzle where level_id = :level")
    suspend fun updateLevelPuzzle(level: Int, newPuzzle: String)

    @Query("insert into letter_info_table (owner_id, home_index, friend_index, letter) values(:level, :homeIndex, :friendIndex, :letter)")
    suspend fun insertLetterData(level: Int, homeIndex:Int, friendIndex:Int, letter:Char)

    @Query("update letter_info_table set home_index = :newIndex where home_index = :oldIndex and owner_id = :level")
    suspend fun updateHomeIndex(oldIndex:Int, newIndex:Int, level: Int)

    @Query("update letter_info_table set friend_index = :newFriendIndex where home_index = :homeIndex and owner_id = :level")
    suspend fun updateFriendIndex(homeIndex:Int, newFriendIndex:Int, level: Int)

    @Query("update letter_info_table set letter = :letter where home_index = :homeIndex and owner_id = :level")
    suspend fun updateLetter(homeIndex:Int, letter:Char, level: Int)

    @Query("select letter from letter_info_table where owner_id=:levelNr and friend_index!=-666 order by friend_index asc")
    suspend fun currentSolution(levelNr: Int): List<Char>

    @Transaction
    suspend fun switchHomeIndexes(index1:Int, index2:Int, level:Int)
    {
        updateHomeIndex(index1,-420, level)
        updateHomeIndex(index2, index1, level)
        updateHomeIndex(-420, index2, level)
    }

    @Update
    suspend fun updateLevelData(levelInfo: LevelData)

    @Update
    suspend fun updateLettersProperties(level: List<LetterProperties>)

    @Transaction
    suspend fun updateLevels(levels: List<Level>)
    {
        levels.forEach {
            updateLevelData(it.levelInfo)
            updateLettersProperties(it.lettersProperties)
        }
    }

    @Transaction
    @Query("select * from level_info_table")
    suspend fun getAllLevels(): List<Level>

    @Query("select last_modified_timestamp from meta_data limit 1")
    suspend fun getLocalLastModifiedTimestamp(): Long

}