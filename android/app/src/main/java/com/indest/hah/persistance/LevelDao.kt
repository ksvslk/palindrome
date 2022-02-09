package com.indest.hah.persistance

import android.util.Log
import androidx.room.*
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import kotlinx.coroutines.flow.Flow
import androidx.room.Update
import com.indest.hah.persistance.dto.Sku

@Dao
interface LevelDao {

    @Transaction
    @Query("delete from letter_info_table where owner_id = :levelNr")
    suspend fun deleteLetterInfo(levelNr: Int)

    @Transaction
    @Query("select * from level_info_table where level_id = :nr")
    suspend fun getLevel(nr: Int): Level

    @Transaction
    @Query("select *, max(time_modified) from level_info_table")
    fun getLastLevel(): Flow<Level>

    @Transaction
    @Query("select *, max(time_modified) from level_info_table")
    suspend fun getLatestLevel(): Level

    @Query("insert or ignore into letter_info_table (owner_id, home_index, friend_index, letter, locked) values(:level, :homeIndex, :friendIndex, :letter, :locked)")
    suspend fun insertLetterData(
        level: Int,
        homeIndex: Int,
        friendIndex: Int,
        letter: Char,
        locked: Boolean
    )

    @Query("update or replace letter_info_table set friend_index = :newFriendIndex where home_index = :homeIndex and owner_id = :level")
    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int)

    @Query("update letter_info_table set locked = 1 where home_index = :homeIndex and owner_id = :level")
    suspend fun setLetterLocked(homeIndex: Int, level: Int)

    @Query("update or replace letter_info_table set letter = :letter where home_index = :homeIndex and owner_id = :level")
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

    @Query("update letter_info_table set home_index = :newIndex where home_index = :oldIndex and owner_id = :level")
    suspend fun updateHomeIndex(oldIndex: Int, newIndex: Int, level: Int)

    @Update
    suspend fun updateLevelData(levelInfo: LevelData)

    @Update
    suspend fun updateLevelDataAndBestScore(levelInfo: LevelData, score: Int) {
        updateLevelData(levelInfo)
        updateBestScore(score, levelInfo.levelId)
    }

    @Query("update level_info_table set progress = :progress where level_id = :current + 1")
    suspend fun updateNextLevelProgress(current: Int, progress: LevelProgress)

    @Query("select progress from level_info_table where level_id = :current + 1")
    suspend fun getNextLevelProgress(current: Int): Int

    @Transaction
    suspend fun unlockNextLevel(current: Int) {
        if (getNextLevelProgress(current) == 0) {
            updateNextLevelProgress(current, LevelProgress.IN_PROGRESS)
        }
        updateNextLevelTimestamp(current, current + 1)
    }

    @Query("update level_info_table set best_score = :score where best_score < :score and level_id = :level")
    suspend fun updateBestScore(score: Int, level: Int)

    @Query("select sum(best_score) from level_info_table")
    suspend fun getTotalScore(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(levels: LevelData)

    @Transaction
    suspend fun updateLevels(levels: List<Level>) {
        deleteLettersInfo()
        deleteLevels()
        levels.forEach { level ->
            insert(level.levelInfo)
            level.lettersProperties.forEach { lp ->
                insertLetterData(
                    level = lp.ownerId,
                    homeIndex = lp.homeIndex,
                    friendIndex = lp.friendIndex,
                    letter = lp.letter,
                    locked = lp.locked
                )
            }
        }
    }

    @Query("DELETE FROM letter_info_table")
    suspend fun deleteLettersInfo()

    @Query("DELETE FROM level_info_table")
    suspend fun deleteLevels()

    @Query("select * from level_info_table")
    suspend fun getAllLevels(): List<Level>

    @Query("update level_info_table set score=:newScore where level_id=:level")
    suspend fun updateScore(newScore: Int, level: Int)

    @Query("update level_info_table set category_rewarded=:value where level_id=:level")
    suspend fun updateCategoryRewarded(value: Boolean, level: Int)

    @Query("update sku set purchased = 1 where sku=:sku")
    suspend fun grantSkuEntitlements(sku: String)

    @Query("update sku set purchased = 0")
    suspend fun removeAllEntitlements()

    @Query("select exists(select 1 from sku where sku=:sku and purchased=1)")
    suspend fun isSkuPurchased(sku: String):Boolean

    @Query("select * from sku")
    suspend fun getAllSkus():List<Sku>

    @Query("select category_rewarded from level_info_table where level_id=:level")
    suspend fun isCategoryRewarded(level:Int):Boolean
}