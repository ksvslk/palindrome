package com.indest.heh.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.indest.heh.persistance.LevelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CoroutineSaveGameWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val levelRepository: LevelRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val result = levelRepository.updateCloudSnapshot(appContext)
        return if (result) Result.success() else Result.failure()
    }
}