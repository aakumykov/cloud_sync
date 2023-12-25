package com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.target_writer


import com.github.aakumykov.sync_dir_to_cloud.ArgName
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.target_writer.interfaces.TargetWriter
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor_2.target_writer.interfaces.TargetWriterAssistedFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class LocalTargetWriter @AssistedInject constructor(
    @Assisted(ArgName.AUTH_TOKEN) authToken: String,
    @Assisted(ArgName.TASK_ID) taskId: String
) : TargetWriter {

    override suspend fun write() {
        TODO("Не реализовано")
    }

    @AssistedFactory
    interface Factory : TargetWriterAssistedFactory {
        override fun create(
            @Assisted(ArgName.AUTH_TOKEN) authToken: String,
            @Assisted(ArgName.TASK_ID) taskId: String
        ): LocalTargetWriter
    }

    companion object {
        val TAG: String = LocalTargetWriter::class.java.simpleName
    }
}