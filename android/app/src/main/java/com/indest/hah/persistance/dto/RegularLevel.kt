package com.indest.hah.persistance.dto

import androidx.annotation.Keep

@Keep
@kotlinx.serialization.Serializable
data class RegularLevel(
    val nr: Long,
    val solutionToCategory:Map<String,Map<String,String>>,
    val author:String = ""
)