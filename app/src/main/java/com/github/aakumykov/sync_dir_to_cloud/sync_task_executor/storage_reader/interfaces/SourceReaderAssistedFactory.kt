package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader.interfaces

import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader.strategy.ChangesDetectionStrategy

interface StorageReaderAssistedFactory {
    fun create(authToken: String,
               taskId: String,
               changesDetectionStrategy: ChangesDetectionStrategy): StorageReader
}