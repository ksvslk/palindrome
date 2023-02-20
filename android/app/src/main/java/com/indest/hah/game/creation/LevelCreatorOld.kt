package com.indest.hah.game.creation

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Space
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.views.Letter
import com.indest.hah.game.director.LetterState
import com.indest.hah.views.Slot
import com.indest.hah.persistance.dto.Level
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject


@ActivityScoped
class LevelCreatorOld @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup,
        isBookMode: BookMode,
        windowSizeClassWidth: WindowSizeClass
    ): GridLayout {

        val densitySettings = getDensitySettings(isBookMode, windowSizeClassWidth)
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        Log.d(TAG, info.solutions.toString())
        Log.d(TAG, info.solutions.first().toString())
        addSlots(info.solutions.first().palindrome, level, densitySettings, context)
        Log.d(TAG, "Level col count: ${densitySettings.columnCount.toString()}")
        Log.d(TAG, "Grid col count?: ${level.columnCount.toString()}")
        showFirstIndex(level.columnCount, info)
        return level
    }

    private fun showFirstIndex(newColumnCount: Int, info: Level) {
        val puzzleOnlyLetters = getPuzzle(info)
        val lastRowLettersCount =
            puzzleOnlyLetters.chunked(newColumnCount).last().count()
        Log.d(TAG, "lastRowLettersCount: $lastRowLettersCount")
        Log.d(TAG, "info.levelInfo.columnCount: ${newColumnCount}")
        val firstIndex = if (puzzleOnlyLetters.length <= newColumnCount) {
            if (lastRowLettersCount == newColumnCount) {
                3 * newColumnCount
            } else {
                2 * newColumnCount
            }
        } else {
            if (lastRowLettersCount == newColumnCount) {
                puzzleOnlyLetters.count() + 2 * newColumnCount
            } else {
                puzzleOnlyLetters.count() + (newColumnCount - lastRowLettersCount) + newColumnCount
            }
        }

        Toast.makeText(
            context,
            "First index is: ${firstIndex.toString()}",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun getPuzzle(info: Level): String {
        return info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }
    }


    private fun getDensitySettings(
        isBookMode: BookMode,
        sizeClassWidth: WindowSizeClass
    ): DensitySettingsOld {
        val densityDpi = context.resources?.displayMetrics?.densityDpi
        val margin = context.resources.getDimensionPixelSize(R.dimen.def)
        val screenWidth = context.resources?.displayMetrics?.widthPixels!!
        val boxAndText =
            getBoxAndTextSize(
                screenWidth,
                margin,
                isBookMode,
                sizeClassWidth,
                densityDpi
            )
        val boxSize = boxAndText.first
        val textSize = boxAndText.second

        return DensitySettingsOld(
            context.resources.getInteger(R.integer.defaultColumnsCount),
            boxSize,
            textSize,
        )
    }

    private fun getBoxAndTextSize(
        widthPixels: Int,
        margin: Int,
        isBookMode: BookMode,
        sizeClassWidth: WindowSizeClass,
        densityDpi: Int?
    ): Pair<Int, Int> {

        return when (sizeClassWidth) {
            WindowSizeClass.COMPACT -> {
                when (densityDpi) {
                    in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) - COMPAT"
                        )
                        val box = (widthPixels - margin * 2) / 7
                        val text = box.div(1.4).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) - COMPAT"
                        )
                        val box = (widthPixels - margin * 2) / 7
                        val text = box.div(2.2).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) - COMPAT"
                        )
                        val box = (widthPixels - margin * 3) / 7
                        val text = box.div(3).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                        return if (isBookMode == BookMode.BOOK_MODE) {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - COMPAT and BOOK MODE"
                            )
                            val box = ((widthPixels / 2 - margin) / 7).toInt()
                            val text = box.div(3.8).toInt()
                            Pair(box, text)
                        } else {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - COMPAT and DEFAULT"
                            )
                            val box = ((widthPixels - margin * 3) / 7).toInt()
                            val text = box.div(3.8).toInt()
                            Pair(box, text)
                        }
                    }
                    in DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) - COMPAT"
                        )
                        val box = ((widthPixels - margin * 2.8) / 7).toInt()
                        val text = box.div(5).toInt()
                        Pair(box, text)
                    }
                    else -> {
                        Log.d(TAG, "DisplayMetrics OTHER - COMPAT")
                        val box = (widthPixels - margin * 2) / 7
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
                        val box = ((widthPixels / 1.8 - margin) / 7).toInt()
                        val text = box.div(1.4).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) - MEDIUM"
                        )
                        val box = ((widthPixels / 1.6 - margin * 1.8) / 7).toInt()
                        val text = box.div(1.8).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) - MEDIUM"
                        )
                        val box = ((widthPixels / 1.6 - margin * 2) / 7).toInt()
                        val text = box.div(2.6).toInt()
                        return Pair(box, text)
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {

                        return if (isBookMode == BookMode.BOOK_MODE) {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - MEDIUM and BOOK MODE"
                            )
                            val box = ((widthPixels / 1.8 - margin * 2) / 7).toInt()
                            val text = box.div(3.3).toInt()
                            Pair(box, text)
                        } else {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - MEDIUM"
                            )
                            val box = ((widthPixels / 1.6 - margin * 2) / 7).toInt()
                            val text = box.div(3.2).toInt()
                            Pair(box, text)
                        }
                    }
                    else -> {
                        Log.d(TAG, "DisplayMetrics OTHER - MEDIUM")
                        val box = (widthPixels / 2 - margin * 2) / 7
                        val text = box.div(2.5).toInt()
                        return Pair(box, text)
                    }
                }
            }
            WindowSizeClass.EXPANDED -> {
                Log.d(TAG, "DisplayMetrics ... - EXPANDED")
                val box = (widthPixels / 2 - margin * 2) / 7
                val text = box.div(2.5).toInt()
                return Pair(box, text)
            }
            WindowSizeClass.NOT_SET -> {
                Log.d(TAG, "DisplayMetrics ... - NOT SET")
                val box = (widthPixels - margin * 2) / 7
                val text = box.div(2.5).toInt()
                return Pair(box, text)
            }
        }
    }

    private fun addLetters(
        grid: GridLayout,
        ds: DensitySettingsOld,
        info: Level,
        context: Context
    ) {
        val puzzle = info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }

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

    private fun addSlots(
        puzzle: String,
        grid: GridLayout,
        ds: DensitySettingsOld,
        context: Context
    ) {
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