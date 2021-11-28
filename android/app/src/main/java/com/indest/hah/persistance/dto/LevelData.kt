package com.indest.hah.persistance.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indest.hah.persistance.LevelProgress

@Entity(tableName = "level_info_table")
data class LevelData(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "level_id")
    val levelId: Int,
    @ColumnInfo(name = "solution")
    val solution: String,
    @ColumnInfo(name = "puzzle")
    var puzzle: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "progress")
    val progress: LevelProgress,
    @ColumnInfo(name = "mistakes")
    val mistakes: Int,
    @ColumnInfo(name = "similarity")
    val similarity: Double,
    @ColumnInfo(name = "score")
    val score: Int,
    @ColumnInfo(name = "time_modified")
    val timeModified: Long
)
