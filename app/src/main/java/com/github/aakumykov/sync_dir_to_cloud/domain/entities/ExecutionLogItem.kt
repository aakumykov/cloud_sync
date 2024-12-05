package com.github.aakumykov.sync_dir_to_cloud.domain.entities

import androidx.room.ColumnInfo
import androidx.room.DeleteColumn
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import com.github.aakumykov.sync_dir_to_cloud.enums.ExecutionLogItemType
import com.github.aakumykov.sync_dir_to_cloud.sync_task_executor.NO_OPERATION_ID
import java.util.Date
import java.util.UUID

@Entity(
    tableName = ExecutionLogItem.TABLE_NAME,
    /*indices = [
        Index(value = [
                ExecutionLogItem.TASK_ID_FIELD_NAME,
                ExecutionLogItem.EXECUTION_ID_FIELD_NAME,
                ExecutionLogItem.TIMESTAMP_FIELD_NAME,
                ExecutionLogItem.TYPE_FIELD_NAME,
            ],
            unique = true
        )
    ]*/
)
class ExecutionLogItem (
    @PrimaryKey val id: String,
    @ColumnInfo(name = TASK_ID_FIELD_NAME) val taskId: String,
    @ColumnInfo(name = EXECUTION_ID_FIELD_NAME) val executionId: String,
    @ColumnInfo(name = OPERATION_ID_FIELD_NAME, defaultValue = NO_OPERATION_ID) val operationId: String,
    @ColumnInfo(name = TIMESTAMP_FIELD_NAME) val timestamp: Long,
    @ColumnInfo(name = TYPE_FIELD_NAME) val type: ExecutionLogItemType,
    val message: String,
    @ColumnInfo(name = IS_CANCELABLE_FIELD_NAME, defaultValue = false.toString()) val isCancelable: Boolean,
) {
    companion object {

        fun createStartingItem(taskId: String,
                               executionId: String,
                               operationId: String,
                               message: String,
                               isCancelable: Boolean,
        ): ExecutionLogItem = create(
            taskId = taskId,
            executionId = executionId,
            operationId = operationId,
            itemType = ExecutionLogItemType.START,
            message = message,
            isCancelable = isCancelable,
        )


        fun createFinishingItem(taskId: String,
                                executionId: String,
                                operationId: String,
                                message: String,
        ): ExecutionLogItem = create(
            taskId = taskId,
            executionId = executionId,
            operationId = operationId,
            itemType = ExecutionLogItemType.FINISH,
            message = message,
            isCancelable = false,
        )


        fun createErrorItem(taskId: String,
                            executionId: String,
                            operationId: String,
                            message: String,
        ): ExecutionLogItem = create(
            taskId = taskId,
            executionId = executionId,
            operationId = operationId,
            itemType = ExecutionLogItemType.ERROR,
            message = message,
            isCancelable = false,
        )


        private fun create(taskId: String,
                           executionId: String,
                           operationId: String,
                           itemType: ExecutionLogItemType,
                           message: String,
                           isCancelable: Boolean,
        ): ExecutionLogItem = ExecutionLogItem(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            executionId = executionId,
            operationId = operationId,
            timestamp = Date().time,
            type = itemType,
            message = message,
            isCancelable = isCancelable,
        )

        const val TABLE_NAME = "execution_log"
        const val TASK_ID_FIELD_NAME = "task_id"
        const val EXECUTION_ID_FIELD_NAME = "execution_id"
        const val OPERATION_ID_FIELD_NAME = "operation_id"
        const val TIMESTAMP_FIELD_NAME = "timestamp"
        const val TYPE_FIELD_NAME = "type"
        const val OPERATION_STATE_FIELD_NAME = "operation_state"
        const val IS_CANCELABLE_FIELD_NAME = "is_cancelable"
    }


    /*override fun toString(): String {
        return "ExecutionLogItem(message='$message', type=$type)"
    }*/




    @RenameColumn(tableName = TABLE_NAME, fromColumnName = "executionId", toColumnName = EXECUTION_ID_FIELD_NAME)
    @RenameColumn(tableName = TABLE_NAME, fromColumnName = "taskId", toColumnName = TASK_ID_FIELD_NAME)
    class RenameColumnsAutoMigrationSpec1 : AutoMigrationSpec

    @DeleteColumn(tableName = TABLE_NAME, columnName = OPERATION_STATE_FIELD_NAME)
    class RemoveOperationStateFieldSpec : AutoMigrationSpec

    override fun toString(): String {
        return "ExecutionLogItem(id='$id', taskId='$taskId', executionId='$executionId', timestamp=$timestamp, type=$type, message='$message')"
    }
}