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
import com.indest.hah.game.director.LetterState
import com.indest.hah.persistance.dto.Level
import com.indest.hah.views.Letter
import com.indest.hah.views.Slot
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlin.math.min


@ActivityScoped
class LevelCreator @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup
    ): Pair<GridLayout, List<Pair<Int,Int>>> {
        val maxWordLength = info.solutions.first().palindrome.split(" ").maxBy { it.length }.length
        val densitySettings =
            getDensitySettings(maxWordLength, container, info)
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(info.solutions.first().palindrome, level, densitySettings, context)
        return Pair(
            level,
            densitySettings.homeIndexNewFriendIndexes
        )
    }

    private fun getUpdatedLevelWithPossiblyNewColumnCount(info: Level, newColumnCount: Int):MutableList<Pair<Int,Int>>
    {
        val newFriendIndexesForHomes = mutableListOf<Pair<Int,Int>>()

        if (info.levelInfo.columnCount != -1 && info.levelInfo.columnCount != newColumnCount) {
            val palindrome = info.solutions.first().palindrome
            val puzzleOnlyLetters = getPuzzle(info)
            val puzzleOnlyCount = puzzleOnlyLetters.count()
            val emptySlotsInTheEndNew =
                newColumnCount - puzzleOnlyLetters.chunked(newColumnCount).last().count()
            val emptySlotsInTheEndOld =
                    info.levelInfo.columnCount - puzzleOnlyLetters.chunked(info.levelInfo.columnCount).last().count()
            val firstIndexForNewColumnsCount = puzzleOnlyCount + emptySlotsInTheEndNew + newColumnCount
            val firstIndexForOldColumnsCount = puzzleOnlyCount + emptySlotsInTheEndOld + info.levelInfo.columnCount
            val oldList = getSolutionRows(palindrome, info.levelInfo.columnCount)
            val newList = getSolutionRows(palindrome, newColumnCount)
            val newIndexes = newList
                .joinToString("")
                .withIndex()
                .filter { it.value != ' ' }
                .map { i -> firstIndexForNewColumnsCount + i.index }
            val oldIndexes = oldList
                .joinToString("")
                .withIndex()
                .filter { it.value != ' ' }
                .map { i -> firstIndexForOldColumnsCount + i.index }
            info.lettersProperties.filter {it.friendIndex != -666 }.forEach {
                val g = oldIndexes.indexOf(oldIndexes.first { nr -> nr == it.friendIndex })
                newFriendIndexesForHomes.add(Pair(it.homeIndex, newIndexes[g]))
            }
            return newFriendIndexesForHomes
        }
        return mutableListOf()
    }


    private fun getPuzzle(info: Level): String {
        return info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }
    }

    private fun getDensitySettings(
        maxWordLength: Int,
        container: ViewGroup,
        info: Level
    ): DensitySettings {
        var columns = maxWordLength

        val puzzleOnlyLetters = getPuzzle(info)
        val palindrome = info.solutions.first().palindrome

        var boxSize = 1.00f
        val boxDecreaseRate = 1.05f

        val smallGap = context.resources.getDimensionPixelSize(R.dimen._2sdp)
        val intermediateGap = context.resources.getDimensionPixelSize(R.dimen._10sdp)

        val usableWidth = container.width
        val usableHeight = container.height
        val widthAndHeight = Pair(usableWidth, usableHeight)
        val rowsCount = puzzleOnlyLetters.chunked(columns).count() + getSolutionRows(
            palindrome,
            columns
        ).count()
        var heightExceeded = isHeightLimitExceeded(
            widthAndHeight,
            rowsCount,
            boxSize,
            smallGap,
            intermediateGap
        )
        var widthExceeded = isWidthLimitExceeded(widthAndHeight, columns, boxSize, smallGap)

        // Expand boxes as big as possible
        while (!heightExceeded.first && !widthExceeded.first) {
            val potentialBoxSize = boxSize * boxDecreaseRate
            val potentialRowsCount = puzzleOnlyLetters.chunked(columns).count() + getSolutionRows(
                palindrome,
                columns
            ).count()
            val testWidth =
                isWidthLimitExceeded(widthAndHeight, columns, potentialBoxSize, smallGap)
            val testHeight = isHeightLimitExceeded(
                widthAndHeight,
                potentialRowsCount,
                potentialBoxSize,
                smallGap,
                intermediateGap
            )
            if (testWidth.first || testHeight.first) break
            else {
                boxSize = potentialBoxSize
                heightExceeded = testHeight
                widthExceeded = testWidth
            }

        }

        // Put more columns to make the boxes even bigger
        while (!heightExceeded.first && !widthExceeded.first) {
            val potentialColumns = columns + 1
            val potentialRowsCount =
                puzzleOnlyLetters.chunked(potentialColumns).count() + getSolutionRows(
                    palindrome,
                    potentialColumns
                ).count()
            val testWidth =
                isWidthLimitExceeded(widthAndHeight, potentialColumns, boxSize, smallGap)
            val testHeight = isHeightLimitExceeded(
                widthAndHeight,
                potentialRowsCount,
                boxSize,
                smallGap,
                intermediateGap
            )

            if (testWidth.first || testHeight.first) break
            // After column increase was possible we will check if this new column
            // count will provide bigger boxes, if not we break and DO NOT increase columns
            val potentialBoxSize = boxSize * boxDecreaseRate
            val testWidth2 =
                isWidthLimitExceeded(widthAndHeight, potentialColumns, potentialBoxSize, smallGap)
            val testHeight2 = isHeightLimitExceeded(
                widthAndHeight,
                potentialRowsCount,
                potentialBoxSize,
                smallGap,
                intermediateGap
            )
            if (testWidth2.first || testHeight2.first || boxSize > potentialBoxSize) {
                break
            }
            else {
                columns = potentialColumns
                boxSize = potentialBoxSize
                heightExceeded = testHeight2
                widthExceeded = testWidth2
            }
        }

        val homeIndexNewFriendIndexes = getUpdatedLevelWithPossiblyNewColumnCount(
            info = info,
            newColumnCount = columns,
        )

        val maxBoxSize = context.resources.getDimension(R.dimen.max_box_size)
        val finalBoxSize = min(boxSize, maxBoxSize)

        Log.d(TAG, "TAKE THE SMALLEST FROM: boxSize: ${boxSize} and maxBoxSize:${maxBoxSize}")

        return DensitySettings(
            columns,
            finalBoxSize.toInt(),
            homeIndexNewFriendIndexes,
        )
    }


    private fun isHeightLimitExceeded(
        widthAndHeight: Pair<Int, Int>,
        rowsCount: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int,
        intermediateGap: Int
    ): Pair<Boolean, Float> {
        val usedHeight =
            (rowsCount * (boxSize + smallGapBetweenLetters)) + intermediateGap + boxSize*2 + 3 * smallGapBetweenLetters
        return Pair((widthAndHeight.second - usedHeight) <= 0, usedHeight)
    }

    private fun isWidthLimitExceeded(
        widthAndHeight: Pair<Int, Int>,
        columns: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int
    ): Pair<Boolean, Float> {
        val usedWidth =
            (columns * (boxSize + smallGapBetweenLetters) + boxSize*2 + 3 * smallGapBetweenLetters)
        return Pair((widthAndHeight.first - usedWidth) <= 0, usedWidth)
    }

    private fun addLetters(grid: GridLayout, ds: DensitySettings, info: Level, context: Context) {
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
                        //textSize = ds.textSize.toFloat()
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
                        //textSize = ds.textSize.toFloat()
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