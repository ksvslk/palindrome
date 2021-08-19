package com.indest.oc

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.Util
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.view.*
import com.indest.oc.declarations.initInsetsController
import com.indest.oc.databinding.ActivityGameBinding
import com.indest.oc.game.GameState
import com.indest.oc.game.LevelFactory
import com.indest.oc.game.Level
import com.indest.oc.letter.IBox

@AndroidEntryPoint
class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val TAG = "GameActivity"
    private var gameState: GameState = GameState.PLAYING // TODO: Move to BLL
    private lateinit var binding: ActivityGameBinding
    private lateinit var puzzles: Array<Level> // TODO: Move to BLL
    private lateinit var insetsController: WindowInsetsControllerCompat
    @Inject lateinit var boxDirector: BoxDirector // TODO: Move to BLL

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Scale factor (density): ${resources.displayMetrics.density}")
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)
        insetsController = initInsetsController(rootView)

        // Set text switcher animations
        binding.levelNrSwitcher.setInAnimation(this, R.anim.slide_in_bottom)
        binding.levelNrSwitcher.setOutAnimation(this, R.anim.slide_out_top)

        LevelFactory.puzzles = Util.getPuzzles(application)
        setUpLevelSelect()
    }

    /*
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return gameState == GameState.PLAYING
    }
     */

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
        val currentValue = scoreView.text.toString().toInt()
        val animator = ValueAnimator.ofInt(currentValue, currentValue + plus)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = animation.animatedValue.toString().padStart(5, '0')
        }
        animator.start()
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
        Log.i(TAG, "TextSwitcher is ${binding.levelNrSwitcher}")
        binding.levelNrSwitcher.setText("${levelNr + 1}")
        val level = LevelFactory.getFreshLevel(
            levelNr,
            resources,
            this,
            binding.gridContainer,
            layoutInflater
        )
        boxDirector.boxes = level.grid.children.filterIsInstance<IBox>().toMutableList() // TODO: Move to BLL
        binding.gridContainer.removeAllViewsInLayout()
        binding.gridContainer.addView(level.grid)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "onNothingSelected")
    }
}



