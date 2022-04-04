package com.indest.hah.persistance

import androidx.room.TypeConverter
import com.indest.hah.persistance.level.LevelProgress

class Converters {

    @TypeConverter
    fun toProgress(value: Int) = enumValues<LevelProgress>()[value]

    @TypeConverter
    fun fromProgress(value: LevelProgress) = value.ordinal

}