package com.github.aakumykov.sync_dir_to_cloud.di

import com.github.aakumykov.sync_dir_to_cloud.ViewModelFactory
import com.github.aakumykov.sync_dir_to_cloud.appComponent
import com.github.aakumykov.sync_dir_to_cloud.di.modules.CloudReaderFactoriesModule
import com.github.aakumykov.sync_dir_to_cloud.di.annotations.AppScope
import com.github.aakumykov.sync_dir_to_cloud.di.modules.ApplicationModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.CloudAuthRepositoryInterfacesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.CloudWriterAssistedFactoriesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.ContextModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.CoroutineModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.FileListerCreatorsModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.GsonModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.NotificationModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.OkhttpModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.RoomDAOModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.SourceFileStreamSupplierAssistedFactoriesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.SourceFileStreamSupplierFactoryModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.StorageReaderAssistedFactoriesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.StorageWriterAssistedFactoriesModule3
import com.github.aakumykov.sync_dir_to_cloud.di.modules.SyncObjectRepositoryInterfacesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.SyncTaskRepositoryInterfacesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.ViewModelsModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.WorkerInterfacesModule
import com.github.aakumykov.sync_dir_to_cloud.di.modules.WorkerModule
import com.github.aakumykov.sync_dir_to_cloud.domain.use_cases.cloud_auth.CloudAuthManagingUseCase
import com.github.aakumykov.sync_dir_to_cloud.domain.use_cases.sync_task.SchedulingSyncTaskUseCase
import com.github.aakumykov.sync_dir_to_cloud.domain.use_cases.sync_task.StartStopSyncTaskUseCase
import com.github.aakumykov.sync_dir_to_cloud.domain.use_cases.sync_task.SyncTaskManagingUseCase
import com.github.aakumykov.sync_dir_to_cloud.factories.CloudAuthenticatorFactoryAssistedFactory
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.cloud_auth.CloudAuthAdder
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.cloud_auth.CloudAuthChecker
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.cloud_auth.CloudAuthReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskRunningTimeUpdater
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_task.SyncTaskStateChanger
import com.github.aakumykov.sync_dir_to_cloud.repository.room.SyncTaskStateDAO
import com.github.aakumykov.sync_dir_to_cloud.source_file_stream_supplier.factory_and_creator.SourceFileStreamSupplierCreator
import com.github.aakumykov.sync_dir_to_cloud.storage_writer2.StorageWriters2_Module
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.AuthHolder
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.SyncTaskExecutor
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.SyncTaskNotificator
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_reader.creator.StorageReaderCreator
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.storage_writer.factory_and_creator.StorageWriterCreator
import com.google.gson.Gson
import dagger.Component

@Component(
    modules = [
        ApplicationModule::class,
        ContextModule::class,
        NotificationModule::class,
        RoomDAOModule::class,
        SyncTaskRepositoryInterfacesModule::class,
        CloudAuthRepositoryInterfacesModule::class,
        SyncObjectRepositoryInterfacesModule::class,
        WorkerInterfacesModule::class,
        WorkerModule::class,
        CoroutineModule::class,
        ViewModelsModule::class,
        StorageReaderAssistedFactoriesModule::class,
        CloudWriterAssistedFactoriesModule::class,
        StorageWriterAssistedFactoriesModule3::class,
        OkhttpModule::class,
        GsonModule::class,
        SourceFileStreamSupplierFactoryModule::class,
        SourceFileStreamSupplierAssistedFactoriesModule::class,
        FileListerCreatorsModule::class,
        CloudReaderFactoriesModule::class,
        StorageWriters2_Module::class
    ]
)
@AppScope
interface AppComponent {

    fun getViewModelFactory(): ViewModelFactory

    // TODO: убрать это
    fun getSyncTaskManagingUseCase(): SyncTaskManagingUseCase
    fun getStartStopSyncTaskUseCase(): StartStopSyncTaskUseCase
    fun getTaskSchedulingUseCase(): SchedulingSyncTaskUseCase

    fun getCloudAuthManagingUseCase(): CloudAuthManagingUseCase

    // FIXME: временное
    fun getCloudAuthAdder(): CloudAuthAdder

    fun getCloudAuthChecker(): CloudAuthChecker

    fun getSyncTaskReader(): SyncTaskReader

    fun getCloudAuthReader(): CloudAuthReader

    fun getSyncTaskExecutor(): SyncTaskExecutor

    fun getSyncTaskNotificator(): SyncTaskNotificator

    fun getSyncObjectReader(): SyncObjectReader

    fun getSyncTaskStateChanger(): SyncTaskStateChanger

    fun getSyncTaskRunningTimeUpdater(): SyncTaskRunningTimeUpdater

    fun getSyncTaskStateDAO(): SyncTaskStateDAO

    fun getGson(): Gson

    fun getCloudAuthenticatorFactoryAssistedFactory(): CloudAuthenticatorFactoryAssistedFactory

    fun getStorageReaderCreator(): StorageReaderCreator

    fun getStorageWriterCreator(): StorageWriterCreator

    fun getSourceFileStreamSupplierCreator(): SourceFileStreamSupplierCreator

    fun getAuthHolder(): AuthHolder
}

val authHolder: AuthHolder get() = appComponent.getAuthHolder()