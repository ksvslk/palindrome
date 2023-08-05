package com.indest.hah.persistance.dto

data class PlayerLevel(
    val levelId: Long,
    val player: String,
    val solutionToCategory:Map<String,Map<String,String>>,
    val countryCode: String // ISO 3166-1-alpha-2 code
)