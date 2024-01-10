package com.github.aakumykov.sync_dir_to_cloud.workers

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.github.aakumykov.sync_dir_to_cloud.App
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.SyncTaskNotificator
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncTaskWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val syncTaskNotificator: SyncTaskNotificator by lazy {
        App.getAppComponent().getSyncTaskNotificator()
    }

    override suspend fun doWork(): Result {

        val taskId: String = inputData.getString(TASK_ID)
            ?: return Result.failure(errorData("Task id not found in input data"))

        val syncTask = App.getAppComponent().getSyncTaskReader().getSyncTask(taskId)

        MainScope().launch {
            App.getAppComponent().getSyncTaskStateReader().getSyncTaskStateAsFlow(taskId).collect {
                syncTaskNotificator.showNotification(taskId)
            }
        }

        try {
            App.getAppComponent().getSyncTaskExecutor().executeSyncTask(syncTask)
        }
        catch (t: Throwable) {
            Log.e(TAG, ExceptionUtils.getErrorMessage(t), t)
            return Result.failure(errorData(ExceptionUtils.getErrorMessage(t)))
        }
        finally {
            // TODO: унифицировать по аргументу с методом show()
            syncTaskNotificator.hideNotification(syncTask.notificationId)
        }

        return Result.success()
    }


    // FIXME: арогумент key подразумевается один
    private fun errorData(value: String): Data {
        return Data.Builder().apply {
            putString(ERROR_MSG, value)
        }.build()
    }

    companion object {
        val TAG: String = SyncTaskWorker::class.java.simpleName
        const val TASK_ID: String = "TASK_ID"
        const val ERROR_MSG: String = "ERROR_MSG"
    }
}