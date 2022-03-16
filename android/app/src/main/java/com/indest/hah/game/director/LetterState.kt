package com.indest.hah.game.director

import android.animation.ObjectAnimator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.findFragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.indest.hah.R
import com.indest.hah.fragments.LevelFragment
import com.indest.hah.game.director.enums.ColorSelector
import com.indest.hah.views.Letter
import com.indest.hah.other.C.NEUTRAL_LAYER
import com.indest.hah.other.C.TOP_LAYER
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.log2

const val TAG = "LetterState"

/**
 * State of Letter objects and all the behaviour they can execute in each state
 * is restricted with this special enum class.
 */

enum class LetterState : ILetterState {
    HOME {
        override fun draw(letter: Letter) {

        }

        override fun executeActionDownBehaviour(letter: Letter, view: View, event: MotionEvent) {}
        override fun executeActionMoveBehaviour(letter: Letter, view: View, event: MotionEvent) {}
        override fun executeActionUpBehaviour(letter: Letter) {}
        override fun executeAnimationBehaviour(
            letter: Letter,
            moveByX: Int,
            moveByY: Int,
            durationModifier: Int
        ) {
        }
    },
    DRAGGING {
        override fun draw(letter: Letter) {}
        override fun executeActionDownBehaviour(letter: Letter, view: View, event: MotionEvent) {

            // Calculate the distance moved
            letter.dX = view.x - event.rawX
            letter.dY = view.y - event.rawY

            // View and its parent should be always on top when dragging
            letter.translationZ = TOP_LAYER

            val color = if (letter.hasFriend()) {
                letter.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND)
            } else {
                letter.effects.getColors(ColorSelector.ACTION_DOWN_NO_FRIEND)
            }

            animateNow(
                letter,
                "backgroundColor",
                color.viewStart,
                color.viewEnd,
                letter.effects.onClickColorChangeDuration
            )
            animateNow(
                letter,
                "textColor",
                color.textStart,
                color.textEnd,
                letter.effects.onClickColorChangeDuration
            )

            letter.animate().start()
        }

        override fun executeActionMoveBehaviour(letter: Letter, view: View, event: MotionEvent) {
            view.x = event.rawX + letter.dX
            view.y = event.rawY + letter.dY
        }

        override fun executeActionUpBehaviour(letter: Letter) {}
        override fun executeAnimationBehaviour(
            letter: Letter,
            moveByX: Int,
            moveByY: Int,
            durationModifier: Int
        ) {
        }
    },
    RESOLVING {
        override fun draw(letter: Letter) {}
        override fun executeActionDownBehaviour(letter: Letter, view: View, event: MotionEvent) {}
        override fun executeActionMoveBehaviour(letter: Letter, view: View, event: MotionEvent) {}
        override fun executeActionUpBehaviour(letter: Letter) {
            try {
                letter.findFragment<LevelFragment>().cellDirector?.handleCollisions(letter)
            }
            catch (e:IllegalStateException)
            {
                Log.e(TAG, e.message.toString())
            }
        }

        override fun executeAnimationBehaviour(
            letter: Letter,
            moveByX: Int,
            moveByY: Int,
            durationModifier: Int
        ) {
        }
    },
    ANIMATION {
        override fun draw(letter: Letter) {}
        override fun executeActionDownBehaviour(letter: Letter, view: View, event: MotionEvent) {}
        override fun executeActionMoveBehaviour(letter: Letter, view: View, event: MotionEvent) {}
        override fun executeActionUpBehaviour(letter: Letter) {}
        override fun executeAnimationBehaviour(
            letter: Letter,
            moveByX: Int,
            moveByY: Int,
            durationModifier: Int
        ) {

            val color = if (letter.hasFriend()) {
                letter.effects.getColors(ColorSelector.ACTION_UP_FRIEND)
            } else {
                letter.effects.getColors(ColorSelector.ACTION_UP_NO_FRIEND)
            }

            val endColor = if (durationModifier == 1) {
                color.viewEnd
            } else {
                ContextCompat.getColor(letter.context, R.color.letterRewardedColor)
            }

            val endTextColor = if (durationModifier == 1) {
                color.textEnd
            } else {
                ContextCompat.getColor(letter.context, R.color.textOnRewardedColor)
            }

            val onReleaseDuration = if (durationModifier == 1) {
                letter.effects.onReleaseColorChangeDuration
            } else {
                letter.effects.onReleaseColorChangeDuration * durationModifier
            }

            animateNow(
                letter, "backgroundColor", color.viewStart,
                endColor, onReleaseDuration
            )

            animateNow(
                letter, "textColor", color.textStart,
                endTextColor, onReleaseDuration
            )

            // Get some value that represents how much was moved ignoring the actual physical
            // distance. Log applied to Euclidean distance to make it very small.
            val distance = log2((abs(moveByX) + abs(moveByY)).toDouble())

            letter.animate()
                .xBy(moveByX.toFloat())
                .yBy(moveByY.toFloat())
                //.z(layer)
                .setInterpolator(FastOutSlowInInterpolator())
                // Here we use the log-distance to calculate the duration of the animation.
                // Longer distance should be only slightly longer animation and vice-versa
                .setDuration(distance.toLong() * letter.effects.animateToHomeDurationMultiplier * durationModifier)
                .withEndAction {
                    letter.state = HOME
                    letter.translationZ = NEUTRAL_LAYER
                    letter.currentPos.x = letter.pos.x
                    letter.currentPos.y = letter.pos.y

                  /*  if (durationModifier != 1) {
                        try {
                            //letter.findFragment<LevelFragment>().playUiContainerState()
                        }
                        catch (e:IllegalStateException)
                        {
                            Log.e(TAG, e.message.toString())
                        }
                    }*/
                }
                .start()
        }

    };

    fun animateNow(
        letter: Letter,
        property: String,
        startColor: Int,
        endColor: Int,
        aDuration: Long
    ) {
        ObjectAnimator.ofArgb(letter, property, startColor, endColor)
            .apply {
                duration = aDuration
                start()
            }
    }

}



