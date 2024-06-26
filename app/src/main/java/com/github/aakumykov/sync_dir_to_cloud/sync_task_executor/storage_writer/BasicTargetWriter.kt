package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_writer

import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.ModificationState
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.ExecutionState
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageHalf
import com.github.aakumykov.sync_dir_to_cloud.extensions.absolutePathIn
import com.github.aakumykov.sync_dir_to_cloud.extensions.stripMultiSlash
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.github.aakumykov.sync_dir_to_cloud.source_file_stream_supplier.SourceFileStreamSupplier
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.ReadingStrategy
import com.github.aakumykov.sync_dir_to_cloud.utils.MyLogger
import com.github.aakumykov.sync_dir_to_cloud.utils.currentTime
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import java.io.InputStream

abstract class BasicStorageWriter (
    private val sourceDirPath: String,
    private val targetDirPath: String,
    private val taskId: String,
    private val syncObjectReader: SyncObjectReader,
    private val syncObjectStateChanger: SyncObjectStateChanger,
)
    : StorageWriter
{
    protected abstract val cloudWriter: CloudWriter?
    protected abstract val tag: String


    // TODO: ExceptionableActualWork --> kotlin.coroutines.Runnable
    @FunctionalInterface
    internal interface ExceptionableActualWork {
        @Throws(Throwable::class)
        suspend fun run()
    }


    override suspend fun write(
        sourceFileStreamSupplier: SourceFileStreamSupplier?,
        readingStrategy: ReadingStrategy,
        overwriteIfExists: Boolean
    ) {
        deleteDeletedFiles()
        deleteDeletedDirs()
        copyDirs(readingStrategy)
        copyFiles(sourceFileStreamSupplier, readingStrategy, overwriteIfExists)
    }


    private suspend fun copyDirs(readingStrategy: ReadingStrategy) {

        syncObjectReader.getObjectsNeedsToBeSynced(StorageHalf.SOURCE, taskId)
            .filter { it.isDir }
            .filter { readingStrategy.isAcceptedForSync(it) }
            .forEach { dirSyncObject ->

//                MyLogger.d(TAG, "Создание каталога (${classNameWithHash()}): '${dirSyncObject.name}'")

                writeSyncObjectToTarget(dirSyncObject, object: ExceptionableActualWork {
                    override suspend fun run() {

                        val parentDirName = targetDirPath
                        val childDirName = (dirSyncObject.relativeParentDirPath + CloudWriter.DS + dirSyncObject.name).stripMultiSlash()

                        cloudWriter?.createDir(
                            basePath = parentDirName,
                            dirName = childDirName
                        )
                    }
                })
            }
    }


    private suspend fun copyFiles(
        sourceFileStreamSupplier: SourceFileStreamSupplier?,
        readingStrategy: ReadingStrategy,
        overwriteIfExists: Boolean
    ) {
        syncObjectReader.getObjectsNeedsToBeSynced(StorageHalf.SOURCE, taskId)
            .filter { !it.isDir }
            .filter { readingStrategy.isAcceptedForSync(it) }
            .forEach { fileSyncObject ->

//                MyLogger.d(TAG, "Отправка файла (${classNameWithHash()}): '${syncObject.name}'")

                writeSyncObjectToTarget(fileSyncObject, object: ExceptionableActualWork {
                    override suspend fun run() {

                        val pathInSource = fileSyncObject.absolutePathIn(sourceDirPath)
                        val pathInTarget = fileSyncObject.absolutePathIn(targetDirPath)

                        sourceFileStreamSupplier
                            ?.getSourceFileStream(pathInSource)
                            ?.getOrThrow()
                            .also { inputStream ->
                                writeFromInputStreamToTarget(
                                    inputStream,
                                    pathInTarget,
                                    overwriteIfExists
                                )
                            }
                    }
                })
            }
    }


    private suspend fun writeSyncObjectToTarget(syncObject: SyncObject, writeAction: ExceptionableActualWork) {

//        kotlinx.coroutines.Runnable {  }

        try {
            syncObjectStateChanger.changeSyncState(syncObject.id, ExecutionState.RUNNING, "")
            writeAction.run()
            syncObjectStateChanger.changeSyncState(syncObject.id, ExecutionState.SUCCESS, "")
            syncObjectStateChanger.setSyncDate(syncObject.id, currentTime())
        }
        catch (t: Throwable) {
            ExceptionUtils.getErrorMessage(t).also { errorMsg ->
                markObjectAsFailed(syncObject, errorMsg)
                MyLogger.e(tag, errorMsg, t)
            }
        }
    }

    private fun writeFromInputStreamToTarget(inputStream: InputStream?, pathInTarget: String, overwriteIfExists: Boolean) {
        inputStream?.use {
            cloudWriter?.putFile(
                inputStream,
                pathInTarget,
                overwriteIfExists
            )
        }
    }


    private suspend fun markObjectAsFailed(syncObject: SyncObject, errorMsg: String) {
        syncObjectStateChanger.changeSyncState(syncObject.id, ExecutionState.ERROR, errorMsg)
    }


    private suspend fun deleteDeletedFiles() {
        syncObjectReader.getObjectsForTaskWithModificationState(StorageHalf.TARGET, taskId, ModificationState.DELETED)
            .filter { !it.isDir }
            .forEach { syncObject -> deleteObjectInTarget(syncObject) }
    }


    private suspend fun deleteDeletedDirs() {
        syncObjectReader.getObjectsForTaskWithModificationState(StorageHalf.TARGET, taskId, ModificationState.DELETED)
            .filter { it.isDir }
            .forEach { syncObject -> deleteObjectInTarget(syncObject) }
    }


    private suspend fun deleteObjectInTarget(syncObject: SyncObject) {
        writeSyncObjectToTarget(syncObject, object: ExceptionableActualWork {
            override suspend fun run() {
                val basePath = CloudWriter.composeFullPath(targetDirPath, syncObject.relativeParentDirPath)
                cloudWriter?.deleteFile(basePath, syncObject.name)
            }
        })
    }


    companion object {
        val TAG: String = BasicStorageWriter::class.java.simpleName
    }
}
