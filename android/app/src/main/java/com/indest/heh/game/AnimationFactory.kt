package com.indest.heh.game

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.view.animation.AccelerateInterpolator
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter

object AnimationFactory {

    private fun getCellColorAnimation(
        letter: Letter,
        currentColor: Int,
        successColor: Int,
        aDuration: Long
    ): ObjectAnimator {
        return ObjectAnimator.ofArgb(
            letter,
            "backgroundColor",
            currentColor,
            successColor
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = aDuration
        }
    }

    private fun getTextColorAnimation(
        letter: Letter,
        successTextColor: Int,
        aDuration: Long
    ): ObjectAnimator {
        return ObjectAnimator.ofInt(
            letter,
            "textColor",
            letter.currentTextColor,
            successTextColor
        ).apply {
            setEvaluator(ArgbEvaluator())
            interpolator = AccelerateInterpolator()
            duration = aDuration
        }
    }

    fun getCorrectSolutionAnimation(
        successTextColor: Int,
        currentBoxColor: Int,
        boxSuccessColor: Int,
        cells: MutableList<Cell<*, *>>
    ): AnimatorSet {
        val animatorSet = AnimatorSet()
        val specialChars = ArrayList<Letter>()
        val letters = cells
            .filterIsInstance<Letter>()
            .filter {
                if (it.text?.contains("^([?!.,’])".toRegex()) == false) {
                    true
                } else {
                    specialChars.add(it)
                    false
                }
            }
            .sortedBy { it.friendIndex }

        val odd = letters.size % 2 != 0
        val count = letters.size / 2
        val animationDuration = 3000L / (letters.size + specialChars.size) * 2

        for (i in 0 until count) {
            val animations = animatorSet.childAnimations
            val letterAhead = letters[i]
            val letterBehind = letters[letters.size - 1 - i]

            val letterAheadTextColorAnimation = getTextColorAnimation(
                letterAhead,
                successTextColor,
                animationDuration
            )
            val letterAheadBoxColorAnimation = getCellColorAnimation(
                letterAhead,
                currentBoxColor,
                boxSuccessColor,
                animationDuration
            )
            val letterBehindTextColorAnimation = getTextColorAnimation(
                letterBehind,
                successTextColor,
                animationDuration
            )
            val letterBehindBoxColorAnimation = getCellColorAnimation(
                letterBehind,
                currentBoxColor,
                boxSuccessColor,
                animationDuration
            )

            if (animations.size > 0) {
                animatorSet
                    .play(letterAheadTextColorAnimation)
                    .with(letterBehindTextColorAnimation)
                    .with(letterAheadBoxColorAnimation)
                    .with(letterBehindBoxColorAnimation)
                    .after(animations.last())
            } else {
                animatorSet.playTogether(
                    letterAheadTextColorAnimation,
                    letterBehindTextColorAnimation,
                    letterAheadBoxColorAnimation,
                    letterBehindBoxColorAnimation
                )
            }
        }

        if (odd) {
            val animations = animatorSet.childAnimations
            val letter = letters[count]
            val letterTextColorAnimation = getTextColorAnimation(
                letter,
                successTextColor,
                animationDuration
            )
            val letterBoxColorAnimation = getCellColorAnimation(
                letter,
                currentBoxColor,
                boxSuccessColor,
                animationDuration
            )

            if (animations.size > 0) {
                animatorSet
                    .play(letterTextColorAnimation)
                    .with(letterBoxColorAnimation)
                    .after(animations.last())
            } else {
                animatorSet.playTogether(letterTextColorAnimation, letterBoxColorAnimation)
            }
        }

        for (letter in specialChars) {
            val animations = animatorSet.childAnimations

            val letterTextAnimation = getTextColorAnimation(
                letter,
                successTextColor,
                animationDuration
            )
            val letterAnimation = getCellColorAnimation(
                letter,
                currentBoxColor,
                boxSuccessColor,
                animationDuration
            )
            animatorSet
                .play(letterTextAnimation)
                .with(letterAnimation)
                .after(animations.last())
        }

        return animatorSet
    }


}