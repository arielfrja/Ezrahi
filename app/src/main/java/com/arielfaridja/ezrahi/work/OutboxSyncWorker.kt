package com.arielfaridja.ezrahi.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arielfaridja.ezrahi.core.network.transport.TacticalDispatchEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OutboxSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dispatchEngine: TacticalDispatchEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        dispatchEngine.flushOutbox()
        return if (dispatchEngine.pendingCount() == 0) Result.success() else Result.retry()
    }
}