package com.indest.hah.components

import android.animation.*
import android.view.animation.AccelerateInterpolator
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import javax.inject.Inject

class Animations @Inject constructor(
    private val soundPlayer: SoundPlayer,
    private val colors: Colors,
) {

    private val animationDuration: Long = 400L

    private fun getCellColorAnimation(
        letter: Letter,
        index: Int
    ): ObjectAnimator {
        val currentBoxColor = if (letter.isLocked) colors.lockedColor else colors.currentBoxColor
        return ObjectAnimator.ofArgb(
            letter,
            "backgroundColor",
            currentBoxColor,
            colors.boxSuccessColor
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = animationDuration
            this.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    soundPlayer.playNote(index)
                }
            })
        }
    }

    private fun getTextColorAnimation(
        letter: Letter,
    ): ObjectAnimator {
        return ObjectAnimator.ofInt(
            letter,
            "textColor",
            letter.currentTextColor,
            colors.successTextColor
        ).apply {
            setEvaluator(ArgbEvaluator())
            interpolator = AccelerateInterpolator()
            duration = animationDuration
        }
    }

    fun getCorrectSolutionAnimation(
        cells: MutableList<Cell<*, *>>,
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

        for (i in 0 until count) {
            val animations = animatorSet.childAnimations
            val letterAhead = letters[i]
            val letterBehind = letters[letters.size - 1 - i]

            val noteSoundId = if (i == count - 1) 11 else i

            val letterAheadTextColorAnimation = getTextColorAnimation(
                letterAhead
            )
            val letterAheadBoxColorAnimation = getCellColorAnimation(
                letterAhead,
                noteSoundId
            )
            val letterBehindTextColorAnimation = getTextColorAnimation(
                letterBehind,
            )
            val letterBehindBoxColorAnimation = getCellColorAnimation(
                letterBehind,
                noteSoundId
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
            )
            val letterBoxColorAnimation = getCellColorAnimation(
                letter,
                count,
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
            )
            val letterAnimation = getCellColorAnimation(
                letter,
                count + 1
            )
            animatorSet
                .play(letterTextAnimation)
                .with(letterAnimation)
                .with(animations.last())
        }

        return animatorSet
    }

}