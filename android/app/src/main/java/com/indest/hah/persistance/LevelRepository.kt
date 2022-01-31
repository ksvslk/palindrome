package com.indest.hah.persistance

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.Drive.SCOPE_APPFOLDER
import com.google.android.gms.games.Games
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.util.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
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

}