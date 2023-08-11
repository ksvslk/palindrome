package com.indest.hah.persistance.dto

import androidx.annotation.Keep
import androidx.room.*

@Keep
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
    @ColumnInfo(name = "hint_cn", defaultValue = "")
    val hint_cn: String = "",
    @ColumnInfo(name = "hint_de", defaultValue = "")
    val hint_de: String = "",
    @ColumnInfo(name = "hint_es", defaultValue = "")
    val hint_es: String = "",
    @ColumnInfo(name = "hint_et", defaultValue = "")
    val hint_et: String = "",
    @ColumnInfo(name = "hint_fi", defaultValue = "")
    val hint_fi: String = "",
    @ColumnInfo(name = "hint_fr", defaultValue = "")
    val hint_fr: String = "",
    @ColumnInfo(name = "hint_id", defaultValue = "")
    val hint_id: String = "",
    @ColumnInfo(name = "hint_it", defaultValue = "")
    val hint_it: String = "",
    @ColumnInfo(name = "hint_ja", defaultValue = "")
    val hint_ja: String = "",
    @ColumnInfo(name = "hint_ko", defaultValue = "")
    val hint_ko: String = "",
    @ColumnInfo(name = "hint_nl", defaultValue = "")
    val hint_nl: String = "",
    @ColumnInfo(name = "hint_pl", defaultValue = "")
    val hint_pl: String = "",
    @ColumnInfo(name = "hint_pt", defaultValue = "")
    val hint_pt: String = "",
    @ColumnInfo(name = "hint_ro", defaultValue = "")
    val hint_ro: String = "",
    @ColumnInfo(name = "hint_sv", defaultValue = "")
    val hint_sv: String = "",
    @ColumnInfo(name = "hint_tr", defaultValue = "")
    val hint_tr: String = "",
    @ColumnInfo(name = "hint_uk", defaultValue = "")
    val hint_uk: String = "",
    @ColumnInfo(name = "solved")
    val solved: Boolean,
)