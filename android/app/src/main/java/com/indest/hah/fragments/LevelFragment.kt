package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.indest.hah.R
import com.indest.hah.components.Animations
import com.indest.hah.components.LevelCreator
import com.indest.hah.components.SoundPlayer
import com.indest.hah.databinding.LevelFragmentBinding
import com.indest.hah.game.CellDirector
import com.indest.hah.game.LevelViewModel
import com.indest.hah.game.LevelViewModel.Solution.*
import com.indest.hah.game.MessageSnackBar
import com.indest.hah.letter.Cell
import com.indest.hah.persistance.dto.Level
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
    lateinit var cellDirector: CellDirector

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
        levelUiUpdater()
        solutionUiUpdater()
    }

    private fun levelUiUpdater() {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                WindowInfoTracker.getOrCreate(requireContext())
                    .windowLayoutInfo(requireActivity())
                    .collect { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookMode = foldingFeature?.state == FoldingFeature.State.FLAT
                        viewModel.isBookMode = isBookMode
                        viewModel.latestLevelFlow().collect { latestLevel ->
                            if (binding.levelFragment.childCount == 0) {
                                initializeLevel(latestLevel, isBookMode)
                            } else {
                                binding.levelFragment.removeAllViewsInLayout()
                                initializeLevel(latestLevel, isBookMode)
                            }
                        }
                    }
            }
        }
    }

    private fun initializeLevel(level: Level, isBookMode: Boolean) {
        val levelGridLayout = levelCreator.getLevel(
            level,
            layoutInflater,
            binding.levelFragment,
            isBookMode,
            viewModel.windowSizeClassWidth
        )
        levelGridLayout.children.filterIsInstance<Cell<*, *>>().apply {
            viewModel.persistInitialLettersInfo(this)
            levelGridLayout.doOnPreDraw {
                cellDirector = CellDirector(
                    level,
                    this,
                    viewModel
                )
            }
        }
        binding.levelFragment.addView(levelGridLayout)
    }

    private fun solutionUiUpdater() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.solution.collect {
                    when (it.first) {
                        CORRECT -> {
                            Log.d(TAG, "CORRECT")
                        }
                        WRONG -> {
                            cellDirector.sendWrongSolutionLetterHome(it.second)
                            updateUiWhenWrongMessage(WRONG)
                        }
                        WRONG_PALIDROME -> {
                            cellDirector.sendWrongSolutionLetterHome(it.second)
                            updateUiWhenWrongMessage(WRONG_PALIDROME)
                        }
                        NONE -> {
                            Log.d(TAG, "NONE")
                        }
                    }
                }
            }
        }
    }

    private fun updateUiWhenWrongMessage(solutionType: LevelViewModel.Solution)
    {
        animations.getWrongSolutionAnimation(binding.levelFragment).start()
        soundPlayer.vibrateDoubleClick()
        val messageToDisplay =
            if (solutionType == WRONG_PALIDROME)
                resources.getStringArray(R.array.palindrome_wrong_texts).random()
            else
                resources.getStringArray(R.array.solution_wrong_texts).random()

        val message = MessageSnackBar.make(
            binding.levelFragment,
            messageToDisplay,
            2000,
            viewModel.windowSizeClassWidth,
            viewModel.isBookMode,
            MessageSnackBar.Companion.TextSize.SMALL,
            false
        )
        message.show()
    }

}