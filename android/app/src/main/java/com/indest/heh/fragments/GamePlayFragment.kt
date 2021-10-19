package com.indest.heh.fragments

import android.os.Bundle
import android.view.*
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.indest.heh.LevelFactory
import com.indest.heh.R
import com.indest.heh.databinding.GameplayFragmentBinding
import com.indest.heh.letter.*
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GamePlayFragment: Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    @Inject
    lateinit var boxDirector: BoxDirector
    private lateinit var binding: GameplayFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        boxDirector.registerLifeCycleOwner(lifecycle)
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
        setFragmentResultListener("requestKey") { _, bundle ->
            val levelNr = bundle.getInt("bundleKey")
            loadLevel(levelNr)
        }
        return binding.root
    }

    private fun loadLevel(levelNr: Int) {
        // Level loading & displaying calls
        lifecycleScope.launchWhenCreated {
            boxDirector.levelRepository.insertLevel(
                LevelData(1, "WOW", "WWO", "Surprise", LevelProgress.TODO, 0)
            )
            boxDirector.levelRepository.insertLevel(
                LevelData(2, "DAD", "ADD", "Parent", LevelProgress.TODO, 0)
            )
            val levelAndLettersInfo = boxDirector.levelRepository.getLevel(levelNr)
            boxDirector.currentLevelInfo = levelAndLettersInfo.levelInfo
            displayLevel(levelAndLettersInfo)
        }
    }

    private fun displayLevel(info: Level) {
        val level = LevelFactory.getLevel(
            info,
            boxDirector,
            requireContext(),
            layoutInflater,
            binding.gameplayFragmentContainer,
            lifecycleScope
        )
        binding.gameplayFragmentContainer.addView(level)
    }
}