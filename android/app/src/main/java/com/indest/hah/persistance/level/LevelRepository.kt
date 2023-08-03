package com.indest.hah.persistance.level

import android.util.Log
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

    suspend fun getMaxLevelId():Int = levelDao.getMaxLevelId()
    suspend fun getMaxPlayerLevelId():Int
        = levelDao.getMaxPlayerLevelId()

    suspend fun getMinLevelIdForCountry(cc:String):Int
        = levelDao.getMinLevelIdForCountry(cc)

    suspend fun getMaxLevelIdForCountry(cc:String):Int
            = levelDao.getMaxLevelIdForCountry(cc)

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

    suspend fun resetLetterInfo(levelNr: Int)
    {
        levelDao.resetLetterInfo(levelNr)
    }

    suspend fun getCurrentSolution(level: Int): String {
        val guess = levelDao.getCurrentSolution(level).joinToString("")
        return guess.map<Any> { char -> if ("^([?!.,’])".contains(char)) "" else char }
            .joinToString("")
    }

    suspend fun updateScore(newScore: Int, level: Int) {
        levelDao.updateScore(newScore, level)
    }

    suspend fun getTotalUpdatedScore(level: Int, countryCode:String = ""): Long {
        levelDao.updateBestScoreQuery(level)
        if (level in 1001..9999)
        {
            val playerLevelScore = levelDao.getTotalPlayerLevelsScore()
            return playerLevelScore?.toLong() ?: 0
        }
        else if (level > 10000)
        {
            return levelDao.getTotalScoreForCountry(countryCode).toLong()
        }
        return levelDao.getTotalScore().toLong()
    }

    suspend fun getAllLevels(): List<Pair<Boolean, LevelData>> {
        val result = levelDao.getAllLevels().map {
            Pair(it.solutions.size > 1, it.levelInfo)
        }
        return result
    }

    suspend fun getUniqueCountries():List<String>
        = levelDao.getUniqueCountries()

    suspend fun getUniqueCountriesWithLevelCount():List<Pair<String,Int>>
        = levelDao.getUniqueCountriesWithLevelCount().map { Pair(it.key, it.value) }

    suspend fun getLevelsForCountry(cc:String):List<LevelData>
        = levelDao.getLevelsForCountry(cc).map { it.levelInfo }

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

    suspend fun executeLevelCompleteTransaction(current: Int, hintInterval: String, chosenLevel: Int) {
        levelDao.levelCompletedActions(current, hintInterval, chosenLevel)
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

    suspend fun updatePictureRewarded(value: Boolean, level: Int) =
        levelDao.updatePictureRewarded(value, level)

    suspend fun getLocalSaveGame(): SaveGame = levelDao.getLocalSaveGame()

    suspend fun updateLocalGame(saveGame: SaveGame) {
        val dbIdentityHash = levelDao.getDatabaseIdentityViaQuery()
        if (dbIdentityHash == saveGame.dbIdentityHash)
        {
            levelDao.updateLocalGame(saveGame)
        }
        Log.e(TAG, "DB id for save game is: ${saveGame.dbIdentityHash} and database id is $dbIdentityHash. Does it match? ${dbIdentityHash == saveGame.dbIdentityHash}")
    }

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

    suspend fun updateColumnCount(newCols:Int, level: Int)
    {
        levelDao.changeColumnCount(level, newColumnCount = newCols)
    }

    suspend fun updateSubmissionTimestamp()
    {
        val millis = Instant.now().toEpochMilli()
        levelDao.updateSubmissionTimestamp(millis)
    }

    suspend fun getMaximumTimestamp():Long = levelDao.getMaximumTimestamp() ?: 0

    suspend fun insertLanguageLevelPack(levels:List<LanguageLevel>, language:String)
        = levelDao.insertLanguagePackLevel(levels, language)


}