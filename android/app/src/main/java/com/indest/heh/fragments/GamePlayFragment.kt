package com.indest.heh.fragments

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
import androidx.fragment.app.commit
import androidx.fragment.app.replace


@AndroidEntryPoint
class GamePlayFragment : Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    private lateinit var binding: GameplayFragmentBinding
    var cellDirector: CellDirector = CellDirector(this)
    val gameViewModel: GameViewModel by activityViewModels()
    var gameState: GameState = GameState.BUSY
    private lateinit var soundPool: SoundPool
    private var levelCompleteSound by Delegates.notNull<Int>()
    private lateinit var audioManager:AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        soundPool = SoundPool.Builder().build()
        levelCompleteSound = soundPool.load(requireContext(), R.raw.level_complete, 1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        postponeEnterTransition()
        binding = GameplayFragmentBinding.inflate(layoutInflater, container, false)
        binding.root.doOnPreDraw {
            startPostponedEnterTransition()
        }
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        loadLevel()
        return binding.root
    }

    private fun loadLevel() {

        if(binding.gameplayFragment.childCount > 0) {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<GamePlayFragment>(R.id.fragments_container)
            }
        }

        gameViewModel.level
            .observe(viewLifecycleOwner, { levelAndLettersInfo ->
                if(levelAndLettersInfo != null)
                {
                    val level = LevelFactory.getLevel(
                        levelAndLettersInfo,
                        requireContext(),
                        layoutInflater,
                        binding.gameplayFragment
                    )
                    cellDirector.cells = level.children.filterIsInstance<Cell<*, *>>().toMutableList()
                    level.doOnPreDraw {
                        cellDirector.restoreSlotPlacements(levelAndLettersInfo)
                        if (levelAndLettersInfo.lettersProperties.isEmpty()) gameViewModel.persistInitialLettersInfo(
                            cellDirector.cells
                        )
                    }
                    binding.gameplayFragment.addView(level)
                    gameState = GameState.READY
                    handleGameSolutions()
                }

            })
    }

    private fun handleGameSolutions() {
        gameViewModel.realSolutionAndGuess.observe(viewLifecycleOwner, { it ->
            Log.i(TAG, it.toString())
            if (it.first?.length == it.second?.length) {
                val playerSolution = it.second?.map {
                        char -> if("^([?!.,’])".contains(char)) "" else char
                }?.joinToString("")
                Log.i(TAG, playerSolution.toString())
                when {
                    it.first == it.second -> {
                        Log.i(TAG, "Solution is correct! ${it.first}, ${it.second}")
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
        })
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
            cellDirector.cells
        )

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            soundPool.play(levelCompleteSound, 1.0f, 1.0f, 1, 0, 1.0f)
        }
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
