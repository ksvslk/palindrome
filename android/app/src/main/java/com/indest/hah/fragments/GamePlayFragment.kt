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
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.indest.hah.LevelFactory
import com.indest.hah.R
import com.indest.hah.databinding.GameplayFragmentBinding
import com.indest.hah.game.*
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.letter.Slot
import com.indest.hah.letter.effects.SoundSelector
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
    private var hintNotification by Delegates.notNull<Int>()
    private lateinit var audioManager: AudioManager
    private lateinit var hintBadge: BadgeDrawable
    lateinit var cellDirector: CellDirector
    var gameState: GameState = GameState.BUSY

    private var guesses: Int by Delegates.observable(0) { _, _, new ->
        val guessesBeforeHint = resources.getInteger(R.integer.guessesBeforeHint)
        if (new == guessesBeforeHint && !hintBadge.isVisible) {
            soundPool.playWhileSoundOn(
                requireActivity(),
                hintNotification,
                1,
                audioManager = audioManager
            )
            hintBadge.isVisible = true
            activity?.getPreferences(Context.MODE_PRIVATE)?.let { sharedPref ->
                with(sharedPref.edit()) {
                    putBoolean(getString(R.string.level_solution_guesses), true)
                    apply()
                }
            }
            guesses = 0
        }
    }

    val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        requireActivity().getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {
            if (!it.getBoolean(getString(R.string.categories_purchased), false) ||
                !it.getBoolean(getString(R.string.place_letter_purchased), false)
            ) {
                loadRewardedAd(getString(R.string.rewarded_ad_unit))
            }
        }

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
                        randomLetter.isLocked = true
                        if (correctSlot.hasFriend()) {
                            correctSlot.friend?.let {
                                cellDirector.switchLetterPositions(
                                    randomLetter,
                                    it, true
                                )
                            }
                        } else {
                            if (candidateLetters.size == 1) { // Last letter to be placed in the slot
                                cellDirector.placeLetterInEmptyBox(
                                    randomLetter, correctSlot,
                                    isRewarded = true,
                                    isLastRewardedLetter = true
                                )
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

        cellDirector = CellDirector(this)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        soundPool = SoundPool.Builder().build()
        scoreSound = soundPool.load(requireContext(), R.raw.score_tick, 1)
        correctSound = soundPool.load(requireContext(), R.raw.correct, 1)
        hintNotification = soundPool.load(requireContext(), R.raw.hint_notification, 1)
        noteSounds = arrayListOf(
            soundPool.load(requireContext(), R.raw.fs2_edited, 1),
            soundPool.load(requireContext(), R.raw.fs1, 1),
            soundPool.load(requireContext(), R.raw.fs3, 1),
            soundPool.load(requireContext(), R.raw.fs5, 1),
            soundPool.load(requireContext(), R.raw.fs6, 1),
            soundPool.load(requireContext(), R.raw.fs4, 1),
            soundPool.load(requireContext(), R.raw.fs7, 1),
            soundPool.load(requireContext(), R.raw.fs9, 1),
            soundPool.load(requireContext(), R.raw.fs8, 1),
            soundPool.load(requireContext(), R.raw.fs11, 1),
            soundPool.load(requireContext(), R.raw.fs11, 1),
            soundPool.load(requireContext(), R.raw.last_level2, 1),
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gameViewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is LatestLevelEvent.Success -> {
                            if (!(uiState.oldLevel.levelInfo.levelId == 0 && uiState.currentLevel.levelInfo.levelId == 0)) {
                                if (uiState.oldLevel.levelInfo.levelId != uiState.currentLevel.levelInfo.levelId && uiState.oldLevel.levelInfo.levelId != 0) {
                                    if (binding.gameplayFragment.childCount > 0) {
                                        clearPreviousLevel()
                                    } else {
                                        loadLevel(uiState.currentLevel)
                                    }
                                } else if (binding.gameplayFragment.childCount == 0) {
                                    loadLevel(uiState.currentLevel)
                                }
                            }
                        }
                        is LatestLevelEvent.Error -> {
                            Log.i(TAG, "Error getting level from gameViewModel")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_hints) {
                activity?.getPreferences(Context.MODE_PRIVATE)?.let {
                    with(it.edit()) {
                        hintBadge.isVisible = false
                        putBoolean(getString(R.string.level_solution_guesses), false)
                        apply()
                    }
                }
            }
        }
        activity?.getPreferences(Context.MODE_PRIVATE)?.let {
            hintBadge.isVisible = it.getBoolean(getString(R.string.level_solution_guesses), false)
        }
    }

    override fun onPause() {
        super.onPause()
        findNavController().removeOnDestinationChangedListener { _, _, _ -> }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GameplayFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        hintBadge = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView)
            .getOrCreateBadge(R.id.navigation_hints)
        hintBadge.backgroundColor =
            ContextCompat.getColor(requireContext(), R.color.hintsNotificationColor)
        hintBadge.badgeGravity = BadgeDrawable.TOP_END
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.requestApplyInsets(view)
        hideSystemBars(binding.gameplayFragment)
    }

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
        gameState = GameState.READY
        handleGameSolutions()
    }

    private fun clearPreviousLevel() {
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

    private fun handleGameSolutions() {
        gameViewModel.realSolutionAndGuess
            .observe(viewLifecycleOwner, {
                if (it != null && it.first?.length == it.second?.length) {
                    guesses++
                    gameState = GameState.BUSY
                    val playerSolution = it.second?.map { char ->
                        if ("^([?!.,’])".contains(char)) "" else char
                    }?.joinToString("")

                    when {
                        it.first == it.second -> {
                            gameViewModel.realSolutionAndGuess.removeObservers { viewLifecycleOwner.lifecycle }
                            gameViewModel.updateCompletedLevel()
                            playCorrectSolutionAnimation()
                            activity?.getPreferences(Context.MODE_PRIVATE)?.let { sharedPref ->
                                with(sharedPref.edit()) {
                                    putBoolean(getString(R.string.level_solution_guesses), false)
                                    apply()
                                }
                            }
                            hintBadge.isVisible = false
                        }
                        playerSolution?.reversed() == playerSolution -> {
                            playWrongSolutionAnimation()
                            val lastLetter =
                                cellDirector.cells.find { p -> p.homeIndex == it.third } as Letter
                            cellDirector.sendLetterHome(lastLetter)
                            val message = MessageSnackBar.make(
                                binding.gameplayFragment,
                                getString(R.string.not_quite_solution),
                                2000,
                                resources.getDimensionPixelSize(R.dimen._9ssp).toFloat()
                            )
                            message.show()
                        }
                        else -> {
                            playWrongSolutionAnimation()
                            val lastLetter =
                                cellDirector.cells.find { p -> p.homeIndex == it.third } as Letter
                            cellDirector.sendLetterHome(lastLetter)
                            val message = MessageSnackBar.make(
                                binding.gameplayFragment,
                                getString(R.string.wrong_solution),
                                2000,
                                resources.getDimensionPixelSize(R.dimen._9ssp).toFloat()
                            )
                            message.show()
                        }
                    }
                } else if (gameState != GameState.REWARDING) {
                    gameState = GameState.READY
                }
            })
    }

    private fun playCorrectSolutionAnimation() {
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
                if (gameViewModel.isLastLevel()) {
                    soundPool.playWhileSoundOn(
                        requireActivity(),
                        noteSounds[11],
                        2,
                        audioManager = audioManager
                    )
                }
                showWinningMessages()
            }
        })
        animatorSet.start()
    }

    private fun showWinningMessages() {
        val winningMessage = MessageSnackBar.make(
            binding.gameplayFragment,
            getString(R.string.correct_solution),
            2000,
            resources.getDimensionPixelSize(R.dimen._11ssp).toFloat()
        )
        val scoreMessage = MessageSnackBar.make(
            binding.gameplayFragment,
            "",
            2000,
            resources.getDimensionPixelSize(R.dimen._9ssp).toFloat()
        )
        scoreMessage.addCallback(object : BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
            override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                parentFragmentManager.fragments.forEach {
                    if (it is DialogFragment) it.dismiss()
                }
                gameViewModel.changeToNextLevel()
            }
        })
        winningMessage.addCallback(object : BaseTransientBottomBar.BaseCallback<MessageSnackBar>() {
            override fun onDismissed(transientBottomBar: MessageSnackBar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                scoreMessage.show()
                startCountAnimation(scoreMessage.view.findViewById(R.id.message))
            }
        })
        soundPool.playWhileSoundOn(requireActivity(), correctSound, 2, audioManager = audioManager)
        winningMessage.show()
    }

    private fun startCountAnimation(scoreView: TextView) {
        val currentValue = 0
        val currentAndMaxScore = gameViewModel.getCurrentAndMaxScore()
        val animator =
            ValueAnimator.ofInt(currentValue, currentValue.plus(currentAndMaxScore.first))
        val paddingLength = currentAndMaxScore.second.toString().length
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = StringBuilder()
                .append("Score: ")
                .append(animation.animatedValue.toString().padStart(paddingLength, '0'))
        }
        soundPool.playWhileSoundOn(requireActivity(), scoreSound, 2, audioManager = audioManager)
        animator.start()
    }

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

    private fun doubleClickVibration() {
        val rContext = requireContext()
        rContext.getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {
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

    private fun loadRewardedAd(adUnit: String) {
        if (gameViewModel.rewardedAd == null) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                requireActivity(), adUnit, adRequest,
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
