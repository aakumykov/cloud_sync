package com.github.aakumykov.sync_dir_to_cloud.aa_v2.use_cases.reading_from_source

import android.util.Log
import com.github.aakumykov.file_lister_navigator_selector.recursive_dir_reader.RecursiveDirReader
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.CloudAuth
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.ExecutionState
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.factories.recursive_dir_reader.RecursiveDirReaderFactory
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectAdder
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectUpdater
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader.strategy.ChangesDetectionStrategy
import com.github.aakumykov.sync_dir_to_cloud.utils.calculateRelativeParentDirPath
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import javax.inject.Inject

class StorageToDatabaseLister @Inject constructor(
    private val recursiveDirReaderFactory: RecursiveDirReaderFactory,
    private val syncObjectReader: SyncObjectReader,
    private val syncObjectAdder: SyncObjectAdder,
    private val syncObjectUpdater: SyncObjectUpdater,
    private val syncTaskStateChanger: SyncTaskStateChanger,
) {
    suspend fun readFromPath(
        pathReadingFrom: String?,
        changesDetectionStrategy: ChangesDetectionStrategy,
        cloudAuth: CloudAuth?,
        taskId: String
    ): Result<Boolean> {

        return try {
            if (null == pathReadingFrom)
                throw IllegalArgumentException("path argument is null")

            if (null == cloudAuth)
                throw IllegalArgumentException("cloudAuth argument is null")

            syncTaskStateChanger.setSourceReadingState(taskId, ExecutionState.RUNNING)

            recursiveDirReaderFactory.create(cloudAuth.storageType, cloudAuth.authToken)
                ?.listDirRecursively(
                    path = pathReadingFrom,
                    foldersFirst = true
                )
                ?.apply {
                    syncTaskStateChanger.setSourceReadingState(taskId, ExecutionState.SUCCESS)
                }
                ?.forEach { fileListItem ->
                    addOrUpdateFileListItem(fileListItem, pathReadingFrom, taskId, changesDetectionStrategy)
                }

            Result.success(true)

        } catch (e: Exception) {
            ExceptionUtils.getErrorMessage(e).also { errorMsg ->
                syncTaskStateChanger.setSourceReadingState(taskId, ExecutionState.ERROR, errorMsg)
                Log.e(TAG, errorMsg, e)
            }
            Result.failure(e)
        }
    }

    private suspend fun addOrUpdateFileListItem(
        fileListItem: RecursiveDirReader.FileListItem,
        pathReadingFrom: String,
        taskId: String,
        changesDetectionStrategy: ChangesDetectionStrategy
    ) {
        val inTargetParentDirPath = calculateRelativeParentDirPath(fileListItem, pathReadingFrom)

        val existingObject = syncObjectReader.getSyncObject(
            taskId,
            fileListItem.name,
            inTargetParentDirPath)


        if (null == existingObject) {
            syncObjectAdder.addSyncObject(
                // TODO: сделать определение нового родительского пути более понятным
                SyncObject.createAsNew(
                    taskId,
                    fileListItem,
                    inTargetParentDirPath
                )
            )
        }
        else {
            syncObjectUpdater.updateSyncObject(
                SyncObject.createFromExisting(
                    existingSyncObject = existingObject,
                    modifiedFSItem = fileListItem,
                    changesDetectionStrategy.detectItemModification(pathReadingFrom, fileListItem, existingObject)
                )
            )
        }
    }

    companion object {
        val TAG: String = StorageToDatabaseLister::class.java.simpleName
    }
}