package com.indest.heh.letter

import android.animation.ObjectAnimator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.findFragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.indest.heh.fragments.GamePlayFragment
import com.indest.heh.letter.effects.ColorSelector
import com.indest.heh.util.C.NEUTRAL_LAYER
import com.indest.heh.util.C.TOP_LAYER
import kotlin.math.abs
import kotlin.math.log2

const val TAG = "LetterState"

/**
 * State of Letter objects and all the behaviour they can execute in each state
 * is restricted with this special enum class.
 */
enum class LetterState : ILetterState {
    HOME {
        override fun draw(l: Letter) {

        }
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(
            l: Letter,
            moveByX: Int,
            moveByY: Int
        ) {
        }
    },
    DRAGGING {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {

            val fragment = l.findFragment<GamePlayFragment>()


            // Calculate the distance moved
            l.dX = v.x - e.rawX
            l.dY = v.y - e.rawY

            // View and its parent should be always on top when dragging
            l.z = TOP_LAYER

            val color = if (l.hasFriend()) {
                l.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND)
            } else {
                l.effects.getColors(ColorSelector.ACTION_DOWN_NO_FRIEND)
            }

            animateNow(l, "backgroundColor", color.viewStart, color.viewEnd, l.effects.onClickColorChangeDuration)
            animateNow(l, "textColor", color.textStart, color.textEnd, l.effects.onClickColorChangeDuration)

            l.animate()
                .z(TOP_LAYER)
                .start()
        }

        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {
            v.x = e.rawX + l.dX
            v.y = e.rawY + l.dY
        }

        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(
            l: Letter,
            moveByX: Int,
            moveByY: Int
        ) {
        }
    },
    RESOLVING {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {
            l.findFragment<GamePlayFragment>().cellDirector.handleCollisions(l)
        }

        override fun executeAnimationBehaviour(
            l: Letter,
            moveByX: Int,
            moveByY: Int
        ) {
        }
    },
    ANIMATION {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(
            l: Letter,
            moveByX: Int,
            moveByY: Int
        ) {

            val color = if (l.hasFriend()) {
                l.effects.getColors(ColorSelector.ACTION_UP_FRIEND)
            } else {
                l.effects.getColors(ColorSelector.ACTION_UP_NO_FRIEND)
            }

            animateNow(l, "backgroundColor", color.viewStart,
                color.viewEnd, l.effects.onReleaseColorChangeDuration)
            animateNow(l, "textColor", color.textStart,
                color.textEnd, l.effects.onReleaseColorChangeDuration)

            // Get some value that represents how much was moved ignoring the actual physical
            // distance. Log applied to Euclidean distance to make it very small.
            val distance = log2((abs(moveByX) + abs(moveByY)).toDouble())

            l.animate()
                .xBy(moveByX.toFloat())
                .yBy(moveByY.toFloat())
                .z(NEUTRAL_LAYER)
                .setInterpolator(FastOutSlowInInterpolator())
                // Here we use the log-distance to calculate the duration of the animation.
                // Longer distance should be only slightly longer animation and vice-versa
                .setDuration(distance.toLong() * l.effects.animateToHomeDurationMultiplier)
                .withEndAction {
                    l.state = HOME
                    l.z = NEUTRAL_LAYER
                    l.currentPos.x = l.pos.x
                    l.currentPos.y = l.pos.y
                    Log.i(TAG, "ANIMATION END for $l.")
                }
                .start()
        }


    };

    fun animateNow(letter: Letter, property: String, startColor:Int, endColor:Int, aDuration:Long)
    {
        ObjectAnimator.ofArgb(letter, property, startColor, endColor)
            .apply {
                duration = aDuration
                start()
            }
    }

}



