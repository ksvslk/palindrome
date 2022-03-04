package com.indest.hah.savegame

import android.util.Log
import kotlinx.coroutines.delay

class SnapshotHelper {
    private val TAG = "SnapshotHelper"

    suspend fun loadGameFromCloud(): Boolean {
        delay(3000)
        val ran = (0..1).random()
        if (ran == 1) {
            throw RuntimeException("Could not load game")
        } else {
            Log.d(TAG, "loadded game!")
            return true
        }
    }
}