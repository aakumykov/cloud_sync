package com.github.aakumykov.sync_dir_to_cloud.sync_task_logger

import android.util.Log
import com.github.aakumykov.sync_dir_to_cloud.di.annotations.ExecutionScope
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.ExecutionLogItem
import com.github.aakumykov.sync_dir_to_cloud.repository.SyncTaskLogRepository

@ExecutionScope
class DatabaseSyncTaskLogger (
    private val syncTaskLogRepository: SyncTaskLogRepository
): SyncTaskLogger {

    override suspend fun log(executionLogItem: ExecutionLogItem) {
        Log.d(TAG, "log() called with: taskLogEntry = $executionLogItem")
//        syncTaskLogRepository.addLogEntry(taskLogEntry)
    }

    companion object {
        val TAG: String = DatabaseSyncTaskLogger::class.java.simpleName
    }
}