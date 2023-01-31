package com.indest.hah.persistance

import androidx.room.TypeConverter
import com.indest.hah.persistance.level.LevelProgress
import java.util.*

class Converters {

    @TypeConverter
    fun toProgress(value: Int) = enumValues<LevelProgress>()[value]

    @TypeConverter
    fun fromProgress(value: LevelProgress) = value.ordinal

    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

}