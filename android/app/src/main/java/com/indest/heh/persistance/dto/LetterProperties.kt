package com.indest.heh.persistance.dto

import androidx.room.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = LevelData::class,
            childColumns = ["owner_id"],
            parentColumns = ["level_id"]
        )
    ],
    tableName = "letter_info_table",
    indices = [
        Index(value = ["owner_id"]),
        Index(value = ["home_index", "owner_id"], unique = true)
    ]
)
data class LetterProperties(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "info_id")
    val infoId: Int,
    @ColumnInfo(name = "owner_id")
    val ownerId: Int,
    @ColumnInfo(name = "home_index")
    val homeIndex: Int,
    @ColumnInfo(name = "friend_index")
    val friendIndex: Int,
    @ColumnInfo(name = "letter")
    val letter: Char,
    @ColumnInfo(name = "hint_used")
    val hintUsed: Boolean
)
