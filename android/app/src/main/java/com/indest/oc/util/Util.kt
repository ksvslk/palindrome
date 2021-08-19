package com.indest.oc.util

import android.app.Application
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.indest.oc.game.Level

object Util {
    fun getDensitySettings(
        density: Float,
        scaledDensity: Float,
        puzzle: String
    ): DensitySettings {

        var ds = DensitySettings(6, 50, 12f, ORIENTATION_PORTRAIT)
        if (puzzle.length < 6) {
            ds = DensitySettings(puzzle.length, 50, 12f, ORIENTATION_PORTRAIT)
        }

        when (density) {
            in 1.50f..1.9f ->
                return DensitySettings(
                    ds.columnCount,
                    (ds.boxSize * density + 0.5).toInt(),
                    ds.textSize * scaledDensity / 2,
                    ORIENTATION_PORTRAIT
                )
            in 2.00f..2.9f ->
                return DensitySettings(
                    ds.columnCount,
                    (ds.boxSize * density + 0.5).toInt(),
                    ds.textSize * scaledDensity / 1.5f,
                    ORIENTATION_PORTRAIT
                )
            in 3.0f..Float.MAX_VALUE ->
                return DensitySettings(
                    ds.columnCount,
                    ((ds.boxSize - 5f) * density + 0.5).toInt(),
                    ds.textSize * scaledDensity / 1.6f,
                    ORIENTATION_PORTRAIT
                )
        }
        return ds
    }

    fun getPuzzles(application: Application): ArrayList<Level> {
        val puzzleString = application.assets.open("puzzles.json").bufferedReader().use {
            it.readText()
        }
        val type = object : TypeToken<ArrayList<Level>>() {}.type

        return Gson()
            .newBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
            .fromJson(puzzleString, type)
    }
}
