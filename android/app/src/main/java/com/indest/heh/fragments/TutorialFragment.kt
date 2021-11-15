package com.indest.heh.fragments

import android.animation.*
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Space
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.heh.LevelFactory
import com.indest.heh.R
import com.indest.heh.databinding.TutorialFragmentBinding
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter
import com.indest.heh.persistance.LevelProgress
import com.indest.heh.persistance.dto.Level
import com.indest.heh.persistance.dto.LevelData
import kotlin.math.abs
import kotlin.math.roundToInt

class TutorialFragment : Fragment() {

    private lateinit var binding: TutorialFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TutorialFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView).visibility =
            View.INVISIBLE

        binding.continueButton.setOnClickListener {
            parentFragmentManager.commit {
                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                setReorderingAllowed(true)
                replace<SecondTutorialFragment>(R.id.fragments_container)
            }
        }

        binding.skipButton.setOnClickListener {
            requireActivity().getPreferences(Context.MODE_PRIVATE)?.let {
                with(it.edit()) {
                    putBoolean(getString(R.string.tutorial_passed), true)
                    apply()
                }
            }
            parentFragmentManager.commit {
                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                setReorderingAllowed(true)
                replace<GamePlayFragment>(R.id.fragments_container)
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView).visibility =
                    View.VISIBLE
            }
        }
        view.doOnPreDraw {
            val rect = Rect()
            binding.letter1.getGlobalVisibleRect(rect)
            val rect2 = Rect()
            binding.hand.getGlobalVisibleRect(rect2)

            ObjectAnimator.ofFloat(
                binding.hand,
                "x",
                -(rect2.centerX() - rect.centerX()).toFloat()
            ).apply {
                duration = 1000
            }.start()

            ObjectAnimator.ofFloat(
                binding.hand,
                "y",
                -(rect2.centerY() - rect.centerY()).toFloat()
            ).apply {
                duration = 1000
            }.start()
            //val animation = getLetterAnimation(binding.letter1, binding.space1, binding.hand)
            //animation.start()
        }

    }


    private fun getLetterAnimation(
        letter: AppCompatTextView,
        space: AppCompatTextView,
        hand: ImageView
    ): AnimatorSet {
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(
                letter,
                "x",
                space.x
            ).apply {
                duration = 1000
            }, ObjectAnimator.ofFloat(
                letter,
                "y",
                space.y
            ).apply {
                duration = 1000
            },
            ObjectAnimator.ofFloat(
                hand,
                "x",
                space.x,
            ).apply {
                duration = 1000
            },
            ObjectAnimator.ofFloat(
                hand,
                "y",
                space.y
            ).apply {
                duration = 1000
            }

        )
        return animatorSet
    }
}