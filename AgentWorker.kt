package com.meshai.runtime

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshai.control.PolicyEngine
import com.meshai.core.model.AgentTask
import com.meshai.core.model.AgentNode
import com.meshai.core.model.TaskType
import com.meshai.storage.AgentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * WorkManager worker for constraint-aware deferred tasks.
 * Survives process death. Runs when battery/network constraints are met.
 */
@HiltWorker
class AgentWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val meshRouter: MeshRouter,
    private val agentRepository: AgentRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID)
            ?: return Result.failure()
        val task = agentRepository.getTask(taskId)
            ?: return Result.failure()
        val node = agentRepository.getLocalNode()
            ?: return Result.failure()

        return try {
            meshRouter.route(task, node)
            Timber.i("[AgentWorker] Task $taskId completed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "[AgentWorker] Task $taskId failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}
