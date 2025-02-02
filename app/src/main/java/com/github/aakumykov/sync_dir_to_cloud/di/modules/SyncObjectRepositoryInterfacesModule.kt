package com.github.aakumykov.sync_dir_to_cloud.di.modules

import com.github.aakumykov.sync_dir_to_cloud.interfaces.SyncTaskFileObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectAdder
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectDeleter
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateResetter
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectUpdater
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.repository.SyncFileObjectRepository
import com.github.aakumykov.sync_dir_to_cloud.repository.SyncTaskRepository
import dagger.Module
import dagger.Provides

@Module
class SyncObjectRepositoryInterfacesModule {

    @Provides
    fun provideSyncObjectAdder(syncObjectRepository: SyncFileObjectRepository): SyncObjectAdder {
        return syncObjectRepository
    }

    @Provides
    fun provideSyncObjectUpdater(syncObjectRepository: SyncFileObjectRepository): SyncObjectUpdater {
        return syncObjectRepository
    }

    @Provides
    fun provideSyncTaskStateChanger(syncTaskRepository: SyncTaskRepository): SyncTaskStateChanger {
        return syncTaskRepository
    }

    @Provides
    fun provideSyncObjectReader(syncObjectRepository: SyncFileObjectRepository): SyncObjectReader {
        return syncObjectRepository
    }

    @Provides
    fun provideSyncObjectStateChanger(syncObjectRepository: SyncFileObjectRepository): SyncObjectStateChanger {
        return syncObjectRepository
    }

    @Provides
    fun provideSyncObjectStateResetter(syncObjectRepository: SyncFileObjectRepository): SyncObjectStateResetter {
        return syncObjectRepository
    }

    @Provides
    fun provideSyncObjectDeleter(syncObjectRepository: SyncFileObjectRepository): SyncObjectDeleter {
        return syncObjectRepository
    }

    @Provides
    fun provideSyncTaskFileObjectReader(syncObjectRepository: SyncFileObjectRepository): SyncTaskFileObjectReader {
        return syncObjectRepository
    }
}