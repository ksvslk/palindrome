package com.indest.hah.persistance.level

import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.dto.PlayerLevel
import com.indest.hah.persistance.dto.SaveGame
import kotlinx.coroutines.flow.Flow
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

    suspend fun getMaxLevelId():Int = levelDao.getMaxLevelId()

    suspend fun insertPlayerLevels(playerLevels:List<PlayerLevel>):Pair<LevelData,Int> {
        return levelDao.insertPlayerLevels(playerLevels)
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

    suspend fun getCurrentSolution(level: Int): String {
        val guess = levelDao.getCurrentSolution(level).joinToString("")
        return guess.map<Any> { char -> if ("^([?!.,’])".contains(char)) "" else char }
            .joinToString("")
    }

    suspend fun updateScore(newScore: Int, level: Int) {
        levelDao.updateScore(newScore, level)
    }

    suspend fun getTotalUpdatedScore(level: Int): Long {
        levelDao.updateBestScoreQuery(level)
        return levelDao.getTotalScore().toLong()
    }

    suspend fun getAllLevels(): List<Pair<Boolean, LevelData>> {
        val result = levelDao.getAllLevels().map {
            Pair(it.solutions.size > 1, it.levelInfo)
        }
        return result
    }

    suspend fun getAllPalindromes():MutableSet<String> {
        return levelDao.getAllPalindromes().map { it.replace("[!?’,.;:'\\-\"\\s]".toRegex(), "") }.toHashSet()
    }

    suspend fun nextLevelActions(current: Int, chosenLevel: Int) {
        levelDao.nextLevelActions(current, chosenLevel)
    }

    suspend fun setCountTo(name: String, count: Int) {
        levelDao.setCountTo(name, count)
    }

    suspend fun getIntervalCount(name: String): Int {
        return levelDao.getIntervalCount(name)
    }

    suspend fun increaseCountAndGetResult(name: String): Boolean {
        return levelDao.increaseCountTransaction(name)
    }

    suspend fun increaseCount(name:String)
    {
        levelDao.increaseCurrentCount(name)
    }

    suspend fun isLimitAttained(name:String):Boolean {
        return levelDao.isLimitAttained(name)
    }

    suspend fun executeLevelCompleteTransaction(current: Int, hintInterval: String) {
        levelDao.levelCompletedActions(current, hintInterval)
    }

    suspend fun executeLevelCompleteTransactionFinal(current: Int, hintInterval: String) {
        levelDao.levelCompletedActionsFinal(current, hintInterval)
    }

    fun latestLevelFlow(): Flow<Level> {
        return levelDao.getLastLevel()
    }

    fun intervalAttainedFlow(name:String): Flow<Boolean> {
        return levelDao.sadModeIntervalFlow(name)
    }

    suspend fun updateLevelProgress(level: Int, progress: LevelProgress) {
        levelDao.updateProgress(level, progress)
    }

    suspend fun updateCategoryRewarded(value: Boolean, level: Int) =
        levelDao.updateCategoryRewarded(value, level)

    suspend fun getLocalSaveGame(): SaveGame = levelDao.getLocalSaveGame()

    suspend fun updateLocalGame(saveGame: SaveGame) = levelDao.updateLocalGame(saveGame)

    suspend fun isFinalLevel(currentLevel:Int) = levelDao.isFinalLevel(currentLevel)

}