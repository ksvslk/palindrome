package com.indest.oc

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.transition.*
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
import androidx.core.view.*
import com.indest.oc.declarations.getInsetsController
import kotlin.collections.ArrayList

@AndroidEntryPoint
class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = "GameActivity"
    private lateinit var gameLayout: LinearLayoutCompat
    private lateinit var letters: androidx.gridlayout.widget.GridLayout
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
        insetsController = getInsetsController(gameLayout)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        letters = findViewById(R.id.boxes_gridlayout)
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
        result = countSentencesWith(from, to, doesNotContain) as List<*>
        Log.i(
            TAG,
            "There are ${result.size} puzzles which have a word with length in range $from to $to (and does not contain $doesNotContain)"
        )
        Log.i(TAG, "${result.count { (it as String).length < 25 }}")
        Log.i(TAG, "${result.filter { (it as String).length < 25 }}")
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            (result.indices).toList()
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

        val transition =
            TransitionInflater.from(this).inflateTransition(R.transition.letters_transition)

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {
            }

            override fun onTransitionEnd(transition: Transition) {
                boxDirector.boxes.forEach { b ->
                    if (b is Letter) {
                        b.homePos = b.pos
                        b.currentPos = b.pos
                        b.state = State.HOME
                    }
                }
            }

            override fun onTransitionCancel(transition: Transition) {
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionResume(transition: Transition) {
            }
        })
        boxDirector.boxes.clear()
        letters.forEachIndexed { index, view ->
            if (view is Slot) {
                letters.removeViewAt(index)
                letters.addView(Space(this), index)
            }
        }
        letters.removeAllViews()
        TransitionManager.beginDelayedTransition(letters, transition)


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
                    })
        }
        if (puzzle.length > ds.colCount) {
            for (i in 1..ds.colCount - (puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / ds.colCount) * ds.colCount)) {
                letters.addView(Space(this))
            }
        } else {
            for (i in 1..ds.colCount - puzzle.length) {
                letters.addView(Space(this))
            }
        }

        val solutionRows = getSolutionRows(p, ds.colCount)
        assert(solutionRows.all { it.length <= ds.colCount }) {
            "Solution rows had a string that is longer than ${ds.colCount}: $solutionRows"
        }
        Log.i(TAG, solutionRows.toString())
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> letters.addView(Space(this))
                    else -> letters.addView(Slot(this).also {
                        boxDirector.boxes.add(it)
                        it.textSize = ds.textSize
                        it.width = ds.boxSize
                        it.height = ds.boxSize
                        it.text = " "
                    })
                }
            }
        }
        // Solution grid
        /*
        var charsLeftInRow = ds.colCount
        for (word in listOfWords) {
            if ((charsLeftInRow - word.length) > 0) {

                for (char in word) {
                    letters.addView(
                        Slot(this).also {
                            boxDirector.boxes.add(it)
                            it.textSize = ds.textSize
                            it.width = ds.boxSize
                            it.height = ds.boxSize
                            it.text = " "
                        })
                    charsLeftInRow -= 1
                }

            } else {
                for (i in 1..charsLeftInRow) {
                    letters.addView(Space(this))
                }
                charsLeftInRow = ds.colCount

                for (char in word) {
                    letters.addView(
                        Slot(this).also {
                            boxDirector.boxes.add(it)
                            it.textSize = ds.textSize
                            it.width = ds.boxSize
                            it.height = ds.boxSize
                            it.text = " "
                        })
                }
                charsLeftInRow -= word.length
            }
        }
         */
    }

    private fun getSolutionRows(puzzle: String, cols: Int): List<String> {
        return puzzle.split(' ').fold(mutableListOf(), { acc: List<String>, s: String ->
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


