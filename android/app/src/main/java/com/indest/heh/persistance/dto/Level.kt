package com.indest.heh.persistance

import androidx.room.Embedded
import androidx.room.Relation

class Level(
    @Embedded val levelInfo: LevelData,
    @Relation(
        parentColumn = "level_id",
        entityColumn = "owner_id"
    )
    val lettersInfo: List<LetterInfo>,
)
