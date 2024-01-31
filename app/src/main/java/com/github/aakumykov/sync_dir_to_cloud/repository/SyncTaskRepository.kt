package com.github.aakumykov.sync_dir_to_cloud.repository

import androidx.lifecycle.LiveData
import com.github.aakumykov.sync_dir_to_cloud.di.annotations.AppScope
import com.github.aakumykov.sync_dir_to_cloud.di.annotations.DispatcherIO
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskCreatorDeleter
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskUpdater
import com.github.aakumykov.sync_dir_to_cloud.repository.room.SyncTaskDAO
import com.github.aakumykov.sync_dir_to_cloud.repository.room.SyncTaskSchedulingStateDAO
import com.github.aakumykov.sync_dir_to_cloud.repository.room.SyncTaskStateDAO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AppScope
class SyncTaskRepository @Inject constructor(
    private val syncTaskDAO: SyncTaskDAO,
    private val syncTaskStateDAO: SyncTaskStateDAO,
    private val syncTaskSchedulingStateDAO: SyncTaskSchedulingStateDAO,
    private val coroutineScope: CoroutineScope,
    @DispatcherIO private val coroutineDispatcher: CoroutineDispatcher // FIXME: не нравится мне это здесь
)
    : SyncTaskCreatorDeleter, SyncTaskReader, SyncTaskUpdater, SyncTaskStateChanger
{
    override suspend fun listSyncTasks(): LiveData<List<SyncTask>> {
//        return syncTaskLocalDataSource.listSyncTasks()
        return syncTaskDAO.list()
    }

    override suspend fun getSyncTask(id: String): SyncTask {
//        return syncTaskLocalDataSource.getTask(id)
        return syncTaskDAO.get(id)
    }

    override suspend fun getSyncTaskAsLiveData(taskId: String): LiveData<SyncTask> {
        return syncTaskDAO.getAsLiveData(taskId)
    }

    override suspend fun createSyncTask(syncTask: SyncTask) {
//        syncTaskLocalDataSource.addTask(syncTask)
        syncTaskDAO.addSuspend(syncTask)
    }

    override suspend fun deleteSyncTask(syncTask: SyncTask) {
//        syncTaskLocalDataSource.delete(syncTask)
        syncTaskDAO.deleteSuspend(syncTask)
    }

    override fun updateSyncTask(syncTask: SyncTask) {
        coroutineScope.launch(coroutineDispatcher) {
            syncTaskDAO.update(syncTask)
        }
    }

    override suspend fun changeState(taskId: String, newSate: SyncTask.State) {
        syncTaskStateDAO.setStateSuspend(taskId, newSate)
    }

    override suspend fun changeSchedulingState(taskId: String, newSate: SyncTask.SimpleState, errorMsg: String) {
        when(newSate) {
            SyncTask.SimpleState.IDLE -> syncTaskSchedulingStateDAO.setIdleState(taskId)
            SyncTask.SimpleState.BUSY -> syncTaskSchedulingStateDAO.setBusyState(taskId)
            SyncTask.SimpleState.ERROR -> syncTaskSchedulingStateDAO.setErrorState(taskId, errorMsg)
        }
    }

    override suspend fun changeSyncTaskEnabled(taskId: String, isEnabled: Boolean) {
        syncTaskStateDAO.setEnabled(taskId, isEnabled)
    }


    companion object {
        val TAG: String = SyncTaskRepository::class.java.simpleName
    }
}