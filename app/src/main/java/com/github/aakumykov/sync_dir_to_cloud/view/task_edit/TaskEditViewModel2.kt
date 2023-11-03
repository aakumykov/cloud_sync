package com.github.aakumykov.sync_dir_to_cloud.view.task_edit

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.aakumykov.sync_dir_to_cloud.R
import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncTask
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.TaskManagingViewModel
import com.github.aakumykov.sync_dir_to_cloud.view.common_view_models.op_state.OpState
import com.github.aakumykov.sync_dir_to_cloud.view.view_utils.TextMessage
import kotlinx.coroutines.launch

class TaskEditViewModel2(application: Application) : TaskManagingViewModel(application) {

    private var _currentSyncTask: SyncTask? = null
    val currentSyncTask get(): SyncTask? = _currentSyncTask

    private val _syncTaskMutableLiveData: MutableLiveData<SyncTask> = MutableLiveData()
    val syncTaskLiveData: LiveData<SyncTask> = _syncTaskMutableLiveData


    fun saveSyncTask() {
        viewModelScope.launch {
            currentSyncTask?.let {
                setOpState(OpState.Busy(TextMessage(R.string.saving_new_task)))
                syncTaskManagingUseCase.createOrUpdateSyncTask(it)
                setOpState(OpState.Success(TextMessage(R.string.task_saved)))
            }
        }
    }


    fun prepareForEdit(taskId: String) {
        viewModelScope.launch {
            val syncTask = syncTaskManagingUseCase.getSyncTask(taskId)
            _currentSyncTask = syncTask
            _syncTaskMutableLiveData.value = syncTask
        }
    }


    fun prepareForCreate() {
        val newSyncTask = SyncTask()
        _currentSyncTask = newSyncTask
        _syncTaskMutableLiveData.value = newSyncTask
    }
}