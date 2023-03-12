package com.indest.hah.savegame

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient.DataOrConflict
import com.google.android.gms.games.SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.indest.hah.other.C
import com.indest.hah.persistance.dto.SaveGame
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@ActivityScoped
class SnapshotHelper @Inject constructor() {

    private val TAG = "SnapshotHelper"
    private val MY_RESOLUTION_POLICY = RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED

    fun save(activity: Activity, saveGame: SaveGame, calculatedGameProgress:Long)  {
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)

        Log.d(TAG, "Trying to save game..")
        snapshotsClient.open(
            C.SAVE_GAME,
            true,
            MY_RESOLUTION_POLICY
        ).addOnCompleteListener { task ->
            try {
                if (task.isSuccessful) {
                    task.result.data?.let { snapshot ->
                        val coverImage = getCoverImage(activity)
                        val bytes = gameToBytes(saveGame)
                        writeSnapshot(
                            snapshot = snapshot,
                            data = bytes,
                            coverImage = coverImage,
                            desc = "Calculated progress ${calculatedGameProgress}.",
                            progress = calculatedGameProgress,
                            activity
                        ).addOnCompleteListener {
                            if (task.isSuccessful) {
                                Log.d(TAG, "Successfully written the snapshot!")
                            } else {
                                Log.d(TAG, "Failed to write snapshot!")
                            }
                        }
                    }
                } else {
                    throw task.exception ?: Exception("Could not save the snapshot.")
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Something went wrong while saving the game :(" + e.message.toString())
            }
        }
    }

    suspend fun load(activity: Activity): SaveGame?  {
        try {
            val bytes = getSaveGameBytes(activity).await()
            if (bytes != null) {
                return bytesToGame(bytes)
            } else {
                throw Exception("Could not load game. Bytes return from the task was null.")
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Message: ${e.message}. Cause: ${e.cause} ")
        }
        return null
    }

    private fun getSaveGameBytes(activity: Activity): Task<ByteArray?> {
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)
        return snapshotsClient.open(C.SAVE_GAME, true, MY_RESOLUTION_POLICY)
            .addOnFailureListener { Log.e(TAG, "Error while opening Snapshot.", it.cause) }
            .continueWith(object : Continuation<DataOrConflict<Snapshot?>?, ByteArray?> {
                override fun then(task: Task<DataOrConflict<Snapshot?>?>): ByteArray? {
                    try {
                        val snapshot = task.result?.data
                        return snapshot?.snapshotContents?.readFully()
                    } catch (e: java.lang.Exception) {
                        Log.e(TAG, "Error while reading Snapshot.", e.cause)
                    }
                    return null
                }
            }).addOnCompleteListener { return@addOnCompleteListener }
    }


    private fun getCoverImage(activity: Activity): Bitmap {
        return BitmapFactory.decodeResource(activity.resources, com.indest.hah.R.drawable.o3)
            ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }


    private fun writeSnapshot(
        snapshot: Snapshot,
        data: ByteArray,
        coverImage: Bitmap,
        desc: String,
        progress: Long,
        activity: Activity
    ): Task<SnapshotMetadata?> {

        val snapshotsClient = PlayGames.getSnapshotsClient(
            activity
        )
        // Set the data payload for the snapshot
        snapshot.snapshotContents.writeBytes(data)

        // Create the change operation
        val metadataChange = SnapshotMetadataChange.Builder()
            .setCoverImage(coverImage)
            .setDescription(desc)
            .setProgressValue(progress)
            .build()

        // Commit the operation
        return snapshotsClient.commitAndClose(snapshot, metadataChange)
    }

    fun bytesToGame(byteArray: ByteArray): SaveGame {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return json.decodeFromString(byteArray.decodeToString())
    }

    fun gameToBytes(saveGame: SaveGame): ByteArray {
        return Json.encodeToString(saveGame).toByteArray()
    }

}