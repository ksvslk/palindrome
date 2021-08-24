package com.indest.oc.persistance

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.indest.oc.game.LevelProgress

@Entity(tableName = "level_info_table")
data class Level(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "original_puzzle")
    val originalPuzzle: String,
    @ColumnInfo(name = "shuffled_puzzle")
    var shuffledPuzzle: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "letter_positions")
    val letterIndexes: ArrayList<Int>?,
    @ColumnInfo(name = "progress")
    val progress: LevelProgress,
    @ColumnInfo(name = "score")
    val score: Int
)
