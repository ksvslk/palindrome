package com.indest.hah.fx

import android.animation.*
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.transition.Transition
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.views.CellContainer
import com.indest.hah.views.MessageSnackBar
import com.indest.hah.views.Cell
import com.indest.hah.views.Letter
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityScoped
class Animations @Inject constructor(
    private val soundPlayer: SoundPlayer,
    private val colors: Colors,
    @ActivityContext private val context: Context
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
                override fun onAnimationStart(animation: Animator) {
                    soundPlayer.playNote(index)
                    super.onAnimationStart(animation)
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
            .toMutableList()

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
                .after(animations.last())
        }

        return animatorSet
    }

    fun getLayoutTransition(): LayoutTransition {
        val scaleDown: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)
        )
        scaleDown.duration = 300
        scaleDown.interpolator = DecelerateInterpolator()
        val scaleUp: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)
        )
        scaleUp.duration = 300
        scaleUp.startDelay = 300
        scaleUp.interpolator = OvershootInterpolator(1.2f)

        val itemLayoutTransition = LayoutTransition()
        //itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp)
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, scaleDown)

        return itemLayoutTransition
    }

    fun getLayoutTransitionFade(): LayoutTransition {
   /*     val fadeOut: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
        )
        fadeOut.duration = 300
        fadeOut.interpolator = DecelerateInterpolator()*/
        val fadeIn: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
        )
        fadeIn.duration = 300
        fadeIn.interpolator = AccelerateInterpolator()

        val itemLayoutTransition = LayoutTransition()
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, fadeIn)
        //itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, fadeOut)

        return itemLayoutTransition
    }

    fun getTrainAnimation(fixed:Boolean = false, fixedValue:Int = 20): LinearLayoutCompat {
        val activity = (context as MainActivity)
        val trainLinearLayout = activity.findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)
        trainLinearLayout.alpha = 1f
        trainLinearLayout.removeAllViews()
        val lastCarriage = arrayOf(
            R.drawable.ic_carriage_last,
            R.drawable.ic_carriage_last_blue,
            R.drawable.ic_carriage_last_green,
            R.drawable.ic_carriage_last_yellow
        ).random()

        val centerCarriages = arrayOf(
            R.drawable.ic_carriage_center,
            R.drawable.ic_carriage_center_blue,
            R.drawable.ic_carriage_center_green,
            R.drawable.ic_carriage_center_yellow
        )
        val numberOfCarriages = if (!fixed) (1..5).random() else fixedValue

        for (i in 1..numberOfCarriages) {
            if (i == 1) {
                activity.layoutInflater.inflate(
                    R.layout.carriage_last,
                    trainLinearLayout,
                    false
                ).also {
                    it.setBackgroundResource(lastCarriage)
                    trainLinearLayout.addView(it)
                }
            } else {
                activity.layoutInflater.inflate(
                    R.layout.carriage_center,
                    trainLinearLayout,
                    false
                )
                    .also {
                        val randomCarriage = centerCarriages.random()
                        it.setBackgroundResource(randomCarriage)
                        trainLinearLayout.addView(it)
                    }
            }
        }

        activity.layoutInflater.inflate(R.layout.locomotive_view, trainLinearLayout, false)
            .apply {
                (this.background as AnimationDrawable).start()
                trainLinearLayout.addView(this)
            }

        return trainLinearLayout
    }

    fun getScoreAnimation(scoreView: TextView, score: Int, maxScore: Int): ValueAnimator {
        val currentValue = 0
        val animator =
            ValueAnimator.ofInt(currentValue, currentValue.plus(score))
        val paddingLength = if (score > 0) maxScore.toString().length else 0
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            scoreView.text = StringBuilder()
                .append(animation.animatedValue.toString().padStart(paddingLength, '0'))
                .append(" of $maxScore points")
        }
        if (score > 0) soundPlayer.play(soundPlayer.sounds.scoreSound, volume = 0.2f)
        return animator
    }

    fun getRainConfetti(): List<Party> {
        return listOf(
            Party(
                speed = 0f,
                maxSpeed = 15f,
                damping = 0.9f,
                angle = Angle.BOTTOM,
                spread = Spread.SMALL,
                colors = listOf(0xff0000, 0x2196F3, 0xCDDC39, 0xFFC107),
                emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(25),
                position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
            )
        )
    }

    fun getParade(): List<Party> {
        val party = Party(
            speed = 30f,
            maxSpeed = 40f,
            damping = 0.9f,
            angle = Angle.RIGHT - 60,
            spread = Spread.SMALL,
            colors = listOf(0xff0000, 0x2196F3, 0xCDDC39, 0xFFC107),
            emitter = Emitter(duration = 60, TimeUnit.SECONDS).perSecond(8),
            position = Position.Relative(0.0, 0.4)
        )

        return listOf(
            party,
            party.copy(
                angle = Angle.LEFT + 60, // flip angle from right to left
                position = Position.Relative(1.0, 0.4)
            ),
        )
    }

    fun getWrongSolutionAnimation(container: CellContainer): AnimatorSet {
        val animatorSet = AnimatorSet()
        val shakeX = ObjectAnimator
            .ofFloat(
                container,
                "translationX",
                0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
            ).apply {
                duration = 1000
            }
        animatorSet.play(shakeX)
        return animatorSet
    }

    fun getContinueAnimation(tapToContinue: MessageSnackBar): ObjectAnimator {
        val tapToContinueAnimation =
            ObjectAnimator.ofFloat(tapToContinue.view, View.ALPHA, 1f, 0.5f)
        tapToContinueAnimation.duration = 1000
        tapToContinueAnimation.repeatCount = ObjectAnimator.INFINITE
        tapToContinueAnimation.repeatMode = ObjectAnimator.REVERSE
        return tapToContinueAnimation
    }

}