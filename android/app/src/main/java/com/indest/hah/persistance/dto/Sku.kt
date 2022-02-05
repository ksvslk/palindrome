package com.indest.hah.persistance.dto

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sku")
data class Sku(
    @PrimaryKey
    @ColumnInfo(name = "sku")
    val sku: String,
    @ColumnInfo(name = "purchased")
    val purchased: Boolean = false
)