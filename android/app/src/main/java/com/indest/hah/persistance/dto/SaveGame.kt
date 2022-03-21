package com.indest.hah.persistance.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveGame(
    val game:List<Level>,
    val interval:List<Interval>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SaveGame

        if (game != other.game) return false
        if (interval != other.interval) return false

        return true
    }

    override fun hashCode(): Int {
        var result = game.hashCode()
        result = 31 * result + interval.hashCode()
        return result
    }
}
