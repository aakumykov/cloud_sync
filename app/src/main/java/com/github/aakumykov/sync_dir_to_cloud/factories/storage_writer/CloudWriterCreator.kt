package com.github.aakumykov.sync_dir_to_cloud.factories.storage_writer

import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageType
import javax.inject.Inject

/**
 * Фактически, это фабрика фабрик. Dagger2 собирает реализации CloudWriterFactory
 * в словарь по типам хранилищ. Потом фабрика, соответствующая типу хранилища,
 * создаёт объект CloudWriter, основываясь на данных авторизации.
 */
class CloudWriterCreator @Inject constructor(
    private val map: Map<StorageType, @JvmSuppressWildcards CloudWriterAssistedFactory>
){
    fun createCloudWriter(storageType: StorageType?, authToken: String?): CloudWriter? {
        return map[storageType]
                ?.createCloudWriterFactory(authToken)
                ?.createCloudWriter()
    }
}