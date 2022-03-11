package com.indest.hah.persistance.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@kotlinx.serialization.Serializable
@Entity(tableName = "interval")
data class Interval(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    // Increased when level complete
    @ColumnInfo(name = "current_count")
    val currentCount: Int = 0,
    // Constant - when reached, currentCount will reset
    @ColumnInfo(name = "limit_count")
    val limitCount: Int = 0
):Serializable