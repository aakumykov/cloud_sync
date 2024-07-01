package com.github.aakumykov.sync_dir_to_cloud.aa_v2.use_cases.writing_to_target.dirs

import android.util.Log
import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.CloudAuth
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.ModificationState
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.enums.SyncMode
import com.github.aakumykov.sync_dir_to_cloud.extensions.absolutePathIn
import com.github.aakumykov.sync_dir_to_cloud.factories.storage_writer.CloudWriterCreator
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import java.io.File
import javax.inject.Inject

class DeletedFilesDeleter @Inject constructor(
    private val cloudWriterCreator: CloudWriterCreator,
    private val syncObjectReader: SyncObjectReader,
    private val syncObjectStateChanger: SyncObjectStateChanger
) {
    suspend fun doWork(syncTask: SyncTask, targetAuth: CloudAuth) {

        val cloudWriter = cloudWriterCreator.createCloudWriter(syncTask.targetStorageType, targetAuth.authToken)

        syncObjectReader.getAllObjectsForTask(syncTask.id)
            .filter { it.isDir && it.isDeleted }
            .forEach {  syncObject ->

                val objectId = syncObject.id
                val basePath = CloudWriter.composeFullPath(syncTask.targetPath!!, syncObject.relativeParentDirPath)

                try {
                    syncObjectStateChanger.markAsBusy(objectId)
                    // TODO: использование аргументов мудрёно
                    cloudWriter?.deleteDirRecursively(
                        // FIXME: избавиться от "!!"
                        basePath,
                        syncObject.name
                    )
                    syncObjectStateChanger.markAsSuccessfullySynced(objectId)

                } catch (t: Throwable) {
                    ExceptionUtils.getErrorMessage(t).also { errorMsg ->
                        syncObjectStateChanger.markAsError(objectId, errorMsg)
                        Log.e(TAG, errorMsg, t)
                    }
                }
            }
    }

    companion object {
        val TAG: String = DeletedFilesDeleter::class.java.simpleName
    }
}

val SyncObject.isDeleted: Boolean get() = ModificationState.DELETED == modificationState