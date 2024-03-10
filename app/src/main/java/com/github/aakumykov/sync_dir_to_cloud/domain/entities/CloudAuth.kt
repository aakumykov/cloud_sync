package com.github.aakumykov.sync_dir_to_cloud.domain.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import java.util.UUID

@Entity(
    tableName = "cloud_auth",
    indices = [ Index(value = ["name"], unique = true) ]
)
class CloudAuth (
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "auth_token") val authToken: String
) {
    @Ignore
    constructor(name: String, authToken: String) : this(
        UUID.randomUUID().toString(),
        name,
        authToken)
}