package com.github.aakumykov.sync_dir_to_cloud.cloud_writer

import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageType
import javax.inject.Inject

class CloudWriterCreator @Inject constructor(
    private val map: Map<StorageType, @JvmSuppressWildcards CloudWriterFactory>
){
    fun createCloudWriter(storageType: StorageType, authToken: String?): CloudWriter? {
        return if (null != authToken) map[storageType]?.create(authToken) else null
    }
}