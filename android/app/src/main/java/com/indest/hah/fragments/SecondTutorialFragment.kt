package com.indest.hah.fragments

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.SecondTutorialFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel

class SecondTutorialFragment : BottomSheetDialogFragment() {
    private var _binding: SecondTutorialFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()
    private var mainAnimatorSet:AnimatorSet? = AnimatorSet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = SecondTutorialFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    override fun onDestroyView() {
        cleanUp()
        super.onDestroyView()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueButton.setOnClickListener {
            parentFragmentManager.popBackStack()
            parentFragmentManager.commit {
                cleanUp()
            /*    setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                add(ThirdTutorialFragment(), ThirdTutorialFragment().tag)
                addToBackStack(ThirdTutorialFragment().tag)*/
                levelViewModel.logTutorialComplete()
                parentFragmentManager.popBackStack()
            }
        }
        view.doOnLayout {
            repeatingTutorialAnimation()
        }
    }


    private fun cleanUp() {
        mainAnimatorSet?.childAnimations?.forEach {
            it.cancel()
            it.removeAllListeners()
        }
        mainAnimatorSet?.cancel()
        mainAnimatorSet?.removeAllListeners()
        _binding = null
    }

    private fun repeatingTutorialAnimation() {
        val frameFadeInAnimation = getAlphaAnimator(binding.tutorialGameplay, 0f, 1f, 500)

        val handToStartPosition = getPositionAnimator(what = binding.hand, to = binding.letter1)
        val letterToCenter = getPositionAnimator(what = binding.letter1, to = binding.space2)
        val handToCenter = getPositionAnimator(what = binding.hand, to = binding.space2)

        val handToSecondLetter = getPositionAnimator(what = binding.hand, to = binding.letter2)
        val secondLetterToFirstPos =
            getPositionAnimator(what = binding.letter2, to = binding.space1)
        val handToFirstPos = getPositionAnimator(what = binding.hand, to = binding.space1)

        val handToThirdLetter = getPositionAnimator(what = binding.hand, to = binding.letter3)
        val thirdLetterToThirdPos = getPositionAnimator(what = binding.letter3, to = binding.space3)
        val handToThirdPos = getPositionAnimator(what = binding.hand, to = binding.space3)

        val hideHandAnimation = getAlphaAnimator(binding.hand, 1f, 0f)
        val blankAnimation = getAlphaAnimator(binding.hand, 0f, 0f, 2000)

        val r1Animation = getAlphaAnimator(binding.r1, 0f, 1f, 500)
        val r2Animation = getAlphaAnimator(binding.r2, 0f, 1f, 500)
        val r3Animation = getAlphaAnimator(binding.r3, 0f, 1f, 500)
        val l1Animation = getAlphaAnimator(binding.l1, 0f, 1f, 500)
        val l2Animation = getAlphaAnimator(binding.l2, 0f, 1f, 500)
        val l3Animation = getAlphaAnimator(binding.l3, 0f, 1f, 500)

        val handToStartAnimation = AnimatorSet().apply {
            play(handToStartPosition.first).with(handToStartPosition.second)
        }

        val firstLetterAnimation = AnimatorSet().apply {
            doOnStart {
                binding.hand.background.state = intArrayOf(R.attr.animating)
            }
            doOnEnd {
                binding.hand.background.state = intArrayOf(-R.attr.animating)
            }
            playTogether(
                letterToCenter.first,
                letterToCenter.second,
                handToCenter.first,
                handToCenter.second
            )
        }

        val handToSecondLetterAnimation = AnimatorSet().apply {
            playTogether(handToSecondLetter.first, handToSecondLetter.second)
        }

        val secondLetterAnimation = AnimatorSet().apply {
            doOnStart {
                binding.hand.background.state = intArrayOf(R.attr.animating)
            }
            doOnEnd {
                binding.hand.background.state = intArrayOf(-R.attr.animating)
            }
            playTogether(
                secondLetterToFirstPos.first,
                secondLetterToFirstPos.second,
                handToFirstPos.first,
                handToFirstPos.second
            )
        }

        val handToThirdLetterAnimation = AnimatorSet().apply {
            playTogether(handToThirdLetter.first, handToThirdLetter.second)
        }

        val thirdLetterAnimation = AnimatorSet().apply {
            doOnStart {
                binding.hand.background.state = intArrayOf(R.attr.animating)
            }
            doOnEnd {
                binding.hand.background.state = intArrayOf(-R.attr.animating)
            }
            playTogether(
                thirdLetterToThirdPos.first,
                thirdLetterToThirdPos.second,
                handToThirdPos.first,
                handToThirdPos.second
            )
        }

        val rightArrowsAnimation = AnimatorSet().apply {
            playSequentially(r1Animation, r2Animation, r3Animation)
        }

        val leftArrowsAnimation = AnimatorSet().apply {
            playSequentially(l1Animation, l2Animation, l3Animation)
        }

        mainAnimatorSet?.apply {
            playSequentially(
                frameFadeInAnimation,
                handToStartAnimation,
                firstLetterAnimation,
                handToSecondLetterAnimation,
                secondLetterAnimation,
                handToThirdLetterAnimation,
                thirdLetterAnimation,
                hideHandAnimation,
                rightArrowsAnimation,
                leftArrowsAnimation,
                blankAnimation
            )
        }

        mainAnimatorSet?.start()
    }

    private fun getAlphaAnimator(
        what: View,
        start: Float,
        end: Float,
        aDuration: Long = 1000
    ): Animator {
        val animator =  ObjectAnimator.ofFloat(
            what,
            View.ALPHA,
            start,
            end
        ).apply {
            duration = aDuration
        }
        return animator
    }

    private fun getPositionAnimator(what: View, to: View): Pair<Animator, Animator> {

        val toRect = Rect()
        to.getGlobalVisibleRect(toRect)
        val whatRect = Rect()
        what.getGlobalVisibleRect(whatRect)

        val animatorX = ObjectAnimator.ofFloat(
            what,
            View.TRANSLATION_X,
            (toRect.centerX() - whatRect.centerX()).toFloat()
        ).apply {
            duration = 1000
        }

        val animatorY = ObjectAnimator.ofFloat(
            what,
            View.TRANSLATION_Y,
            (toRect.centerY() - whatRect.centerY()).toFloat()
        ).apply {
            duration = 1000
        }

        return Pair(animatorX, animatorY)
    }
}