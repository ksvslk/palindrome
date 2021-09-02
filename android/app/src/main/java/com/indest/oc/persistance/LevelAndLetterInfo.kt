package com.indest.oc.persistance

import androidx.room.Embedded
import androidx.room.Relation

class LevelAndLetterInfo(
    @Embedded val level: Level,
    @Relation(
        parentColumn = "level_id",
        entityColumn = "owner_id"
    )
    val lettersInfo: List<LetterInfo>
)
