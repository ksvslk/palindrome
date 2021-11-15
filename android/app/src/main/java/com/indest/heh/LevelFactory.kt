package com.indest.heh;

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Space
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.indest.heh.game.CellContainer
import com.indest.heh.letter.Letter
import com.indest.heh.letter.LetterState
import com.indest.heh.letter.Slot
import com.indest.heh.persistance.dto.Level
import com.indest.heh.util.C
import com.indest.heh.util.DensitySettings

object LevelFactory {
    /*
    Creates a new level and restores box positions based on the level info and context.
    If level is created the first time persists letters positions to database.
    Returned view is ready to be added into the container in GamePlayFragment.
     */
    fun getLevel(
        info: Level,
        context: Context,
        layoutInflater: LayoutInflater,
        container: ViewGroup
    ): GridLayout {

        val densitySettings = getDensitySettings(context)
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(info.levelInfo.solution, level, densitySettings, context)
        return level
    }

    private fun getDensitySettings(context: Context): DensitySettings {
        Log.i("Dpi", context.resources.displayMetrics.densityDpi.toString())
        val isTablet = context.resources?.displayMetrics?.xdpi?.compareTo(600f) == 1
        val boxTextSize = (
                context.resources.getDimensionPixelSize(R.dimen._18ssp) /
                        context.resources.displayMetrics.scaledDensity) +
                context.resources.displayMetrics.scaledDensity * 2

        val boxSize = if (isTablet) {
            context.resources.getDimensionPixelSize(R.dimen.box_size).toFloat()
        } else {
            context.resources.getDimensionPixelSize(R.dimen._35sdp)
        }

        val textSize = if (isTablet) {
            context.resources.getDimensionPixelSize(R.dimen.box_text_size).toFloat()
        } else {
            boxTextSize.toInt()
        }

        return DensitySettings(
            C.DEFAULT_COLUMNS_COUNT,
            boxSize.toInt(),
            textSize.toInt(),
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
                    else -> grid.addView(Slot(context).apply {
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
