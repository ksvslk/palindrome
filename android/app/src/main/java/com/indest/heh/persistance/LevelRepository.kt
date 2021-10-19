package com.indest.heh.persistance

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import com.google.android.gms.games.SnapshotsClient
import kotlinx.coroutines.tasks.await
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import android.graphics.Bitmap
import com.google.android.gms.games.snapshot.Snapshot
import com.google.gson.Gson
import com.indest.heh.util.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


@Singleton
class LevelRepository @Inject constructor(
    private val levelDao: LevelDao
) {

    private val TAG = "LevelRepository"
    lateinit var coverImage: Bitmap

    // Local
    suspend fun getLevel(nr: Int) = levelDao.getLevel(nr)

    suspend fun getLastLevel() = levelDao.getLastLevel()

    suspend fun updateLevelPuzzle(level: Int, newPuzzle: String) {
        levelDao.updateLevelPuzzle(level, newPuzzle)
    }

    suspend fun insertLetterInfo(level: Int, homeIndex: Int, friendIndex: Int, letter: Char) {
        levelDao.insertLetterData(level, homeIndex, friendIndex, letter)
    }

    suspend fun insertLevel(levelInfo: LevelData) {
        levelDao.insert(levelInfo)
    }

    suspend fun getAllLevels(): List<Level> = levelDao.getAllLevels()

    suspend fun updateFriendIndex(homeIndex: Int, newFriendIndex: Int, level: Int) {
        levelDao.updateFriendIndex(homeIndex, newFriendIndex, level)
    }

    suspend fun updateLetter(homeIndex: Int, letter: Char, level: Int) {
        levelDao.updateLetter(homeIndex, letter, level)
    }

    private suspend fun updateLevels(levels: List<Level>) {
        levelDao.updateLevels(levels)
    }

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
        if (lastSignedInAccount != null)
        {
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
    private fun loadBytes(snapshot: Snapshot?)
    {
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
        val desc = "Level ${getLastLevel().levelInfo.levelId}"

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