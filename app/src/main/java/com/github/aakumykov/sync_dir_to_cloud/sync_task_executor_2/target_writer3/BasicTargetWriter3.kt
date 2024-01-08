package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.target_writer3

import android.util.Log
import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import java.io.File

abstract class BasicTargetWriter3 constructor(
    private val syncObjectReader: SyncObjectReader,
    private val syncObjectStateChanger: SyncObjectStateChanger,
    private val taskId: String,
    private val sourceDirPath: String,
    private val targetDirPath: String
)
    : TargetWriter3
{
    @Throws(IllegalStateException::class)
    override suspend fun writeToTarget(overwriteIfExists: Boolean) {

        if (null == cloudWriter())
            throw IllegalStateException("Cloud writer is null.")

        // FIXME: при ошибке создания одного каталога весь процесс прекращается, это неверно.
        syncObjectReader.getSyncObjectsForTask(taskId).filter { it.isDir }
            .forEach { syncObject ->
                writeSyncObjectToTarget(syncObject) {

                    val parentDirName = targetDirPath
                    val childDirName = syncObject.relativeParentDirPath + syncObject.name

                    try {
                        cloudWriter()?.createDir(
                            parentDirName = parentDirName,
                            childDirName = childDirName
                        )
                    } catch (e: CloudWriter.AlreadyExistsException) {
                        Log.d(tag(), "Каталог '$childDirName' уже существует в '$parentDirName'.")
                    }
                }
            }

        syncObjectReader.getSyncObjectsForTask(taskId).filter { !it.isDir }
            .forEach { syncObject ->
                writeSyncObjectToTarget(syncObject) {

                    val pathInSource = sourceDirPath + CloudWriter.DS + syncObject.relativeParentDirPath + CloudWriter.DS + syncObject.name
                    val pathInTarget = targetDirPath + CloudWriter.DS + syncObject.relativeParentDirPath + CloudWriter.DS + syncObject.name

                    cloudWriter()?.putFile(
                        file = File(pathInSource),
                        targetPath = pathInTarget,
                        overwriteIfExists = overwriteIfExists
                    )
                }
            }
    }

    private suspend fun writeSyncObjectToTarget(syncObject: SyncObject, writeAction: Runnable) {
        try {
            syncObjectStateChanger.changeState(syncObject.id, SyncObject.State.RUNNING)
            writeAction.run()
            syncObjectStateChanger.changeState(syncObject.id, SyncObject.State.SUCCESS)
        }
        catch (t: Throwable) {
            syncObjectStateChanger.setErrorState(syncObject.id, ExceptionUtils.getErrorMessage(t))
//            throw t
        }
    }

    protected abstract fun cloudWriter(): CloudWriter?

    protected abstract fun tag(): String
}
