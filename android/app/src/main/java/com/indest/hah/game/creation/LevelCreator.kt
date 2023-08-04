package com.indest.hah.game.creation

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Space
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.google.android.material.color.MaterialColors
import com.indest.hah.R
import com.indest.hah.game.director.LetterState
import com.indest.hah.persistance.dto.Level
import com.indest.hah.views.Letter
import com.indest.hah.views.Slot
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min


@ActivityScoped
class LevelCreator @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup
    ): Triple<GridLayout, Int, Map<Int, Int>> {
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        val palindrome = info.solutions.first().palindrome.replace(" - ", "- ")
        val densitySettings = getDensitySettings(container, info, palindrome)
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addSlots(palindrome, level, densitySettings, context, info.levelInfo.countryCode)
        return Triple(level, level.columnCount, densitySettings.updatedIndexes)
    }

    private fun getPuzzle(info: Level): String {
        return info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }
    }

    private fun getDensitySettings(
        container: ViewGroup,
        info: Level,
        palindrome: String
    ): DensitySettings {
        val puzzleOnlyLetters = getPuzzle(info)
        var columns = min(7, palindrome.split(" ").maxBy { it.length }.length)
        val smallGap = context.resources.getDimensionPixelSize(R.dimen.gap_between_letters)
        val usableWidth = container.width
        val usableHeight = container.height
        val totalAreaRatio = (usableWidth.toFloat() / usableHeight.toFloat())
        var closestColumnsRatio = Pair(columns, Float.MAX_VALUE)

        while (columns <= palindrome.length) {
            val usedAreaRatio = getUsedAreaRatio(
                columns = columns,
                puzzleOnlyLetters = puzzleOnlyLetters,
                palindrome = palindrome,
                smallGap = smallGap
            )
            val newDistance = Math.abs(usedAreaRatio - totalAreaRatio)
            val currentBestDistance = Math.abs(closestColumnsRatio.second - totalAreaRatio)
            if (newDistance < currentBestDistance) {
                closestColumnsRatio = Pair(columns, usedAreaRatio)
            }
            columns += 1
        }

        val solutionRows = getSolutionRows(puzzle = palindrome, cols = closestColumnsRatio.first)
        var boxSize = context.resources.getDimension(R.dimen.max_letter_box_size)
        val boxDecreaseRate = 1.05f

        var exceedingWidth = getUsedWidth(
            columns = closestColumnsRatio.first,
            boxSize = boxSize,
            smallGapBetweenLetters = smallGap
        ) >= usableWidth

        var exceedingHeight = getUsedHeight(
            rowsCount = solutionRows.size,
            boxSize = boxSize,
            smallGapBetweenLetters = smallGap
        ) >= usableHeight

        while (exceedingWidth || exceedingHeight) {
            boxSize /= boxDecreaseRate
            exceedingWidth = getUsedWidth(
                columns = closestColumnsRatio.first,
                boxSize = boxSize,
                smallGapBetweenLetters = smallGap
            ) >= usableWidth
            exceedingHeight = getUsedHeight(
                rowsCount = solutionRows.size,
                boxSize = boxSize,
                smallGapBetweenLetters = smallGap
            ) >= usableHeight
        }

        var updatedIndexes = mapOf<Int, Int>()
        if (info.levelInfo.columnCount != -1 && info.levelInfo.columnCount != closestColumnsRatio.first) {
            updatedIndexes = getUpdatedOldFriendToNewFriendIndexes(
                palindrome,
                puzzleOnlyLetters,
                info,
                closestColumnsRatio.first
            )
        }

        return DensitySettings(
            closestColumnsRatio.first,
            boxSize.toInt(),
            updatedIndexes
        )
    }

    private fun getUsedAreaRatio(
        columns: Int,
        puzzleOnlyLetters: String,
        palindrome: String,
        smallGap: Int
    ): Float {
        val rowsCount = puzzleOnlyLetters.chunked(columns).count() + getSolutionRows(
            palindrome,
            columns
        ).count()
        val boxSize = 1f
        val usedHeight = getUsedHeight(rowsCount, boxSize, smallGap)
        val usedWidth = getUsedWidth(columns, boxSize, smallGap)
        return usedWidth / usedHeight
    }

    private fun getUsedHeight(rowsCount: Int, boxSize: Float, smallGapBetweenLetters: Int): Float {
        return (rowsCount * (boxSize + smallGapBetweenLetters)) + 2 * (boxSize + smallGapBetweenLetters)
    }

    private fun getUsedWidth(columns: Int, boxSize: Float, smallGapBetweenLetters: Int): Float {
        return (columns * (boxSize + smallGapBetweenLetters) + 2 * (boxSize + smallGapBetweenLetters))
    }

    private fun addLetters(grid: GridLayout, ds: DensitySettings, info: Level, context: Context) {
        val puzzle = info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> if (li.letter != '-') acc + li.letter else acc }

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
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
                        gravity = Gravity.CENTER

                        setBackgroundColor(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorSurfaceVariant,
                                Color.RED
                            )
                        )
                        setTextColor(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorOnSurfaceVariant,
                                Color.RED
                            )
                        )

                    })
        }

        val emptySpaces = getEmptySpacesCountForRow(puzzle, ds.columnCount)
        for (i in 1..emptySpaces) {
            grid.addView(Space(context))
        }
    }


    private fun getEmptySpacesCountForRow(puzzle: String, columnCount: Int): Int {
        val puzzleWithoutWhitespace = puzzle.replace("\\s".toRegex(), "")
        val countOfRowLetters =
            puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / columnCount) * columnCount
        return if (countOfRowLetters == 0) 0 else columnCount - countOfRowLetters
    }


    private fun addSlots(
        puzzle: String,
        grid: GridLayout,
        ds: DensitySettings,
        context: Context,
        cc: String
    ) {
        val solutionRows = getSolutionRows(puzzle, ds.columnCount)
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> grid.addView(Space(context))
                    '-' -> grid.addView(Slot(context, char).apply {
                        contentDescription = "Slot nr ${this.homeIndex}"
                        width = ds.boxSize
                        height = ds.boxSize
                        // set slot color and border
                        val drawable = GradientDrawable()
                        drawable.shape = GradientDrawable.RECTANGLE
                        drawable.setStroke(2, Color.TRANSPARENT)
                        drawable.setColor(Color.TRANSPARENT)
                        background = drawable
                        typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
                        gravity = Gravity.CENTER
                        setTextColor(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorOutline,
                                Color.DKGRAY
                            )
                        )
                        isHyphen = true
                        text = "–"
                    })

                    else -> grid.addView(Slot(context, char).apply {
                        contentDescription = "Slot nr ${this.homeIndex}"
                        width = ds.boxSize
                        height = ds.boxSize

                        // set slot color and border
                        val drawable = GradientDrawable()
                        drawable.shape = GradientDrawable.RECTANGLE
                        drawable.setStroke(2, Color.TRANSPARENT)
                        drawable.setColor(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorOutline,
                                Color.DKGRAY
                            )
                        )
                        background = drawable

                        typeface = ResourcesCompat.getFont(context, R.font.roboto_bold)
                        gravity = Gravity.CENTER
                        setTextColor(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorSecondaryContainer,
                                Color.RED
                            )
                        )
                        text = " "
                    })
                }
            }
        }
    }

    // Takes a word and chunks it to given lengths adding "-"
    // in the end
    private fun getChunkedStringWithHyphens(word: String, cols: Int): String {
        val windowed = word.windowed(cols, cols, true).toMutableList()
        val lastElement = windowed.last()
        if (lastElement.length == 1) {
            val newWindowed = windowed.dropLast(1).toMutableList()
            val newLastElement = newWindowed[newWindowed.size - 1]
            newWindowed[newWindowed.size - 1] = newLastElement.removeSuffix("-") + lastElement
            return newWindowed.joinToString("- ")
        }
        return windowed.joinToString("- ").replace("--", "-")
    }

    private fun getUpdatedOldFriendToNewFriendIndexes(
        palindrome: String,
        puzzleOnlyLetters: String,
        info: Level,
        newColumnCount: Int
    ): Map<Int, Int> {
        val firstIndexForNewColumnsCount =
            puzzleOnlyLetters.chunked(newColumnCount).count() * newColumnCount
        val firstIndexForOldColumnsCount = puzzleOnlyLetters.chunked(info.levelInfo.columnCount)
            .count() * info.levelInfo.columnCount
        val oldList = getSolutionRows(puzzle = palindrome, cols = info.levelInfo.columnCount)
        val newList = getSolutionRows(puzzle = palindrome, cols = newColumnCount)
        val newIndexes = newList
            .joinToString("")
            .withIndex()
            .filter { it.value != ' ' && it.value != '-' }
            .map { firstIndexForNewColumnsCount + it.index }
        val oldIndexes = oldList
            .joinToString("")
            .withIndex()
            .filter { it.value != ' ' && it.value != '-' }
            .map { firstIndexForOldColumnsCount + it.index }
        return oldIndexes.zip(newIndexes).toMap()
    }

    private fun getSolutionRows(puzzle: String, cols: Int): List<String> {
        val modified = puzzle
            .split(' ')
            .fold("") { acc: String, s: String ->
                if (s.length <= cols) {
                    "$acc$s "
                } else {
                    acc + getChunkedStringWithHyphens(s, cols - 1) + " "
                }
            }
        val puzzleWords = modified.split(' ')
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


}