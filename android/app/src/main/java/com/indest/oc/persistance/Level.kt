package com.indest.oc.persistance

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_info_table")
data class Level(
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
    @ColumnInfo(name = "score")
    val score: Int
)
