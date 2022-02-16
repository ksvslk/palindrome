package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.animation.*
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.components.Animations
import com.indest.hah.components.LevelCreator
import com.indest.hah.components.SoundPlayer
import com.indest.hah.databinding.LevelFragmentBinding
import com.indest.hah.game.CellDirector
import com.indest.hah.game.LevelViewModel
import com.indest.hah.game.LevelViewModel.Solution.*
import com.indest.hah.game.LevelViewModel.UiState.*
import com.indest.hah.game.MessageSnackBar
import com.indest.hah.letter.Cell
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.dto.Level
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LevelFragment : Fragment(R.layout.level_fragment) {
    private val TAG = "LevelFragment"

    private val viewModel: LevelViewModel by activityViewModels()
    private lateinit var binding: LevelFragmentBinding
    lateinit var cellDirector: CellDirector

    @Inject
    lateinit var levelCreator: LevelCreator

    @Inject
    lateinit var animations: Animations

    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LevelFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding.levelFragment.layoutTransition = animations.getLayoutTransition()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiStateUpdater()
        levelUiUpdater()
        solutionUiUpdater()
    }

    private fun levelUiUpdater() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                WindowInfoTracker.getOrCreate(requireContext())
                    .windowLayoutInfo(requireActivity())
                    .collect { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookMode = foldingFeature?.state == FoldingFeature.State.FLAT
                        viewModel.isBookMode = isBookMode
                        viewModel.latestLevelFlow().collect { level ->
                            binding.levelFragment.removeAllViews()
                            if (level.levelInfo.progress == LevelProgress.INTERMEDIATE) {
                                displayLevel(level, isBookMode, isLevelFinished = true)
                            } else {
                                displayLevel(level, isBookMode)
                            }
                            //TODO: Collect reward flow here?
                        }
                    }
            }
        }
    }

    private fun displayLevel(level: Level, isBookMode: Boolean, isLevelFinished:Boolean = false) {
        val levelGridLayout = levelCreator.getLevel(
            level,
            layoutInflater,
            binding.levelFragment,
            isBookMode,
            viewModel.windowSizeClassWidth
        )
        levelGridLayout.children.filterIsInstance<Cell<*, *>>().apply {
            if (!isLevelFinished) viewModel.persistInitialLettersInfo(this)
            levelGridLayout.doOnPreDraw {
                cellDirector = CellDirector(
                    level,
                    this,
                    viewModel
                )
            }
        }
        binding.levelFragment.notClickable = isLevelFinished
        binding.levelFragment.addView(levelGridLayout)
        if (isLevelFinished) showContinueMessage()
        viewModel.setState(TOUCH_ENABLED)
    }

    private fun uiStateUpdater()
    {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { uiState ->
                    with(binding)
                    {
                        when(uiState)
                        {
                            LOADING -> {
                                progressCircular.visibility = View.VISIBLE
                                levelFragment.notClickable = true
                            }
                            TOUCH_ENABLED -> {
                                progressCircular.visibility = View.GONE
                                levelFragment.notClickable = false
                            }
                            TOUCH_DISABLED -> {
                                progressCircular.visibility = View.GONE
                                levelFragment.notClickable = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun solutionUiUpdater() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.solution.collect { solutionData ->
                    when (solutionData.solution) {
                        CORRECT -> {
                            updateUiWhenCorrect(solutionData.score, solutionData.maxScore)
                        }
                        WRONG -> {
                            cellDirector.sendWrongSolutionLetterHome(solutionData.index)
                            showWrongMessage(WRONG)
                        }
                        BOGUS_PALINDROME -> {
                            cellDirector.sendWrongSolutionLetterHome(solutionData.index)
                            showWrongMessage(BOGUS_PALINDROME)
                        }
                        NONE -> {
                            Log.d(TAG, "Solution is: NONE")
                        }
                    }
                }
            }
        }
    }

    private fun updateUiWhenCorrect(score: Int, maxScore: Int) {
        val cells = cellDirector.getCells()
        val cellsAnimation = animations.getCorrectSolutionAnimation(cells)
        cellsAnimation.doOnEnd {
            if (isAdded) {
                playMaxScoreAnimation(score, maxScore)
                val winningMessage = getWinningMessage(score, maxScore)
                val scoreMessage = getScoreMessage()
                val winningMessageCallBack = object :
                    BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
                    override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                        if (isVisible) {
                            super.onDismissed(transientBottomBar, event)
                            scoreMessage.show()
                            val scoreView = scoreMessage.view.findViewById<TextView>(R.id.message)
                            val scoreAnimation =
                                animations.getScoreAnimation(scoreView, score, maxScore)
                            scoreAnimation.start()
                        }
                    }
                }
                winningMessage.addCallback(winningMessageCallBack)
                val scoreMessageCallback = object :
                    BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
                    override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                        if (isVisible) {
                            super.onDismissed(transientBottomBar, event)
                            showContinueMessage()
                        }
                    }
                }
                scoreMessage.addCallback(scoreMessageCallback)
                winningMessage.show()
            }
        }
        cellsAnimation.start()
    }

    private fun playMaxScoreAnimation(score: Int, maxScore: Int) {
        if (score == maxScore) {
            (requireActivity() as? MainActivity)?.let {
                val rain = animations.getRainConfetti()
                it.binding.konfettiView.start(rain)
            }
        }
    }

    private fun getWinningMessage(score: Int, maxScore: Int): MessageSnackBar {
        val randomCorrectMessage = if (score == maxScore) {
            resources.getStringArray(R.array.very_good_texts).random()
        } else {
            resources.getStringArray(R.array.good_texts).random()
        }

        return MessageSnackBar.make(
            view = binding.levelFragment,
            message = randomCorrectMessage,
            duration = 2000,
            windowSizeClassWidth = viewModel.windowSizeClassWidth,
            isBookMode = viewModel.isBookMode,
            textSize = MessageSnackBar.Companion.TextSize.BIG,
        )
    }

    private fun getScoreMessage(): MessageSnackBar {
        return MessageSnackBar.make(
            view = binding.levelFragment,
            message = "",
            duration = 2000,
            windowSizeClassWidth = viewModel.windowSizeClassWidth,
            isBookMode = viewModel.isBookMode,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
        )
    }

    private fun showWrongMessage(solutionType: LevelViewModel.Solution) {
        animations.getWrongSolutionAnimation(binding.levelFragment).start()
        soundPlayer.vibrateDoubleClick()
        val messageToDisplay =
            if (solutionType == BOGUS_PALINDROME)
                resources.getStringArray(R.array.palindrome_wrong_texts).random()
            else
                resources.getStringArray(R.array.solution_wrong_texts).random()

        val message = MessageSnackBar.make(
            binding.levelFragment,
            messageToDisplay,
            2000,
            viewModel.windowSizeClassWidth,
            viewModel.isBookMode,
            MessageSnackBar.Companion.TextSize.SMALL,
            false
        )
        message.show()
    }

    private fun showContinueMessage() {
        val continueMessage = MessageSnackBar.make(
            view = binding.levelFragment,
            message = getString(R.string.tap_to_continue),
            duration = Int.MAX_VALUE,
            windowSizeClassWidth = viewModel.windowSizeClassWidth,
            isBookMode = viewModel.isBookMode,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
        )
        val tapToContinueAnimation = animations.getContinueAnimation(continueMessage)
        tapToContinueAnimation.start()

        continueMessage.view.setOnClickListener {
            if (isVisible) {
                continueMessage.dismiss()
                soundPlayer.play(soundPlayer.sounds.levelTransition)
                viewModel.nextLevel()
            }
        }
        continueMessage.show()
    }

}