package com.indest.hah.game.creation

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Space
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.gridlayout.widget.GridLayout
import com.google.android.play.core.assetpacks.dp
import com.indest.hah.R
import com.indest.hah.game.director.LetterState
import com.indest.hah.persistance.dto.Level
import com.indest.hah.views.Letter
import com.indest.hah.views.Slot
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil


@ActivityScoped
class LevelCreator @Inject constructor(@ActivityContext private val context: Context) {

    private val TAG = "LevelCreator"

    fun getLevel(
        info: Level,
        layoutInflater: LayoutInflater,
        container: ViewGroup,
        isBookMode: BookMode,
        windowSizeClassWidth: WindowSizeClass,
        bottomNavHeight:Int
    ): GridLayout {
        val maxWordLength = info.solutions.first().palindrome.split(" ").maxBy { it.length }.length
        val densitySettings =
            getDensitySettings(isBookMode, windowSizeClassWidth, maxWordLength, container, info, bottomNavHeight)
        val level = layoutInflater.inflate(R.layout.grid, container, false) as GridLayout
        level.columnCount = densitySettings.columnCount
        addLetters(level, densitySettings, info, context)
        addBlanks(densitySettings.columnCount, level, layoutInflater)
        Log.d(TAG, info.solutions.toString())
        Log.d(TAG, info.solutions.first().toString())
        addSlots(info.solutions.first().palindrome, level, densitySettings, context)
        return level
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
        sizeClassWidth: WindowSizeClass,
        maxWordLength: Int,
        container: ViewGroup,
        info: Level,
        bot: Int
    ): DensitySettings {
        var columns =
            if (maxWordLength > context.resources.getInteger(R.integer.defaultColumnsCount))
                maxWordLength
            else context.resources.getInteger(R.integer.defaultColumnsCount)
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
        //var boxSize = boxAndText.first
        //var textSize = boxAndText.second
        val puzzleOnlyLetters = getPuzzle(info)
        val palindrome = info.solutions.first().palindrome
        var rowsCount = puzzleOnlyLetters.chunked(columns).count() + getSolutionRows(palindrome, columns).count()

        Log.d(TAG, "greedyWordwrap: ${greedyWordwrap(palindrome, columns)}")
        Log.d(TAG, "getSolutionRows(palindrome, columns): ${getSolutionRows(palindrome, columns)}")

        val boxDecreaseRate = 1.05f
        val textDecreaseRate = 1.04f
        val smallGap = context.resources.getDimensionPixelSize(R.dimen._2sdp)
        val intermediateGap = context.resources.getDimensionPixelSize(R.dimen._10sdp)

        var boxSize = 1.00f
        var textSize = 1.00f
        val bottomNavHeight =  bot
        var perfectRatio = container.width.toDouble() / (container.height.toDouble() - bottomNavHeight)

        var heightExceeded = isHeightLimitExceeded(container, rowsCount, boxSize, smallGap, intermediateGap, bottomNavHeight)
        var widthExceeded = isWidthLimitExceeded(container, columns, boxSize, smallGap)
        Log.d(TAG, "heightExceeded: ${heightExceeded.first}")
        Log.d(TAG, "widthExceeded: ${widthExceeded.first}")

        while(!heightExceeded.first && !widthExceeded.first)
        {
            val potentialBoxSize = boxSize * boxDecreaseRate
            val potentialColumns = columns + 1
            val potentialRowsCount = puzzleOnlyLetters.chunked(potentialColumns).count() + getSolutionRows(palindrome, potentialColumns).count()
            val testHeight = isHeightLimitExceeded(container, potentialRowsCount, potentialBoxSize, smallGap, intermediateGap, bottomNavHeight)
            val testWidth = isWidthLimitExceeded(container, potentialColumns, potentialBoxSize, smallGap)
            val widthChangedRatio = abs(testWidth.second.toDouble() / heightExceeded.second.toDouble() - perfectRatio)
            val heightChangedRatio = abs(widthExceeded.second.toDouble() / testHeight.second.toDouble()- perfectRatio)

            //Log.d(TAG,"widthChangedRatio: $widthChangedRatio")
            //Log.d(TAG,"heightChangedRatio: $heightChangedRatio")
            Log.d(TAG,"**************************************************")
            Log.d(TAG,"Was test height over the edge? ${testHeight.first}")
            Log.d(TAG,"Was test width over the edge? ${testWidth.first}")
            Log.d(TAG,"Actual height minus bottomnav: ${container.height - bottomNavHeight}")
            Log.d(TAG,"Test used height: ${testHeight.second}")
            Log.d(TAG,"bottomNavHeight height ${bottomNavHeight}")
            //Log.d(TAG,"testWidth: $testWidth")
            //Log.d(TAG,"perfectRatio: $perfectRatio")
            Log.d(TAG,"boxSize: $boxSize")

            val widthHasMoreRoom = ((container.width - testWidth.second) > (container.height - bottomNavHeight - testHeight.second))

            if (testWidth.first || testHeight.first ) break

            if (widthChangedRatio <= heightChangedRatio && widthHasMoreRoom && !testWidth.first)
            {
                Log.d(TAG, "Width wins: ADDING COLUMNS!")
                columns = potentialColumns
                boxSize = potentialBoxSize
                widthExceeded = testWidth
                if (!testHeight.first) heightExceeded = testHeight
            }
            else if (!testHeight.first) {
                Log.d(TAG, "Height wins: INCREASE BOX SIZE")
                boxSize = potentialBoxSize
                heightExceeded = testHeight
                if (!testWidth.first) widthExceeded = testWidth
                Log.d(TAG, "Height after change: $heightExceeded")
                Log.d(TAG, "boxSize after change: $boxSize")

            }
            else {
                break
            }

        }

        Log.d(TAG, "Solution rows: ${getSolutionRows(palindrome, columns)}")

        val divider = getTextDividerValue(isBookMode, sizeClassWidth, densityDpi)
        textSize = boxSize.div(divider)
        //textSize = ceil((dip * context.resources.displayMetrics.scaledDensity).toDouble()).toInt().toFloat()


        return DensitySettings(
            columns,
            boxSize.toInt(),
            2
        )
    }

    fun greedyWordwrap(text: String, lineWidth: Int): String {
        val words = text.split(' ')
        val sb = StringBuilder(words[0])
        var spaceLeft = lineWidth - words[0].length
        for (word in words.drop(1)) {
            val len = word.length
            if (len + 1 > spaceLeft) {
                sb.append("\n").append(word)
                spaceLeft = lineWidth - len
            }
            else {
                sb.append(" ").append(word)
                spaceLeft -= (len + 1)
            }
        }
        return sb.toString()
    }

    private fun isHeightLimitExceeded(
        container: ViewGroup,
        rowsCount: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int,
        intermediateGap: Int,
        bottomNavHeight:Int
    ): Pair<Boolean, Float> {
        val usedHeight = (rowsCount * (boxSize + smallGapBetweenLetters)) + intermediateGap + boxSize + 2*smallGapBetweenLetters+bottomNavHeight
        return Pair((container.height - usedHeight) <= 0, usedHeight)
    }

    private fun isWidthLimitExceeded(
        container: ViewGroup,
        columns: Int,
        boxSize: Float,
        smallGapBetweenLetters: Int,
    ): Pair<Boolean, Float> {
        val usedWidth = (columns * (boxSize + smallGapBetweenLetters) + boxSize + 2*smallGapBetweenLetters)
        return Pair((container.width - usedWidth) <= 0, usedWidth)
    }

    private fun getTextDividerValue(isBookMode: BookMode,
                                    sizeClassWidth: WindowSizeClass,
                                    densityDpi: Int?):Float
    {
        return when (sizeClassWidth) {
            WindowSizeClass.COMPACT -> {
                when (densityDpi) {
                    in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) - COMPAT"
                        )
                        return 1.4f
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) - COMPAT"
                        )
                        return 2.2f
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) - COMPAT"
                        )
                        return 3.0f
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                        return if (isBookMode == BookMode.BOOK_MODE) {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - COMPAT and BOOK MODE"
                            )
                            3.8f
                        } else {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - COMPAT and DEFAULT"
                            )
                            return 3.8f
                        }
                    }
                    in DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) - COMPAT"
                        )
                        return 5.0f
                    }
                    else -> {
                        Log.d(TAG, "DisplayMetrics OTHER - COMPAT")
                        return 3.9f
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
                        return 1.4f
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) - MEDIUM"
                        )
                        return 1.8f
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        Log.d(
                            TAG,
                            "DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) - MEDIUM"
                        )
                        return 2.6f
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {

                        return if (isBookMode == BookMode.BOOK_MODE) {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - MEDIUM and BOOK MODE"
                            )
                            3.3f
                        } else {
                            Log.d(
                                TAG,
                                "DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) - MEDIUM"
                            )
                            return 3.2f
                        }
                    }
                    else -> {
                        Log.d(TAG, "DisplayMetrics OTHER - MEDIUM")
                        return 2.5f
                    }
                }
            }
            WindowSizeClass.EXPANDED -> {
                Log.d(TAG, "DisplayMetrics ... - EXPANDED")
                return 2.5f
            }
            WindowSizeClass.NOT_SET -> {
                Log.d(TAG, "DisplayMetrics ... - NOT SET")
                return 2.5f
            }
        }
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