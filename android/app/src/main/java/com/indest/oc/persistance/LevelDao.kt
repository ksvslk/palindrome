package com.indest.oc.persistance

import androidx.room.*

@Dao
interface LevelDao {

    @Delete
    suspend fun delete(level: Level)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(level: Level)

    @Query("select * from level_info_table where level_id = :key")
    suspend fun get(key: Int): Level

    // TODO: change progress to 1
    @Transaction
    @Query("select * from level_info_table where progress = 0 order by level_id asc limit 1")
    suspend fun getLastLevel(): LevelAndLetterInfo

    @Query("update level_info_table set puzzle = :newPuzzle where level_id = :level")
    suspend fun updateLevelPuzzle(level: Int, newPuzzle: String)

    @Query("insert into letter_info_table (owner_id, home_index, friend_index, letter) values(:level, :homeIndex, :friendIndex, :letter)")
    suspend fun insertLetterInfo(level: Int, homeIndex:Int, friendIndex:Int, letter:Char)

    @Query("update letter_info_table set home_index = :newIndex where home_index = :oldIndex and owner_id = :level")
    suspend fun updateHomeIndex(oldIndex:Int, newIndex:Int, level: Int)

    @Query("update letter_info_table set friend_index = :newFriendIndex where home_index = :homeIndex and owner_id = :level")
    suspend fun updateFriendIndex(homeIndex:Int, newFriendIndex:Int, level: Int)

    @Query("update letter_info_table set letter = :letter where home_index = :homeIndex and owner_id = :level")
    suspend fun updateLetter(homeIndex:Int, letter:Char, level: Int)

    @Transaction
    suspend fun switchHomeIndexes(index1:Int, index2:Int, level:Int)
    {
        updateHomeIndex(index1,-420, level)
        updateHomeIndex(index2, index1, level)
        updateHomeIndex(-420, index2, level)
    }

    @Query("delete from letter_info_table")
    suspend fun deleteLetterInfo()
}