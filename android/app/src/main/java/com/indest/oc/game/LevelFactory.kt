package com.indest.oc.game

import android.content.Context
import android.content.res.Resources
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Space
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.indest.oc.R
import com.indest.oc.letter.Letter
import com.indest.oc.letter.LetterState
import com.indest.oc.letter.Slot
import com.indest.oc.util.DensitySettings
import java.util.*
import kotlin.random.Random

object LevelFactory {

    fun getFreshLevel(
        nr:Int,
        resources: Resources,
        context: Context,
        container: LinearLayoutCompat,
        layoutInflater: LayoutInflater,
        /*puzzles: ArrayList<Level>): Level */): Int
    {

     /*   val level = puzzles[nr]
        val shuffledPuzzle = getShuffledPuzzle(level.puzzle)
        val densitySettings = Util.getDensitySettings(
            resources.displayMetrics.density,
            resources.displayMetrics.scaledDensity,
            shuffledPuzzle
        )
        level.grid = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.grid.columnCount = densitySettings.columnCount
        addLetters(shuffledPuzzle, level.grid, densitySettings, false, context)
        addBlanks(densitySettings.columnCount, level.grid, layoutInflater)
        addSlots(level.puzzle, level.grid, densitySettings, context)
        return level*/
        return 0
    }

    private fun getShuffledPuzzle(originalPuzzle: String): String
    {
        val timeNow = Date().time
        val random = Random(timeNow)
        val shuffledChars = originalPuzzle.toCharArray()
        while (shuffledChars.concatToString() == originalPuzzle) {
            shuffledChars.shuffle(random)
        }
        return shuffledChars.concatToString()
    }

    private fun addLetters(
        puzzle: String,
        grid: GridLayout,
        ds: DensitySettings,
        levelComplete:Boolean = false,
        context:Context
    )
    {
        val backgroundColor = if (levelComplete) R.color.colorSecondary else R.color.colorOnSecondary
        val state = if (levelComplete) LetterState.DISABLED else LetterState.INITIALIZING

        // Add letters
        for (l in puzzle) {
            if (l.isWhitespace()) {
                continue
            }
            grid.addView(
                Letter(
                    context,
                    l.toString(),
                    state
                )
                    .apply {
                        width = ds.boxSize
                        height = ds.boxSize
                        textSize = ds.textSize
                        typeface = ResourcesCompat.getFont(context, R.font.yantramanav_bold)
                        gravity = Gravity.CENTER
                        setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
                        setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
                    })
        }

        // Add blanks
        if (!levelComplete)
        {
            val puzzleWithoutWhitespace = puzzle.replace("\\s".toRegex(), "")
            val someValue = puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / ds.columnCount) * ds.columnCount
            for (i in 1..ds.columnCount - someValue) {
                grid.addView(Space(context))
            }
        }
    }

    private fun addBlanks(columnCount:Int, grid: GridLayout, layoutInflater: LayoutInflater)
    {
        for (i in 1..columnCount) {
            layoutInflater.inflate(R.layout.space, grid)
        }
    }

    private fun addSlots(originalPuzzle: String, grid: GridLayout, ds: DensitySettings, context: Context)
    {
        val solutionRows = getSolutionRows(originalPuzzle, ds.columnCount)

        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> grid.addView(Space(context))
                    else -> grid.addView(Slot(context).apply {
                        textSize = ds.textSize
                        width = ds.boxSize
                        height = ds.boxSize
                        background = ResourcesCompat.getDrawable(resources, R.drawable.slot_edge_fade, null)
                        typeface = ResourcesCompat.getFont(context, R.font.yantramanav_bold)
                        gravity = Gravity.CENTER
                        setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryVariant))
                        text = " "
                    })
                }
            }
        }
    }

    private fun getSolutionRows(puzzle: String, cols: Int): List<String> {
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
}