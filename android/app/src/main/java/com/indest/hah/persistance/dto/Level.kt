package com.indest.hah.persistance.dto

import androidx.annotation.Keep
import androidx.room.*
import com.indest.hah.persistance.level.LevelProgress

@Keep
@kotlinx.serialization.Serializable
class Level(
    @Embedded val levelInfo: LevelData,

    @Relation(
        parentColumn = "level_id",
        entityColumn = "owner_id",
        entity = LetterProperties::class
    )
    val lettersProperties: List<LetterProperties>,

    @Relation(
        parentColumn = "level_id",
        entityColumn = "parent_id",
        entity = Solution::class
    )
    val solutions: List<Solution>
) {
    constructor() : this(
        LevelData(0,"_EMPTY_CONSTRUCTOR_",false, LevelProgress.IN_PROGRESS,0, 0, 0, "", 7),
        emptyList(),
        emptyList()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Level

        if (levelInfo != other.levelInfo) return false
        if (lettersProperties != other.lettersProperties) return false
        if (solutions != other.solutions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = levelInfo.hashCode()
        result = 31 * result + lettersProperties.hashCode()
        result = 31 * result + solutions.hashCode()
        return result
    }

}
