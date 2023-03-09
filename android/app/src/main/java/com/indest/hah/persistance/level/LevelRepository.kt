package com.indest.hah.persistance.level

import com.indest.hah.persistance.dto.*
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant

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

    suspend fun changeFriendIndexesBasedOnColumnCount(level: Int, columnCount:Int, letters:List<LetterProperties>, homeIndexNewFriendIndexes:List<Pair<Int,Int>>) {
        levelDao.changeFriendIndexesBasedOnColumnCount(level, columnCount, letters, homeIndexNewFriendIndexes)
    }


    suspend fun changeColumnCount(level: Int, columnCount: Int){
        levelDao.changeColumnCount(level, columnCount)
    }

    suspend fun getMaxLevelId():Int = levelDao.getMaxLevelId()

    suspend fun getMaxGameLevelId():Int = levelDao.getMaxGameLevels()

    suspend fun insertPlayerLevels(playerLevels:List<PlayerLevel>):Pair<LevelData,Int> {
        return levelDao.insertPlayerLevels(playerLevels)
    }

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
        if (level > 1000)
        {
            val playerLevelScore = levelDao.getTotalPlayerLevelsScore()
            return playerLevelScore?.toLong() ?: 0
        }
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

    suspend fun chooseLevelTransaction(currentLevel: Int, chosenLevel: Int, hintInterval: String) {
        levelDao.chooseLevelActions(currentLevel, chosenLevel, hintInterval)
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

    suspend fun isFinalLevel(currentLevel:Int, minLimit:Int, maxLimit:Int) = levelDao.isFinalLevel(currentLevel, minLimit, maxLimit)

    suspend fun isAllowedToSubmitPalindrome():Boolean
    {
        val lastSubmission = levelDao.getLastSubmissionTimestamp()
        if (lastSubmission == -1L || lastSubmission == null) return true
        val lastTime = Instant.ofEpochMilli(lastSubmission)
        val currentTime = Instant.now()
        val duration = Duration.between(lastTime, currentTime)
        return duration.toHours() >= 24
    }

    suspend fun updateSubmissionTimestamp()
    {
        val millis = Instant.now().toEpochMilli()
        levelDao.updateSubmissionTimestamp(millis)
    }

    suspend fun updateAuthorToMigrated(
        level:Int,
        newColumnCount:Int,
        letters: List<LetterProperties>,
        homeIndexNewFriendIndexes: List<Pair<Int, Int>>) {
        levelDao.updateAuthorToMigrated(level, newColumnCount, letters, homeIndexNewFriendIndexes)
    }


    suspend fun getMaximumTimestamp():Long = levelDao.getMaximumTimestamp() ?: 0


}