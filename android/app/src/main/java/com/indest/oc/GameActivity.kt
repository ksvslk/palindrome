package com.indest.oc

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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import com.indest.oc.declarations.getInsetsController
import com.google.gson.Gson
import com.indest.oc.util.Level


@AndroidEntryPoint
class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = "GameActivity"
    private lateinit var gameLayout: LinearLayoutCompat
    private lateinit var gridContainer: LinearLayoutCompat
    private lateinit var spinner: Spinner
    private lateinit var textSwitcher: TextSwitcher
    private lateinit var scoreView: TextView
    private lateinit var puzzles: Array<Level>
    private lateinit var insetsController: WindowInsetsControllerCompat

    @Inject
    lateinit var boxDirector: BoxDirector

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate game")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        gameLayout = findViewById(R.id.game_container)
        gridContainer = findViewById(R.id.top_llc)
        insetsController = getInsetsController(gameLayout)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        spinner = findViewById(R.id.action_bar_spinner)

        textSwitcher = findViewById(R.id.level_nr_switcher)
        scoreView = findViewById(R.id.score_text)
        textSwitcher.setInAnimation(this, R.anim.slide_in_bottom)
        textSwitcher.setOutAnimation(this, R.anim.slide_out_top)

        val pz = application.assets.open("puzzles.json").bufferedReader().use { it.readText() }

        val gson = Gson()
        puzzles = gson.fromJson(pz, Array<Level>::class.java)

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
                    .apply {
                        boxDirector.boxes.add(this)
                        width = ds.boxSize
                        height = ds.boxSize
                        textSize = ds.textSize
                        typeface = ResourcesCompat.getFont(context, R.font.yantramanav_bold)
                        gravity = Gravity.CENTER
                        setBackgroundColor(ContextCompat.getColor(context, R.color.colorOnSecondary))
                        setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
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
                    else -> letters.addView(Slot(this).apply {
                        boxDirector.boxes.add(this)
                        textSize = ds.textSize
                        width = ds.boxSize
                        height = ds.boxSize
                        typeface = ResourcesCompat.getFont(context, R.font.yantramanav_bold)
                        gravity = Gravity.CENTER
                        setBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondaryVariant))
                        setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryVariant))
                        text = " "
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
                else -> { // New line
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
        val puzzle = puzzles[levelNr].puzzle
        Log.i(TAG, "TextSwitcher is $textSwitcher")
        textSwitcher.setText("${levelNr + 1}")
        populateGridWithLetters(puzzle)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "onNothingSelected")
    }

}



