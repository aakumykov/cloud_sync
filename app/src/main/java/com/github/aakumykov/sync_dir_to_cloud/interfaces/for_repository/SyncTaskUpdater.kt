package com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository

import com.github.aakumykov.entities.SyncTask

interface SyncTaskUpdater {
    fun updateSyncTask(syncTask: SyncTask)
}