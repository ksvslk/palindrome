package com.indest.heh;

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Space
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.LifecycleCoroutineScope
import com.indest.heh.game.CellContainer
import com.indest.heh.letter.*
import com.indest.heh.persistance.dto.Level
import com.indest.heh.util.DensitySettings
import com.indest.heh.util.Util
import kotlinx.coroutines.launch

object LevelFactory {
    /*
    Creates a new level and restores box positions based on the level info and context.
    If level is created the first time persists letters positions to database.
    Returned view is ready to be added into the container in GamePlayFragment.
     */
    fun getLevel(info: Level,
                 context: Context,
                 layoutInflater: LayoutInflater,
                 container: CellContainer,
                 scope: LifecycleCoroutineScope
    ): GridLayout
    {
        val puzzle = info.levelInfo.puzzle
        val densitySettings = Util.getDensitySettings(
            context.resources.displayMetrics.density,
            context.resources.displayMetrics.scaledDensity,
            puzzle
        )
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(puzzle, level, densitySettings, context)

        return level
    }

    private fun addLetters(grid: GridLayout, ds: DensitySettings, info: Level, context:Context) {
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
            val char =  l.toString()

            grid.addView(
                    Letter(
                            context,
            char,
            LetterState.HOME
                    )
                        .apply {
                width = ds.boxSize
                height = ds.boxSize
                textSize = ds.textSize
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
        val solutionRows = Util.getSolutionRows(puzzle, ds.columnCount)
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> grid.addView(Space(context))
                    else -> grid.addView(Slot(context).apply {
                        textSize = ds.textSize
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

    private fun addBlanks(columnCount: Int, grid: GridLayout, layoutInflater: LayoutInflater) {
        for (i in 1..columnCount) {
            layoutInflater.inflate(R.layout.space, grid)
        }
    }

}
