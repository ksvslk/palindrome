package com.indest.hah.persistance.dto

import androidx.room.*
import java.io.Serializable

@kotlinx.serialization.Serializable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = LevelData::class,
            childColumns = ["parent_id"],
            parentColumns = ["level_id"]
        )
    ],
    tableName = "solution_table",
    indices = [
        Index(value = ["parent_id"])
    ]
)
data class Solution(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "solution_id")
    val solutionId: Int,
    @ColumnInfo(name = "parent_id")
    val parentId: Int,
    @ColumnInfo(name = "palindrome")
    val palindrome: String,
    @ColumnInfo(name = "hint")
    val hint: String,
    @ColumnInfo(name = "solved")
    val solved: Boolean,
):Serializable