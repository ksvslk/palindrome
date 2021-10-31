package com.indest.heh.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.indest.heh.LevelFactory
import com.indest.heh.R
import com.indest.heh.databinding.GameplayFragmentBinding
import com.indest.heh.game.AnimationFactory
import com.indest.heh.game.GameState
import com.indest.heh.game.GameViewModel
import com.indest.heh.letter.Cell
import com.indest.heh.letter.CellDirector
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates


@AndroidEntryPoint
class GamePlayFragment : Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    private lateinit var binding: GameplayFragmentBinding
    private lateinit var soundPool: SoundPool
    private var noteSounds by Delegates.notNull<List<Int>>()
    private lateinit var audioManager: AudioManager
    lateinit var cellDirector: CellDirector
    val gameViewModel: GameViewModel by activityViewModels()
    var gameState: GameState by Delegates.observable(GameState.BUSY) { _, old, new ->
        Log.i(TAG, "$old -> $new")
        if (old == GameState.ANIMATING && new == GameState.READY) {
            handleGameSolutions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        cellDirector = CellDirector(this)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        soundPool = SoundPool.Builder().build()
        noteSounds = arrayListOf(
            soundPool.load(requireContext(), R.raw.fs2, 1),
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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        binding = GameplayFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated")
        loadLevel()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun loadLevel() {
        Log.i(TAG, "loadLevel")
        gameViewModel.level
            .observe(viewLifecycleOwner, { levelAndLettersInfo ->
                if (levelAndLettersInfo != null) {
                    if (binding.gameplayFragment.childCount > 0) {
                        clearPreviousLevel()
                        return@observe
                    }

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
                        if (levelAndLettersInfo.lettersProperties.isEmpty()) {
                            gameViewModel.persistInitialLettersInfo(cellDirector.cells)
                        }
                    }
                    binding.gameplayFragment.addView(level)
                    Log.i(TAG, "Added level: ${levelAndLettersInfo.levelInfo.solution}")
                    handleGameSolutions()
                    gameState = GameState.READY
                }
            })
    }

    private fun clearPreviousLevel() {
        Log.i(TAG, "clearPreviousLevel")
        gameViewModel.realSolutionAndGuess.postValue(Pair("", "2"))
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
        Log.i(TAG, "handleGameSolutions")
        val it = gameViewModel.realSolutionAndGuess.value
        if (it != null && gameState == GameState.READY && it.first?.length == it.second?.length) {
            val playerSolution = it.second?.map { char ->
                if ("^([?!.,’])".contains(char)) "" else char
            }?.joinToString("")
            Log.i(TAG, playerSolution.toString())
            when {
                it.first == it.second -> {
                    Log.i(TAG, "Solution is correct! ${it.first}, ${it.second}")
                    gameState = GameState.COMPLETED
                    playCorrectSolutionAnimation()
                }
                playerSolution?.reversed() == playerSolution -> {
                    Log.i(
                        TAG,
                        "It is a palindrome, but not the correct one :( ${it.first}, ${it.second}"
                    )
                    playWrongSolutionAnimation(isPalindrome = true)
                }
                else -> {
                    Log.i(TAG, "It is not the correct solution ${it.first}, ${it.second}")
                    playWrongSolutionAnimation(isPalindrome = false)
                }
            }
        }
    }

    private fun playCorrectSolutionAnimation() {
        val successTextColor =
            ResourcesCompat.getColor(resources, R.color.successTextColor, requireContext().theme)
        val currentBoxColor =
            ResourcesCompat.getColor(resources, R.color.colorSecondary, requireContext().theme)
        val boxSuccessColor =
            ResourcesCompat.getColor(resources, R.color.successBoxColor, requireContext().theme)

        val animatorSet = AnimationFactory.getCorrectSolutionAnimation(
            successTextColor,
            currentBoxColor,
            boxSuccessColor,
            cellDirector.cells,
            audioManager,
            soundPool,
            noteSounds,
        )

        animatorSet.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if(gameViewModel.isLastLevel()) {
                    soundPool.play(
                        noteSounds[11], 1.0f, 1.0f, 2, 0, 1.0f
                    )
                }
                val nextLevel = gameViewModel.level.value?.levelInfo?.levelId?.plus(1)
                nextLevel?.let { gameViewModel.changeLevel(it) }
            }
        })
        animatorSet.start()
    }

    private fun playWrongSolutionAnimation(isPalindrome: Boolean = false) {
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
        if (isPalindrome) {
            MessageDialogFragment(
                title = getString(R.string.palindrome_found),
                message = getString(R.string.palindrome_but_not_correct)
            ).show(childFragmentManager, MessageDialogFragment.TAG)
        }
    }

    private fun doubleClickVibration() {
        val doubleClickPattern = longArrayOf(0, 50, 50, 50)
        val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
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
