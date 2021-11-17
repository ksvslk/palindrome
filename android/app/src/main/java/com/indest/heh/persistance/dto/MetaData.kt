package com.indest.heh.persistance.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meta_data")
data class MetaData(
    @PrimaryKey
    @ColumnInfo(name = "last_modified_timestamp")
    val lastModifiedTimestamp: Long
)