package com.indest.hah.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.*
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
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
import com.indest.hah.util.VaryingProbability
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
    var cellDirector: CellDirector? = null
    private var allAnimations = mutableSetOf<ValueAnimator>()

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
        containerStateUpdater()
        levelUpdater()
        solutionEffectsUpdater()
    }

    private fun levelUpdater() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                WindowInfoTracker.getOrCreate(requireContext())
                    .windowLayoutInfo(requireActivity())
                    .collect { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookMode = foldingFeature?.state == FoldingFeature.State.FLAT
                        viewModel.isBookMode = isBookMode
                        viewModel.latestDistinctLevelFlow().collect { level ->
                            if (level.levelInfo.levelId != 0) {
                                binding.levelFragment.removeAllViews()
                                if (level.levelInfo.progress == LevelProgress.INTERMEDIATE) {
                                    displayLevel(level, isBookMode, isLevelFinished = true)
                                } else {
                                    displayLevel(level, isBookMode)
                                }
                                hideHintsBadge()
                                showPlayerCenteredScores()
                                //TODO: Collect reward flow here?
                            }
                        }
                    }
            }
        }
    }

    private fun showPlayerCenteredScores() {
        lifecycleScope.launch {
            if (viewModel.isIntervalAttained(getString(R.string.player_centered_scores))) {
                parentFragmentManager.commit {
                    addToBackStack(ScoresFragment().tag)
                    add(ScoresFragment(), ScoresFragment().tag)
                }
            }
        }
    }

    private fun displayLevel(level: Level, isBookMode: Boolean, isLevelFinished: Boolean = false) {
        val levelGridLayout = levelCreator.getLevel(
            level,
            layoutInflater,
            binding.levelFragment,
            isBookMode,
            viewModel.windowSizeClassWidth
        )
        levelGridLayout.children.filterIsInstance<Cell<*, *>>().apply {
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

    private fun containerStateUpdater() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { uiState ->
                    with(binding)
                    {
                        when (uiState) {
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

    private fun solutionEffectsUpdater() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.solution.collect { solutionData ->
                    when (solutionData.solution) {
                        CORRECT -> {
                            showCorrectEffects(solutionData.score, solutionData.maxScore)
                        }
                        WRONG -> {
                            cellDirector?.sendWrongSolutionLetterHome(solutionData.index)
                            showWrongEffects(WRONG, solutionData.showHintsBadge)
                        }
                        BOGUS_PALINDROME -> {
                            cellDirector?.sendWrongSolutionLetterHome(solutionData.index)
                            showWrongEffects(BOGUS_PALINDROME, solutionData.showHintsBadge)
                        }
                        NONE -> {
                            Log.d(TAG, "Solution is: NONE")
                        }
                    }
                }
            }
        }
    }

    private fun showCorrectEffects(score: Int, maxScore: Int) {
        cellDirector?.getCells()?.let { cells ->
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
                                val scoreView =
                                    scoreMessage.view.findViewById<TextView>(R.id.message)
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
                    soundPlayer.play(soundPlayer.sounds.correctSound)
                }
            }
            cellsAnimation.start()
        }
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

    private fun showWrongEffects(solutionType: LevelViewModel.Solution, showHintBadge: Boolean) {
        animations.getWrongSolutionAnimation(binding.levelFragment).start()
        soundPlayer.vibrateDoubleClick()
        val messageToDisplay =
            if (solutionType == BOGUS_PALINDROME)
                resources.getStringArray(R.array.palindrome_wrong_texts).random()
            else
                resources.getStringArray(R.array.solution_wrong_texts).random()

        val message = MessageSnackBar.make(
            view = binding.levelFragment,
            message = messageToDisplay,
            duration = 2000,
            windowSizeClassWidth = viewModel.windowSizeClassWidth,
            isBookMode = viewModel.isBookMode,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
            animateText = false
        )
        message.show()
        if (showHintBadge) showHintsBadge()
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
        allAnimations.add(tapToContinueAnimation)
        tapToContinueAnimation.start()

        continueMessage.view.setOnClickListener {
            if (isVisible) {
                continueMessage.dismiss()
                soundPlayer.play(soundPlayer.sounds.levelTransition)
                playTrainAnimation()
                viewModel.executeLevelCompleteActions()
            }
        }
        continueMessage.show()
    }

    private fun playTrainAnimation() {
        val childCount =
            activity?.findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)?.childCount
        if (VaryingProbability.isAnimationPlayed(viewModel.currentLevel.value.levelInfo.levelId) && childCount == 0) {
            val trainLinearLayout = animations.getTrainAnimation()
            trainLinearLayout.doOnNextLayout {
                val offscreenX = resources.displayMetrics.widthPixels + trainLinearLayout.width
                val trainAnimator = ObjectAnimator.ofFloat(
                    trainLinearLayout,
                    "x",
                    (0f - it.width),
                    offscreenX.toFloat()
                )
                trainAnimator.duration = 20000
                allAnimations.add(trainAnimator)
                trainAnimator.doOnEnd {
                    activity?.findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)
                        ?.removeAllViews()
                }
                trainAnimator.start()
            }
        }
    }

    private fun showHintsBadge() {
        if (isVisible) {
            val hintBadge = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView)
                .getOrCreateBadge(R.id.navigation_hints)
            hintBadge.backgroundColor =
                ContextCompat.getColor(requireContext(), R.color.hintsNotificationColor)
            hintBadge.badgeGravity = BadgeDrawable.TOP_END
            if (!hintBadge.isVisible) {
                soundPlayer.play(soundPlayer.sounds.hintNotification)
                hintBadge.isVisible = true
            }
        }
    }

    private fun hideHintsBadge() {
        if (isVisible) {
            val hintBadge = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView)
                .getOrCreateBadge(R.id.navigation_hints)
            hintBadge.isVisible = false
        }
    }

    override fun onDestroy() {
        allAnimations.forEach {
            it.cancel()
            it.removeAllListeners()
        }
        super.onDestroy()
    }
}