package com.indest.hah.game.creation

data class DensitySettings(
    val columnCount: Int,
    val boxSize: Int,
    val homeIndexNewFriendIndexes: List<Pair<Int,Int>>
)
