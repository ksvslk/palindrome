package com.indest.hah.game

import android.animation.*
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.view.animation.AccelerateInterpolator
import com.indest.hah.letter.Cell
import com.indest.hah.letter.Letter
import com.indest.hah.util.playWhileSoundOn

object AnimationFactory {

    private fun getCellColorAnimation(
        activity: Context,
        letter: Letter,
        currentColor: Int,
        successColor: Int,
        aDuration: Long,
        index: Int,
        audioManager: AudioManager,
        soundPool: SoundPool,
        lockedColor:Int,
        soundEffect:Boolean = true
    ): ObjectAnimator {
        val cColor = if (letter.isLocked) lockedColor else currentColor
        return ObjectAnimator.ofArgb(
            letter,
            "backgroundColor",
            cColor,
            successColor
        ).apply {
            setEvaluator(ArgbEvaluator())
            duration = aDuration
            if (soundEffect)
            {
                if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    this.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            soundPool.playWhileSoundOn(activity, index, 1, audioManager = audioManager)
                        }
                    })
                }
            }
        }
    }

    private fun getTextColorAnimation(
        letter: Letter,
        successTextColor: Int,
        aDuration: Long,
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
        activity: Context,
        successTextColor: Int,
        currentBoxColor: Int,
        boxSuccessColor: Int,
        cells: MutableList<Cell<*, *>>,
        audioManager: AudioManager,
        soundPool: SoundPool,
        noteSounds: List<Int>,
        lockedColor:Int
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
        //val animationDuration = (kotlin.math.log(50000.0, letters.size.toDouble()) * 100).toLong()
        val animationDuration = 400L

        for (i in 0 until count) {
            val animations = animatorSet.childAnimations
            val letterAhead = letters[i]
            val letterBehind = letters[letters.size - 1 - i]
            val sound = if (i == count - 1) noteSounds[11] else noteSounds[i]

            val letterAheadTextColorAnimation = getTextColorAnimation(
                letterAhead,
                successTextColor,
                animationDuration,
            )
            val letterAheadBoxColorAnimation = getCellColorAnimation(
                activity,
                letterAhead,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                sound,
                audioManager,
                soundPool,
                lockedColor,
                soundEffect = true
            )
            val letterBehindTextColorAnimation = getTextColorAnimation(
                letterBehind,
                successTextColor,
                animationDuration,
            )
            val letterBehindBoxColorAnimation = getCellColorAnimation(
                activity,
                letterBehind,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                sound,
                audioManager,
                soundPool,
                lockedColor,
                soundEffect = false
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
            )
            val letterBoxColorAnimation = getCellColorAnimation(
                activity,
                letter,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                noteSounds[count],
                audioManager,
                soundPool,
                lockedColor,
                soundEffect = true
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
            )
            val letterAnimation = getCellColorAnimation(
                activity,
                letter,
                currentBoxColor,
                boxSuccessColor,
                animationDuration,
                noteSounds[count + 1],
                audioManager,
                soundPool,
                lockedColor,
                soundEffect = false
            )
            animatorSet
                .play(letterTextAnimation)
                .with(letterAnimation)
                .with(animations.last())
        }

        return animatorSet
    }

}