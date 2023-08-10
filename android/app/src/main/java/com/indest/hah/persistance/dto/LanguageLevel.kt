package com.indest.hah.persistance.dto

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
data class LanguageLevel(
    val solutionToCategory:Map<String,Map<String,String>>
)