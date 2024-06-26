package com.github.aakumykov.sync_dir_to_cloud.view.task_state

import com.github.aakumykov.sync_dir_to_cloud.domain.entities.SyncObject

class SyncStateItem(private val syncObject: SyncObject) {

    val title: String get() = "${syncObject.name} (${modificationState()}/${synchronizationState()})"


    private fun modificationState(): String = syncObject.modificationState.name

    private fun synchronizationState(): String = syncObject.syncState.name
}
