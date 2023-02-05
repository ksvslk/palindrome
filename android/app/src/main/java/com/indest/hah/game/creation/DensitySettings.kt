package com.indest.hah.game.creation

data class DensitySettings(
    val columnCount: Int,
    val boxSize: Int,
    val emptySpacesInTheEndChars: Triple<Int,Int,Int>,
    val indexDifferences: List<Int>
)
