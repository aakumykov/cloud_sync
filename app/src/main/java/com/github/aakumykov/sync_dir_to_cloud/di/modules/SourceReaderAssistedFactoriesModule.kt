package com.github.aakumykov.sync_dir_to_cloud.di.modules

import com.github.aakumykov.sync_dir_to_cloud.di.annotations.KeyStorageType
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageType
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.source_reader.LocalSourceReader
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.source_reader.YandexSourceReader
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.source_reader.interfaces.SourceReader
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface SourceReaderAssistedFactoriesModule {

    @Binds
    @IntoMap
    @KeyStorageType(StorageType.LOCAL)
    fun bindLocalSourceReader(localSourceReader: LocalSourceReader): SourceReader
    
    @Binds
    @IntoMap
    @KeyStorageType(StorageType.YANDEX_DISK)
    fun bindYandexSourceReader(yandexSourceReader: YandexSourceReader): SourceReader
}