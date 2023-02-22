package com.indest.hah.game.creation

import android.content.Context
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
import kotlin.math.*


@ActivityScoped
class LevelCreator @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup,
        densitySettings: DensitySettings
    ): Pair<GridLayout, Pair<Boolean, List<Pair<Int, Int>>>> {
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        Log.d(TAG, "DensitySettings: $densitySettings")
        addLetters(level, densitySettings, info, context)
        //addBlanks(densitySettings.columnCount, level, layoutInflater)
        addSlots(info.solutions.first().palindrome, level, densitySettings, context)
        return Pair(
            level,
            densitySettings.homeIndexNewFriendIndexes
        )

    }

    fun getUpdatedLevelWithPossiblyNewColumnCount(
        info: Level,
        newColumnCount: Int
    ): Pair<Boolean, MutableList<Pair<Int, Int>>> {

        val newFriendIndexesForHomes = mutableListOf<Pair<Int, Int>>()
        try {
            if (info.levelInfo.author == "NewVersion" && info.levelInfo.columnCount != -1) {
                return Pair(true, migrateFromDatabase3_to_4(info, newColumnCount))
            }
            else if (info.levelInfo.author == "NewVersion") {
                return Pair(true, newFriendIndexesForHomes)
            }
            if (info.levelInfo.columnCount != -1 && info.levelInfo.columnCount != newColumnCount) {

                val palindrome = info.solutions.first().palindrome
                val puzzleOnlyLetters = getPuzzle(info)
                val puzzleOnlyCount = puzzleOnlyLetters.count()
                val emptySlotsInTheEndNew =
                    newColumnCount - puzzleOnlyLetters.chunked(newColumnCount).last().count()
                val emptySlotsInTheEndOld =
                    info.levelInfo.columnCount - puzzleOnlyLetters.chunked(info.levelInfo.columnCount)
                        .last().count()
                val firstIndexForNewColumnsCount = puzzleOnlyCount + emptySlotsInTheEndNew
                val firstIndexForOldColumnsCount = puzzleOnlyCount + emptySlotsInTheEndOld
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

                info.lettersProperties.filter { it.friendIndex != -666 }.forEach {
                    val placementInArrayOfMatchingFriend =
                        oldIndexes.first { nr -> nr == it.friendIndex }
                    val indexOfMatchingSlot = oldIndexes.indexOf(placementInArrayOfMatchingFriend)
                    newFriendIndexesForHomes.add(Pair(it.homeIndex, newIndexes[indexOfMatchingSlot]))
                }
                return Pair(false, newFriendIndexesForHomes)
            }
        }
        catch (e:java.lang.Exception)
        {
            Log.e(TAG, "We did not migrate successfully.", e)
            Log.i(TAG, "info.levelInfo: ${info.levelInfo}")
            Log.i(TAG, "info.lettersProperties: ${info.lettersProperties}")

            val palindrome = info.solutions.first().palindrome
            val puzzleOnlyLetters = getPuzzle(info)
            val puzzleOnlyCount = puzzleOnlyLetters.count()
            val emptySlotsInTheEndNew =
                newColumnCount - puzzleOnlyLetters.chunked(newColumnCount).last().count()
            val emptySlotsInTheEndOld =
                info.levelInfo.columnCount - puzzleOnlyLetters.chunked(info.levelInfo.columnCount)
                    .last().count()
            val firstIndexForNewColumnsCount = puzzleOnlyCount + emptySlotsInTheEndNew
            val firstIndexForOldColumnsCount = puzzleOnlyCount + emptySlotsInTheEndOld
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
            Log.i(TAG, "puzzleOnlyCount: $puzzleOnlyCount")
            Log.i(TAG, "palindrome: $palindrome")
            Log.i(TAG, "firstIndexForOldColumnsCount: $firstIndexForOldColumnsCount")
            Log.i(TAG, "oldList: $oldList")
            Log.i(TAG, "oldIndexes: $oldIndexes")
            Log.i(TAG, "firstIndexForNewColumnsCount: $firstIndexForNewColumnsCount")
            Log.i(TAG, "newList: $newList")
            Log.i(TAG, "newIndexes: $newIndexes")
        }

        return Pair(false, mutableListOf())
    }


    private fun migrateFromDatabase3_to_4(
        info: Level,
        newColumnCount: Int
    ): MutableList<Pair<Int, Int>> {
        val newFriendIndexesForHomes = mutableListOf<Pair<Int, Int>>()
        val palindrome = info.solutions.first().palindrome
        val puzzleOnlyLetters = getPuzzle(info)
        val puzzleOnlyCount = puzzleOnlyLetters.count()
        val emptySlotsInTheEndNew =
            newColumnCount - puzzleOnlyLetters.chunked(newColumnCount).last().count()
        val firstIndexForNewColumnsCount = puzzleOnlyCount + emptySlotsInTheEndNew
        val firstIndexForOldColumnsCount = getFirstIndexForMigration(info.levelInfo.columnCount, info)
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

        try {
            info.lettersProperties.filter { it.friendIndex != -666 }.forEach {
                val placementInArrayOfMatchingFriend =
                    oldIndexes.first { nr -> nr == it.friendIndex }
                val indexOfMatchingSlot = oldIndexes.indexOf(placementInArrayOfMatchingFriend)
                newFriendIndexesForHomes.add(Pair(it.homeIndex, newIndexes[indexOfMatchingSlot]))
            }
        } catch(e:java.util.NoSuchElementException) {
            Log.e(TAG, "Were not able to generate newFriendIndexesForHomes", e)
            Log.i(TAG, "Letters properties: ${info.lettersProperties.filter { it.friendIndex != -666 }.joinToString()}")
            Log.i(TAG, "puzzleOnlyCount: $puzzleOnlyCount")
            Log.i(TAG, "palindrome: $palindrome")
            Log.i(TAG, "firstIndexForOldColumnsCount: $firstIndexForOldColumnsCount")
            Log.i(TAG, "oldList: $oldList")
            Log.i(TAG, "oldIndexes: $oldIndexes")
            Log.i(TAG, "firstIndexForNewColumnsCount: $firstIndexForNewColumnsCount")
            Log.i(TAG, "newList: $newList")
            Log.i(TAG, "newIndexes: $newIndexes")
        }
        return newFriendIndexesForHomes
    }


    private fun getFirstIndexForMigration(newColumnCount: Int, info: Level): Int {
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
        return firstIndex
    }

    private fun getPuzzle(info: Level): String {
        return info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }
    }


    fun getDensitySettings(
        container: ViewGroup,
        info: Level
    ): DensitySettings {
        val puzzleOnlyLetters = getPuzzle(info)
        val palindrome = info.solutions.first().palindrome
        val columns = palindrome.split(" ").maxBy { it.length }.length
        val boxDecreaseRate = 1.01f
        val smallGap = context.resources.getDimensionPixelSize(R.dimen._2sdp)
        val usableWidth = container.width
        val usableHeight = container.height
        val widthAndHeight = Pair(usableWidth, usableHeight)
        val maxBoxSize = context.resources.getDimension(R.dimen.max_box_size)

        val totalAreaRatio = (usableWidth.toFloat() / usableHeight.toFloat())
        Log.d(TAG, "Container width: $usableWidth")
        Log.d(TAG, "Container height: $usableHeight")
        Log.d(TAG, "Total area ratio: $totalAreaRatio")
        Log.d(TAG, "Minimum word length (min columns): $columns")

        var closestColumnsRatio = Pair(-1, Float.MAX_VALUE)
        val columnsEnd = max(7, palindrome.length)
        for (cols in columns..columnsEnd) {
            val usedAreaRatio = getUsedAreaRatio(cols, puzzleOnlyLetters, palindrome, smallGap)
            Log.d(TAG, "Used area ratio with column count $cols is: $usedAreaRatio")
            val newDistance = abs(usedAreaRatio - totalAreaRatio)
            val currentBestDistance = abs(closestColumnsRatio.second - totalAreaRatio)
            if (newDistance < currentBestDistance) {
                closestColumnsRatio = Pair(cols, usedAreaRatio)
            }
        }
        val rowsCount =
            puzzleOnlyLetters.chunked(closestColumnsRatio.first).count() + getSolutionRows(
                palindrome,
                closestColumnsRatio.first
            ).count()

        Log.d(
            TAG,
            "Best columns count for this screen is ${closestColumnsRatio.first} with ratio ${closestColumnsRatio.second} (ideal is $totalAreaRatio)"
        )

        var boxSize = maxBoxSize

        var exceedingWidth = isWidthLimitExceeded(
            widthAndHeight = widthAndHeight,
            columns = closestColumnsRatio.first,
            boxSize = boxSize,
            smallGapBetweenLetters = smallGap
        )

        var exceedingHeight = isHeightLimitExceeded(
            widthAndHeight = widthAndHeight,
            rowsCount = rowsCount,
            boxSize = boxSize,
            smallGapBetweenLetters = smallGap
        )

        while (exceedingWidth.first || exceedingHeight.first) {
            boxSize /= boxDecreaseRate
            exceedingWidth = isWidthLimitExceeded(
                widthAndHeight = widthAndHeight,
                columns = closestColumnsRatio.first,
                boxSize = boxSize,
                smallGapBetweenLetters = smallGap
            )
            exceedingHeight = isHeightLimitExceeded(
                widthAndHeight = widthAndHeight,
                rowsCount = rowsCount,
                boxSize = boxSize,
                smallGapBetweenLetters = smallGap
            )

        }
        Log.d(TAG, "Final exceedingWidth: $exceedingWidth")
        Log.d(TAG, "Final exceedingHeight: $exceedingHeight")
        Log.d(TAG, "Actual width and height: $widthAndHeight")

        val homeIndexNewFriendIndexes = getUpdatedLevelWithPossiblyNewColumnCount(
            info = info,
            newColumnCount = closestColumnsRatio.first,
        )

        return DensitySettings(
            closestColumnsRatio.first,
            min(boxSize, maxBoxSize).toInt(),
            homeIndexNewFriendIndexes,
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

    private fun isHeightLimitExceeded(
        widthAndHeight: Pair<Int, Int>,
        rowsCount: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int
    ): Pair<Boolean, Float> {
        val usedHeight = getUsedHeight(rowsCount, boxSize, smallGapBetweenLetters)
        return Pair((widthAndHeight.second - usedHeight) <= 0, usedHeight)
    }

    private fun isWidthLimitExceeded(
        widthAndHeight: Pair<Int, Int>,
        columns: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int
    ): Pair<Boolean, Float> {
        val usedWidth = getUsedWidth(columns, boxSize, smallGapBetweenLetters)
        return Pair((widthAndHeight.first - usedWidth) <= 0, usedWidth)
    }

    private fun addLetters(grid: GridLayout, ds: DensitySettings, info: Level, context: Context) {
        val puzzle = info.lettersProperties
            .sortedBy { it.homeIndex }
            .fold(
                ""
            ) { acc, li -> acc + li.letter }

        for ((a, l) in puzzle.withIndex()) {
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


    private fun addSlots(puzzle: String, grid: GridLayout, ds: DensitySettings, context: Context) {
        val solutionRows = getSolutionRows(puzzle, ds.columnCount)
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> grid.addView(Space(context))
                    else -> grid.addView(Slot(context, char).apply {
                        contentDescription = "Slot nr ${this.homeIndex}"
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