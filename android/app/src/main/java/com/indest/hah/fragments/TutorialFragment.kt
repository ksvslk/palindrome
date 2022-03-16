package com.indest.hah.fragments

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.TutorialFragmentBinding


class TutorialFragment : BottomSheetDialogFragment() {

    private val TAG = "TutorialFragment"
    private var _binding: TutorialFragmentBinding? = null
    private val binding get() = _binding!!
    private val mainAnimatorSet = AnimatorSet()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = TutorialFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onDestroyView() {
        cleanUp()
        super.onDestroyView()
    }

    private fun cleanUp()
    {
        mainAnimatorSet.end()
        mainAnimatorSet.childAnimations.forEach {
            it.end()
            it.removeAllListeners()
        }
        mainAnimatorSet.removeAllListeners()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueButton.setOnClickListener {
            parentFragmentManager.popBackStack()
           parentFragmentManager.commit {
                cleanUp()
                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
               add(SecondTutorialFragment(), SecondTutorialFragment().tag)
               addToBackStack(SecondTutorialFragment().tag)
            }
        }
        view.doOnLayout {
            repeatingTutorialAnimation()
        }
    }

    private fun repeatingTutorialAnimation()
    {
        val blankAnimation = getAlphaAnimator(binding.letter1, 1f, 1f, 2000)

        val r1Animation = getAlphaAnimator(binding.r1, 0f, 1f)
        val r2Animation = getAlphaAnimator(binding.r2, 0f, 1f)
        val r3Animation = getAlphaAnimator(binding.r3, 0f, 1f)
        val l1Animation = getAlphaAnimator(binding.l1, 0f, 1f)
        val l2Animation = getAlphaAnimator(binding.l2, 0f, 1f)
        val l3Animation = getAlphaAnimator(binding.l3, 0f, 1f)

        val r1FadeAnimation = getAlphaAnimator(binding.r1, 1f, 0f, 500)
        val r2FadeAnimation = getAlphaAnimator(binding.r2, 1f, 0f, 500)
        val r3FadeAnimation = getAlphaAnimator(binding.r3, 1f, 0f, 500)
        val l1FadeAnimation = getAlphaAnimator(binding.l1, 1f, 0f, 500)
        val l2FadeAnimation = getAlphaAnimator(binding.l2, 1f, 0f, 500)
        val l3FadeAnimation = getAlphaAnimator(binding.l3, 1f, 0f, 500)


        val rightArrowsAnimation = AnimatorSet().apply {
            playSequentially(r1Animation, r2Animation, r3Animation)
        }

        val leftArrowsAnimation = AnimatorSet().apply {
            playSequentially(l1Animation, l2Animation, l3Animation)
        }

        val rightArrowsFadeAnimation = AnimatorSet().apply {
            playSequentially(r1FadeAnimation, r2FadeAnimation, r3FadeAnimation)
        }

        val leftArrowsFadeAnimation = AnimatorSet().apply {
            playSequentially(l1FadeAnimation, l2FadeAnimation, l3FadeAnimation)
        }

        mainAnimatorSet.apply {
            playSequentially(
                rightArrowsAnimation,
                leftArrowsAnimation,
                blankAnimation,
                leftArrowsFadeAnimation,
                rightArrowsFadeAnimation,
                )
        }

        mainAnimatorSet.doOnEnd {
            it.start()
        }
        mainAnimatorSet.start()
    }

    private fun getAlphaAnimator(what: View, start:Float, end:Float, aDuration:Long = 1000):Animator
    {
        return ObjectAnimator.ofFloat(
            what,
            View.ALPHA,
            start,
            end
        ).apply {
            duration = aDuration
        }
    }

}