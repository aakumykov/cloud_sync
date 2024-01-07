package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.target_writer3

import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.sync_dir_to_cloud.AssistedArgName
import com.github.aakumykov.sync_dir_to_cloud.cloud_writer.CloudWriterCreator
import com.github.aakumykov.sync_dir_to_cloud.enums.StorageType
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectReader
import com.github.aakumykov.sync_dir_to_cloud.interfaces.for_repository.sync_object.SyncObjectStateChanger
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.target_writer3.factory_and_creator.TargetWriterFactory3
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

// TODO: базовый класс, который будет менять состояние и прочее

class LocalTargetWriter3 @AssistedInject constructor(
    private val syncObjectReader: SyncObjectReader,
    private val cloudWriterCreator: CloudWriterCreator,
    private val syncObjectStateChanger: SyncObjectStateChanger,
    @Assisted(AssistedArgName.AUTH_TOKEN) private val authToken: String, // не используется
    // TODO: вместо этого SyncTask или его часть через интерфейс
    @Assisted(AssistedArgName.TASK_ID) private val taskId: String,
    @Assisted(AssistedArgName.TARGET_DIR_PATH) private val targetDirPath: String
) : TargetWriter3 {

    private val localCloudWriter: CloudWriter?  by lazy {
        cloudWriterCreator.createCloudWriter(StorageType.LOCAL, taskId)
    }

    @Throws(IllegalStateException::class)
    override suspend fun writeToTarget() {

        if (null == localCloudWriter)
            throw IllegalStateException("Cloud writer is null.")

        syncObjectReader.getSyncObjectsForTask(taskId).filter {it.isDir }
            .forEach { syncObject ->
                localCloudWriter?.createDir(targetDirPath, syncObject.sourcePath)
            }
    }


    @AssistedFactory
    interface Factory : TargetWriterFactory3 {
        override fun create(
            @Assisted(AssistedArgName.AUTH_TOKEN) authToken: String,
            @Assisted(AssistedArgName.TASK_ID) taskId: String,
            @Assisted(AssistedArgName.TARGET_DIR_PATH) targetDirPath: String
        ): LocalTargetWriter3
    }
}