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
import android.view.Gravity
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.gridlayout.widget.GridLayout
import com.indest.oc.declarations.initInsetsController
import com.google.gson.Gson
import com.indest.oc.databinding.ActivityGameBinding
import com.indest.oc.util.DensitySettings
import com.indest.oc.util.Level

@AndroidEntryPoint
class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = "GameActivity"
    private lateinit var binding: ActivityGameBinding
    private lateinit var puzzles: Array<Level>
    private lateinit var insetsController: WindowInsetsControllerCompat
    @Inject
    lateinit var boxDirector: BoxDirector

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Scale factor (density): ${resources.displayMetrics.density}")
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)
        insetsController = initInsetsController(rootView)

        // Set text switcher animations
        binding.levelNrSwitcher?.setInAnimation(this, R.anim.slide_in_bottom)
        binding.levelNrSwitcher?.setOutAnimation(this, R.anim.slide_out_top)

        val pz = application.assets.open("puzzles.json").bufferedReader().use {
            it.readText()
        }
        puzzles = Gson().fromJson(pz, Array<Level>::class.java)

        setUpLevelSelect()
    }

    private fun setUpLevelSelect()
    {
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            (1..96).toList()
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.actionBarSpinner.adapter = arrayAdapter
        binding.actionBarSpinner.onItemSelectedListener = this

        binding.scoreText.setOnClickListener {
            increaseScore(binding.scoreText, 1234)
        }
    }

    private fun increaseScore(scoreView: TextView, plus: Int) {
        val puzzle = "WOW"
        val letters = binding.gridContainer.getChildAt(0) as androidx.gridlayout.widget.GridLayout
        letters.removeAllViews()

        val ds = Util.getDensitySettings(
            resources.displayMetrics.density,
            resources.displayMetrics.scaledDensity,
            puzzle
        )
        addLetters(puzzle, letters, ds, true)
      /*  boxDirector.checkSolution()
        val currentValue = scoreView.text.toString().toInt()
        val animator = ValueAnimator.ofInt(currentValue, currentValue + plus)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = animation.animatedValue.toString().padStart(5, '0')
        }
        animator.start()*/
    }

    private fun initializeLevel(originalPuzzle: String) {

        val shuffledPuzzle = getShuffledPuzzle(originalPuzzle)
        val lettersGrid = binding.gridContainer.getChildAt(0) as? GridLayout

        // Level disappears to the left
        lettersGrid?.startAnimation(
            AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right))

        resetLevel()

        val letters = binding.gridContainer.getChildAt(0) as GridLayout
        val ds = Util.getDensitySettings(
            resources.displayMetrics.density,
            resources.displayMetrics.scaledDensity,
            shuffledPuzzle
        )
        letters.columnCount = ds.colCount

        // Add letters to grid
        addLetters(shuffledPuzzle, letters, ds)

        // Add separating blank spaces (in between letters and slots)
        for (i in 1..ds.colCount) layoutInflater.inflate(R.layout.space, letters)

        // Add solution slots to grid
        addSlots(originalPuzzle, letters, ds)

        // Level appears from the left
        letters.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left))
    }

    private fun resetLevel()
    {
        boxDirector.boxes.clear()
        binding.gridContainer.removeAllViews()
        layoutInflater.inflate(R.layout.grid, binding.gridContainer) as LinearLayoutCompat
    }

    private fun getShuffledPuzzle(originalPuzzle: String): String
    {
        val timeNow = Date().time
        val random = Random(timeNow)
        val shuffledChars = originalPuzzle.toCharArray()
        while (shuffledChars.concatToString() == originalPuzzle) {
            shuffledChars.shuffle(random)
        }
        return shuffledChars.concatToString()
    }

    private fun addLetters(
        puzzle: String,
        letters: GridLayout,
        ds: DensitySettings,
        levelComplete:Boolean = false
    )
    {
        val backgroundColor = if (levelComplete) R.color.colorSecondary else R.color.colorOnSecondary
        val state = if (levelComplete) State.DISABLED else State.INITIALIZING

        // Add letters
        for (l in puzzle) {
            if (l.isWhitespace()) {
                continue
            }
            letters.addView(
                Letter(
                    this,
                    l.toString(),
                    state
                )
                    .apply {
                        boxDirector.boxes.add(this)
                        width = ds.boxSize
                        height = ds.boxSize
                        textSize = ds.textSize
                        typeface = ResourcesCompat.getFont(context, R.font.yantramanav_bold)
                        gravity = Gravity.CENTER
                        setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
                        setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
                    })
        }

        // Add blanks
        if (!levelComplete)
        {
            val puzzleWithoutWhitespace = puzzle.replace("\\s".toRegex(), "")
            val someValue = puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / ds.colCount) * ds.colCount
            for (i in 1..ds.colCount - someValue) {
                letters.addView(Space(this))
            }
        }
    }

    private fun addSlots(originalPuzzle: String, letters: GridLayout, ds: DensitySettings)
    {
        val solutionRows = getSolutionRows(originalPuzzle, ds.colCount)

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
        Log.i(TAG, "TextSwitcher is ${binding.levelNrSwitcher}")
        binding.levelNrSwitcher?.setText("${levelNr + 1}")
        initializeLevel(puzzle)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "onNothingSelected")
    }
}



