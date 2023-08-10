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
    @ColumnInfo(name = "hint_cn")
    val hint_cn: String = "",
    @ColumnInfo(name = "hint_de")
    val hint_de: String = "",
    @ColumnInfo(name = "hint_es")
    val hint_es: String = "",
    @ColumnInfo(name = "hint_et")
    val hint_et: String = "",
    @ColumnInfo(name = "hint_fi")
    val hint_fi: String = "",
    @ColumnInfo(name = "hint_fr")
    val hint_fr: String = "",
    @ColumnInfo(name = "hint_id")
    val hint_id: String = "",
    @ColumnInfo(name = "hint_it")
    val hint_it: String = "",
    @ColumnInfo(name = "hint_ja")
    val hint_ja: String = "",
    @ColumnInfo(name = "hint_ko")
    val hint_ko: String = "",
    @ColumnInfo(name = "hint_nl")
    val hint_nl: String = "",
    @ColumnInfo(name = "hint_pl")
    val hint_pl: String = "",
    @ColumnInfo(name = "hint_pt")
    val hint_pt: String = "",
    @ColumnInfo(name = "hint_ro")
    val hint_ro: String = "",
    @ColumnInfo(name = "hint_sv")
    val hint_sv: String = "",
    @ColumnInfo(name = "hint_tr")
    val hint_tr: String = "",
    @ColumnInfo(name = "hint_uk")
    val hint_uk: String = "",
    @ColumnInfo(name = "solved")
    val solved: Boolean,
)