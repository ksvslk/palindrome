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

    suspend fun matchFirstLevelWithUserLocale(currentTag:String) {
        levelDao.matchFirstLevelWithUserLocale(currentTag)
    }

    suspend fun getMaxLevelId():Int = levelDao.getMaxLevelId()
    suspend fun getMaxPlayerLevelId():Int?
        = levelDao.getMaxPlayerLevelId()

    suspend fun getMaxLevelDefault():Int = levelDao.getMaxLevelDefault()

    suspend fun insertPlayerLevels(playerLevels:List<PlayerLevel>):Pair<LevelData,Int> {
        return levelDao.insertPlayerLevels(playerLevels)
    }

    suspend fun insertRegularLevels(playerLevels:List<RegularLevel>) {
        return levelDao.insertRegularLevels(playerLevels)
    }

    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int): String {
        return levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
    }

    suspend fun isLanguagePackCompleted(countryCode: String):Boolean
    {
        return levelDao.isLanguagePackCompleted(countryCode) == 1
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

    suspend fun getUniqueCountriesWithNewLevelCount():List<Triple<String,Int,Int?>> {
        val languageNewLevelsCount = levelDao.getUniqueCountriesWithNewLevelCount()
        val languageLevelsCount = levelDao.getUniqueCountriesWithLevelCount()
        return languageLevelsCount.map {
            Triple(it.key, it.value, languageNewLevelsCount[it.key])
        }
    }

    suspend fun getLevelsForCountry(cc:String):List<LevelData>
        = levelDao.getLevelsForCountry(cc).map { it.levelInfo }

    suspend fun isNewPlayer(cc:String): Boolean {
        return if (cc == "gb") {
            levelDao.getUnlockedGameLevelsCount() <= 2
        } else {
            levelDao.getUnlockedCountryLevelsCount(cc) <= 2
        }
    }

    suspend fun getAllPalindromes():Map<String,List<String>> {
        return levelDao.getAllPalindromes()
    }

    suspend fun setCountTo(name: String, count: Int) {
        levelDao.setCountTo(name, count)
    }

    suspend fun increaseCountAndGetResult(name: String): Boolean {
        return levelDao.increaseCountTransaction(name)
    }

    suspend fun increaseCount(name:String)
    {
        levelDao.increaseCurrentCount(name)
    }

    suspend fun getNextLevelForLanguageLevel(levelId:Int, countryCode: String):Int?
    {
        return levelDao.getNextLevelForLanguageLevel(levelId, countryCode)
    }

    suspend fun addInterval(interval:Interval)
    {
        levelDao.insertInterval(interval)
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

    suspend fun resetLevel(level: Int)
    {
        levelDao.resetLevel(level)
    }


    suspend fun deleteLevel(level: Int)
    {
        levelDao.deleteLevel(level)
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

    suspend fun updateSadModeLimit(newLimit:Int) = levelDao.updateSadModeLimit(newLimit)

    suspend fun updateLocalGame(saveGame: SaveGame) {
        val dbIdentityHash = levelDao.getDatabaseIdentityViaQuery()
        if (dbIdentityHash == saveGame.dbIdentityHash)
        {
            levelDao.updateLocalGame(saveGame)
        }
        Log.e(TAG, "DB id for save game is: ${saveGame.dbIdentityHash} and database id is $dbIdentityHash. Does it match? ${dbIdentityHash == saveGame.dbIdentityHash}")
    }

    suspend fun isCloudSaveGameNewer(saveGame: SaveGame):Boolean
    {
        val dbIdentityHash = levelDao.getDatabaseIdentityViaQuery()
        if (dbIdentityHash == saveGame.dbIdentityHash) {
            val cloudMaxTimestamp = saveGame.game.maxOfOrNull { it.levelInfo.timeModified } ?: -1
            val localMaxTimestamp = levelDao.getMaximumTimestamp() ?: 0
            if (cloudMaxTimestamp > localMaxTimestamp) {
                return true
            }
        }
        return false
    }


    suspend fun isFinalLevel(currentLevel:Int, minLimit:Int, maxLimit:Int) =
        levelDao.isFinalLevel(currentLevel, minLimit, maxLimit)

    suspend fun isLanguagePackFinalLevel(currentLevel: Int, countryCode: String) =
        levelDao.isLanguagePackFinalLevel(currentLevel, countryCode) == 1

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