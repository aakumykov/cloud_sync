package com.github.aakumykov.sync_dir_to_cloud.domain.entities

import androidx.room.*
import androidx.room.ForeignKey.Companion.CASCADE
import com.github.aakumykov.cloud_writer.CloudWriter
import com.github.aakumykov.file_lister_navigator_selector.fs_item.FSItem
import com.github.aakumykov.file_lister_navigator_selector.fs_item.SimpleFSItem
import com.github.aakumykov.sync_dir_to_cloud.utils.sha256

@Entity(
    tableName = "sync_objects",
    foreignKeys = [
        ForeignKey(
            entity = SyncTask::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ],
    indices = [
        Index("task_id")
    ]
)
class SyncObject (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    val name: String,
    @ColumnInfo(name = "relative_parent_dir_path") val relativeParentDirPath: String,
    @ColumnInfo(name = "is_dir") val isDir: Boolean,
    @ColumnInfo(name = "sync_state") val syncState: SyncState,
    @ColumnInfo(name = "execution_error") var executionError: String,
    @ColumnInfo(name = "modification_state") var modificationState: ModificationState,
    @ColumnInfo(name = "m_time") val mTime: Long,
    val size: Long,
    @ColumnInfo(name = "sync_date") val syncDate: Long,
) {
    override fun toString(): String {
        return SyncObject::class.simpleName + " { $relativeParentDirPath/$name ($syncState) }"
    }

    fun toFSItem(basePath: String): FSItem {
        return SimpleFSItem(
            name = name,
            parentPath = relativeParentDirPath,
            absolutePath = basePath + CloudWriter.DS + relativeParentDirPath + CloudWriter.DS + name,
            isDir = isDir,
            mTime = mTime,
            size = size
        )
    }

    companion object {

        fun id(fsItem: FSItem): String = sha256(fsItem.absolutePath)

        fun create(
            taskId: String,
            fsItem: FSItem,
            relativeParentDirPath: String,
            modificationState: ModificationState
        ): SyncObject {

            return SyncObject(
                id = id(fsItem),
                taskId = taskId,
                name = fsItem.name,
                relativeParentDirPath = relativeParentDirPath,
                isDir = fsItem.isDir,
                syncState = SyncState.NEVER,
                executionError = "",
                modificationState = modificationState,
                mTime = fsItem.mTime,
                size = fsItem.size,
                syncDate = 0L,
            )
        }
    }

}