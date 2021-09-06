package com.indest.oc

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.indest.oc.letter.BoxDirector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.indest.oc.util.initInsetsController
import com.indest.oc.databinding.ActivityGameBinding
import com.indest.oc.fragments.ExitGameDialogFragment
import com.indest.oc.persistance.LevelProgress
import com.indest.oc.letter.IBox
import com.indest.oc.letter.Letter
import com.indest.oc.letter.LetterState
import com.indest.oc.letter.Slot
import com.indest.oc.letter.effects.ColorSelector
import com.indest.oc.persistance.Level
import com.indest.oc.persistance.LevelAndLetterInfo
import com.indest.oc.util.DensitySettings
import com.indest.oc.util.Util
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

import com.google.android.gms.tasks.Task

import androidx.annotation.NonNull

import com.google.android.gms.tasks.OnCompleteListener

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.CommonStatusCodes


@AndroidEntryPoint
class GameActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener,
    ExitGameDialogFragment.ExitGameDialogListener {
    private val TAG = "GameActivityInit"
    private lateinit var binding: ActivityGameBinding
    private lateinit var insetsController: WindowInsetsControllerCompat
    @Inject
    lateinit var bd: BoxDirector

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Scale factor (density): ${resources.displayMetrics.density}")
        super.onCreate(savedInstanceState)

        signInSilently()

        binding = ActivityGameBinding.inflate(layoutInflater)
        val rootView = binding.root
        setContentView(rootView)
        insetsController = initInsetsController(rootView)
        // Set text switcher animations
        binding.levelNrSwitcher.setInAnimation(this, R.anim.slide_in_bottom)
        binding.levelNrSwitcher.setOutAnimation(this, R.anim.slide_out_top)

        setUpLevelSelect()
        bd.registerLifeCycleOwner(lifecycle)

        lifecycleScope.launchWhenCreated {
            bd.levelRepository.insertLevel(
                Level(1, "WOW", "WWO", "Surprise", LevelProgress.TODO, 0 )
            )
            val levelAndLettersInfo = bd.levelRepository.getLastLevel()
            Log.i(TAG, levelAndLettersInfo.lettersInfo.toString())
            bd.currentLevel = levelAndLettersInfo.level
            displayCurrentLevel(levelAndLettersInfo)
        }

        binding.levelNrSwitcher.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            Toast.makeText(applicationContext, "Restarted activity", Toast.LENGTH_LONG).show()
        }

        binding.levelNrSwitcher.setOnLongClickListener {
            lifecycleScope.launch {
                bd.levelRepository.deleteLetterInfo()
            }
            Toast.makeText(applicationContext, "Deleted level letters info from DB", Toast.LENGTH_LONG).show()
            true
        }
        binding.hintsButton.setOnClickListener {
            Toast.makeText(applicationContext,
                Util.gridToPuzzleMapper(binding.gridContainer.getChildAt(0) as GridLayout), Toast.LENGTH_LONG).show()
        }

    }

    private fun signInSilently() {
        val signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            val signedInAccount = account
        } else {
            // Haven't been signed-in before. Try the silent sign-in first.
            val signInClient = GoogleSignIn.getClient(this, signInOptions)
            signInClient
                .silentSignIn()
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {
                        // The signed in account is stored in the task's result.
                        val signedInAccount = task.result
                        Log.i(TAG, "Sign in successful for ${signedInAccount.displayName}")
                    } else {
                        Log.i(TAG, "Player will need to sign-in explicitly using via UI: ${task.exception}")
                        // Player will need to sign-in explicitly using via UI.
                        // See [sign-in best practices](http://developers.google.com/games/services/checklist) for guidance on how and when to implement Interactive Sign-in,
                        // and [Performing Interactive Sign-in](http://developers.google.com/games/services/android/signin#performing_interactive_sign-in) for details on how to implement
                        // Interactive Sign-in.
                    }
                }
        }
    }


    /*
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return gameState == GameState.PLAYING
    }
    */

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        binding.gridContainer.removeAllViews()
        bd.boxes.clear()
        super.onDestroy()
    }

    private fun displayCurrentLevel(info: LevelAndLetterInfo) {
        var currentPuzzle = bd.currentLevel.puzzle
        if (currentPuzzle.isEmpty()) {
            currentPuzzle = Util.getShuffledPuzzle(bd.currentLevel.solution)
            lifecycleScope.launch {
                bd.levelRepository.updateLevelPuzzle(bd.currentLevel.levelId, currentPuzzle)
            }
            bd.currentLevel.puzzle = currentPuzzle
        }

        val densitySettings = Util.getDensitySettings(
            resources.displayMetrics.density,
            resources.displayMetrics.scaledDensity,
            bd.currentLevel.puzzle
        )

        val board =
            layoutInflater.inflate(R.layout.grid, binding.gridContainer, false) as GridLayout
        board.columnCount = densitySettings.columnCount
        addLetters(board, densitySettings, info)
        addBlanks(densitySettings.columnCount, board)
        addSlots(board, densitySettings)

        bd.boxes = board.children.filterIsInstance<IBox>().toMutableList()
        val lettersTransition =
            TransitionInflater.from(this).inflateTransition(R.transition.letters_transition)
        TransitionManager.beginDelayedTransition(binding.gridContainer, lettersTransition)
        board.doOnPreDraw {
            Log.i(TAG, "doOnPreDraw")
            board.children.filterIsInstance<Letter>().forEach { Log.i(TAG, "$it") }
            restoreSlotPlacements(info, board)
            if (info.lettersInfo.isEmpty()) addInitialLetterInfo()
        }
        binding.gridContainer.addView(board)
    }

    private fun addInitialLetterInfo()
    {
        lifecycleScope.launch {
            bd.boxes.filterIsInstance<Letter>().forEach {
                bd.levelRepository.insertLetterInfo(
                    level = bd.currentLevel.levelId,
                    homeIndex = it.homeIndex,
                    friendIndex = it.friendIndex,
                    letter = it.text.first()
                )
            }
        }
    }

    private fun restoreSlotPlacements(info: LevelAndLetterInfo, board: GridLayout) {
        Log.i(TAG, "restoreSlotPlacements")
        when {
            info.lettersInfo.isNotEmpty() -> {
                info.lettersInfo.forEach { li ->
                    if (li.friendIndex != -666)
                    {
                        val slot = bd.boxes.find { it.homeIndex == li.friendIndex } as Slot
                        val letter = bd.boxes.filterIsInstance<Letter>().find { it.homeIndex == li.homeIndex } as Letter
                        letter.x = slot.x
                        letter.y = slot.y
                        letter.setFriend(slot)
                        slot.setFriend(letter)
                        letter.friendIndex = slot.homeIndex
                        letter.currentPos = slot.pos
                        val colors = letter.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND)
                        letter.setBackgroundColor(colors.viewEnd)
                    }
                }
            }
        }

        bd.boxes.filterIsInstance<Letter>().forEach {
            it.state = LetterState.HOME
        }
    }

    private fun addLetters(grid: GridLayout, ds: DensitySettings, info: LevelAndLetterInfo) {
        val backgroundColor = if (bd.currentLevel.progress == LevelProgress.DONE) {
            R.color.colorSecondary
        } else {
            R.color.colorOnSecondary
        }
        val state = if (bd.currentLevel.progress == LevelProgress.DONE) {
            LetterState.DISABLED
        } else {
            LetterState.INITIALIZING
        }

        // Also part of restoring the state
        val puzzle = if (info.lettersInfo.isEmpty()) bd.currentLevel.puzzle
        else {
            info.lettersInfo
                .sortedBy { it.homeIndex }
                .fold("", { acc, li -> acc + li.letter }
                )
        }

        // Add letters
        for ((index,l) in puzzle.withIndex()) {
            if (l.isWhitespace()) {
                continue
            }
            val char = if (info.lettersInfo.isEmpty()) l.toString()
            else info.lettersInfo.first { it.homeIndex == index }.letter.toString()

            grid.addView(
                Letter(
                    this,
                    char,
                    state
                )
                    .apply {
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
        if (bd.currentLevel.progress != LevelProgress.DONE) {
            val puzzleWithoutWhitespace = bd.currentLevel.solution.replace("\\s".toRegex(), "")
            val someValue =
                puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / ds.columnCount) * ds.columnCount
            for (i in 1..ds.columnCount - someValue) {
                grid.addView(Space(this))
            }
        }
    }

    private fun addBlanks(columnCount: Int, grid: GridLayout) {
        for (i in 1..columnCount) {
            layoutInflater.inflate(R.layout.space, grid)
        }
    }

    private fun addSlots(grid: GridLayout, ds: DensitySettings) {
        val solutionRows = Util.getSolutionRows(bd.currentLevel.solution, ds.columnCount)
        for (row in solutionRows) {
            for (char in row) {
                when (char) {
                    ' ' -> grid.addView(Space(this))
                    else -> grid.addView(Slot(this).apply {
                        textSize = ds.textSize
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

    private fun setUpLevelSelect() {
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

    // TODO: Move to BLL or viewmodel?
    private fun increaseScore(scoreView: TextView, plus: Int) {
        /*  val currentValue = scoreView.text.toString().toInt()
            val animator = ValueAnimator.ofInt(currentValue, currentValue + plus)
            animator.duration = 1000
            animator.addUpdateListener { animation ->
                scoreView.text = animation.animatedValue.toString().padStart(5, '0')
            }
            animator.start()*/
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        newConfig.uiMode
        super.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        signInSilently()
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
        /*   binding.gridContainer.removeAllViewsInLayout()
           boxDirector.boxes.clear()
           boxDirector.boxes =
               binding.gridContainer.children.filterIsInstance<IBox>().toMutableList() // TODO: Move to BLL

            val lettersTransition =
                TransitionInflater.from(this).inflateTransition(R.transition.letters_transition)

            TransitionManager.beginDelayedTransition(binding.gridContainer, lettersTransition)
            binding.gridContainer.removeAllViewsInLayout()
            binding.gridContainer.addView(level!!.grid)*/
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(TAG, "onNothingSelected")
    }

    override fun onBackPressed() {
        showExitDialog()
    }

    private fun showExitDialog() {
        val dialog = ExitGameDialogFragment()
        dialog.show(supportFragmentManager, "ExitGameDialogFragment")
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        finish()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }
}



