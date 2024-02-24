package com.github.aakumykov.sync_dir_to_cloud.domain.use_cases.sync_task

import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncState
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskUpdater
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_work_manager.SyncTaskScheduler
import com.github.aakumykov.sync_dir_to_cloud.utils.MyLogger
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import javax.inject.Inject

class SchedulingSyncTaskUseCase @Inject constructor(
    private val syncTaskReader: SyncTaskReader,
    private val syncTaskScheduler: SyncTaskScheduler,
    private val syncTaskUpdater: SyncTaskUpdater,
    private val syncTaskStateChanger: SyncTaskStateChanger
) {
    // TODO: испытать поведение при ошибках

    suspend fun toggleTaskScheduling(taskId: String) {
        syncTaskReader.getSyncTask(taskId).also { syncTask ->
            if (syncTask.isEnabled) unScheduleSyncTask(syncTask)
            else scheduleSyncTask(syncTask)
        }
    }


    private suspend fun scheduleSyncTask(syncTask: SyncTask) {
        try {
            syncTaskStateChanger.changeSchedulingState(syncTask.id, SyncState.RUNNING)
            syncTaskScheduler.scheduleSyncTask(syncTask)
            syncTaskStateChanger.changeSchedulingState(syncTask.id, SyncState.SUCCESS)
            syncTaskStateChanger.changeSyncTaskEnabled(syncTask.id, true)
        }
        catch (t: Throwable) {
            syncTaskStateChanger.changeSchedulingState(syncTask.id, SyncState.ERROR, ExceptionUtils.getErrorMessage(t))
        }
    }


    suspend fun unScheduleSyncTask(syncTask: SyncTask) {
        try {
            syncTaskStateChanger.changeSchedulingState(syncTask.id, SyncState.RUNNING)
            syncTaskScheduler.unScheduleSyncTask(syncTask)
            syncTaskStateChanger.changeSchedulingState(syncTask.id, SyncState.NEVER)
            syncTaskStateChanger.changeSyncTaskEnabled(syncTask.id, false)
        }
        catch (t: Throwable) {
            syncTaskStateChanger.changeSchedulingState(syncTask.id, SyncState.ERROR, ExceptionUtils.getErrorMessage(t))
        }
    }


    private fun setSyncTaskEnabledState(syncTask: SyncTask, isEnabled: Boolean) {
        syncTask.isEnabled = isEnabled
        syncTask.schedulingError = null
        syncTaskUpdater.updateSyncTask(syncTask)
    }

    private fun setSyncTaskErrorState(syncTask: SyncTask, error: Throwable) {
        MyLogger.e(TAG, ExceptionUtils.getErrorMessage(error), error)
        syncTask.schedulingError = ExceptionUtils.getErrorMessage(error)
        syncTaskUpdater.updateSyncTask(syncTask)
    }


    companion object {
        private val TAG = SchedulingSyncTaskUseCase::class.java.simpleName
    }
}