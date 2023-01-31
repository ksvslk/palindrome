package com.indest.hah.persistance.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wrapper class for the entire structure
@Serializable
data class SolutionToCategory(
    val solutionToCategory: Map<String, HintTranslations>
)