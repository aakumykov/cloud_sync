package com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object

interface SyncObjectClearer {
    suspend fun clearSyncObjectsOfTask(taskId: String)
}