package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor

import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.cloud_auth.CloudAuthReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectClearer
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.source_reader.creator.SourceReaderCreator
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.source_reader.interfaces.SourceReader
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.target_writer.TargetWriter
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.target_writer.factory_and_creator.TargetWriterCreator
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import javax.inject.Inject

class SyncTaskExecutor @Inject constructor(
    private val sourceReaderCreator: SourceReaderCreator,
    private val targetWriterCreator: TargetWriterCreator,
    private val cloudAuthReader: CloudAuthReader,
    private val syncTaskReader: SyncTaskReader,
    private val syncTaskStateChanger: SyncTaskStateChanger,
    private val syncTaskNotificator: SyncTaskNotificator,
    private val syncObjectClearer: SyncObjectClearer
) {
    private var sourceReader: SourceReader? = null
    private var mTargetWriter: TargetWriter? = null


    // FIXME: Не ловлю здесь исключения, чтобы их увидел SyncTaskWorker. А как устойчивость к ошибкам?
    suspend fun executeSyncTask(taskId: String) {
        syncTaskReader.getSyncTask(taskId).also {  syncTask ->
            prepareReader(syncTask)
            prepareWriter(syncTask)
            doWork(syncTask)
        }
    }

    // TODO: перенести в отдельный класс?
    suspend fun taskSummary(taskId: String): String = syncTaskReader.getSyncTask(taskId).summary()


    private fun prepareReader(syncTask: SyncTask) {
        // TODO: реализовать sourceAuthToken
        sourceReader = sourceReaderCreator.create(syncTask.sourceType, "", syncTask.id)
    }


    private suspend fun prepareWriter(syncTask: SyncTask) {

        // FIXME: убрать двойное !!

        val targetAuthToken = cloudAuthReader.getCloudAuth(syncTask.cloudAuthId!!)?.authToken
            ?: throw IllegalStateException("Target auth token cannot be null")

        mTargetWriter = targetWriterCreator.create(
            syncTask.targetType!!,
            targetAuthToken,
            syncTask.id,
            syncTask.sourcePath!!,
            syncTask.targetPath!!
        )
    }

    // FIXME: убрать!! в sourcePath
    private suspend fun doWork(syncTask: SyncTask) {

        val taskId = syncTask.id
        val notificationId = syncTask.notificationId

        syncObjectClearer.clearSyncObjectsOfTask(taskId)

        try {
            syncTaskStateChanger.changeExecutionState(taskId, SyncTask.SimpleState.BUSY)

            syncTaskNotificator.showNotification(
                taskId,
                notificationId,
                SyncTask.State.READING_SOURCE
            )
            sourceReader?.read(syncTask.sourcePath!!)

            syncTaskNotificator.showNotification(
                taskId,
                notificationId,
                SyncTask.State.WRITING_TARGET
            )
            mTargetWriter?.writeToTarget()

            syncTaskStateChanger.changeExecutionState(taskId, SyncTask.SimpleState.IDLE)
        }
        catch (t: Throwable) {
            syncTaskStateChanger.changeExecutionState(taskId, SyncTask.SimpleState.ERROR, ExceptionUtils.getErrorMessage(t))
        }
        finally {
            syncTaskNotificator.hideNotification(taskId, notificationId)
        }
    }

    companion object {
        val TAG: String = SyncTaskExecutor::class.java.simpleName
    }
}