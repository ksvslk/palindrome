package com.indest.oc

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.transition.*
import com.indest.oc.letter.Letter
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.letter.effects.Effects
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.Util
import com.indest.oc.util.Util.activateFullScreen
import java.time.LocalDateTime
import kotlin.random.Random

class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = "GameActivity"
    private lateinit var gameLayout: LinearLayoutCompat
    private lateinit var letters: androidx.gridlayout.widget.GridLayout
    private lateinit var spinner: Spinner
    private lateinit var data: Effects
    private val boxDirector = BoxDirector
    private lateinit var puzzles: Array<*>
    private lateinit var result: List<*>

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        gameLayout = findViewById(R.id.game_container)
        letters = findViewById(R.id.boxes_gridlayout)
        spinner = findViewById(R.id.action_bar_spinner)

        data = Effects(applicationContext, resources)
        puzzles = application.assets.open("sorteeritud.txt").bufferedReader().lines().filter { it.length < 25 }
            .toArray() as Array<*>

        fun countSentencesWith(from:Int, to:Int, doesNotContain:Regex) = puzzles.filter {
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

        val from = 1
        val to = 6
        val doesNotContain = "[@]".toRegex()

        result = countSentencesWith(from,to, doesNotContain) as List<*>

        Log.i(TAG, "There are ${result.size} puzzles which have a word with length in range $from to $to (and does not contain $doesNotContain)")
        Log.i(TAG, "${result.count {(it as String).length < 25}}")
        Log.i(TAG, "${result.filter { (it as String).length < 25 }}")
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            (0..result.size-1).toList()
        )

        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = this
        Log.i(TAG, "Scale factor (density): ${resources.displayMetrics.density}")
        activateFullScreen(window, findViewById(R.id.game_container))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateGridWithLetters(p: String) {
        val random = Random(LocalDateTime.now().nano.toLong())
        val listOfWords = p.splitToSequence(" ").toList()
        val shuffledChars = p.toCharArray()
        while (shuffledChars.concatToString() == p) shuffledChars.shuffle(random)
        val puzzle = shuffledChars.concatToString()

        val ds = Util.getDensitySettings(
            resources.displayMetrics.density,
            resources.displayMetrics.scaledDensity, puzzle, listOfWords)

        val transition = TransitionInflater.from(this).inflateTransition(R.transition.letters_transition)
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
        TransitionManager.beginDelayedTransition(letters, transition)
        letters.removeAllViews()
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
                    data,
                    boxDirector,
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

        // Solution grid
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
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        activateFullScreen(window, findViewById(R.id.game_container))
        super.onResume()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        changeToNextLevel(position)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun changeToNextLevel(levelNr: Int)
    {
        val puzzle = (result[levelNr] as String).uppercase()
        val levelTextView = findViewById<TextView>(R.id.level_text)
        levelTextView.text = "Level ${levelNr+1}"
        populateGridWithLetters(puzzle)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "onNothingSelected")
    }

}


