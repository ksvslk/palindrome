package com.indest.hah.persistance

import androidx.room.TypeConverter
import com.indest.hah.persistance.dto.LevelPack
import com.indest.hah.persistance.level.LevelProgress

class Converters {

    @TypeConverter
    fun toProgress(value: Int) = enumValues<LevelProgress>()[value]

    @TypeConverter
    fun fromProgress(value: LevelProgress) = value.ordinal

    @TypeConverter
    fun toLevelPack(value: Int) = enumValues<LevelPack>()[value]

    @TypeConverter
    fun fromLevelPack(value: LevelPack) = value.ordinal
}