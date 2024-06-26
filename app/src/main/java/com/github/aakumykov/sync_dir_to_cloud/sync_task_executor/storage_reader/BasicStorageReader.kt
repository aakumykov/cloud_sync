package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader

import android.util.Log
import com.github.aakumykov.file_lister_navigator_selector.file_lister.FileLister
import com.github.aakumykov.file_lister_navigator_selector.file_lister.SimpleSortingMode
import com.github.aakumykov.file_lister_navigator_selector.recursive_dir_reader.RecursiveDirReader
import com.github.aakumykov.sync_dir_to_cloud.factories.RecursiveDirReaderFactory
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageHalf
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageType
import com.github.aakumykov.sync_dir_to_cloud.extensions.tag
import com.github.aakumykov.sync_dir_to_cloud.repository.SyncObjectRepository
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader.interfaces.StorageReader
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader.strategy.ChangesDetectionStrategy
import com.github.aakumykov.sync_dir_to_cloud.utils.calculateRelativeParentDirPath

abstract class BasicStorageReader(
    private val taskId: String,
    private val authToken: String,
    private val recursiveDirReaderFactory: RecursiveDirReaderFactory,
    private val changesDetectionStrategy: ChangesDetectionStrategy,
    private val syncObjectRepository: SyncObjectRepository
)
    : StorageReader
{
    protected abstract val storageType: StorageType


    private val recursiveDirReader: RecursiveDirReader?
        get() = recursiveDirReaderFactory.create(storageType, authToken)


    @Throws(FileLister.NotADirException::class)
    override suspend fun read(storageHalf: StorageHalf, sourcePath: String?) {

        if (null == sourcePath) {
            Log.e(tag, "Source path is null.")
            return
        }

        recursiveDirReader?.getRecursiveList(
            path = sourcePath,
            sortingMode = SimpleSortingMode.NAME,
            reverseOrder = false,
            foldersFirst = true,
            dirMode = false
        )?.forEach { fileListItem: RecursiveDirReader.FileListItem ->

            val existingObject = syncObjectRepository.getSyncObject(storageHalf, taskId, fileListItem.name)

            if (null == existingObject) {
                SyncObject.createAsNew(
                    storageHalf = storageHalf,
                    taskId = taskId,
                    fsItem = fileListItem,
                    relativeParentDirPath = calculateRelativeParentDirPath(fileListItem, sourcePath),
                )
                    .also {
                        syncObjectRepository.addSyncObject(it)
                    }
            }
            else {
                SyncObject.createAsExisting(
                    existingObject,
                    fileListItem,
                    changesDetectionStrategy.detectItemModification(sourcePath, fileListItem, existingObject)
                )
                    .also {
                        syncObjectRepository.updateSyncObject(it)
                    }
            }
        }
    }
}