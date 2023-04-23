package com.indest.hah.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.play.core.review.ReviewManagerFactory
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.ads.RewardType.LETTER
import com.indest.hah.databinding.LevelFragmentBinding
import com.indest.hah.fx.Animations
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.creation.LevelCreator
import com.indest.hah.game.director.CellDirector
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.game.viewmodel.Solution
import com.indest.hah.game.viewmodel.Solution.*
import com.indest.hah.game.viewmodel.SolutionData
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.VaryingProbability
import com.indest.hah.other.safeNavigate
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.views.Cell
import com.indest.hah.views.MessageSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LevelFragment : Fragment(R.layout.level_fragment) {
    private val TAG = "LevelFragment"

    private val levelViewModel: LevelViewModel by activityViewModels()
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private var _binding: LevelFragmentBinding? = null
    private val binding get() = _binding!!

    var cellDirector: CellDirector? = null
    private var allAnimations = mutableSetOf<ValueAnimator>()
    private var messages = mutableListOf<MessageSnackBar>()

    @Inject
    lateinit var levelCreator: LevelCreator

    @Inject
    lateinit var animations: Animations

    @Inject
    lateinit var soundPlayer: SoundPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                if (levelViewModel.isShowScores()) {
                    showPlayerCenteredScores()
                }
                showTutorial()

                generalViewModel.rewardState.collect { rewardType ->
                    if (rewardType == LETTER)
                    {
                        generalViewModel.updateRewardState(RewardType.NONE)
                        cellDirector?.rewardWithRandomLetter()
                    }
                }

            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.doOnLayout {
            collectGameUpdates()
        }
    }


    override fun onDestroy() {
        allAnimations.forEach {
            it.cancel()
            it.removeAllListeners()
        }
        clearMessages()
        super.onDestroy()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LevelFragmentBinding.inflate(layoutInflater, container, false)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        binding.levelFragment.layoutTransition = animations.getLayoutTransitionFade()
        return binding.root
    }


    private fun showPlayerCenteredScores() {
        findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_scores)
    }

    override fun onResume() {
        super.onResume()
    }

    private fun clearMessages()
    {
        messages.forEach {
            it.removeMessageCallback()
            it.dismiss()
        }
        messages.clear()
    }


    private fun collectGameUpdates() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED)
            {
                levelViewModel.gameEventFlow.collect { event ->
                    when (event) {
                        is LevelViewModel.GameEvent.DisplayLevel -> {
                            disableLoading()
                            clearMessages()
                            if (event.level.levelInfo.progress == LevelProgress.INTERMEDIATE) {
                                displayLevel(event.level)
                                showContinueMessage(levelViewModel.isLastLevel())
                            }
                            else {
                                displayLevel(event.level)
                                (requireActivity() as MainActivity).updateProgressBar(event.progress)
                            }
                        }
                        is LevelViewModel.GameEvent.SolutionResult -> {
                            when (event.solutionData.solution) {
                                CORRECT -> {
                                    showCorrectEffects(event.solutionData, event.isLastLevel)
                                    binding.levelFragment.notClickable = true
                                    hideHintsBadge()
                                }
                                WRONG, BOGUS_PALINDROME -> {
                                    cellDirector?.sendWrongSolutionLetterHome(event.solutionData)
                                    showWrongEffects(event.solutionData)
                                }
                                NONE -> {
                                }
                            }
                        }
                        LevelViewModel.GameEvent.ShowLoading -> {
                            enableLoading()
                        }
                    }
                }
            }
        }
    }


    private fun enableLoading()
    {
        deactivateBottomNavBar()
        binding.progressCircular.visibility = View.VISIBLE
        binding.levelFragment.notClickable = true
        binding.levelFragment.alpha = .2f
    }


    private fun disableLoading()
    {
        activateBottomNavBar()
        binding.progressCircular.visibility = View.GONE
        binding.levelFragment.notClickable = false
        binding.levelFragment.alpha = 1f
    }


    private fun displayLevel(level: Level) {
        binding.levelFragment.removeAllViews()
        binding.levelFragment.removeAllViewsInLayout()
        val densitySettings = levelCreator.getDensitySettings(binding.levelFragment, level)
        val areColumnsDifferent =
            level.levelInfo.columnCount != densitySettings.columnCount || level.levelInfo.author == "NewVersion"
        lifecycleScope.launch(Dispatchers.Main)
        {
            if (areColumnsDifferent) {
                val newIndexes =
                    levelCreator.getUpdatedLevelWithPossiblyNewColumnCount(
                        level,
                        densitySettings.columnCount
                    )
                levelViewModel.changeFriendIndexesBasedOnColumnCount(
                    densitySettings.columnCount,
                    newIndexes
                )
                return@launch
            }
            val levelGridLayout = levelCreator.getLevel(
                level,
                layoutInflater,
                binding.levelFragment,
                densitySettings
            )
            levelGridLayout.first.children.filterIsInstance<Cell<*, *>>().toMutableList().apply {
                levelGridLayout.first.doOnPreDraw {
                    cellDirector = CellDirector(
                        this,
                        levelViewModel
                    )
                }
            }
            binding.levelFragment.addView(levelGridLayout.first)
        }
        if (areColumnsDifferent) {
            return
        }

        askForReview(level.levelInfo.levelId)
        showSubmitPalindromesBadge(level.levelInfo.levelId)

    }




    private fun activateBottomNavBar() {
        (activity as? MainActivity)?.let { mainActivity ->
            mainActivity.binding.bottomNavView.visibility = View.VISIBLE
            mainActivity.binding.bottomNavView.menu.forEach { menuItem ->
                menuItem.isEnabled = true
                when (menuItem.itemId) {
                    R.id.navigation_google_games -> {
                        menuItem.setIcon(R.drawable.games_controller_grey)
                    }
                    R.id.navigation_goods -> {
                        menuItem.setIcon(R.drawable.lightbulb)
                    }
                    R.id.navigation_stages -> {
                        menuItem.setIcon(R.drawable.baseline_list_alt_black_36)
                    }
                    R.id.navigation_settings -> {
                        menuItem.setIcon(R.drawable.settings)
                    }
                }
            }
        }
    }

    private fun deactivateBottomNavBar() {
        (activity as? MainActivity)?.let { mainActivity ->
            mainActivity.binding.bottomNavView.menu.forEach { menuItem ->
                menuItem.isEnabled = false
                menuItem.setIcon(R.drawable.ic_baseline_disabled_by_default_24)
            }
        }
    }


    private fun showCorrectEffects(
        solutionData: SolutionData,
        isLastLevel: Pair<Boolean, Boolean> = Pair(false, false)
    ) {

        cellDirector?.getCells()?.let { cells ->
            val cellsAnimation = animations.getCorrectSolutionAnimation(cells)

            cellsAnimation.doOnEnd {
                if (isAdded) {

                    playMaxScoreAnimation(solutionData.score, solutionData.maxScore)
                    val winningMessage = getWinningMessage(solutionData)
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
                                    animations.getScoreAnimation(
                                        scoreView,
                                        solutionData.score,
                                        solutionData.maxScore
                                    )
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
                                showContinueMessage(isLastLevel)
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
            (activity as? MainActivity)?.let {
                val rain = animations.getRainConfetti()
                it.binding.konfettiView.start(rain)
            }
        }
    }

    private fun getWinningMessage(solutionData: SolutionData): MessageSnackBar {
        val randomCorrectMessage =
            if (solutionData.score == solutionData.maxScore) {
                resources.getStringArray(R.array.very_good_texts).random()
            } else {
                resources.getStringArray(R.array.good_texts).random()
            }

        val winningMessage = MessageSnackBar.make(
            view = binding.levelFragment,
            message = randomCorrectMessage,
            duration = 2000,
            textSize = MessageSnackBar.Companion.TextSize.BIG,
        )
        messages.add(winningMessage)
        return winningMessage
    }

    private fun getScoreMessage(): MessageSnackBar {
        val scoreMessage = MessageSnackBar.make(
            view = binding.levelFragment,
            message = "",
            duration = 2000,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
        )
        messages.add(scoreMessage)
        return scoreMessage
    }

    private fun getContinueMessage(isLastLevel: Pair<Boolean, Boolean>):MessageSnackBar {
        val continueMessage = MessageSnackBar.make(
            view = binding.levelFragment,
            message = if (!isLastLevel.first && !isLastLevel.second) getString(R.string.tap_to_continue) else getString(
                R.string.tap_to_finish
            ),
            duration = Int.MAX_VALUE,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
        )
        messages.add(continueMessage)
        return continueMessage
    }

    private fun showWrongEffects(solutionData: SolutionData) {
        animations.getWrongSolutionAnimation(binding.levelFragment).start()
        soundPlayer.vibrateDoubleClick()
        val messageToDisplay =
            if (solutionData.solution == BOGUS_PALINDROME)
                resources.getStringArray(R.array.palindrome_wrong_texts).random()
            else
                resources.getStringArray(R.array.solution_wrong_texts).random()

        val message = MessageSnackBar.make(
            view = binding.levelFragment,
            message = messageToDisplay,
            duration = 2000,
            textSize = MessageSnackBar.Companion.TextSize.SMALL,
            animateText = false,
            isViewEnabled = false
        )
        message.show()
        if (solutionData.showHintsBadge) showHintsBadge()
    }


    private fun showContinueMessage(
        isLastLevel: Pair<Boolean, Boolean> = Pair(false, false)
    ) {
        val continueMessage = getContinueMessage(isLastLevel)
        continueMessage.view.isClickable = true
        continueMessage.view.setOnClickListener {
            if (isVisible) {
                activateBottomNavBar()
                it.isClickable = false
                continueMessage.dismiss()
                soundPlayer.play(soundPlayer.sounds.levelTransition, volume = 0.2f)
                playTrainAnimation()
                when (isLastLevel) {
                    Pair(false, false) -> {
                        levelViewModel.executeLevelCompleteActions()
                    }
                    Pair(true, false), Pair(false, true) -> {
                        levelViewModel.executeLevelCompleteActionsFinal()
                        soundPlayer.play(soundPlayer.sounds.triumph, volume = 0.2f)
                        findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_final_screen)
                    }
                }
            }
        }
        continueMessage.show()
    }


    private fun playTrainAnimation() {
        val childCount =
            activity?.findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)?.childCount
        if (levelViewModel.currentLevel.replayCache.firstOrNull()?.levelInfo?.let {
                VaryingProbability.isAnimationPlayed(
                    it.levelId
                )
            } == true && childCount == 0) {
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


    private fun showTutorial() {
        context?.getSharedPreferences(
            getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let { sp ->
            val tutorialPassed = sp.getBoolean(getString(R.string.tutorial_passed), false)
            if (!tutorialPassed) {
                levelViewModel.logTutorialBegin()
                findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_tutorial)
                with(sp.edit()) {
                    putBoolean(getString(R.string.tutorial_passed), true)
                    apply()
                }
            }
        }
    }


    private fun showHintsBadge() {
        if (isVisible) {
            activity?.let {
                val hintBadge = it.findViewById<BottomNavigationView>(R.id.bottomNavView)
                    .getOrCreateBadge(R.id.navigation_goods)
                hintBadge.backgroundColor =
                    ContextCompat.getColor(it.applicationContext, R.color.hintsNotificationColor)
                hintBadge.badgeGravity = BadgeDrawable.TOP_END
                val text = getString(R.string.hints_are_available)
                val duration = Toast.LENGTH_LONG
                Toast.makeText(it.applicationContext, text, duration).show()
                soundPlayer.play(soundPlayer.sounds.hintNotification)
                if (!hintBadge.isVisible) {
                    hintBadge.isVisible = true
                }
            }
        }
    }


    private fun showSubmitPalindromesBadge(levelId: Int) {
        if (levelId == 5)
        {
            if (levelViewModel.isAllowedToSubmitAndSignedInToFirebase())
            {
                if (isVisible) {
                    activity?.let {
                        val stagesBadge = it.findViewById<BottomNavigationView>(R.id.bottomNavView)
                            .getOrCreateBadge(R.id.navigation_stages)
                        stagesBadge.backgroundColor =
                            ContextCompat.getColor(it.applicationContext, R.color.hintsNotificationColor)
                        stagesBadge.badgeGravity = BadgeDrawable.TOP_END
                        val text = getString(R.string.send_a_cool_palindrome)
                        val duration = Toast.LENGTH_LONG
                        Toast.makeText(it.applicationContext, text, duration).show()
                        soundPlayer.play(soundPlayer.sounds.hintNotification)
                        if (!stagesBadge.isVisible) {
                            stagesBadge.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun askForReview(level: Int) {
        if (level == 8) {
            activity?.let {
                val reviewManager = ReviewManagerFactory.create(it.applicationContext)
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = reviewManager.launchReviewFlow(it, reviewInfo)
                        flow.addOnCompleteListener { _ ->
                        }
                    } else {
                        Log.e(TAG, "Error requesting review flow: ${task.exception?.message}")
                    }
                }
            }
        }
    }


    private fun hideHintsBadge() {
        if (isVisible) {
            activity?.let {
                val hintBadge = it.findViewById<BottomNavigationView>(R.id.bottomNavView)
                    .getOrCreateBadge(R.id.navigation_goods)
                hintBadge.isVisible = false
            }
        }
    }

}