package com.github.aakumykov.sync_dir_to_cloud.better_task_executor

import android.util.Log
import androidx.annotation.StringRes
import com.github.aakumykov.sync_dir_to_cloud.R
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.bas_state_resetter.BadDateResetter
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.beter_source_reader.BetterSourceReaderAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.better_backuper.BetterBackuperAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.better_dir_maker.BetterDirMakerAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.better_file_copier.BetterFileCopierAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.better_file_deleter.BetterFileDeleterAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.better_target_reader.BetterTargetReaderAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.dir_deleter_creator.BetterDirDeleterAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.exceptions.TaskExecutionException
import com.github.aakumykov.sync_dir_to_cloud.better_task_executor.sync_state_logger.SyncStateLogger
import com.github.aakumykov.sync_dir_to_cloud.extensions.errorMsg
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.view.other.utils.TextMessage
import javax.inject.Inject

class BetterTaskExecutor @Inject constructor(
    private val syncTaskReader: SyncTaskReader,

    private val syncTaskStateChanger: SyncTaskStateChanger,
    private val syncStateLogger: SyncStateLogger,

    private val badStateResetter: BadDateResetter,

    private val sourceReaderFactory: BetterSourceReaderAssistedFactory,
    private val targetReaderFactory: BetterTargetReaderAssistedFactory,

    private val backuperFactory: BetterBackuperAssistedFactory,

    private val fileDeleterCreator: BetterFileDeleterAssistedFactory,
    private val dirDeleterCreator: BetterDirDeleterAssistedFactory,

    private val dirCreatorFactory: BetterDirMakerAssistedFactory,
    private val fileCopierFactory: BetterFileCopierAssistedFactory,
) {
    //
    // Код, чтобы отличать один запуск от другого.
    // Задание одно, эпизодов его запуска много.
    //
    private val executionId: String = hashCode().toString()

    //
    // Оформлено именно так, чтобы бросало исключения, если забыл
    // инициализировать.
    //
    private var _currentTaskId: String? = null
    private val taskId: String get() = _currentTaskId!!


    suspend fun executeSyncTask(taskId: String) {

        try {
            // установка состояния "выполняется"
            syncTaskStateChanger.setRunningState(taskId)

            //
            // Получаю здесь SyncTask и передаю его остальным участниками,
            // чтобы все работали с идентинчым экземпляром, а не получали
            // его из БД по ходу выполнения задачи, когда он уже мог измениться.
            //
            val syncTask = syncTaskReader.getSyncTask(taskId)
            if (null == syncTask)
                throw TaskExecutionException.CriticalException.TaskNotFoundException(taskId)


            // сбросить ошибочные статусы, оставшиеся при аварийном завершении программы
            logSyncState(R.string.SYNC_OBJECT_LOGGER_resetting_bad_sates)
            badStateResetter.resetBadStates(syncTask)


            // прочитать источник
            // fixme: отразить в названии то, что не просто читается состояние файлов источника,
            //  а идёт их сравнение с существующими файлами (точнее, записями о них в БД)
            logSyncState(R.string.SYNC_OBJECT_LOGGER_reading_source)
            sourceReaderFactory.create(syncTask).readSourceFilesState()


            // прочитать приёмник
            // fixme: отразить в названии то, что не просто читается состояние файлов приёмника,
            //  а идёт их сравнение с существующими файлами (точнее, записями о них в БД)
            logSyncState(R.string.SYNC_OBJECT_LOGGER_reading_target)
            targetReaderFactory.create(syncTask).readTargetFilesState()


            // забекапить изменённое/удалённое
            logSyncState(R.string.SYNC_OBJECT_LOGGER_backuping_modified_deleted)
            backuperFactory.create(syncTask).backupItems()


            // удалить удалённые файлы (делать это перед удалением каталогов!)
            logSyncState(R.string.SYNC_OBJECT_LOGGER_deleting_file)
            fileDeleterCreator.create(syncTask).deleteDeletedFiles()

            // удалить удалённые каталоги (делать это после удалением файлов!)
            dirDeleterCreator.create(syncTask).deleteDeletedDirs()


            // создать каталоги
            logSyncState(R.string.SYNC_OBJECT_LOGGER_create_new_dir)
            dirCreatorFactory.create(syncTask).createDirs()

            logSyncState(R.string.SYNC_OBJECT_LOGGER_create_never_processed_dir)
//            createNeverSyncedDirs(syncTask)

            logSyncState(R.string.SYNC_OBJECT_LOGGER_create_in_target_lost_dir)
//            createLostDirsAgain(syncTask)


            // скопировать файлы
            logSyncState(R.string.SYNC_OBJECT_LOGGER_copy_new_file)
            fileCopierFactory.create(syncTask).copyFiles()

            logSyncState(R.string.SYNC_OBJECT_LOGGER_copy_modified_file)
//            copyModifiedFiles(syncTask)

            logSyncState(R.string.SYNC_OBJECT_LOGGER_copy_previously_forgotten_file)
//            copyPreviouslyForgottenFiles(syncTask)

            logSyncState(R.string.SYNC_OBJECT_LOGGER_copy_in_target_lost_file)
//            copyLostFilesAgain(syncTask )


            // установка состояния "успешно выполнено"
            syncTaskStateChanger.setSuccessState(taskId)
        }
        catch (e: TaskExecutionException.NonCriticalException) {
            // TODO: регистрировать здесь или в классе, который это делает?
            Log.e(TAG, e.errorMsg)
        }
        catch (e: Exception) {
            // установка состояния "успешно выполнено"
            syncTaskStateChanger.setErrorState(taskId, e)

            // Выбрасываю исключение, чтобы оно ушло Worker-у
            throw e
        }
    }

    private fun logSyncState(@StringRes stringRes: Int) {
        syncStateLogger.logSyncState(taskId, executionId, TextMessage(stringRes))
    }


    companion object {
        val TAG: String = BetterTaskExecutor::class.java.simpleName
    }
}