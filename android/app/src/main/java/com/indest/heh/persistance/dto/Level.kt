package com.indest.heh.persistance.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.indest.heh.persistance.LevelProgress
import java.io.Serializable

class Level(
    @Embedded val levelInfo: LevelData,
    @Relation(
        parentColumn = "level_id",
        entityColumn = "owner_id"
    )
    val lettersProperties: List<LetterProperties>,
):Serializable {
    constructor() : this(
        LevelData(0,"_EMPTY_CONSTRUCTOR_","","",LevelProgress.IN_PROGRESS,0, 0.0, 0, 0),
        emptyList()
    )
}
