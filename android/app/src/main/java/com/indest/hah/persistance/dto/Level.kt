package com.indest.hah.persistance.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.indest.hah.persistance.level.LevelProgress
import java.io.Serializable

@kotlinx.serialization.Serializable
class Level(
    @Embedded val levelInfo: LevelData,
    @Relation(
        parentColumn = "level_id",
        entityColumn = "owner_id"
    )
    val lettersProperties: List<LetterProperties>,
):Serializable {
    constructor() : this(
        LevelData(0,"_EMPTY_CONSTRUCTOR_","","",false, LevelProgress.IN_PROGRESS,0, 0, 0, LevelPack.NONE),
        emptyList()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Level

        if (levelInfo != other.levelInfo) return false
        if (lettersProperties != other.lettersProperties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = levelInfo.hashCode()
        result = 31 * result + lettersProperties.hashCode()
        return result
    }
}
