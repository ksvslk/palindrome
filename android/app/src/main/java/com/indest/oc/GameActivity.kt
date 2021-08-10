package com.indest.oc

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import com.indest.oc.letter.Letter
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.Util
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlin.random.Random
import android.animation.ValueAnimator
import android.view.Gravity
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import com.indest.oc.declarations.getInsetsController

@AndroidEntryPoint
class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = "GameActivity"
    private lateinit var gameLayout: LinearLayoutCompat
    private lateinit var gridContainer: LinearLayoutCompat
    //private lateinit var letters: androidx.gridlayout.widget.GridLayout
    private lateinit var spinner: Spinner
    private lateinit var textSwitcher: TextSwitcher
    private lateinit var scoreView: TextView
    private lateinit var puzzles: List<*>
    private lateinit var result: List<*>
    private lateinit var insetsController: WindowInsetsControllerCompat

    @Inject
    lateinit var boxDirector: BoxDirector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate game")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        gameLayout = findViewById(R.id.game_container)
        gridContainer = findViewById(R.id.top_llc)
        insetsController = getInsetsController(gameLayout)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        //letters = findViewById(R.id.boxes_gridlayout)
        spinner = findViewById(R.id.action_bar_spinner)

        textSwitcher = findViewById(R.id.level_nr_switcher)
        scoreView = findViewById(R.id.score_text)
        textSwitcher.setInAnimation(this, R.anim.slide_in_bottom)
        textSwitcher.setOutAnimation(this, R.anim.slide_out_top)

        puzzles = application.assets.open("sorteeritud.txt").bufferedReader().lineSequence()
            .filter { it.length < 25 }.toList()
        val from = 1
        val to = 6
        val doesNotContain = "[@]".toRegex()
        result = countSentencesWith(from, to, doesNotContain)
        Log.i(
            TAG,
            "There are ${result.size} puzzles which have a word with length in range $from to $to (and does not contain $doesNotContain)"
        )
        Log.i(TAG, "${result.count { (it as String).length < 25 }}")
        Log.i(TAG, "${result.filter { (it as String).length < 25 }}")
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            (1..96).toList()
        )

        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = this
        Log.i(TAG, "Scale factor (density): ${resources.displayMetrics.density}")

        scoreView.setOnClickListener {
            increaseScore(scoreView, 1234)
        }
    }

    private fun increaseScore(scoreView: TextView, plus: Int) {
        val currentValue = scoreView.text.toString().toInt()
        val animator = ValueAnimator.ofInt(currentValue, currentValue + plus)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = animation.animatedValue.toString().padStart(5, '0')
        }
        animator.start()
    }

    private fun countSentencesWith(from: Int, to: Int, doesNotContain: Regex) =
        puzzles.filter { it ->
            (it as String)
                .splitToSequence(" ")
                .filterNot { it.contains(doesNotContain) }
                .toList()
                .maxWithOrNull { o1, o2 ->
                    when {
                        o1 == null && o2 == null -> 0
                        o1 == null && o2 != null -> -1
                        o1 != null && o2 == null -> 1
                        o1 != null && o2 != null && o1.length < o2.length -> -1
                        o1 != null && o2 != null && o1.length > o2.length -> 1
                        o1?.length == o2?.length -> 0
                        else -> 0
                    }
                }!!.length in from..to
        }

    // TODO: use chunk
    private fun populateGridWithLetters(p: String) {
        val timeNow = Date().time
        val random = Random(timeNow)
        val listOfWords = p.splitToSequence(" ").toList()
        val shuffledChars = p.toCharArray()
        while (shuffledChars.concatToString() == p) shuffledChars.shuffle(random)
        val puzzle = shuffledChars.concatToString()

        val ds = Util.getDensitySettings(
            resources.displayMetrics.density,
            resources.displayMetrics.scaledDensity, puzzle, listOfWords
        )
        val ls = gridContainer.getChildAt(0) as? androidx.gridlayout.widget.GridLayout
        ls?.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))

        boxDirector.boxes.clear()
        gridContainer.removeAllViews()
        layoutInflater.inflate(R.layout.grid, gridContainer) as LinearLayoutCompat
        val letters = gridContainer.getChildAt(0) as androidx.gridlayout.widget.GridLayout

        val puzzleWithoutWhitespace = puzzle.replace("\\s".toRegex(), "")
        letters.columnCount = ds.colCount

        // Letters grid
        for (l in puzzle) {
            if (l.isWhitespace()) {
                continue
            }
            letters.addView(
                Letter(
                    this,
                    l.toString(),
                    State.INITIALIZING,
                    R.style.LetterView
                )
                    .also {
                        boxDirector.boxes.add(it)
                        it.width = ds.boxSize
                        it.height = ds.boxSize
                        it.textSize = ds.textSize
                        it.typeface = ResourcesCompat.getFont(this, R.font.yantramanav_bold)
                        it.gravity = Gravity.CENTER
                        it.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnSecondary))
                        it.setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary))
                    })
        }

        val someValue = puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / ds.colCount) * ds.colCount
        for (i in 1..ds.colCount - someValue) {
            letters.addView(Space(this))
        }

        for (i in 1..ds.colCount)
        {
            layoutInflater.inflate(R.layout.space, letters)
        }

        val solutionRows = getSolutionRows(p, ds.colCount)
        assert(solutionRows.all { it.length <= ds.colCount }) {
            "Solution rows had a string that is longer than ${ds.colCount}: $solutionRows"
        }
        Log.i(TAG, "Solution rows: $solutionRows")
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> letters.addView(Space(this))
                    else -> letters.addView(Slot(this).also {
                        boxDirector.boxes.add(it)
                        it.textSize = ds.textSize
                        it.width = ds.boxSize
                        it.height = ds.boxSize
                        it.typeface = ResourcesCompat.getFont(this, R.font.yantramanav_bold)
                        it.gravity = Gravity.CENTER
                        it.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSecondaryVariant))
                        it.setTextColor(ContextCompat.getColor(this, R.color.colorSecondaryVariant))
                        it.text = " "
                    })
                }
            }
        }

        letters.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))

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
                else -> {
                    // uus rida, aga täienda eelmist
                    val blankCount = cols - acc.last().length
                    val last = acc.last().plus(" ".repeat(blankCount))
                    acc.subList(0, acc.lastIndex) + last + s
                }
            }
        })
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        super.onResume()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        changeToNextLevel(position)
    }

    private fun changeToNextLevel(levelNr: Int) {
        val puzzle = (result[levelNr] as String).uppercase()
        Log.i(TAG, "TextSwitcher is $textSwitcher")
        textSwitcher.setText("${levelNr + 1}")
        populateGridWithLetters(puzzle)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "onNothingSelected")
    }

}



