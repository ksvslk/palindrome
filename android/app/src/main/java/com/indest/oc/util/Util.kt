package com.indest.oc.util

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.Log

object Util {
    fun getDensitySettings(
        density: Float,
        scaledDensity: Float,
        puzzle: String,
        listOfWords: List<String>
    ): DensitySettings {

        val densitySettings = DensitySettings(6, 50, 12f, ORIENTATION_PORTRAIT)
        val longestWord = listOfWords.maxWithOrNull { o1, o2 ->
            when {
                o1 == null && o2 == null -> 0
                o1 == null && o2 != null -> -1
                o1 != null && o2 == null -> 1
                o1 != null && o2 != null && o1.length < o2.length -> -1
                o1 != null && o2 != null && o1.length > o2.length -> 1
                o1?.length == o2?.length -> 0
                else -> 0
            }
        }

        var colCount = densitySettings.colCount
        var boxSize = densitySettings.boxSize
        var textSize = densitySettings.textSize

        if (puzzle.length < 6) {
            colCount = puzzle.length
        } else if (longestWord?.length?.compareTo(6) == 1) {
            boxSize -= ((longestWord.length.minus(6)).times(10))
            colCount = longestWord.length
        }

        Log.i("UTIL", "Longest word was $longestWord")
        when (density) {
            in 1.50f..1.9f ->
                return DensitySettings(
                    colCount,
                    (boxSize * density + 0.5).toInt(),
                    textSize * scaledDensity / 2,
                    ORIENTATION_PORTRAIT
                )
            in 2.00f..2.9f ->
                return DensitySettings(
                    colCount,
                    (boxSize * density + 0.5).toInt(),
                    textSize * scaledDensity / 1.5f,
                    ORIENTATION_PORTRAIT
                )
            in 3.0f..Float.MAX_VALUE ->
                return DensitySettings(
                    colCount,
                    ((boxSize - 5f) * density + 0.5).toInt(),
                    textSize * scaledDensity / 1.8f,
                    ORIENTATION_PORTRAIT
                )
        }
        return densitySettings
    }
}
