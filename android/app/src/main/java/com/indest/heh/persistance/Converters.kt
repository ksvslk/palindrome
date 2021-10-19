package com.indest.heh.persistance

import androidx.room.TypeConverter
import com.google.gson.Gson

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {

    @TypeConverter
    fun fromString(value: String?): ArrayList<Int>? {
        val listType: Type = object : TypeToken<ArrayList<Int>>() {}.type
        val gson = Gson()
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(letterIndexes: ArrayList<Int>?): String? {
        val gson = Gson()
        return gson.toJson(letterIndexes)
    }

    @TypeConverter
    fun toProgress(value: Int) = enumValues<LevelProgress>()[value]

    @TypeConverter
    fun fromProgress(value: LevelProgress) = value.ordinal
}