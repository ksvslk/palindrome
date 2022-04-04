package com.indest.hah.persistance.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indest.hah.persistance.level.LevelProgress
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "level_info_table")
data class LevelData(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "level_id")
    val levelId: Int,
    @ColumnInfo(name = "puzzle")
    var puzzle: String,
    @ColumnInfo(name = "category_rewarded")
    val categoryRewarded: Boolean,
    @ColumnInfo(name = "progress")
    val progress: LevelProgress,
    @ColumnInfo(name = "score")
    val score: Int,
    @ColumnInfo(name = "best_score")
    val best_score: Int,
    @ColumnInfo(name = "time_modified")
    val timeModified: Long,
)
