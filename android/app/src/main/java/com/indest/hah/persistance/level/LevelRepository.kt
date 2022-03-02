package com.indest.hah.persistance.level

import androidx.room.Query
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

    suspend fun init() {
        levelDao.init()
    }

    // REFACTOR
    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int): String {
        return levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
    }

    suspend fun setLetterLocked(homeIndex: Int, level: Int) {
        levelDao.setLetterLocked(homeIndex, level)
    }

    suspend fun switchLetters(
        homeIndex1: Int,
        homeIndex2: Int,
        letter1: Char,
        letter2: Char,
        level: Int
    ): String {
        return levelDao.updateLetters(homeIndex1, homeIndex2, letter1, letter2, level)
    }

    suspend fun updateScore(newScore: Int, level: Int) {
        levelDao.updateScore(newScore, level)
    }

    suspend fun getTotalUpdatedScore(level: Int): Long {
        levelDao.updateBestScoreQuery(level)
        return levelDao.getTotalScore().toLong()
    }

    suspend fun getAllLevels(): List<LevelData> {
        val result = levelDao.getAllLevels().map {
            it.levelInfo
        }
        return result
    }

    suspend fun nextLevelActions(current: Int, chosenLevel: Int) {
        levelDao.nextLevelActions(current, chosenLevel)
    }

    suspend fun setCountTo(name: String, count: Int) {
        levelDao.setCountTo(name, count)
    }

    suspend fun getCount(name: String): Int {
        return levelDao.getCount(name)
    }

    suspend fun increaseCount(name: String): Boolean {
        return levelDao.increaseCountTransaction(name)
    }

    suspend fun executeLevelCompleteTransaction(current: Int, hintInterval: String) {
        levelDao.levelCompletedActions(current, hintInterval)
    }

    fun latestLevelFlow(): Flow<Level> {
        return levelDao.getLastLevel()
    }

    suspend fun updateLevelProgress(level: Int, progress: LevelProgress) {
        levelDao.updateProgress(level, progress)
    }

    suspend fun getLocalLevels(): List<Level> {
        return levelDao.getAllLevels()
    }

    suspend fun updateLevels(levels: List<Level>) {
        levelDao.updateLevels(levels)
    }

    suspend fun updateCategoryRewarded(value: Boolean, level: Int) =
        levelDao.updateCategoryRewarded(value, level)

}