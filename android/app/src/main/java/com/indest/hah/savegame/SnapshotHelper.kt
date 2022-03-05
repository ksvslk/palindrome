package com.indest.hah.savegame

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SnapshotHelper {
    private val TAG = "SnapshotHelper"

    suspend fun loadGameFromCloud(): Long = withContext(Dispatchers.Default) {
        val random = (0..1).random()
        delay(3000)
        if (random == 1) {
            throw RuntimeException("Could not load game")
        } else {
            Log.d(TAG, "loadded game!")
            return@withContext 1000
        }
    }

}