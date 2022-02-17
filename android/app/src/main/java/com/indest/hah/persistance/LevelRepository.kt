package com.indest.hah.persistance

import android.util.Log
import com.indest.hah.persistance.dto.LetterProperties
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelRepository @Inject constructor(
    private val levelDao: LevelDao
) {
    private val TAG = "LevelRepository"

    suspend fun init()
    {
        levelDao.init()
    }

    private suspend fun assertInvariant(functionName:String, level:Int)
    {
        Log.d(TAG, functionName)
        assert(levelDao.checkLettersInvariant(level))
    }

    // REFACTOR
    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int):String {
        val result = levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
        assertInvariant("updateFriendIndex", level)
        return result
    }

    suspend fun setLetterLocked(homeIndex: Int, level: Int) {
        levelDao.setLetterLocked(homeIndex, level)
        assertInvariant("setLetterLocked", level)
    }

    suspend fun switchLetters(
        homeIndex1: Int,
        homeIndex2: Int,
        letter1: Char,
        letter2: Char,
        level: Int
    ):String {
        val result = levelDao.updateLetters(homeIndex1, homeIndex2, letter1,letter2, level)
        assertInvariant("switchLetters", level)
        return result
    }

    suspend fun updateScore(newScore: Int, level: Int) {
        levelDao.updateScore(newScore, level)
        assertInvariant("updateScore", level)
    }

    suspend fun getAllLevels(): List<LevelData> {
        val result = levelDao.getAllLevels().map {
            it.levelInfo
        }
        return result
    }

    suspend fun nextLevelActions(current: Int, chosenLevel: Int) {
        levelDao.nextLevelActions(current, chosenLevel)
        assertInvariant("updateNextLevelTimestamp", current)
    }

    suspend fun setCountTo(name:String, count:Int) {
        levelDao.setCountTo(name, count)
    }

    suspend fun executeLevelCompleteTransaction(current: Int, hintInterval:String)
    {
        levelDao.levelCompletedActions(current, hintInterval)
        assertInvariant("executeLevelCompleteTransaction", current)
    }

    fun latestLevelFlow(): Flow<Level> {
        val result = levelDao.getLastLevel()
        return result
    }

    suspend fun updateLevelProgress(level:Int, progress: LevelProgress)
    {
        levelDao.updateProgress(level, progress)
    }

}