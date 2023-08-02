package com.indest.hah.game.creation

data class DensitySettings(
    val columnCount: Int,
    val boxSize: Int,
    val updatedIndexes: Map<Int,Int>
)
