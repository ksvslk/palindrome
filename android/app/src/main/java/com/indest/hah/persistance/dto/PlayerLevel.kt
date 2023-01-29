package com.indest.hah.persistance.dto

data class PlayerLevel(
    val levelId: Long,
    val player: String,
    val solutionToCategory:Map<String,String>,
)