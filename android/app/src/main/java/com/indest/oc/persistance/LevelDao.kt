package com.indest.oc.persistance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LevelDao {
    @Insert
    suspend fun insert(level: Level)

    @Update
    suspend fun update(level: Level)

    @Query("SELECT * from level_info_table WHERE id = :key")
    suspend fun get(key: Int): Level

    @Query("DELETE FROM level_info_table")
    suspend fun clear()

    @Query("SELECT * FROM level_info_table")
    suspend fun getAllLevels(): List<Level>
}