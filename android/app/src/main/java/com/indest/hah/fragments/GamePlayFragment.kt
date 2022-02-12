package com.indest.hah.fragments

import android.animation.*
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.*
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.Games
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.play.core.review.ReviewManagerFactory
import com.indest.hah.GameActivity
import com.indest.hah.LevelFactory
import com.indest.hah.R
import com.indest.hah.components.Animations
import com.indest.hah.components.SoundPlayer
import com.indest.hah.databinding.GameplayFragmentBinding
import com.indest.hah.game.*
import com.indest.hah.game.enums.GameState
import com.indest.hah.game.enums.InternetState
import com.indest.hah.game.enums.LoadingState
import com.indest.hah.game.enums.RewardType
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.letter.Slot
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.util.*
import com.indest.hah.util.C.HINT_FRAGMENT_TAG
import com.indest.hah.util.C.HINT_REQUEST_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.ln
import kotlin.properties.Delegates

@AndroidEntryPoint
class GamePlayFragment : Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    private var _binding: GameplayFragmentBinding? = null
    private val binding get() = _binding!!
    private var hintBadge: BadgeDrawable? = null
    private var textBigAndSmall: Pair<Float, Float>? = null
    private var limitReached = false
    lateinit var cellDirector: CellDirector
    var gameState: GameState = GameState.BUSY
    val gameViewModel: GameViewModel by activityViewModels()
    private var allAnimations: MutableSet<ValueAnimator> = mutableSetOf()
    private var snackBars: HashMap<MessageSnackBar, BaseTransientBottomBar.BaseCallback<MessageSnackBar>> =
        hashMapOf()
    private var tapToContinueMessage: MessageSnackBar? = null
    private var isBookMode = false
    @Inject
    lateinit var soundPlayer: SoundPlayer
    @Inject
    lateinit var animations: Animations

    private fun increaseHintGuesses() {
        lifecycleScope.launchWhenCreated {
            val hintGuessesReached =
                gameViewModel.increaseIntervalCount(getString(R.string.hint_suggestion))

            if (hintGuessesReached) {
                soundPlayer.play(soundPlayer.sounds.hintNotification)
                hintBadge?.isVisible = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*     audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
             soundPool = SoundPool.Builder().build()
             scoreSound = soundPool.load(requireContext(), R.raw.score_tick, 1)
             correctSound = soundPool.load(requireContext(), R.raw.correct, 1)
             hintNotification = soundPool.load(requireContext(), R.raw.hint_notification, 1)
             levelTransition = soundPool.load(requireContext(), R.raw.level_transition, 1)
             noteSounds = arrayListOf(
                 soundPool.load(requireContext(), R.raw.c3_outer, 1),
                 soundPool.load(requireContext(), R.raw.c4_outer, 1),
                 soundPool.load(requireContext(), R.raw.g3_outer, 1),
                 soundPool.load(requireContext(), R.raw.c4_outer, 1),
                 soundPool.load(requireContext(), R.raw.c5_outer, 1),
                 soundPool.load(requireContext(), R.raw.g4_outer, 1),
                 soundPool.load(requireContext(), R.raw.c5_outer, 1),
                 soundPool.load(requireContext(), R.raw.g5_outer, 1),
                 soundPool.load(requireContext(), R.raw.c5_outer, 1),
                 soundPool.load(requireContext(), R.raw.c3_outer, 1),
                 soundPool.load(requireContext(), R.raw.c5_outer, 1),
                 soundPool.load(requireContext(), R.raw.c6_outer, 1),
             )*/
        resetHintsBadgeOnFragmentResult()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                WindowInfoTracker.getOrCreate(requireContext())
                    .windowLayoutInfo(requireActivity())
                    .collectLatest { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookModeEnabled = isBookMode(foldingFeature)
                        textBigAndSmall = setTextSizes(isBookModeEnabled)
                        isBookMode = isBookModeEnabled
                    }
                observeViewModel()
            }
        }


    }

    // CLEAN
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = GameplayFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        createHintsBadge()

        val scaleDown: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)
        )
        scaleDown.duration = 300
        scaleDown.interpolator = DecelerateInterpolator()

        val scaleUp: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)
        )
        scaleUp.duration = 300
        scaleUp.startDelay = 300
        scaleUp.interpolator = OvershootInterpolator()

        val itemLayoutTransition = LayoutTransition()
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp)
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, scaleDown)

        binding.gameplayFragment.layoutTransition = itemLayoutTransition

        return b.root
    }

    private fun resetHintsBadgeOnFragmentResult() {
        setFragmentResultListener(HINT_REQUEST_KEY) { _, bundle ->
            run {
                if (bundle[HINT_FRAGMENT_TAG] != null) {
                    resetHintsBadge()
                }
            }
        }
    }

    private fun rewardUser(hintType: RewardType) {
        if (hintType == RewardType.NEXT_LEVEL) {
            continueToNextLevelActions()
        } else if (gameState == GameState.READY) {
            when (hintType) {
                RewardType.HINT -> {
                    gameViewModel.setCategoryRewarded(true)
                }
                RewardType.LETTER -> {
                    gameState = GameState.REWARDING
                    val candidateLetters = cellDirector.cells
                        .filterIsInstance<Letter>()
                        .filter { !it.hasFriend() }

                    if (candidateLetters.isNotEmpty()) {
                        val randomLetter = candidateLetters.random()
                        val correctSlot = cellDirector.cells.filterIsInstance<Slot>().first {
                            (it.realChar == randomLetter.text.first() && !it.hasFriend()) ||
                                    (it.realChar == randomLetter.text.first() &&
                                            it.hasFriend() &&
                                            it.getTheFriend()?.text?.first() != it.realChar)
                        }
                        val incorrectSlot =
                            cellDirector.cells.filterIsInstance<Slot>().firstOrNull {
                                it.hasFriend() &&
                                        it.getTheFriend()?.text?.first() != it.realChar
                            }
                        randomLetter.isLocked = true
                        if (correctSlot.hasFriend()) {
                            correctSlot.friend?.let {
                                cellDirector.switchLetterPositions(
                                    randomLetter,
                                    it,
                                    isRewarded = true
                                )
                            }
                        } else {
                            if (candidateLetters.size == 1 && incorrectSlot == null) { // Last letter to be placed in the slot
                                cellDirector.placeLetterInEmptyBox(
                                    randomLetter, correctSlot,
                                    isRewarded = true,
                                    isLastRewardedLetter = true
                                )
                            } else if (candidateLetters.size == 1 && incorrectSlot != null) {
                                incorrectSlot.getTheFriend()
                                    ?.let {
                                        cellDirector.sendLetterHome(it, isRewarded = true)
                                        cellDirector.placeLetterInEmptyBox(
                                            randomLetter, correctSlot,
                                            isRewarded = true
                                        )
                                    }
                            } else {
                                cellDirector.placeLetterInEmptyBox(randomLetter, correctSlot, true)
                            }
                        }
                    } else {
                        Log.e(
                            TAG,
                            "Something went terribly wrong while trying to reward with letter. No letters available :("
                        )
                    }

                }
                else -> {
                    Log.d(TAG, "No reward")
                }
            }
        }
    }

    private fun loadingFragmentSwitcher() {
        lifecycleScope.launchWhenResumed {
            gameViewModel.loadingState.collect {
                if (it.state == LoadingState.LOADING) {
                    parentFragmentManager.commit {
                        setCustomAnimations(
                            R.anim.fade_in_short,
                            R.anim.fade_out_short
                        )
                        setReorderingAllowed(true)
                        replace<LoadingFragment>(R.id.fragments_container)
                    }
                }
            }
        }
    }


    // CLEAN
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.requestApplyInsets(view)
        hideSystemBars(binding.gameplayFragment)
    }

    // CLEAN
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        resetHintsBadge()
        resetLevel()
        loadLevel(gameViewModel.currentLevel)
    }

    // CLEAN
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        findNavController(this).removeOnDestinationChangedListener { _, _, _ -> }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    gameViewModel.userReward.collect { userRewardEvent ->
                        when (userRewardEvent) {
                            is UserRewardEvent.Success -> {
                                if (userRewardEvent.rewardEarned) {
                                    Log.d(TAG, "UserRewardEvent.Success: reward earned")
                                    gameViewModel.resetRewardingUser()
                                    rewardUser(userRewardEvent.rewardType)
                                }
                            }
                            is UserRewardEvent.Error -> {
                                Log.d(TAG, "Error getting userRewardEvent from viewModel")
                            }
                        }
                    }
                }
                launch {
                    gameViewModel.levelChange.collect() { levelChangeEvent ->
                        when (levelChangeEvent) {
                            is LevelChangeEvent.Success -> {
                                levelChangeEvent.level?.let {
                                    Log.d(
                                        TAG,
                                        "LevelChangeEvent.Success: Should load next level now: ${it.levelInfo}"
                                    )
                                    resetLevel()
                                    loadLevel(it)
                                }
                            }
                            is LevelChangeEvent.Error -> {
                                Log.d(TAG, "Error getting userRewardEvent from viewModel")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resetLevel() {
        gameState = GameState.BUSY
        gameViewModel.realSolutionAndGuess.value = Triple("", "2", null)
        binding.gameplayFragment.removeAllViews()
        binding.gameplayFragment.removeAllViewsInLayout()
    }

    private fun isBookMode(foldFeature: FoldingFeature?) =
        foldFeature?.state == FoldingFeature.State.FLAT

    private fun playSpecialAnimation() {
        Log.d(TAG, "Play special animation")
        val trainLinearLayout =
            requireActivity().findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)
        val oldTrainDisappearingAnimation = ObjectAnimator.ofFloat(
            trainLinearLayout,
            "alpha",
            1f,
            0f,
        )
        allAnimations.add(oldTrainDisappearingAnimation)
        oldTrainDisappearingAnimation?.duration = 500
        oldTrainDisappearingAnimation?.start()
        oldTrainDisappearingAnimation?.doOnEnd {
            trainLinearLayout.alpha = 1f
            oldTrainDisappearingAnimation.removeAllListeners()
            trainLinearLayout.removeAllViews()

            val lastCarriage = arrayOf(
                R.drawable.ic_carriage_last,
                R.drawable.ic_carriage_last_blue,
                R.drawable.ic_carriage_last_green,
                R.drawable.ic_carriage_last_yellow
            ).random()

            val centerCarriages = arrayOf(
                R.drawable.ic_carriage_center,
                R.drawable.ic_carriage_center_blue,
                R.drawable.ic_carriage_center_green,
                R.drawable.ic_carriage_center_yellow
            )
            val randomNumberOfTrains = (1..5).random()

            for (i in 1..randomNumberOfTrains) {
                if (i == 1) {
                    layoutInflater.inflate(R.layout.carriage_last, trainLinearLayout, false).also {
                        it.setBackgroundResource(lastCarriage)
                        trainLinearLayout.addView(it)
                    }
                } else {
                    layoutInflater.inflate(R.layout.carriage_center, trainLinearLayout, false)
                        .also {
                            val randomCarriage = centerCarriages.random()
                            it.setBackgroundResource(randomCarriage)
                            trainLinearLayout.addView(it)
                        }
                }
            }

            layoutInflater.inflate(R.layout.locomotive_view, trainLinearLayout, false).also {
                (it.background as AnimationDrawable).start()
                trainLinearLayout.addView(it)
            }

            trainLinearLayout.doOnNextLayout {
                val offscreenX = resources.displayMetrics.widthPixels + trainLinearLayout.width
                val trainAnimator = ObjectAnimator.ofFloat(
                    trainLinearLayout,
                    "x",
                    (0f - it.width),
                    offscreenX.toFloat()
                )
                allAnimations.add(trainAnimator)
                trainAnimator.duration = 20000
                trainAnimator.start()
            }
        }
    }

    private fun showReviewOption(level: Int) {
        if (level == 21) {
            requireActivity().getSharedPreferences(
                getString(R.string.app_preferences),
                Context.MODE_PRIVATE
            )?.let {
                if (!(it.getBoolean(
                        requireContext().getString(R.string.review_displayed_to_user),
                        false
                    ))
                ) {
                    // val reviewManager = FakeReviewManager(requireContext())
                    val reviewManager = ReviewManagerFactory.create(requireContext())
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow =
                                reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                            flow.addOnCompleteListener {
                                Log.d(
                                    TAG,
                                    "Review completed (whatever the action)."
                                )
                            }
                        } else {
                            Log.d(
                                TAG,
                                "Review flow did not complete successfully: ${task.exception?.message}"
                            )
                        }
                    }
                    with(it.edit()) {
                        putBoolean(getString(R.string.review_displayed_to_user), true)
                        apply()
                    }
                }
            }
        }
    }

    // CLEAN
    private fun resetHintsBadge() {
        hintBadge?.isVisible = false
    }

    // CLEAN
    private fun createHintsBadge() {
        hintBadge = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView)
            .getOrCreateBadge(R.id.navigation_hints)
        hintBadge?.backgroundColor =
            ContextCompat.getColor(requireContext(), R.color.hintsNotificationColor)
        hintBadge?.badgeGravity = BadgeDrawable.TOP_END
    }

    // CLEAN
    private fun loadLevel(levelAndLettersInfo: Level) {
        Log.d(TAG, "loadLevel")
        cellDirector = CellDirector(this)
        val level = LevelFactory.getLevel(
            levelAndLettersInfo,
            requireContext(),
            layoutInflater,
            binding.gameplayFragment,
            isBookMode,
            gameViewModel.windowSizeClassWidth
        )

        cellDirector.cells = level.children
            .filterIsInstance<Cell<*, *>>()
            .toMutableList()

        level.doOnPreDraw {
            cellDirector.restoreSlotPlacements(levelAndLettersInfo)
            gameViewModel.persistInitialLettersInfo(cellDirector.cells)
        }

        binding.gameplayFragment.addView(level)

        if (levelAndLettersInfo.levelInfo.progress == LevelProgress.INTERMEDIATE) {
            gameState = GameState.LEVEL_COMPLETED
            tapToContinueMessage = MessageSnackBar.make(
                binding.gameplayFragment,
                getString(R.string.tap_to_continue),
                Int.MAX_VALUE,
                textBigAndSmall?.second
                    ?: resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
            )

            val tapToContinueAnimation =
                ObjectAnimator.ofFloat(tapToContinueMessage?.view, View.ALPHA, 1f, 0.5f)
            allAnimations.add(tapToContinueAnimation)
            tapToContinueAnimation.duration = 1000
            tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
            tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
            tapToContinueAnimation.start()

            val messageViewListener = View.OnClickListener {
                soundPlayer.play(soundPlayer.sounds.levelTransition)
                showAdContinue()
            }

            tapToContinueMessage?.view?.setOnClickListener(messageViewListener)
            tapToContinueMessage?.show()

        } else {
            gameState = GameState.READY
            handleGameSolutions()
            if (VaryingProbability.isAnimationPlayed(levelAndLettersInfo.levelInfo.levelId)) playSpecialAnimation()
        }
        setLevelProgressIndicator(levelAndLettersInfo.levelInfo)
    }

    private fun setLevelProgressIndicator(level: LevelData) {
        val levelNumber = level.levelId
        val levelsCount = gameViewModel.levels.value?.size ?: 100

        val levelNrView = (requireActivity() as GameActivity).binding.levelNumber
        val progressIndicatorWidth =
            (requireActivity() as GameActivity).binding.gameProgress.measuredWidth

        val progressX =
            (levelNumber * progressIndicatorWidth / levelsCount) - ln(levelNumber.toFloat()) * 15
        levelNrView.text = levelNumber.toString()
        levelNrView.x = progressX
        (requireActivity() as GameActivity).binding.gameProgress.progress =
            (levelNumber.toFloat() / levelsCount.toFloat() * 100.0).toInt()
        showReviewOption(level.levelId)
    }

    // CLEAN
    private fun replaceFragmentWithSelf() {
        gameViewModel.realSolutionAndGuess.postValue(Triple("", "2", null))
        parentFragmentManager.commit {
            setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_top
            )
            setReorderingAllowed(true)
            replace<GamePlayFragment>(R.id.fragments_container, tag = C.GAMEPLAY_FRAGMENT_TAG)
        }
    }

    // CLEAN
    private fun handleGameSolutions() {
        gameViewModel.realSolutionAndGuess
            .observe(viewLifecycleOwner) {
                if (gameState != GameState.LEVEL_COMPLETED) {
                    if (it != null && it.first?.length == it.second?.length) {
                        gameState = GameState.BUSY
                        val playerSolution = it.second?.map { char ->
                            if ("^([?!.,’])".contains(char)) "" else char
                        }?.joinToString("")

                        when {
                            it.first == it.second -> {
                                gameState = GameState.LEVEL_COMPLETED
                                Log.d(TAG, "${it.first} and ${it.second}")
                                val currentAndMaxScore = gameViewModel.getCurrentAndMaxScore()
                                playCorrectSolutionAnimation(currentAndMaxScore)
                                correctSolutionActions(
                                    it.first?.count(),
                                    currentAndMaxScore.first == currentAndMaxScore.second
                                )
                            }
                            playerSolution?.reversed() == playerSolution -> {
                                increaseHintGuesses()
                                playWrongSolutionAnimation()
                                wrongPalindromeActions(it)
                            }
                            else -> {
                                increaseHintGuesses()
                                playWrongSolutionAnimation()
                                wrongSolutionActions(it)
                            }
                        }
                    } else if (gameState != GameState.REWARDING) {
                        gameState = GameState.READY
                    }
                }
            }
    }

    // MIXED, SCORE CAN BE QUERIED IMMEDIATELY - NO OBSERVING NEEDED
    private fun correctSolutionActions(puzzleLength: Int?, didNoMistakes: Boolean) {
        gameViewModel.realSolutionAndGuess.removeObservers { viewLifecycleOwner.lifecycle }
        lifecycleScope.launch {
            val totalScore = gameViewModel.getTotalScore()
            val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
            if (account != null) {
                Games.getLeaderboardsClient(requireActivity(), account)
                    .submitScore(
                        getString(R.string.leaderboard_crme_de_la_crme),
                        totalScore.toLong()
                    )
                Log.d(TAG, "New score: $totalScore submitted to leaderboards.")
            }
        }

        gameViewModel.updateCompletedLevel()

        activity?.getSharedPreferences(getString(R.string.app_preferences), Context.MODE_PRIVATE)
            ?.let { sharedPref ->
                with(sharedPref.edit()) {
                    putBoolean(getString(R.string.level_solution_guesses), false)
                    apply()
                }
            }
        hintBadge?.isVisible = false

        GoogleSignIn.getLastSignedInAccount(requireActivity())?.let { googleSignInAccount ->

            if (puzzleLength?.compareTo(4) == 1 && didNoMistakes) {
                Games.getAchievementsClient(requireActivity(), googleSignInAccount)
                    .unlock(getString(R.string.achievement_zero_mistakes))
            }
            unlockAchievementForLevel(gameViewModel.getLevelNumber(), googleSignInAccount)
        }
    }

    // CLEAN
    private fun wrongPalindromeActions(info: Triple<String?, String?, Int?>) {
        val randomWrongPalindromeMessage =
            resources.getStringArray(R.array.palindrome_wrong_texts).random()
        val lastLetter =
            cellDirector.cells.find { p -> p.homeIndex == info.third } as Letter
        cellDirector.sendLetterHome(lastLetter)
        val message = MessageSnackBar.make(
            binding.gameplayFragment,
            randomWrongPalindromeMessage,
            2000,
            textBigAndSmall?.second ?: resources.getDimensionPixelSize(R.dimen.other_message_size)
                .toFloat()
        )
        message.show()
        GoogleSignIn.getLastSignedInAccount(requireActivity())?.let {
            Games.getAchievementsClient(requireActivity(), it)
                .unlock(getString(R.string.achievement_wrong_palindrome))
        }
    }

    // CLEAN
    private fun wrongSolutionActions(info: Triple<String?, String?, Int?>) {
        val randomWrongMessage = resources.getStringArray(R.array.solution_wrong_texts).random()
        val lastLetter =
            cellDirector.cells.find { p -> p.homeIndex == info.third } as Letter
        cellDirector.sendLetterHome(lastLetter)
        val message = MessageSnackBar.make(
            binding.gameplayFragment,
            randomWrongMessage,
            2000,
            textBigAndSmall?.second ?: resources.getDimensionPixelSize(R.dimen.other_message_size)
                .toFloat()
        )
        message.show()
    }

    private fun playCorrectSolutionAnimation(currentAndMaxScore: Pair<Int, Int>) {
        val correctAnimation = animations.getCorrectSolutionAnimation(cellDirector.cells)
        correctAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                showWinningMessages(currentAndMaxScore)
            }
        })
        correctAnimation.start()
    }

    // MIXED
    private fun showWinningMessages(currentAndMaxScore: Pair<Int, Int>) {
        lifecycleScope.launchWhenCreated {
            Log.d(
                TAG,
                "showWinningMessages for Level ${gameViewModel.currentLevel.levelInfo.levelId}"
            )
            val randomCorrectMessage = if (currentAndMaxScore.first == currentAndMaxScore.second) {
                resources.getStringArray(R.array.very_good_texts).random()
            } else {
                resources.getStringArray(R.array.good_texts).random()
            }

            if (currentAndMaxScore.first == currentAndMaxScore.second) {
                val konfettiView = (requireActivity() as GameActivity).binding.konfettiView
                konfettiView.start(rain())
            }

            val winningMessage = MessageSnackBar.make(
                binding.gameplayFragment,
                randomCorrectMessage,
                2000,
                textBigAndSmall?.first
                    ?: resources.getDimensionPixelSize(R.dimen.winning_message_size)
                        .toFloat()
            )
            val scoreMessage = MessageSnackBar.make(
                binding.gameplayFragment,
                "",
                2000,
                textBigAndSmall?.second
                    ?: resources.getDimensionPixelSize(R.dimen.other_message_size)
                        .toFloat()
            )
            limitReached = gameViewModel.increaseIntervalCount(getString(R.string.intermediate_ads))

            val scoreMessageCallback = object :
                BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
                override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    val tapMessage = getString(R.string.tap_to_continue)
                    val adMessage = getString(R.string.watch_ad_and_continue)
                    val messageToShow =
                        if (limitReached && gameViewModel.rewardedAdAvailable.value) adMessage else tapMessage
                    tapToContinueMessage = MessageSnackBar.make(
                        binding.gameplayFragment,
                        messageToShow,
                        Int.MAX_VALUE,
                        textBigAndSmall?.second
                            ?: resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
                    )
                    tapToContinueMessage?.show()

                    val tapToContinueAnimation =
                        ObjectAnimator.ofFloat(tapToContinueMessage?.view, View.ALPHA, 1f, 0.5f)
                    allAnimations.add(tapToContinueAnimation)
                    tapToContinueAnimation.duration = 1000
                    tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
                    tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
                    tapToContinueAnimation.start()

                    val messageViewClickListener = View.OnClickListener {
                        if (isAdded) {
                            soundPlayer.play(soundPlayer.sounds.levelTransition)
                            showAdContinue()
                        }
                    }
                    showPlayerCenteredScores()
                    tapToContinueMessage?.view?.setOnClickListener(messageViewClickListener)
                }
            }
            scoreMessage.addCallback(scoreMessageCallback)
            snackBars[scoreMessage] = scoreMessageCallback

            val winningMessageCallBack = object :
                BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
                override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    scoreMessage.show()
                    startCountAnimation(
                        scoreMessage.view.findViewById(R.id.message),
                        currentAndMaxScore
                    )
                }
            }
            winningMessage.addCallback(winningMessageCallBack)
            snackBars[winningMessage] = winningMessageCallBack
            soundPlayer.play(soundPlayer.sounds.correctSound)
            winningMessage.show()
        }
    }


    // CLEAN
    private fun showPlayerCenteredScores() {
        lifecycleScope.launch(Dispatchers.Main) {
            val limitReached =
                gameViewModel.increaseIntervalCount(getString(R.string.player_centered_scores))
            Log.d(TAG, "LimitReached: $limitReached")
            if (gameViewModel.internetState.value.state == InternetState.ONLINE && limitReached) {
                Log.d(TAG, "Should show now scores. Limit reached: $limitReached")
                val signedIn = GoogleSignIn.getLastSignedInAccount(requireActivity())
                if (gameViewModel.getLevelNumber() % 5 == 0 && signedIn != null) {
                    parentFragmentManager.commit {
                        addToBackStack(ScoresFragment().tag)
                        add(ScoresFragment(), ScoresFragment().tag)
                    }
                }
            }
        }
    }

    private fun showAdContinue() {
        lifecycleScope.launch {
            if (limitReached) {
                if (gameViewModel.internetState.value.state == InternetState.ONLINE && gameViewModel.rewardedAdAvailable.value) {
                    (requireActivity() as? GameActivity).let { gameActivity ->
                        if (gameActivity != null) {
                            val intermediatePurchased =
                                gameViewModel.skuDetails.value.find {
                                    it.skuDetails.sku == getString(R.string.no_intermediate_ads)
                                }?.purchased
                            val fullNoAdsPurchased =
                                gameViewModel.skuDetails.value.find {
                                    it.skuDetails.sku == getString(R.string.full_no_ads)
                                }?.purchased
                            lifecycleScope.launch {
                                if (!(intermediatePurchased == true || fullNoAdsPurchased == true)) {
                                    gameActivity.showRewardedVideo(RewardType.NEXT_LEVEL)
                                }
                            }
                        }
                    }
                }
            } else {
                continueToNextLevelActions()
            }
        }
    }

    private fun continueToNextLevelActions() {
        lifecycleScope.launch {
            gameViewModel.setCountTo(getString(R.string.hint_suggestion))
        }
        tapToContinueMessage?.view?.setOnClickListener(null)
        tapToContinueMessage?.dismiss()
        gameState = GameState.BUSY
        gameViewModel.unlockNextLevel()
        resetLevel()
        loadLevel(gameViewModel.currentLevel)
        //reloadLevel()
        //replaceFragmentWithSelf()
    }


    // CLEAN
    private fun unlockAchievementForLevel(
        level: Int,
        lastSignedInAccount: GoogleSignInAccount
    ) {
        when (level) {
            20 -> {
                Games.getAchievementsClient(requireActivity(), lastSignedInAccount)
                    .unlock(getString(R.string.achievement_greenhorn))

            }
            40 -> {
                Games.getAchievementsClient(requireActivity(), lastSignedInAccount)
                    .unlock(getString(R.string.achievement_proficient))
            }
            60 -> {
                Games.getAchievementsClient(requireActivity(), lastSignedInAccount)
                    .unlock(getString(R.string.achievement_crackerjack))
            }
            80 -> {
                Games.getAchievementsClient(requireActivity(), lastSignedInAccount)
                    .unlock(getString(R.string.achievement_chief))
            }
            94 -> {
                Games.getAchievementsClient(requireActivity(), lastSignedInAccount)
                    .unlock(getString(R.string.achievement_authority))
            }
        }
    }

    // CLEAN
    private fun startCountAnimation(
        scoreView: TextView,
        currentAndMaxScore: Pair<Int, Int>
    ) {
        val currentValue = 0
        val animator =
            ValueAnimator.ofInt(currentValue, currentValue.plus(currentAndMaxScore.first))
        allAnimations.add(animator)
        val paddingLength = currentAndMaxScore.second.toString().length
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = StringBuilder()
                .append(animation.animatedValue.toString().padStart(paddingLength, '0'))
                .append(" of ${currentAndMaxScore.second} points")
        }
        soundPlayer.play(soundPlayer.sounds.scoreSound)
        animator.start()
    }

    private fun rain(): List<Party> {
        return listOf(
            Party(
                speed = 0f,
                maxSpeed = 15f,
                damping = 0.9f,
                angle = Angle.BOTTOM,
                spread = Spread.ROUND,
                colors = listOf(0xff0000, 0x2196F3, 0xCDDC39, 0xFFC107),
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(25),
                position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
            )
        )
    }


    // CLEAN
    private fun playWrongSolutionAnimation() {
        val animatorSet = AnimatorSet()
        val shakeX = ObjectAnimator
            .ofFloat(
                binding.gameplayFragment,
                "translationX",
                0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
            ).apply {
                duration = 1000
            }
        allAnimations.add(shakeX)
        animatorSet.play(shakeX)
        animatorSet.start()
        doubleClickVibration()
    }


    // CLEAN
    private fun doubleClickVibration() {
        val rContext = requireContext()
        rContext.getSharedPreferences(
            getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (soundPlayer.isRingerModeNotSilent() && it.getBoolean(
                    rContext.getString(R.string.vibration_on_pref),
                    true
                )
            ) {
                val doubleClickPattern = longArrayOf(0, 50, 50, 50)
                val vibrator = rContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                when {
                    Build.VERSION.SDK_INT >= 26 -> {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(doubleClickPattern, -1)
                        )
                    }
                    else -> {
                        vibrator.vibrate(doubleClickPattern, -1)
                    }
                }
            }
        }
    }

    private fun setTextSizes(isBookMode: Boolean): Pair<Float, Float> {
        var textBig = 0
        var textSmall = 0
        val densityDpi = requireContext().resources?.displayMetrics?.densityDpi

        when (gameViewModel.windowSizeClassWidth) {
            WindowSizeClass.COMPACT -> {
                when {
                    densityDpi in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._20ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._19ssp)
                    }
                    densityDpi in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._14ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._13ssp)
                    }
                    densityDpi in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._10ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._9ssp)
                    }
                    densityDpi in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                        if (isBookMode) {
                            textBig = resources.getDimensionPixelSize(R.dimen._5ssp)
                            textSmall = resources.getDimensionPixelSize(R.dimen._4ssp)
                        } else {
                            textBig = resources.getDimensionPixelSize(R.dimen._8ssp)
                            textSmall = resources.getDimensionPixelSize(R.dimen._7ssp)
                        }
                    }
                    densityDpi in DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._7ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._6ssp)
                    }
                    densityDpi?.compareTo(DisplayMetrics.DENSITY_XXXHIGH) == 1 || densityDpi?.compareTo(
                        DisplayMetrics.DENSITY_XXXHIGH
                    ) == 0 -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._6ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._5ssp)
                    }
                    else -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._8ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._7ssp)
                    }
                }
            }
            WindowSizeClass.MEDIUM -> {
                when (densityDpi) {
                    in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._15ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._14ssp)
                    }
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._11ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._10ssp)
                    }
                    in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._8ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._7ssp)
                    }
                    in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                        if (isBookMode) {
                            textBig = resources.getDimensionPixelSize(R.dimen._5ssp)
                            textSmall = resources.getDimensionPixelSize(R.dimen._4ssp)
                        } else {
                            textBig = resources.getDimensionPixelSize(R.dimen._6ssp)
                            textSmall = resources.getDimensionPixelSize(R.dimen._5ssp)
                        }
                    }
                    else -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._7ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._6ssp)
                    }
                }
            }
            WindowSizeClass.EXPANDED -> {
                when (densityDpi) {
                    in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._10ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._9ssp)
                    }
                    else -> {
                        textBig = resources.getDimensionPixelSize(R.dimen._7ssp)
                        textSmall = resources.getDimensionPixelSize(R.dimen._6ssp)
                    }
                }

            }
            WindowSizeClass.NOT_SET -> {
                textBig = resources.getDimensionPixelSize(R.dimen._7ssp)
                textSmall = resources.getDimensionPixelSize(R.dimen._6ssp)
            }
        }

        return Pair(textBig.toFloat(), textSmall.toFloat())
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        hintBadge = null
        val trainLinearLayout =
            requireActivity().findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)
        trainLinearLayout.removeAllViews()
        allAnimations.forEach {
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.cancel()
        }
        snackBars.forEach {
            it.key.removeCallback(it.value)
        }
        tapToContinueMessage?.view?.setOnClickListener(null)
        tapToContinueMessage?.dismiss()
        _binding = null
        super.onDestroyView()
    }

}
