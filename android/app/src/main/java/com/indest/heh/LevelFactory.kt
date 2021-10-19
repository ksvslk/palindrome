package com.indest.heh;

import android.content.Context
import android.view.Gravity
import android.widget.Space
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.indest.heh.letter.BoxDirector
import com.indest.heh.letter.Letter
import com.indest.heh.letter.LetterState
import com.indest.heh.persistance.dto.Level
import com.indest.heh.util.DensitySettings

object B {

    fun addLetters(grid: GridLayout, ds: DensitySettings, info: Level, boxDirector: BoxDirector, context:Context): GridLayout {
        // Also part of restoring the state
        val puzzle = if (info.lettersProperties.isEmpty()) boxDirector.currentLevelInfo.puzzle
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
            LetterState.INITIALIZING
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

        return grid
    }

}
