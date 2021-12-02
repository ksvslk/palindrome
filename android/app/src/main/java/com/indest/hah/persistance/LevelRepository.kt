package com.indest.hah.persistance

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
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
    lateinit var coverImage: Bitmap

    suspend fun resetLevelLettersInfo(levelNr: Int) = levelDao.deleteLetterInfo(levelNr)
    suspend fun updateLevel(level: LevelData) = levelDao.updateLevelData(level)
    suspend fun getLevel(nr: Int) = levelDao.getLevel(nr)
    fun getLastLevel() = levelDao.getLastLevel()

    suspend fun updateLevelPuzzle(level: Int, newPuzzle: String) {
        levelDao.updateLevelPuzzle(level, newPuzzle)
    }

    suspend fun insertLetterInfo(level: Int, homeIndex: Int, friendIndex: Int, letter: Char, hintUsed: Boolean) {
        Log.i(TAG, "insertLetterInfo")
        levelDao.insertLetterData(level, homeIndex, friendIndex, letter, hintUsed)
    }

    suspend fun insertLevel(levelInfo: LevelData) {
        levelDao.insert(levelInfo)
    }

    suspend fun getAllLevels(): List<LevelData> = levelDao.getAllLevels().map {
        it.levelInfo
    }

    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int) {
        levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
    }

    suspend fun updateLetter(homeIndex: Int, letter: Char, level: Int) {
        levelDao.updateLetter(homeIndex, letter, level)
    }


    suspend fun currentSolution(levelNr: Int?): String {
        if (levelNr != null) {
            return levelDao.getCurrentSolution(levelNr).joinToString("")
        }
        return ""
    }

    suspend fun isLettersInitialized(nr:Int) = levelDao.areLettersInitialized(nr) > 0
    private suspend fun updateLevels(levels: List<Level>) = levelDao.updateLevels(levels)
    suspend fun updateNextLevelTimestamp(current:Int, next:Int) = levelDao.updateNextLevelTimestamp(current, next)
    suspend fun updateScore(newScore:Int, level:Int) = levelDao.updateScore(newScore, level)

    // Cloud
    // Upload to cloud onStop()
    suspend fun updateCloudSnapshot(appContext: Context): Boolean {
        var result = false
        val lastSignedInAccount =
            GoogleSignIn.getLastSignedInAccount(appContext) ?: return false
        Log.i(TAG, "lastSignedInAccount OK!")
        val snapshotsClient = Games.getSnapshotsClient(appContext, lastSignedInAccount)
        val conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_LONGEST_PLAYTIME
        val snap = snapshotsClient.open(C.SAVE_GAME, true, conflictResolutionPolicy).await()
        Log.i(TAG, "snapshotsClient open OK!")
        val data = snap?.data
        if (data is Snapshot) {
            if (writeSnapshot(data, snapshotsClient, coverImage)) result = true
        }
        return result
    }

    // Update DB when signed in
    suspend fun updateLocalDb(appContext: Context) {
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(appContext)
        if (lastSignedInAccount != null) {
            Log.i(TAG, "lastSignedInAccount OK!")
            val snapshotsClient = Games.getSnapshotsClient(appContext, lastSignedInAccount)
            val conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_LONGEST_PLAYTIME
            val snap = withContext(Dispatchers.IO) {
                snapshotsClient.open(C.SAVE_GAME, true, conflictResolutionPolicy).await()
            }
            Log.i(TAG, "snapshotsClient open OK!")
            val data = snap?.data
            if (data is Snapshot) {
                loadBytes(data)
            }
        }
    }

    private fun loadBytes(snapshot: Snapshot?) {
        val gson = Gson()
        val bytes = snapshot?.snapshotContents?.readFully()
        val levelsListType: Type = object : TypeToken<ArrayList<Level?>?>() {}.type
        Log.i(TAG, bytes.toString())
        Log.i(TAG, bytes!!.decodeToString())
        val levelsList: List<Level> = gson.fromJson(bytes.decodeToString(), levelsListType)
        Log.i(TAG, levelsList.toString())
    }

    private suspend fun writeSnapshot(
        snapshot: Snapshot,
        snapshotsClient: SnapshotsClient,
        coverImage: Bitmap
    ): Boolean {

        val gson = Gson()
        val levelsData = gson.toJson(getAllLevels()).toByteArray()
        //val desc = "Level ${getLastLevel().levelInfo.levelId}" // H
        val desc = "UNDO!" // TODO:UNDO

        // Set the data payload for the snapshot
        snapshot.snapshotContents.writeBytes(levelsData).run {
            if (this) {
                Log.i(TAG, "succesFullyWrittenToDisk OK: $this")
                // Create the change operation
                Log.i(TAG, "Coverimage: ${coverImage.allocationByteCount}")
                val metadataChange = SnapshotMetadataChange.Builder()
                    .setCoverImage(coverImage)
                    .setDescription(desc)
                    .build()
                // Commit the operation
                kotlin.runCatching {
                    snapshotsClient.commitAndClose(snapshot, metadataChange).await()
                }.onSuccess {
                    Log.i(TAG, "commitAndClose OK!")
                    return true
                }.onFailure {
                    Log.i(TAG, "commitAndClose went SIDEWAYS!")
                    return false
                }
            } else {
                Log.e(TAG, "Could not write the data to disk!")
                return false
            }
        }
        return false
    }

}