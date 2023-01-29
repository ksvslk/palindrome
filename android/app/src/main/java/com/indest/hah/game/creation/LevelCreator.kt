package com.indest.hah.game.creation

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
import com.indest.hah.R
import com.indest.hah.views.Letter
import com.indest.hah.game.director.LetterState
import com.indest.hah.views.Slot
import com.indest.hah.persistance.dto.Level
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import java.lang.Math.abs
import javax.inject.Inject

@ActivityScoped
class LevelCreator @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup,
        isBookMode: BookMode,
        windowSizeClassWidth: WindowSizeClass
    ): GridLayout {

        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        val puzzleOnlyLetters = getPuzzle(info)
        val densitySettings =
            getDensitySettings(isBookMode, windowSizeClassWidth, info, container, puzzleOnlyLetters)
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, puzzleOnlyLetters, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(info.solutions.first().palindrome, level, densitySettings, context)
        return level
    }

    private fun maxHeightExceeded(
        boxSize: Int,
        container: ViewGroup,
        margin: Int,
        rowsCount: Int,
    ): Boolean {
        val space = (container.height - rowsCount * boxSize)
        return space <= margin
    }

    private fun nextIncreaseWillNotExceedWidthLimit(
        boxSize: Int,
        columnCount: Int,
        container: ViewGroup,
        margin: Int
    ): Boolean {
        val space = (container.width - (columnCount + 1) * boxSize)
        return space >= margin || space < 0
    }

    private fun nextIncreaseWillNotExceedHeightLimit(
        boxSize: Int,
        rowsCount: Int,
        container: ViewGroup,
        margin: Int
    ): Boolean {
        val space = (container.height - rowsCount * boxSize)
        return space <= margin || space < 0
    }

    private fun getDensitySettings(
        isBookMode: BookMode,
        sizeClassWidth: WindowSizeClass,
        info: Level,
        container: ViewGroup,
        puzzleOnlyLetters: String
    ): DensitySettings {

        val densityDpi = context.resources?.displayMetrics?.densityDpi
        val margin = context.resources.getDimensionPixelSize(R.dimen.def)
        val screenWidth = context.resources?.displayMetrics?.widthPixels!!
        var initialColumnCount = context.resources.getInteger(R.integer.defaultColumnsCount)
        val smallGap = context.resources.getDimensionPixelSize(R.dimen._2sdp)
        val intermediateGap = context.resources.getDimensionPixelSize(R.dimen._18sdp)
        var rowsCount = puzzleOnlyLetters.chunked(initialColumnCount).count() + getSolutionRows(
            info.solutions.first().palindrome,
            initialColumnCount
        ).count()
        val heightMarginBothWays = margin * 3f
        val widthMarginBothWays = margin * 2
        val boxDecreaseRate = 1.1f
        val textDecreaseRate = 1.04f

        val boxAndText =
            getBoxAndTextSize(
                screenWidth,
                margin,
                isBookMode,
                sizeClassWidth,
                densityDpi,
                initialColumnCount
            )
        var boxSize = boxAndText.first
        var textSize = boxAndText.second

        while (maxHeightExceeded(
                boxSize,
                container,
                (heightMarginBothWays + (intermediateGap) + (rowsCount - 1) * smallGap).toInt(),
                rowsCount
            )
        ) {
            if (nextIncreaseWillNotExceedHeightLimit(
                    boxSize,
                    rowsCount,
                    container,
                    (heightMarginBothWays + (intermediateGap) + (rowsCount - 1) * smallGap).toInt()
                )
            ) {
                boxSize = (boxSize / boxDecreaseRate).toInt()
                textSize = (textSize / textDecreaseRate).toInt()
            }
            if (nextIncreaseWillNotExceedWidthLimit(
                    boxSize,
                    initialColumnCount,
                    container,
                    widthMarginBothWays + (initialColumnCount - 1) * smallGap
                )
            ) {
                initialColumnCount++
                rowsCount = puzzleOnlyLetters.chunked(initialColumnCount).count() + getSolutionRows(
                    info.solutions.first().palindrome,
                    initialColumnCount
                ).count()
            }
        }

        return DensitySettings(
            initialColumnCount,
            boxSize,
            textSize,
        )
    }

    private fun getBoxAndTextSize(
        widthPixels: Int,
        margin: Int,
        isBookMode: BookMode,
        sizeClassWidth: WindowSizeClass,
        densityDpi: Int?,
        columnCount: Int
    ): Pair<Int, Int> {

        return when (sizeClassWidth) {
            WindowSizeClass.COMPACT -> {
                when (densityDpi) {
                    in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) - COMPAT"
                        )
                        val box = (widthPixels - margin * 2) / columnCount
                        val text = box.div(1.4).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) - COMPAT"
                        )
                        val box = (widthPixels - margin * 2) / columnCount
                        val text = box.div(2.2).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) - COMPAT"
                        )
                        val box = (widthPixels - margin * 3) / columnCount
                        val text = box.div(3).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                        return if (isBookMode == BookMode.BOOK_MODE) {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - COMPAT and BOOK MODE"
                            )
                            val box = ((widthPixels / 2 - margin) / columnCount).toInt()
                            val text = box.div(3.8).toInt()
                            Pair(box, text)
                        } else {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - COMPAT and DEFAULT"
                            )
                            val box = ((widthPixels - margin * 3) / columnCount).toInt()
                            val text = box.div(3.8).toInt()
                            Pair(box, text)
                        }
                    }
                    in DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) - COMPAT"
                        )
                        val box = ((widthPixels - margin * 2.8) / columnCount).toInt()
                        val text = box.div(5).toInt()
                        Pair(box, text)
                    }
                    else -> {
                        Log.d(TAG, "DisplayMetrics OTHER - COMPAT")
                        val box = (widthPixels - margin * 2) / columnCount
                        val text = box.div(3.9).toInt()
                        return Pair(box, text)
                    }
                }
            }
            WindowSizeClass.MEDIUM -> {
                when (densityDpi) {
                    in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) - MEDIUM"
                        )
                        val box = ((widthPixels / 1.8 - margin) / columnCount).toInt()
                        val text = box.div(1.4).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) - MEDIUM"
                        )
                        val box = ((widthPixels / 1.6 - margin * 1.8) / columnCount).toInt()
                        val text = box.div(1.8).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) - MEDIUM"
                        )
                        val box = ((widthPixels / 1.6 - margin * 2) / columnCount).toInt()
                        val text = box.div(2.6).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {

                        return if (isBookMode == BookMode.BOOK_MODE) {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - MEDIUM and BOOK MODE"
                            )
                            val box = ((widthPixels / 1.8 - margin * 2) / columnCount).toInt()
                            val text = box.div(3.3).toInt()
                            Pair(box, text)
                        } else {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - MEDIUM"
                            )
                            val box = ((widthPixels / 1.6 - margin * 2) / columnCount).toInt()
                            val text = box.div(3.2).toInt()
                            Pair(box, text)
                        }
                    }
                    else -> {
                        Log.d(TAG, "DisplayMetrics OTHER - MEDIUM")
                        val box = (widthPixels / 2 - margin * 2) / columnCount
                        val text = box.div(2.5).toInt()
                        return Pair(box, text)
                    }
                }
            }
            WindowSizeClass.EXPANDED -> {
                Log.d(TAG, "DisplayMetrics ... - EXPANDED")
                val box = (widthPixels / 2 - margin * 2) / columnCount
                val text = box.div(2.5).toInt()
                return Pair(box, text)
            }
            WindowSizeClass.NOT_SET -> {
                Log.d(TAG, "DisplayMetrics ... - NOT SET")
                val box = (widthPixels - margin * 2) / columnCount
                val text = box.div(2.5).toInt()
                return Pair(box, text)
            }
        }
    }

    private fun getPuzzle(info: Level): String {
        return info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }
    }

    private fun addLetters(
        grid: GridLayout,
        ds: DensitySettings,
        puzzle: String,
        context: Context
    ) {
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
        return puzzleWords.fold(mutableListOf()) { acc: List<String>, s: String ->
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
        }
    }

    private fun addBlanks(columnCount: Int, grid: GridLayout, layoutInflater: LayoutInflater) {
        for (i in 1..columnCount) {
            layoutInflater.inflate(R.layout.space, grid)
        }
    }

}