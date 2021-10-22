package com.indest.heh.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.indest.heh.LevelFactory
import com.indest.heh.R
import com.indest.heh.databinding.GameplayFragmentBinding
import com.indest.heh.game.GameState
import com.indest.heh.game.GameViewModel
import com.indest.heh.letter.*
import com.indest.heh.letter.effects.ColorSelector
import com.indest.heh.persistance.dto.Level
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GamePlayFragment : Fragment(R.layout.gameplay_fragment) {

    private val TAG = "GamePlayFragment"
    private lateinit var binding: GameplayFragmentBinding
    var cellDirector: CellDirector = CellDirector(this)
    val gameViewModel: GameViewModel by viewModels()
    var gameState: GameState = GameState.BUSY

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
        Log.i(TAG, viewLifecycleOwner.lifecycle.currentState.toString())
        gameViewModel.level
            .observe(viewLifecycleOwner, { levelAndLettersInfo ->
                Log.i(TAG, levelAndLettersInfo.toString())
                val level = LevelFactory.getLevel(
                    levelAndLettersInfo,
                    requireContext(),
                    layoutInflater,
                    binding.gameplayFragment,
                    lifecycleScope
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
            })
    }

    fun checkSolution() {
        val boxes = cellDirector.cells
        val letters = boxes.filterIsInstance<Slot>().mapNotNull {
            if (it.getTheFriend()?.text?.contains("^([?!.,’])".toRegex()) == false) it.getTheFriend()
            else null
        }
        val odd = letters.size % 2 != 0
        val pairs = letters.foldIndexed(mutableListOf(), { index: Int,
                                                           acc: List<Pair<Letter, Any>>,
                                                           l: Letter ->
            if (index < letters.size / 2) acc + Pair(l, letters[letters.size - 1 - index])
            else if (index == letters.size / 2 && odd) acc + Pair(l, "*")
            else acc
        })

        val animatorSet = AnimatorSet()
        for (p in pairs) {
            val colorCurrent = p.first.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND).viewStart
            val textColorCurrent =
                p.first.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND).textStart
            val animations = animatorSet.childAnimations
            if (p.second is Letter) {
                val color =
                    if (p.first.text == (p.second as Letter).text) Color.GREEN else Color.BLACK
                val textColor =
                    if (p.first.text == (p.second as Letter).text) textColorCurrent else Color.WHITE
                val a1 =
                    ObjectAnimator.ofArgb(p.first, "backgroundColor", colorCurrent, color).apply {
                        interpolator = FastOutLinearInInterpolator()
                        duration = 350
                    }
                val a2 =
                    ObjectAnimator.ofArgb(p.second, "backgroundColor", colorCurrent, color).apply {
                        interpolator = FastOutLinearInInterpolator()
                        duration = 350
                    }
                val a1t =
                    ObjectAnimator.ofArgb(p.first, "textColor", textColorCurrent, textColor).apply {
                        interpolator = FastOutLinearInInterpolator()
                        duration = 350
                    }
                val a2t = ObjectAnimator.ofArgb(p.second, "textColor", textColorCurrent, textColor)
                    .apply {
                        interpolator = FastOutLinearInInterpolator()
                        duration = 350
                    }
                if (animations.size > 0) animatorSet.play(a1).with(a2).with(a1t).with(a2t)
                    .after(animations.last())
                else animatorSet.playTogether(a1, a2, a1t, a2t)
                if (color == Color.BLACK) break
            } else {
                val a = ObjectAnimator.ofArgb(p.first, "backgroundColor", colorCurrent, Color.GREEN)
                    .apply {
                        interpolator = FastOutLinearInInterpolator()
                        duration = 350
                    }
                if (animations.size > 0) animatorSet.play(a).after(animations.last())
                else animatorSet.playTogether(a)
            }
        }

        Log.i(TAG, "Animations: ${animatorSet.childAnimations}")
        animatorSet.start()
    }

}
