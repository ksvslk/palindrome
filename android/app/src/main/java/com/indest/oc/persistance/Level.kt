package com.indest.oc.persistance

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_info_table")
data class Level(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "original_puzzle")
    val originalPuzzle: String,
    @ColumnInfo(name = "shuffled_puzzle")
    val shuffledPuzzle: String,
    @ColumnInfo(name = "box_indexes")
    val boxIndexes: ArrayList<Int>
)
