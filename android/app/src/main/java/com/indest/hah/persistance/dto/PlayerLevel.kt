package com.indest.hah.persistance.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerLevel(
    val levelId: Long,
    val player: String,
    val solutionToCategory:Map<String, HintTranslations>,
    val countryCode: String // ISO 3166-1-alpha-2 code
)

