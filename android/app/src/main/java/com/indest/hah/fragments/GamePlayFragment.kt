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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.Games
import com.google.android.gms.games.leaderboard.LeaderboardVariant.*
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.play.core.review.ReviewManagerFactory
import com.indest.hah.GameActivity
import com.indest.hah.LevelFactory
import com.indest.hah.R
import com.indest.hah.databinding.GameplayFragmentBinding
import com.indest.hah.game.*
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.letter.Slot
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.dto.Level
import com.indest.hah.util.C.HINT_FRAGMENT_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.properties.Delegates
import android.animation.ObjectAnimator
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.view.*
import com.indest.hah.util.C.HINT_REQUEST_KEY
import kotlin.math.ln
import android.graphics.BitmapFactory

import android.graphics.Bitmap
import com.indest.hah.util.*

@AndroidEntryPoint
class GamePlayFragment : Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    private var _binding: GameplayFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var soundPool: SoundPool
    private var noteSounds by Delegates.notNull<List<Int>>()
    private var scoreSound by Delegates.notNull<Int>()
    private var correctSound by Delegates.notNull<Int>()
    private var levelTransition by Delegates.notNull<Int>()
    private var hintNotification by Delegates.notNull<Int>()
    private lateinit var audioManager: AudioManager
    private var hintBadge: BadgeDrawable? = null
    private var textBigAndSmall: Pair<Float, Float>? = null
    lateinit var cellDirector: CellDirector
    var gameState: GameState = GameState.BUSY
    val gameViewModel: GameViewModel by activityViewModels()
    private var allAnimations: MutableSet<ValueAnimator> = mutableSetOf()
    private var snackBars: HashMap<MessageSnackBar, BaseTransientBottomBar.BaseCallback<MessageSnackBar>> = hashMapOf()

    private var guesses: Int by Delegates.observable(0) { _, _, new ->
        val guessesBeforeHint = resources.getInteger(R.integer.guessesBeforeHint)
        Log.i(TAG, "guessesBeforeHint: $guessesBeforeHint")
        Log.i(TAG, "new: $new")

        if (new == guessesBeforeHint) {
            soundPool.playWhileSoundOn(
                requireContext(),
                hintNotification,
                1,
                audioManager = audioManager
            )
            hintBadge?.isVisible = true
            Log.i(TAG, "hintBadge.isVisible: ${hintBadge?.isVisible}")
            activity?.getSharedPreferences(
                getString(R.string.app_preferences),
                Context.MODE_PRIVATE
            )
                ?.let { sharedPref ->
                    with(sharedPref.edit()) {
                        putBoolean(getString(R.string.level_solution_guesses), true)
                        apply()
                    }
                }
            guesses = 0
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")

        super.onCreate(savedInstanceState)
        cellDirector = CellDirector(this)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
        )
        resetHintsBadgeOnFragmentResult()

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                WindowInfoTracker.getOrCreate(requireContext())
                    .windowLayoutInfo(requireActivity())
                    .collectLatest { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookMode = isBookMode(foldingFeature)
                        textBigAndSmall = setTextSizes(isBookMode)
                        observeViewModel(isBookMode)
                    }
            }
        }
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
        when (hintType) {
            RewardType.HINT -> {
                gameViewModel.setCategoryRewarded(true)
            }
            RewardType.LETTER -> {
                gameState = GameState.REWARDING
                val candidateLetters = cellDirector.cells
                    .filterIsInstance<Letter>()
                    .filter { !it.hasFriend() }
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
                randomLetter.z = C.TOP_LAYER
                if (correctSlot.hasFriend()) {
                    correctSlot.friend?.let {
                        cellDirector.switchLetterPositions(
                            randomLetter,
                            it, true
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
                                cellDirector.sendLetterHome(it)
                                cellDirector.placeLetterInEmptyBox(
                                    randomLetter, correctSlot,
                                    isRewarded = true
                                )
                            }
                    } else {
                        cellDirector.placeLetterInEmptyBox(randomLetter, correctSlot, true)
                    }
                }
            }
            else -> {
                Log.i(TAG, "No reward")
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
        Log.i(TAG, "onCreateView")
        return b.root
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
        resetHintsBadge()
    }

    // CLEAN
    override fun onPause() {
        super.onPause()
        findNavController(this).removeOnDestinationChangedListener { _, _, _ -> }
    }

    private fun observeViewModel(isBookMode: Boolean) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    gameViewModel.uiState.collect { uiState ->
                        if (gameState == GameState.LEVEL_COMPLETED) return@collect
                        when (uiState) {
                            is LevelEvent.Success -> {
                                Log.i(TAG, "LevelEvent.Success")
                                if (!(uiState.oldLevel.levelInfo.levelId == 0 && uiState.currentLevel.levelInfo.levelId == 0)) {
                                    if (uiState.oldLevel.levelInfo.levelId != uiState.currentLevel.levelInfo.levelId && uiState.oldLevel.levelInfo.levelId != 0) {
                                        if (binding.gameplayFragment.childCount > 0) {
                                            Log.i(TAG, "replaceFragmentWithSelf childCount > 0")
                                            replaceFragmentWithSelf()
                                        } else {
                                            Log.i(
                                                TAG,
                                                "loadLevel(uiState.currentLevel, isBookMode) NOT childCount > 0"
                                            )
                                            loadLevel(uiState.currentLevel, isBookMode)
                                        }
                                    } else if (binding.gameplayFragment.childCount == 0) {
                                        Log.i(
                                            TAG,
                                            "  loadLevel(uiState.currentLevel, isBookMode) childCount == 0"
                                        )
                                        loadLevel(uiState.currentLevel, isBookMode)
                                    }
                                }
                                val levelCount = gameViewModel.levels.value?.size ?: 100
                                (requireActivity() as GameActivity).binding.gameProgress.progress =
                                    ((uiState.currentLevel.levelInfo.levelId.toFloat()
                                        .div(levelCount)) * 100).toInt()
                            }
                            is LevelEvent.Error -> {
                                Log.i(TAG, "Error getting level from gameViewModel")
                            }
                        }
                    }
                }
                launch {
                    gameViewModel.userReward.collect { userRewardEvent ->
                        when (userRewardEvent) {
                            is UserRewardEvent.Success -> {
                                if (userRewardEvent.rewardEarned)
                                {
                                    gameViewModel.resetRewardingUser()
                                    rewardUser(userRewardEvent.rewardType)
                                }
                            }
                            is UserRewardEvent.Error -> {
                                Log.i(TAG, "Error getting userRewardEvent from viewModel")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isBookMode(foldFeature: FoldingFeature?) =
        foldFeature?.state == FoldingFeature.State.FLAT

    private fun playSpecialAnimation() {
        Log.i(TAG, "Play special animation")
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
                                Log.i(
                                    TAG,
                                    "Review completed (whatever the action)."
                                )
                            }
                        } else {
                            Log.i(
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
        activity?.getSharedPreferences(
            getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            with(it.edit()) {
                hintBadge?.isVisible = false
                putBoolean(getString(R.string.level_solution_guesses), false)
                apply()
            }
        }
        guesses = 0
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
    private fun loadLevel(levelAndLettersInfo: Level, isBookMode: Boolean) {
        Log.i(TAG, "loadLevel")
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

        val levelNumber = levelAndLettersInfo.levelInfo.levelId
        val levelsCount = gameViewModel.levels.value?.size ?: 100

        if (levelAndLettersInfo.levelInfo.progress == LevelProgress.INTERMEDIATE) {
            gameState = GameState.LEVEL_COMPLETED
            val message = MessageSnackBar.make(
                binding.gameplayFragment,
                getString(R.string.tap_to_continue),
                Int.MAX_VALUE,
                textBigAndSmall?.second
                    ?: resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
            )

            val tapToContinueAnimation =
                ObjectAnimator.ofFloat(message.view, View.ALPHA, 1f, 0.5f)
            allAnimations.add(tapToContinueAnimation)
            tapToContinueAnimation.duration = 1000
            tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
            tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
            tapToContinueAnimation.start()

            val messageViewListener = View.OnClickListener {
                soundPool.playWhileSoundOn(
                    requireActivity(),
                    levelTransition,
                    2,
                    audioManager = audioManager
                )
                message.dismiss()
                tapToContinueAnimation.cancel()
                message.view.setOnClickListener(null)
                continueToNextLevelActions()
            }

            message.view.setOnClickListener(messageViewListener)
            message.show()

        } else {
            gameState = GameState.READY
            handleGameSolutions()
            if (VaryingProbability.isAnimationPlayed(levelNumber)) playSpecialAnimation()
        }

        val levelNrView = (requireActivity() as GameActivity).binding.levelNumber
        val progressIndicatorWidth =
            (requireActivity() as GameActivity).binding.gameProgress.measuredWidth

        val progressX =
            (levelNumber * progressIndicatorWidth / levelsCount) - ln(levelNumber.toFloat()) * 15
        levelNrView.text = levelNumber.toString()
        levelNrView.x = progressX
        showReviewOption(levelAndLettersInfo.levelInfo.levelId)

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
            .observe(viewLifecycleOwner, {
                if (gameState != GameState.LEVEL_COMPLETED) {
                    if (it != null && it.first?.length == it.second?.length) {
                        guesses++
                        gameState = GameState.BUSY
                        val playerSolution = it.second?.map { char ->
                            if ("^([?!.,’])".contains(char)) "" else char
                        }?.joinToString("")

                        when {
                            it.first == it.second -> {
                                gameState = GameState.LEVEL_COMPLETED
                                val currentAndMaxScore = gameViewModel.getCurrentAndMaxScore()
                                playCorrectSolutionAnimation(currentAndMaxScore)
                                correctSolutionActions(
                                    it.first?.count(),
                                    currentAndMaxScore.first == currentAndMaxScore.second
                                )
                            }
                            playerSolution?.reversed() == playerSolution -> {
                                playWrongSolutionAnimation()
                                wrongPalindromeActions(it)
                            }
                            else -> {
                                playWrongSolutionAnimation()
                                wrongSolutionActions(it)
                            }
                        }
                    } else if (gameState != GameState.REWARDING) {
                        gameState = GameState.READY
                    }
                }
            })
    }

    // MIXED, SCORE CAN BE QUERIED IMMEDIATELY - NO OBSERVING NEEDED
    private fun correctSolutionActions(puzzleLength: Int?, didNoMistakes: Boolean) {
        gameViewModel.realSolutionAndGuess.removeObservers { viewLifecycleOwner.lifecycle }
        lifecycleScope.launch {
            gameViewModel.totalScore.observe(viewLifecycleOwner)
            {
                if (it != null) {
                    val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
                    if (account != null) {
                        Games.getLeaderboardsClient(requireActivity(), account)
                            .submitScore(
                                getString(R.string.leaderboard_crme_de_la_crme),
                                it.toLong()
                            )
                        Log.i(TAG, "New score: $it submitted to leaderboards.")
                    }
                }
                gameViewModel.totalScore.removeObserver { viewLifecycleOwner.lifecycle }
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

    // MIXED
    private fun playCorrectSolutionAnimation(currentAndMaxScore: Pair<Int, Int>) {
        val successTextColor =
            ResourcesCompat.getColor(
                resources,
                R.color.successTextColor,
                requireContext().theme
            )
        val currentBoxColor =
            ResourcesCompat.getColor(resources, R.color.colorSecondary, requireContext().theme)
        val boxSuccessColor =
            ResourcesCompat.getColor(resources, R.color.successBoxColor, requireContext().theme)
        val lockedColor =
            ResourcesCompat.getColor(
                resources,
                R.color.letterRewardedColor,
                requireContext().theme
            )

        val animatorSet = AnimationFactory.getCorrectSolutionAnimation(
            requireContext(),
            successTextColor,
            currentBoxColor,
            boxSuccessColor,
            cellDirector.cells,
            audioManager,
            soundPool,
            noteSounds,
            lockedColor
        )

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                showWinningMessages(currentAndMaxScore)
            }
        })
        animatorSet.start()
    }

    // MIXED
    private fun showWinningMessages(currentAndMaxScore: Pair<Int, Int>) {
        val randomCorrectMessage = resources.getStringArray(R.array.correct_texts).random()

        val winningMessage = MessageSnackBar.make(
            binding.gameplayFragment,
            randomCorrectMessage,
            2000,
            textBigAndSmall?.first ?: resources.getDimensionPixelSize(R.dimen.winning_message_size)
                .toFloat()
        )
        val scoreMessage = MessageSnackBar.make(
            binding.gameplayFragment,
            "",
            2000,
            textBigAndSmall?.second ?: resources.getDimensionPixelSize(R.dimen.other_message_size)
                .toFloat()
        )

        val scoreMessageCallback = object :
            BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
            override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                val message = MessageSnackBar.make(
                    binding.gameplayFragment,
                    getString(R.string.tap_to_continue),
                    Int.MAX_VALUE,
                    textBigAndSmall?.second
                        ?: resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
                )
                message.show()

                val tapToContinueAnimation =
                    ObjectAnimator.ofFloat(message.view, View.ALPHA, 1f, 0.5f)
                allAnimations.add(tapToContinueAnimation)
                tapToContinueAnimation.duration = 1000
                tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
                tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
                tapToContinueAnimation.start()

                showPlayerCenteredScores()

                val messageViewClickListener = View.OnClickListener {
                    if(isAdded)
                    {
                        soundPool.playWhileSoundOn(
                            requireActivity(),
                            levelTransition,
                            2,
                            audioManager = audioManager
                        )
                        message.dismiss()
                        tapToContinueAnimation.cancel()
                        message.view.setOnClickListener(null)
                        continueToNextLevelActions()
                    }
                }
                message.view.setOnClickListener(messageViewClickListener)
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

        soundPool.playWhileSoundOn(
            requireActivity(),
            correctSound,
            2,
            audioManager = audioManager
        )
        winningMessage.show()
    }

    // CLEAN
    private fun showPlayerCenteredScores() {
        val signedIn = GoogleSignIn.getLastSignedInAccount(requireActivity())
        if (gameViewModel.getLevelNumber() % 5 == 0 && signedIn != null) {
            parentFragmentManager.commit {
                addToBackStack(ScoresFragment().tag)
                add(ScoresFragment(), ScoresFragment().tag)
            }
        }
    }

    // CLEAN
    private fun continueToNextLevelActions() {
        gameState = GameState.BUSY
        gameViewModel.totalScore.removeObserver { viewLifecycleOwner.lifecycle }
        gameViewModel.unlockNextLevel()
        lifecycleScope.launch {
            gameViewModel.saveSnapshot(requireActivity(), binding.gameplayFragment.drawToBitmap())
        }
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
    private fun startCountAnimation(scoreView: TextView, currentAndMaxScore: Pair<Int, Int>) {
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

        soundPool.playWhileSoundOn(
            requireContext(),
            scoreSound,
            2,
            audioManager = audioManager
        )
        animator.start()
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
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT && it.getBoolean(
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
        Log.i(TAG, "onDestroyView")
        hintBadge = null
        val trainLinearLayout = requireActivity().findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)
        trainLinearLayout.removeAllViews()
        allAnimations.forEach {
            it.removeAllUpdateListeners()
            it.removeAllListeners()
            it.cancel()
        }
        snackBars.forEach {
            it.key.removeCallback(it.value)
        }
        _binding = null
        super.onDestroyView()
    }

}
