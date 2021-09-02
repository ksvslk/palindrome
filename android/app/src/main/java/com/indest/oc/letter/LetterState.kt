package com.indest.oc.letter

import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.indest.oc.letter.effects.ColorSelector
import com.indest.oc.util.NEUTRAL_LAYER
import com.indest.oc.util.TOP_LAYER
import kotlin.math.log2

const val TAG = "LetterState"

enum class LetterState : ILetterState {

    DISABLED {
        override fun draw(l: Letter) {}
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
    INITIALIZING {
        override fun draw(l: Letter) {}
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

            // Calculate the distance moved
            l.dX = v.x - e.rawX
            l.dY = v.y - e.rawY

            // View and its parent should be always on top when dragging
            l.z = TOP_LAYER
            //l.elevation = C.TOP_LAYER

            val color = if (l.hasFriend()) {
                l.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND)
            } else {
                l.effects.getColors(ColorSelector.ACTION_DOWN_NO_FRIEND)
            }

            // Animate background color when starting to drag
            ObjectAnimator.ofArgb(l, "backgroundColor", color.viewStart, color.viewEnd)
                .apply {
                    duration = l.effects.onClickColorChangeDuration
                    start()
                }

            // Animate text color when starting to drag
            ObjectAnimator.ofArgb(l, "textColor", color.textStart, color.textEnd)
                .apply {
                    duration = l.effects.onClickColorChangeDuration
                    start()
                }

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
            l.director.handleCollisions(l)
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

            // Reset background color with animation
            ObjectAnimator.ofArgb(l, "backgroundColor", color.viewStart, color.viewEnd)
                .apply {
                    duration = l.effects.onReleaseColorChangeDuration
                    start()
                }

            // Reset text color with animation
            ObjectAnimator.ofArgb(l, "textColor", color.textStart, color.textEnd).apply {
                duration = l.effects.onReleaseColorChangeDuration
                start()
            }
            // Get some value that represents how much was moved ignoring the actual physical
            // distance. Log applied to Euclidean distance to make it very small.
            val distance = log2((Math.abs(moveByX) + Math.abs(moveByY)).toDouble())

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
                }
                .start()
        }
    };
}



