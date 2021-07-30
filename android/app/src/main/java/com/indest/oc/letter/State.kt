package com.indest.oc.letter

import android.animation.ObjectAnimator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.indest.oc.R
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.C
import kotlin.math.log2

const val TAG = "State"

enum class State : IState {
    HOME {
        override fun draw(l: Letter) {
            if (l.homeX == Int.MAX_VALUE && l.homeY == Int.MAX_VALUE) {
                l.homeX = l.pos().gX
                l.homeY = l.pos().gY
            }
        }
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int) {}
    },
    DRAGGING {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {

            // Calculate the distance moved
            l.dX = v.x - e.rawX
            l.dY = v.y - e.rawY

            // View and its parent should be always on top when dragging
            ViewCompat.setTranslationZ((l.parent as androidx.gridlayout.widget.GridLayout), C.TOP_LAYER)
            l.z = C.TOP_LAYER

            // Sound/vibration effects
            l.addShortVibration()
            val id = l.lr.soundPool.load(l.context, R.raw.start_drag, 1)
            l.lr.soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f)

            // Animate background color when starting to drag
            ObjectAnimator.ofArgb(l, "backgroundColor", l.lr.viewStartColor, l.lr.viewEndColor)
                .apply {
                    duration = l.lr.onClickColorChangeDuration
                    start()
                }

            // Animate text color when starting to drag
            ObjectAnimator.ofArgb(l, "textColor", l.lr.textStartColor, l.lr.textEndColor)
                .apply {
                    duration = l.lr.onClickColorChangeDuration
                    start()
                }
        }

        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {
            v.x = e.rawX + l.dX
            v.y = e.rawY + l.dY
        }

        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int) {}
    },
    RESOLVING {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {

            // Reset background color with animation
            ObjectAnimator.ofArgb(l, "backgroundColor", l.lr.viewEndColor, l.lr.viewStartColor)
                .apply {
                    duration = l.lr.onReleaseColorChangeDuration
                    start()
                }

            // Reset text color with animation
            ObjectAnimator.ofArgb(l, "textColor", l.lr.textEndColor, l.lr.textStartColor).apply {
                duration = l.lr.onReleaseColorChangeDuration
                start()
            }

            // If collisions are detected BoxDirector will handle it.
            // Otherwise we just send the Letter home.
            if (!l.director.handleCollisions(l)) {
                l.state = ANIMATION
                l.state.executeAnimationBehaviour(l, (l.homeX - l.pos().gX), (l.homeY - l.pos().gY))
            }
        }

        override fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int) {}
    },
    ANIMATION {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int) {

            // Get some value that represents how much was moved ignoring the actual physical
            // distance. Log applied to Euclidean distance to make it very small.
            val distance = log2((Math.abs(moveByX) + Math.abs(moveByY)).toDouble())

            l.animate()
                .xBy(moveByX.toFloat())
                .yBy(moveByY.toFloat())
                .setInterpolator(FastOutSlowInInterpolator())
                // Here we use the log-distance to calculate the duration of the animation.
                // Longer distance should be only slightly longer animation and vice-versa
                .setDuration(distance.toLong() * l.lr.animateToHomeDurationMultiplier)
                .withEndAction {
                    ViewCompat.setTranslationZ((l.parent as androidx.gridlayout.widget.GridLayout), C.MIDDLE_LAYER)
                    l.z = C.MIDDLE_LAYER
                    l.state = HOME
                    Log.i("withEndAction", "$l position: ${l.pos()}")
                    Log.i("withEndAction", "$l home position: ${l.homeX},${l.homeY}")
                    Log.i("withEndAction", "Pairs: ${(l.director as BoxDirector).pairs}")
                }
                .start()
        }
    },
}


