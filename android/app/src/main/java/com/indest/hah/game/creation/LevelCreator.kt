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
import kotlin.math.abs


@ActivityScoped
class LevelCreator @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup,
        bottomNavHeight:Int
    ): Triple<GridLayout, Triple<Int,Int,Int>, List<Int>> {
        val maxWordLength = info.solutions.first().palindrome.split(" ").maxBy { it.length }.length
        val densitySettings =
            getDensitySettings(maxWordLength, container, info, bottomNavHeight)
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(info.solutions.first().palindrome, level, densitySettings, context)
        return Triple(level, densitySettings.emptySpacesInTheEndChars, densitySettings.indexDifferences)
    }

   private fun getNewFriendIndexesForLetters(palindrome:String, oldColumnCount:Int, newColumnCount:Int, firstIndex:Int):List<Int>
   {
       if (oldColumnCount != -1 && oldColumnCount != newColumnCount) {

       val oldList = getSolutionRows(palindrome, oldColumnCount)
       val newList = getSolutionRows(palindrome, newColumnCount)
       Log.d(TAG, "old count: ${oldList.count()}, new count: ${newList.count()}")
        Log.d(TAG, "PALINDROME: $palindrome")
       Log.d(TAG, "old start")
           Log.d(TAG, "OLD: ${oldList.joinToString("")}")
       Log.d(TAG, "NEW: ${newList.joinToString("")}")
           val oldIndexToCharList = oldList.joinToString("").withIndex().filter { it.value != ' ' }
           val newIndexToCharList = newList.joinToString("").withIndex().filter { it.value != ' ' }
           Log.d(TAG, "indexes of old: $oldIndexToCharList")
           Log.d(TAG, "indexes of new: $newIndexToCharList")
           // val stuff = oldIndexToCharList.zip(newIndexToCharList) { index1, index2 -> index1.index - index2.index   }
           Log.d(TAG, "FIRST INDEX: $firstIndex")
           val stuff = newIndexToCharList.map { i -> firstIndex + i.index   }
           Log.d(TAG, "indexes zipped (ACTUAL NEW INDEXES): $stuff")
//  updateFriendIndexSub(letter.homeIndex, diff[index], level) -> MIDA VAJA?
        return stuff
       }
       return emptyList()
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
        info: Level,
        bottomNav: Int
    ): DensitySettings {
        var columns =
            if (maxWordLength > context.resources.getInteger(R.integer.defaultColumnsCount))
                maxWordLength
            else context.resources.getInteger(R.integer.defaultColumnsCount)

        val puzzleOnlyLetters = getPuzzle(info)
        val palindrome = info.solutions.first().palindrome

        var boxSize = 1.00f
        val boxDecreaseRate = 1.05f

        val widthGap = context.resources.getDimension(R.dimen._8sdp)
        val smallGap = context.resources.getDimensionPixelSize(R.dimen._2sdp)
        val intermediateGap = context.resources.getDimensionPixelSize(R.dimen._10sdp)
        val perfectRatio = (container.width.toDouble() - widthGap) / (container.height.toDouble() - bottomNav)

        val rowsCount = puzzleOnlyLetters.chunked(columns).count() + getSolutionRows(palindrome, columns).count()
        var heightExceeded = isHeightLimitExceeded(container, rowsCount, boxSize, smallGap, intermediateGap, bottomNav)
        var widthExceeded = isWidthLimitExceeded(container, columns, boxSize, smallGap, widthGap)

        while(!heightExceeded.first && !widthExceeded.first)
        {
            val potentialBoxSize = boxSize * boxDecreaseRate
            val potentialColumns = columns + 1
            val potentialRowsCount = puzzleOnlyLetters.chunked(potentialColumns).count() + getSolutionRows(palindrome, potentialColumns).count()
            val testHeight = isHeightLimitExceeded(
                container,
                potentialRowsCount,
                potentialBoxSize,
                smallGap,
                intermediateGap,
                bottomNav
            )
            val testWidth = isWidthLimitExceeded(container, potentialColumns, potentialBoxSize, smallGap, widthGap)
            val widthChangedRatio = abs(testWidth.second.toDouble() / heightExceeded.second.toDouble() - perfectRatio)
            val heightChangedRatio = abs(widthExceeded.second.toDouble() / testHeight.second.toDouble()- perfectRatio)
            val widthHasMoreRoom = ((container.width - testWidth.second - widthGap) > (container.height - bottomNav - testHeight.second))

            if (testWidth.first || testHeight.first ) break

            if (widthChangedRatio <= heightChangedRatio && widthHasMoreRoom)
            {
                columns = potentialColumns
                boxSize = potentialBoxSize
                widthExceeded = testWidth
                heightExceeded = testHeight
            }
            else  {
                boxSize = potentialBoxSize
                heightExceeded = testHeight
                widthExceeded = testWidth
            }

        }

        var emptySlotsInTheEndNew = 0
        var emptySlotsInTheEndOld = 0

        if (info.levelInfo.columnCount != -1)
        {
            emptySlotsInTheEndNew = columns - puzzleOnlyLetters.chunked(columns).last().count()
            emptySlotsInTheEndOld = info.levelInfo.columnCount - puzzleOnlyLetters.chunked(info.levelInfo.columnCount).last().count()
        }

        val indexDifferences = getNewFriendIndexesForLetters(
            palindrome = palindrome,
            oldColumnCount = info.levelInfo.columnCount,
            newColumnCount = columns,
            firstIndex = puzzleOnlyLetters.count() + emptySlotsInTheEndNew + columns
        )

        return DensitySettings(
            columns,
            boxSize.toInt(),
            Triple(emptySlotsInTheEndOld, emptySlotsInTheEndNew, puzzleOnlyLetters.count() + emptySlotsInTheEndOld + info.levelInfo.columnCount),
            indexDifferences,
        )
    }


    private fun isHeightLimitExceeded(
        container: ViewGroup,
        rowsCount: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int,
        intermediateGap: Int,
        bottomNavHeight:Int
    ): Pair<Boolean, Float> {
        val usedHeight = (rowsCount * (boxSize + smallGapBetweenLetters)) + intermediateGap + boxSize + 2 * smallGapBetweenLetters + bottomNavHeight
        return Pair((container.height - usedHeight) <= 0, usedHeight)
    }

    private fun isWidthLimitExceeded(
        container: ViewGroup,
        columns: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int,
        widthGap:Float
    ): Pair<Boolean, Float> {
        val usedWidth = (columns * (boxSize + smallGapBetweenLetters) + boxSize + 2 * smallGapBetweenLetters + widthGap)
        return Pair((container.width - usedWidth) <= 0, usedWidth)
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