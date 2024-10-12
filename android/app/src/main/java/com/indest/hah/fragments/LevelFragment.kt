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
                showTutorial()
                generalViewModel.rewardState.collect { rewardType ->
                    if (rewardType == LETTER) {
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
        binding.gameFieldContainer.layoutTransition = animations.getLayoutTransition()
        return binding.root
    }

    private fun showPromoDialog() {
      if(!generalViewModel.isSadModePurchased()) {
          levelViewModel.promoDialogData?.let {
              Log.d(TAG, "Promo dialog info: $it")
              if (it.activated == true && it.levelIds?.contains(levelViewModel.currentRelativeLevelNumber) == true) {
                  val promoDialogFragment = PromoDialogFragment(promoData = it)
                  promoDialogFragment.setDrawable(
                      requireContext()
                  )
                  if (!isRemoving && !isDetached)
                  {
                      promoDialogFragment.show(childFragmentManager, PromoDialogFragment.TAG)
                      levelViewModel.logDialogPromoShown()
                  }
              }
          }
      }
    }


    private fun showPlayerCenteredScores() {
        findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_scores)
    }

    private fun clearMessages() {
        messages.forEach {
            it.destroy()
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
                                val isLast = levelViewModel.isLastLevel()
                                displayLevel(event.level)
                                showContinueMessage(isLast)
                                addNextLevelClickListener(isLastLevel = isLast)
                            } else {
                                displayLevel(event.level)
                                (requireActivity() as MainActivity)?.let {
                                    it.updateProgressBar(
                                        event.progress,
                                        event.level.levelInfo.countryCode
                                    )
                                    it.downloadHintPictureIfNeeded(
                                        event.level.levelInfo.levelId
                                    )
                                }
                                if (levelViewModel.isShowScores()) {
                                    showPlayerCenteredScores()
                                }
                                showPromoDialog()
                            }
                        }

                        is LevelViewModel.GameEvent.SolutionResult -> {
                            when (event.solutionData.solution) {
                                CORRECT -> {
                                    showCorrectEffects(event.solutionData, event.isLastLevel)
                                    addNextLevelClickListener(isLastLevel = event.isLastLevel)
                                    binding.gameFieldContainer.notClickable = true
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


    private fun enableLoading() {
        deactivateBottomNavBar()
        binding.progressCircular.visibility = View.VISIBLE
        binding.gameFieldContainer.notClickable = true
        binding.gameFieldContainer.alpha = .2f
    }


    private fun disableLoading() {
        activateBottomNavBar()
        binding.progressCircular.visibility = View.GONE
        binding.gameFieldContainer.notClickable = false
        binding.gameFieldContainer.alpha = 1f
    }

    private fun displayLevel(level: Level) {
        val levelGridLayout = levelCreator.getLevel(
            level,
            layoutInflater,
            binding.gameFieldContainer
        )
        val list = levelGridLayout.first.children.filterIsInstance<Cell<*, *>>().toMutableList()
        levelGridLayout.first.doOnPreDraw {
            cellDirector = CellDirector(
                list,
                levelViewModel,
                levelGridLayout.second,
                levelGridLayout.third
            )
        }

        binding.gameFieldContainer.removeAllViews()
        binding.gameFieldContainer.addView(levelGridLayout.first)

        askForReview()
        showSubmitPalindromesBadge(level.levelInfo.levelId)
    }

    private fun activateBottomNavBar() {
        (activity as? MainActivity)?.let { mainActivity ->
            mainActivity.binding.bottomNavView.visibility = View.VISIBLE
            mainActivity.binding.bottomNavView.menu.forEach { menuItem ->
                menuItem.isEnabled = true
                when (menuItem.itemId) {
                    R.id.navigation_google_games -> {
                        menuItem.setIcon(R.drawable.games_controller_vector)
                    }

                    R.id.navigation_goods -> {
                        menuItem.setIcon(R.drawable.lightbulb)
                    }

                    R.id.navigation_stages -> {
                        menuItem.setIcon(R.drawable.baseline_format_list_bulleted_24)
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
        isLastLevel: Triple<Boolean, Boolean, String> = Triple(false, false, "")
    ) {
        val currentLevel = levelViewModel.currentLevel.value.levelInfo.levelId
        cellDirector?.getCells()?.let { cells ->
            val cellsAnimation = animations.getCorrectSolutionAnimation(cells)
            cellsAnimation.doOnEnd {
                if (isAdded && currentLevel == levelViewModel.currentLevel.value.levelInfo.levelId) {
                    playMaxScoreAnimation(solutionData.score, solutionData.maxScore)
                    val winningMessage = getWinningMessage(solutionData)
                    val scoreMessage = getScoreMessage()
                    val winningMessageCallBack = object :
                        BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
                        override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                            if (isVisible && currentLevel == levelViewModel.currentLevel.value.levelInfo.levelId) {
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
                            if (isVisible && currentLevel == levelViewModel.currentLevel.value.levelInfo.levelId) {
                                super.onDismissed(transientBottomBar, event)
                                showContinueMessage(isLastLevel)
                            }
                        }
                    }
                    scoreMessage.addCallback(scoreMessageCallback)
                    if (currentLevel == levelViewModel.currentLevel.value.levelInfo.levelId) {
                        winningMessage.show()
                    }
                    soundPlayer.play(soundPlayer.sounds.correctSound, 0.01f)
                }
            }
            if (currentLevel == levelViewModel.currentLevel.value.levelInfo.levelId) {
                cellsAnimation.start()
            }
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
            view = binding.gameFieldContainer,
            message = randomCorrectMessage,
            duration = 2000,
        )
        messages.add(winningMessage)
        return winningMessage
    }

    private fun getScoreMessage(): MessageSnackBar {
        val scoreMessage = MessageSnackBar.make(
            view = binding.gameFieldContainer,
            message = "",
            duration = 2000,
        )
        messages.add(scoreMessage)
        return scoreMessage
    }

    private fun getContinueMessage(isLastLevel: Triple<Boolean, Boolean, String>): MessageSnackBar {
        val continueMessage = MessageSnackBar.make(
            view = binding.root,
            message = if (!isLastLevel.first && !isLastLevel.second) getString(R.string.tap_to_continue) else getString(
                R.string.tap_to_finish
            ),
            duration = Int.MAX_VALUE,
            isViewEnabled = false
        )
        messages.add(continueMessage)
        return continueMessage
    }

    private fun showWrongEffects(solutionData: SolutionData) {
        animations.getWrongSolutionAnimation(binding.gameFieldContainer).start()
        soundPlayer.vibrateDoubleClick()
        val messageToDisplay =
            if (solutionData.solution == BOGUS_PALINDROME)
                resources.getStringArray(R.array.palindrome_wrong_texts).random()
            else
                resources.getStringArray(R.array.solution_wrong_texts).random()

        val message = MessageSnackBar.make(
            view = binding.gameFieldContainer,
            message = messageToDisplay,
            duration = 2000,
            animateText = false,
            isViewEnabled = false
        )
        message.show()
        if (solutionData.showHintsBadge) showHintsBadge()
    }


    private fun showContinueMessage(
        isLastLevel: Triple<Boolean, Boolean, String> = Triple(false, false, "")
    ) {
        val continueMessage = getContinueMessage(isLastLevel)
        continueMessage.show()
    }


    private fun addNextLevelClickListener(isLastLevel: Triple<Boolean, Boolean, String>)
    {
        Log.d(TAG, "addNextLevelClickListener")

        activity?.findViewById<FragmentContainerView>(R.id.fragments_container)?.setOnClickListener {
            clearMessages()
            activity?.findViewById<FragmentContainerView>(R.id.fragments_container)?.setOnClickListener(null)
            doNextLevelAction(isLastLevel = isLastLevel)
        }
    }

    private fun doNextLevelAction(isLastLevel: Triple<Boolean, Boolean, String>)
    {
        soundPlayer.play(soundPlayer.sounds.levelTransition, volume = 0.08f)
        playTrainAnimation()

        when (isLastLevel) {
            Triple(false, false, "") -> {
                levelViewModel.executeLevelCompleteActions(justNext = true)
            }

            Triple(true, false, "") -> {
                levelViewModel.executeLevelCompleteActions(1001)
                soundPlayer.play(soundPlayer.sounds.triumph, volume = 0.1f)
                findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_game_pack_ending)
            }

            Triple(false, true, "") -> {
                levelViewModel.executeLevelCompleteActions(1)
                soundPlayer.play(soundPlayer.sounds.triumph, volume = 0.1f)
                findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_players_pack_ending)
            }
        }
        if (isLastLevel.third != "") {
            levelViewModel.executeLevelCompleteActions(1)
            soundPlayer.play(soundPlayer.sounds.triumph, volume = 0.1f)
            findNavController().safeNavigate(R.id.action_navigation_level_to_navigation_language_pack_ending)
        }
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
        if (levelId == 5) {
            if (levelViewModel.isAllowedToSubmitAndSignedInToFirebase()) {
                if (isVisible) {
                    activity?.let {
                        val stagesBadge = it.findViewById<BottomNavigationView>(R.id.bottomNavView)
                            .getOrCreateBadge(R.id.navigation_stages)
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

    private fun askForReview() {
        if (levelViewModel.currentRelativeLevelNumber == 15) {
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