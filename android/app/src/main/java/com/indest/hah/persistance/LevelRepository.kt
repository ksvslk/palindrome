package com.indest.hah.persistance

import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.Sku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelRepository @Inject constructor(
    private val levelDao: LevelDao
) {

    private val TAG = "LevelRepository"
    suspend fun resetLevelLettersInfo(levelNr: Int) = levelDao.deleteLetterInfo(levelNr)
    suspend fun updateLevel(level: LevelData) = levelDao.updateLevelData(level)
    suspend fun updateLevelAndBestScore(level: LevelData, score: Int) =
        levelDao.updateLevelDataAndBestScore(level, score)

    suspend fun unlockNextLevel(current: Int) = levelDao.unlockNextLevel(current)
    suspend fun updateScore(newScore: Int, level: Int) = levelDao.updateScore(newScore, level)
    suspend fun getTotalScore() = levelDao.getTotalScore()
    fun getLastLevel() = levelDao.getLastLevel()
    suspend fun getLatestLevel() = levelDao.getLatestLevel()

    suspend fun insertLetterInfo(
        level: Int,
        homeIndex: Int,
        friendIndex: Int,
        letter: Char,
        locked: Boolean
    ) {
        levelDao.insertLetterData(level, homeIndex, friendIndex, letter, locked)
    }

    suspend fun getAllLevels(): List<LevelData> = levelDao.getAllLevels().map {
        it.levelInfo
    }

    suspend fun getSaveGameData(): List<Level> = levelDao.getAllLevels()

    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int) {
        levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
    }

    suspend fun setLetterLocked(homeIndex: Int, level: Int) {
        levelDao.setLetterLocked(homeIndex, level)
    }

    suspend fun updateLetter(homeIndex: Int, letter: Char, level: Int) {
        levelDao.updateLetter(homeIndex, letter, level)
    }

    suspend fun updateCategoryRewarded(value: Boolean, level: Int) {
        levelDao.updateCategoryRewarded(value, level)
    }

    suspend fun currentSolution(levelNr: Int?): String {
        if (levelNr != null) {
            return levelDao.getCurrentSolution(levelNr).joinToString("")
        }
        return ""
    }

    suspend fun isLettersInitialized(nr: Int) = levelDao.areLettersInitialized(nr) > 0

    suspend fun updateNextLevelTimestamp(current: Int, next: Int) =
        levelDao.updateNextLevelTimestamp(current, next)

    suspend fun updateLevels(list: List<Level>) {
        levelDao.updateLevels(list)
    }

    suspend fun grantSkuEntitlements(sku: String) = levelDao.grantSkuEntitlements(sku)
    suspend fun removeSkuEntitlements(sku: String) = levelDao.removeSkuEntitlements(sku)
    suspend fun isSkuPurchased(sku: String): Boolean = levelDao.isSkuPurchased(sku)
    suspend fun getAllSkus(): List<Sku> = levelDao.getAllSkus()
    suspend fun isCategoryRewarded(level:Int):Boolean = levelDao.isCategoryRewarded(level)
}