package com.indest.heh.util

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.core.view.children
import androidx.gridlayout.widget.GridLayout
import com.indest.heh.letter.Letter
import java.util.*
import kotlin.random.Random

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
            in Float.MIN_VALUE..1.49f ->
                return DensitySettings(
                    ds.columnCount,
                    (ds.boxSize * density + 0.5).toInt(),
                    ds.textSize * scaledDensity / 0.5f,
                    ORIENTATION_PORTRAIT
                )
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
                    ds.textSize * scaledDensity / 1.2f,
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

    fun getSolutionRows(puzzle: String, cols: Int): List<String> {
        val puzzleWords = puzzle.split(' ')
        return puzzleWords.fold(mutableListOf(), { acc: List<String>, s: String ->
            when {
                acc.isEmpty() -> acc + s
                acc.last().length.plus(1) + s.length <= cols -> {
                    val last = acc.last().plus(' ')
                    acc.subList(0, acc.lastIndex) + last.plus(s)
                }
                else -> { // New line
                    val blankCount = cols - acc.last().length
                    val last = acc.last().plus(" ".repeat(blankCount))
                    acc.subList(0, acc.lastIndex) + last + s
                }
            }
        })
    }

    fun getShuffledPuzzle(originalPuzzle: String): String {
        val timeNow = Date().time
        val random = Random(timeNow)
        val shuffledChars = originalPuzzle.toCharArray()
        while (shuffledChars.concatToString() == originalPuzzle) {
            shuffledChars.shuffle(random)
        }
        return shuffledChars.concatToString()
    }

    fun gridToPuzzleMapper(gridLayout: GridLayout): String
    {
        var puzzle = ""
        gridLayout.children
            .filterIsInstance<Letter>()
            .sortedBy { it.homeIndex }
            .forEachIndexed { index, t -> puzzle += t.text }
        return puzzle
    }

/*    fun getPuzzlesFromJson(application: Application): ArrayList<Level> {
        val puzzleString = application.assets.open("puzzles.json").bufferedReader().use {
            it.readText()
        }
        val type = object : TypeToken<ArrayList<Level>>() {}.type

        return Gson()
            .newBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
            .fromJson(puzzleString, type)
    }*/
}
