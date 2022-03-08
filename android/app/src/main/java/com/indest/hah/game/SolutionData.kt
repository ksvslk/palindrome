package com.indest.hah.game

data class SolutionData(
    val solution: Solution = Solution.NONE,
    val index: Int = -1,
    val score: Int = -1,
    val maxScore: Int = -1,
    val showHintsBadge: Boolean = false
)