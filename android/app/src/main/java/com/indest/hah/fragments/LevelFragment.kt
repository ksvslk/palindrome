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
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.play.core.review.ReviewManagerFactory
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.databinding.LevelFragmentBinding
import com.indest.hah.fx.Animations
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.*
import com.indest.hah.game.Solution.*
import com.indest.hah.game.ContainerState.*
import com.indest.hah.game.levelcreation.LevelCreator
import com.indest.hah.other.C
import com.indest.hah.other.VaryingProbability
import com.indest.hah.views.Cell
import com.indest.hah.views.MessageSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
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
        uiStateUpdater()
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

    private fun displayLevel(uiState: UiState) {
        binding.levelFragment.removeAllViews()
        val levelGridLayout = levelCreator.getLevel(
            uiState.level,
            layoutInflater,
            binding.levelFragment,
            uiState.bookMode,
            uiState.windowSizeClass
        )
        levelGridLayout.children.filterIsInstance<Cell<*, *>>().toMutableList().apply {
            levelGridLayout.doOnPreDraw {
                cellDirector = CellDirector(
                    uiState.level,
                    this,
                    viewModel
                )
            }
        }
        binding.levelFragment.addView(levelGridLayout)
    }

    private fun uiStateUpdater() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { uiState ->
                    with(binding)
                    {
                        when (uiState.state) {
                            LOADING -> {
                                progressCircular.visibility = View.VISIBLE
                                levelFragment.notClickable = true
                                levelFragment.alpha = .2f
                                displayLevel(uiState)
                                hideHintsBadge()
                            }
                            READY -> {
                                progressCircular.visibility = View.GONE
                                levelFragment.notClickable = false
                                levelFragment.alpha = 1f
                                when (uiState.solutionData.solution) {
                                    CORRECT -> {
                                        showCorrectEffects(uiState)
                                    }
                                    WRONG -> {
                                        cellDirector?.sendWrongSolutionLetterHome(uiState)
                                        showWrongEffects(uiState)
                                    }
                                    BOGUS_PALINDROME -> {
                                        cellDirector?.sendWrongSolutionLetterHome(uiState)
                                        showWrongEffects(uiState)
                                    }
                                    NONE -> {
                                        Log.d(TAG, "Solution is: NONE")
                                    }
                                }
                            }
                            LEVEL_FINISHED -> {
                                showContinueMessage(uiState)
                                progressCircular.visibility = View.GONE
                                levelFragment.notClickable = true
                                levelFragment.alpha = 1f
                            }
                            BUSY -> {
                                progressCircular.visibility = View.GONE
                                levelFragment.notClickable = true
                                levelFragment.alpha = 1f
                            }
                            PAUSED -> { }
                        }
                    }
                }
            }
        }
    }

    private fun showCorrectEffects(uiState: UiState) {
        cellDirector?.getCells()?.let { cells ->
            val cellsAnimation = animations.getCorrectSolutionAnimation(cells)
            cellsAnimation.doOnEnd {
                if (isAdded) {
                    playMaxScoreAnimation(uiState.solutionData.score, uiState.solutionData.maxScore)
                    val winningMessage = getWinningMessage(uiState)
                    val scoreMessage = getScoreMessage(uiState)
                    val winningMessageCallBack = object :
                        BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
                        override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                            if (isVisible) {
                                super.onDismissed(transientBottomBar, event)
                                scoreMessage.show()
                                val scoreView =
                                    scoreMessage.view.findViewById<TextView>(R.id.message)
                                val scoreAnimation =
                                    animations.getScoreAnimation(scoreView, uiState.solutionData.score, uiState.solutionData.maxScore)
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
                                showContinueMessage(uiState)
                                askForReview(level = viewModel.currentLevel.replayCache.first().levelInfo.levelId)
                            }
                        }
                    }
                    scoreMessage.addCallback(scoreMessageCallback)
                    winningMessage.show()
                    soundPlayer.play(soundPlayer.sounds.correctSound, 0.1f)
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

    private fun getWinningMessage(uiState: UiState): MessageSnackBar {
        val randomCorrectMessage = if (uiState.solutionData.score == uiState.solutionData.maxScore) {
            resources.getStringArray(R.array.very_good_texts).random()
        } else {
            resources.getStringArray(R.array.good_texts).random()
        }

        return MessageSnackBar.make(
            view = binding.levelFragment,
            message = randomCorrectMessage,
            duration = 2000,
            windowSizeClassWidth = uiState.windowSizeClass,
            isBookMode = uiState.bookMode,
            textSize = MessageSnackBar.Companion.TextSize.BIG,
        )
    }

    private fun getScoreMessage(uiState: UiState): MessageSnackBar {
        return MessageSnackBar.make(
            view = binding.levelFragment,
            message = "",
            duration = 2000,
            windowSizeClassWidth = uiState.windowSizeClass,
            isBookMode = uiState.bookMode,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
        )
    }

    private fun showWrongEffects(uiState: UiState) {
        animations.getWrongSolutionAnimation(binding.levelFragment).start()
        soundPlayer.vibrateDoubleClick()
        val messageToDisplay =
            if (uiState.solutionData.solution == BOGUS_PALINDROME)
                resources.getStringArray(R.array.palindrome_wrong_texts).random()
            else
                resources.getStringArray(R.array.solution_wrong_texts).random()

        val message = MessageSnackBar.make(
            view = binding.levelFragment,
            message = messageToDisplay,
            duration = 2000,
            windowSizeClassWidth = uiState.windowSizeClass,
            isBookMode = uiState.bookMode,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
            animateText = false
        )
        message.show()
        if (uiState.solutionData.showHintsBadge) showHintsBadge()
    }

    private fun showContinueMessage(uiState: UiState) {
        val continueMessage = MessageSnackBar.make(
            view = binding.levelFragment,
            message = getString(R.string.tap_to_continue),
            duration = Int.MAX_VALUE,
            windowSizeClassWidth = uiState.windowSizeClass,
            isBookMode = uiState.bookMode,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
        )
        val tapToContinueAnimation = animations.getContinueAnimation(continueMessage)
        allAnimations.add(tapToContinueAnimation)
        tapToContinueAnimation.start()

        continueMessage.view.setOnClickListener {
            if (isVisible) {
                continueMessage.dismiss()
                soundPlayer.play(soundPlayer.sounds.levelTransition, volume = 0.2f)
                playTrainAnimation()
                viewModel.executeLevelCompleteActions()
            }
        }
        continueMessage.show()
    }

    private fun playTrainAnimation() {
        val childCount =
            activity?.findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)?.childCount
        if (VaryingProbability.isAnimationPlayed(viewModel.currentLevel.replayCache.first().levelInfo.levelId) && childCount == 0) {
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

    private fun askForReview(level: Int) {
        if (level == 10) {
            val reviewManager = ReviewManagerFactory.create(requireContext())
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        Log.d(TAG, "Review flow completed.")
                    }
                } else {
                    Log.e(TAG, "Error requesting review flow: ${task.exception?.message}")
                }
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

    private fun rewardUserWithLetter(rewardType: RewardType = RewardType.NONE) {
        when (rewardType) {
            RewardType.LETTER -> {
                Log.d(TAG, "LETTER reward.")
                cellDirector?.rewardWithRandomLetter()
            }
            RewardType.NEXT_LEVEL -> {
                Log.d(TAG, "NEXT_LEVEL reward.")
            }
            RewardType.CLUE -> {}
            RewardType.NONE -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(C.HINTS_REQUEST_KEY) { _, bundle ->
            if (viewModel.uiState.value.state != LEVEL_FINISHED) {
                val premiumLetter = bundle.getBoolean(C.LETTER_PREMIUM_BUNDLE)
                if (!premiumLetter) {
                    val rewardType = RewardType.values()[bundle.getInt(C.LETTER_REWARD_TYPE_BUNDLE)]
                    rewardUserWithLetter(rewardType)
                } else {
                    rewardUserWithLetter(RewardType.LETTER)
                }
            }
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