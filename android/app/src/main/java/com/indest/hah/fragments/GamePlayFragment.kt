package com.indest.hah.fragments

import android.animation.*
import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.Games
import com.google.android.gms.games.leaderboard.LeaderboardVariant.*
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.indest.hah.GameActivity
import com.indest.hah.LevelFactory
import com.indest.hah.R
import com.indest.hah.databinding.GameplayFragmentBinding
import com.indest.hah.game.*
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.letter.Slot
import com.indest.hah.letter.effects.SoundSelector
import com.indest.hah.persistance.LevelProgress
import com.indest.hah.persistance.dto.Level
import com.indest.hah.util.C.CATEGORY
import com.indest.hah.util.C.HINT_REQUEST_KEY
import com.indest.hah.util.C.HINT_TYPE
import com.indest.hah.util.C.LETTER
import com.indest.hah.util.hideSystemBars
import com.indest.hah.util.playWhileSoundOn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@AndroidEntryPoint
class GamePlayFragment : Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    private lateinit var binding: GameplayFragmentBinding
    private lateinit var soundPool: SoundPool
    private var noteSounds by Delegates.notNull<List<Int>>()
    private var scoreSound by Delegates.notNull<Int>()
    private var correctSound by Delegates.notNull<Int>()
    private var levelTransition by Delegates.notNull<Int>()
    private var hintNotification by Delegates.notNull<Int>()
    private lateinit var audioManager: AudioManager
    private var hintBadge: BadgeDrawable? = null
    lateinit var cellDirector: CellDirector
    var gameState: GameState = GameState.BUSY
    val gameViewModel: GameViewModel by activityViewModels()

    private var guesses: Int by Delegates.observable(0) { _, _, new ->
        val guessesBeforeHint = resources.getInteger(R.integer.guessesBeforeHint)
        Log.i(TAG, "guessesBeforeHint: $guessesBeforeHint")
        Log.i(TAG, "new: $new")

        if (new == guessesBeforeHint) {
            soundPool.playWhileSoundOn(
                requireActivity(),
                hintNotification,
                1,
                audioManager = audioManager
            )
            createHintsBadge()
            hintBadge?.isVisible = true
            Log.i(TAG, "hintBadge.isVisible: ${hintBadge?.isVisible}")
            activity?.getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)
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

        loadRewardedAd()
        handleGivingRewards()
        handleLoadingLevel()

    }

    // CLEAN
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GameplayFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        Log.i(TAG, "onCreateView")
        return binding.root
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
        findNavController().removeOnDestinationChangedListener { _, _, _ -> }
    }

    private fun handleLoadingLevel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gameViewModel.uiState.collect { uiState ->
                    if (gameState == GameState.LEVEL_COMPLETED) return@collect
                    when (uiState) {
                        is LatestLevelEvent.Success -> {
                            if (!(uiState.oldLevel.levelInfo.levelId == 0 && uiState.currentLevel.levelInfo.levelId == 0)) {
                                if (uiState.oldLevel.levelInfo.levelId != uiState.currentLevel.levelInfo.levelId && uiState.oldLevel.levelInfo.levelId != 0) {
                                    if (binding.gameplayFragment.childCount > 0) {
                                        replaceFragmentWithSelf()
                                    } else {
                                        loadLevel(uiState.currentLevel)
                                    }
                                } else if (binding.gameplayFragment.childCount == 0) {
                                    loadLevel(uiState.currentLevel)
                                }
                            }
                            (requireActivity() as GameActivity).binding.gameProgress.progress =
                                ((uiState.currentLevel.levelInfo.levelId.toFloat() / 94f) * 100).toInt()
                        }
                        is LatestLevelEvent.Error -> {
                            Log.i(TAG, "Error getting level from gameViewModel")
                        }
                    }
                }
            }
        }
    }

    private fun showReviewOption(level: Int) {
        if (level == 21) {
            requireActivity().getSharedPreferences(
                getString(R.string.preferences),
                Context.MODE_PRIVATE
            )?.let {
                if (!(it.getBoolean(requireContext().getString(R.string.review_displayed_to_user), false))) {
                    // val reviewManager = FakeReviewManager(requireContext())
                    val reviewManager = ReviewManagerFactory.create(requireContext())
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                            flow.addOnCompleteListener { _ ->
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

    // CAN BE CLEANED
    private fun handleGivingRewards() {
        setFragmentResultListener(HINT_REQUEST_KEY) { _, bundle ->
            run {
                Log.i(TAG, "fragmentResultListener")
                when (bundle[HINT_TYPE]) {
                    // Reward player with random letter placed to correct slot
                    LETTER -> {
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
                        Log.d(TAG, "Rewarding with placing a letter.")
                    }
                    // We just play the sound effect (reward given in HintsFragment)
                    CATEGORY -> {
                        cellDirector.cells.filterIsInstance<Letter>().first().effects.playSound(
                            SoundSelector.LETTER_REWARDED
                        )
                        Log.d(TAG, "Rewarding with category.")
                    }
                }
            }
        }
    }

    // CLEAN
    private fun resetHintsBadge() {
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_hints) {
                activity?.getSharedPreferences(
                    getString(R.string.preferences),
                    Context.MODE_PRIVATE
                )?.let {
                    with(it.edit()) {
                        hintBadge?.isVisible = false
                        putBoolean(getString(R.string.level_solution_guesses), false)
                        apply()
                    }
                }
                guesses = 0
            }
        }
        activity?.getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)?.let {
            hintBadge?.isVisible = it.getBoolean(getString(R.string.level_solution_guesses), false)
        }

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
        val level = LevelFactory.getLevel(
            levelAndLettersInfo,
            requireContext(),
            layoutInflater,
            binding.gameplayFragment
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
            val message = MessageSnackBar.make(
                binding.gameplayFragment,
                getString(R.string.tap_to_continue),
                Int.MAX_VALUE,
                resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
            )
            message.show()

            val tapToContinueAnimation =
                ObjectAnimator.ofFloat(message.view, View.ALPHA, 1f, 0.5f)
            tapToContinueAnimation.duration = 1000
            tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
            tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
            tapToContinueAnimation.start()

            message.view.setOnClickListener {
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

        } else {
            gameState = GameState.READY
            handleGameSolutions()
        }
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
            replace<GamePlayFragment>(R.id.fragments_container)
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

        activity?.getSharedPreferences(getString(R.string.preferences), Context.MODE_PRIVATE)
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
        val lastLetter =
            cellDirector.cells.find { p -> p.homeIndex == info.third } as Letter
        cellDirector.sendLetterHome(lastLetter)
        val message = MessageSnackBar.make(
            binding.gameplayFragment,
            getString(R.string.not_quite_solution),
            2000,
            resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
        )
        message.show()
        GoogleSignIn.getLastSignedInAccount(requireActivity())?.let {
            Games.getAchievementsClient(requireActivity(), it)
                .unlock(getString(R.string.achievement_wrong_palindrome))
        }
    }

    // CLEAN
    private fun wrongSolutionActions(info: Triple<String?, String?, Int?>) {
        val lastLetter =
            cellDirector.cells.find { p -> p.homeIndex == info.third } as Letter
        cellDirector.sendLetterHome(lastLetter)
        val message = MessageSnackBar.make(
            binding.gameplayFragment,
            getString(R.string.wrong_solution),
            2000,
            resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
        )
        message.show()
    }

    // MIXED
    private fun playCorrectSolutionAnimation(currentAndMaxScore: Pair<Int, Int>) {
        val successTextColor =
            ResourcesCompat.getColor(resources, R.color.successTextColor, requireContext().theme)
        val currentBoxColor =
            ResourcesCompat.getColor(resources, R.color.colorSecondary, requireContext().theme)
        val boxSuccessColor =
            ResourcesCompat.getColor(resources, R.color.successBoxColor, requireContext().theme)
        val lockedColor =
            ResourcesCompat.getColor(resources, R.color.letterRewardedColor, requireContext().theme)

        val animatorSet = AnimationFactory.getCorrectSolutionAnimation(
            requireActivity(),
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
        val winningMessage = MessageSnackBar.make(
            binding.gameplayFragment,
            getString(R.string.correct_solution),
            2000,
            resources.getDimensionPixelSize(R.dimen.winning_message_size).toFloat()
        )
        val scoreMessage = MessageSnackBar.make(
            binding.gameplayFragment,
            "",
            2000,
            resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
        )
        scoreMessage.addCallback(object : BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
            override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                parentFragmentManager.fragments.forEach {
                    if (it is DialogFragment) it.dismiss()
                }
                val message = MessageSnackBar.make(
                    binding.gameplayFragment,
                    getString(R.string.tap_to_continue),
                    Int.MAX_VALUE,
                    resources.getDimensionPixelSize(R.dimen.other_message_size).toFloat()
                )
                message.show()

                val tapToContinueAnimation =
                    ObjectAnimator.ofFloat(message.view, View.ALPHA, 1f, 0.5f)
                tapToContinueAnimation.duration = 1000
                tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
                tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
                tapToContinueAnimation.start()

                showPlayerCenteredScores()

                message.view.setOnClickListener {
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
        })
        winningMessage.addCallback(object : BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
            override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                scoreMessage.show()
                startCountAnimation(
                    scoreMessage.view.findViewById(R.id.message),
                    currentAndMaxScore
                )
            }
        })
        soundPool.playWhileSoundOn(requireActivity(), correctSound, 2, audioManager = audioManager)
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
    }

    // CLEAN
    private fun unlockAchievementForLevel(level: Int, lastSignedInAccount: GoogleSignInAccount) {
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
        val paddingLength = currentAndMaxScore.second.toString().length
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = StringBuilder()
                .append(animation.animatedValue.toString().padStart(paddingLength, '0'))
                .append(" of ${currentAndMaxScore.second} points")
        }
        soundPool.playWhileSoundOn(requireActivity(), scoreSound, 2, audioManager = audioManager)
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
        animatorSet.play(shakeX)
        animatorSet.start()
        doubleClickVibration()
    }


    // CLEAN
    private fun doubleClickVibration() {
        val rContext = requireContext()
        requireActivity().getSharedPreferences(
            getString(R.string.preferences),
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

    // CLEAN
    private fun loadRewardedAd() {
        requireActivity().getSharedPreferences(
            getString(R.string.preferences),
            Context.MODE_PRIVATE
        )?.let {
            if (gameViewModel.rewardedAd == null &&
                (!it.getBoolean(getString(R.string.clues_purchased), false) ||
                        !it.getBoolean(getString(R.string.place_letter_purchased), false))
            ) {

                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    requireActivity(), getString(R.string.rewarded_ad_unit), adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d(TAG, adError.message)
                            gameViewModel.rewardedAd = null
                        }

                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            Log.d(TAG, "Ad was loaded.")
                            gameViewModel.rewardedAd = rewardedAd
                        }
                    }
                )
            }
        }
    }


}
