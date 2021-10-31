package com.indest.heh.game

import android.animation.*
import android.media.AudioManager
import android.media.SoundPool
import android.view.animation.AccelerateInterpolator
import com.indest.heh.letter.Cell
import com.indest.heh.letter.Letter

object AnimationFactory {

    private fun getCellColorAnimation(
        letter: Letter,
        currentColor: Int,
        successColor: Int,
        aDuration: Long,
        index:Int,
        audioManager: AudioManager,
        soundPool: SoundPool
    ): ObjectAnimator {
        return ObjectAnimator.ofArgb(
            letter,
            "backgroundColor",
            currentColor,
            successColor
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = aDuration
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                this.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        soundPool.play(index, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                })
            }
        }
    }

    private fun getTextColorAnimation(
        letter: Letter,
        successTextColor: Int,
        aDuration: Long,
        index:Int,
        audioManager: AudioManager,
        soundPool: SoundPool
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
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                this.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        soundPool.play(index, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                })
            }
        }
    }

    fun getCorrectSolutionAnimation(
        successTextColor: Int,
        currentBoxColor: Int,
        boxSuccessColor: Int,
        cells: MutableList<Cell<*, *>>,
        audioManager: AudioManager,
        soundPool: SoundPool,
        noteSounds: List<Int>
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
        val animationDuration = (
                kotlin.math.log( 50000.0,letters.size.toDouble())*100).toLong()

        for (i in 0 until count) {
            val animations = animatorSet.childAnimations
            val letterAhead = letters[i]
            val letterBehind = letters[letters.size - 1 - i]

            val letterAheadTextColorAnimation = getTextColorAnimation(
                letterAhead,
                successTextColor,
                animationDuration,
                noteSounds[i],
                audioManager,
                soundPool
            )
            val letterAheadBoxColorAnimation = getCellColorAnimation(
                letterAhead,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                noteSounds[i],
                audioManager,
                soundPool
            )
            val letterBehindTextColorAnimation = getTextColorAnimation(
                letterBehind,
                successTextColor,
                animationDuration,
                noteSounds[i],
                audioManager,
                soundPool
            )
            val letterBehindBoxColorAnimation = getCellColorAnimation(
                letterBehind,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                noteSounds[i],
                audioManager,
                soundPool
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
                animationDuration,
                noteSounds[count],
                audioManager,
                soundPool
            )
            val letterBoxColorAnimation = getCellColorAnimation(
                letter,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                noteSounds[count],
                audioManager,
                soundPool
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
                animationDuration,
                noteSounds[count+1],
                audioManager,
                soundPool
            )
            val letterAnimation = getCellColorAnimation(
                letter,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                noteSounds[count+1],
                audioManager,
                soundPool
            )
            animatorSet
                .play(letterTextAnimation)
                .with(letterAnimation)
                .with(animations.last())
        }

        return animatorSet
    }

}