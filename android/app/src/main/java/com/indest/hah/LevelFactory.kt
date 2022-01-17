package com.indest.hah

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Space
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.indest.hah.letter.Letter
import com.indest.hah.letter.LetterState
import com.indest.hah.letter.Slot
import com.indest.hah.persistance.dto.Level
import com.indest.hah.util.C
import com.indest.hah.util.DensitySettings

object LevelFactory {
    /*
    Creates a new level and restores box positions based on the level info and context.
    If level is created the first time persists letters positions to database.
    Returned view is ready to be added into the container in GamePlayFragment.
     */

    private val TAG = "LevelFactory"

    fun getLevel(
        info: Level,
        context: Context,
        layoutInflater: LayoutInflater,
        container: ViewGroup,
        isBookMode:Boolean
    ): GridLayout {

        val densitySettings = getDensitySettings(context, isBookMode)
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        info.levelInfo.puzzle = shuffledSolution(info.levelInfo.solution)
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(info.levelInfo.solution, level, densitySettings, context)
        return level
    }

    private fun shuffledSolution(solution: String): String {
        val solutionWithoutWhiteSpace = solution.replace(" ", "")
        val shuffledChars = solutionWithoutWhiteSpace.toCharArray()
        while (shuffledChars.concatToString() == solutionWithoutWhiteSpace) shuffledChars.shuffle()
        return shuffledChars.concatToString()
    }

    private fun getDensitySettings(context: Context, isBookMode: Boolean): DensitySettings {
        Log.i("Dpi", context.resources.displayMetrics.densityDpi.toString())
        var boxSize = 0
        var textSize = 0
        val densityDpi = context.resources?.displayMetrics?.densityDpi

        when {
            densityDpi in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                Log.i(TAG, "DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM)")
                boxSize = context.resources.getDimensionPixelSize(R.dimen._25sdp)
                textSize = context.resources.getDimensionPixelSize(R.dimen._15sdp)
            }
            densityDpi in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                Log.i(TAG, "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH)")
                boxSize = context.resources.getDimensionPixelSize(R.dimen._25sdp)
                textSize = context.resources.getDimensionPixelSize(R.dimen._15sdp)
            }
            densityDpi in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                Log.i(TAG, "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH)")
                boxSize = context.resources.getDimensionPixelSize(R.dimen._25sdp)
                textSize = context.resources.getDimensionPixelSize(R.dimen._15sdp)
            }
            densityDpi in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                Log.i(TAG, "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH)")
                if (isBookMode)
                {
                    Log.i(TAG, "Book mode orientation")
                    boxSize = context.resources.getDimensionPixelSize(R.dimen._20sdp)
                    textSize = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                }
                else {
                    Log.i(TAG, "Table top orientation")
                    boxSize = context.resources.getDimensionPixelSize(R.dimen._35sdp)
                    textSize = context.resources.getDimensionPixelSize(R.dimen._10ssp)
                }
            }
            densityDpi?.compareTo(DisplayMetrics.DENSITY_XXXHIGH) == 1 -> {
                Log.i(TAG, "> DisplayMetrics.DENSITY_XXXHIGH")
                boxSize = context.resources.getDimensionPixelSize(R.dimen._25sdp)
                textSize = context.resources.getDimensionPixelSize(R.dimen._15sdp)
            }
        }
        /* val isTablet = context.resources?.displayMetrics?.densityDpi == DisplayMetrics.DENSITY_XHIGH
         val isHugeTablet =
             context.resources?.displayMetrics?.densityDpi == DisplayMetrics.DENSITY_XXHIGH

         val boxTextSize = (
                 context.resources.getDimensionPixelSize(R.dimen._18ssp) /
                         context.resources.displayMetrics.scaledDensity) +
                 context.resources.displayMetrics.scaledDensity * 2

         val boxSize = when {
             isTablet -> {
                 Log.i(TAG, "Device: isTablet ")
                 context.resources.getDimensionPixelSize(R.dimen.box_size).toFloat()
             }
             isHugeTablet -> {
                 Log.i(TAG, "Device: isHugeTablet ")
                 context.resources.getDimensionPixelSize(R.dimen._25sdp)
             }
             else -> {
                 Log.i(TAG, "Device: default ")
                 context.resources.getDimensionPixelSize(R.dimen._35sdp)
             }
         }

         val textSize = when {
             isTablet -> {
                 context.resources.getDimensionPixelSize(R.dimen.box_text_size).toFloat()
             }
             isHugeTablet -> {
                 context.resources.getDimensionPixelSize(R.dimen._15ssp)
             }
             else -> {
                 boxTextSize.toInt()
             }
         }*/



        return DensitySettings(
            C.DEFAULT_COLUMNS_COUNT,
            boxSize,
            textSize,
        )

    }


    private fun addLetters(grid: GridLayout, ds: DensitySettings, info: Level, context: Context) {
        // Also part of restoring the state
        val puzzle = if (info.lettersProperties.isEmpty()) info.levelInfo.puzzle
        else {
            info.lettersProperties
                .sortedBy { it.homeIndex }
                .fold("", { acc, li -> acc + li.letter }
                )
        }

        for ((_, l) in puzzle.withIndex()) {
            if (l.isWhitespace()) {
                continue
            }
            val char = l.toString()

            grid.addView(
                Letter(
                    context,
                    char,
                    LetterState.HOME
                )
                    .apply {
                        width = ds.boxSize
                        height = ds.boxSize
                        textSize = ds.textSize.toFloat()
                        typeface = ResourcesCompat.getFont(context, R.font.yantramanav_bold)
                        gravity = Gravity.CENTER

                        setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.colorPrimary
                            )
                        )
                        setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary))

                    })
        }

        val puzzleWithoutWhitespace = puzzle.replace("\\s".toRegex(), "")
        val someValue =
            puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / ds.columnCount) * ds.columnCount
        for (i in 1..ds.columnCount - someValue) {
            grid.addView(Space(context))
        }
    }

    private fun addSlots(puzzle: String, grid: GridLayout, ds: DensitySettings, context: Context) {
        val solutionRows = getSolutionRows(puzzle, ds.columnCount)
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> grid.addView(Space(context))
                    else -> grid.addView(Slot(context, char).apply {
                        contentDescription = "Slot nr ${this.homeIndex}"
                        textSize = ds.textSize.toFloat()
                        width = ds.boxSize
                        height = ds.boxSize
                        background =
                            ResourcesCompat.getDrawable(resources, R.drawable.slot_edge_fade, null)
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

    private fun addBlanks(columnCount: Int, grid: GridLayout, layoutInflater: LayoutInflater) {
        for (i in 1..columnCount) {
            layoutInflater.inflate(R.layout.space, grid)
        }
    }

}
