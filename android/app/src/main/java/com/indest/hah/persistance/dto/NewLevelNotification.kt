package com.indest.hah.persistance.dto

data class NewLevelNotification(
    val lastAddedLevel: Int,
    val multipleLevels: Int = -1,
    val playerName: String,
    val countryCode: String
)