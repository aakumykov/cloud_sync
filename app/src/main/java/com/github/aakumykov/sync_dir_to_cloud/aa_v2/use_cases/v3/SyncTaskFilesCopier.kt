package com.github.aakumykov.sync_dir_to_cloud.aa_v2.use_cases.v3

import com.github.aakumykov.sync_dir_to_cloud.domain.entities.ModificationState
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import javax.inject.Inject

/**
 * Выполняет копирование всех файловых SyncObject-ов указанного SyncTask,
 * меняя статус обрабатываемого SyncObject.
 */
class SyncTaskFilesCopier @Inject constructor(
    private val syncObjectReader: SyncObjectReader,
    private val syncObjectStateChanger: SyncObjectStateChanger,
    private val syncObjectCopierCreator: SyncObjectCopierCreator
){
    suspend fun copyNewFilesForSyncTask(syncTask: SyncTask) {
        syncObjectReader.getObjectsForTaskWithModificationState(syncTask.id, ModificationState.NEW).also {
            copySyncObjects(
                syncObjectList = it,
                syncTask = syncTask,
                overwriteIfExists = true
            )
        }
    }

    suspend fun copyModifiedFilesForSyncTask(syncTask: SyncTask) {
        syncObjectReader.getObjectsForTaskWithModificationState(syncTask.id, ModificationState.MODIFIED).also {
            copySyncObjects(
                syncObjectList = it,
                syncTask = syncTask,
                overwriteIfExists = true
            )
        }
    }

    private suspend fun copySyncObjects(
        syncObjectList: List<SyncObject>,
        syncTask: SyncTask,
        overwriteIfExists: Boolean
    ) {
        val syncObjectCopier = syncObjectCopierCreator.createFileCopierFor(syncTask)

        syncObjectList.forEach { syncObject ->

            syncObjectStateChanger.markAsBusy(syncObject.id)

            syncObjectCopier
                ?.copySyncObject(syncObject, syncTask, overwriteIfExists)
                ?.onSuccess {
                    syncObjectStateChanger.markAsSuccessfullySynced(syncObject.id)
                }
                ?.onFailure {
                    ExceptionUtils.getErrorMessage(it).also { errorMsg ->
                        syncObjectStateChanger.markAsError(syncObject.id, errorMsg)
                    }
                }
        }
    }
}