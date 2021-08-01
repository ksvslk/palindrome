package com.indest.oc.letter

import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.indest.oc.R
import com.indest.oc.util.C
import kotlin.math.log2

const val TAG = "State"

enum class State : IState {
    HOME {
        override fun draw(l: Letter) {
            if (l.currentPos.x == Int.MAX_VALUE && l.currentPos.y == Int.MAX_VALUE) {
                l.currentPos.x = l.pos.x
                l.currentPos.y = l.pos.y
                l.homePos.x = l.pos.x
                l.homePos.y = l.pos.y
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
            l.z = C.TOP_LAYER

            // Sound/vibration effects
            l.addShortVibration()
            val id = l.effects.soundPool.load(l.context, R.raw.start_drag, 1)
            l.effects.soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f)


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
            l.director.handleCollisions(l)
        }
        override fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int) {}
    },
    ANIMATION {
        override fun draw(l: Letter) {}
        override fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent) {}
        override fun executeActionUpBehaviour(l: Letter) {}
        override fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int) {

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
                .setInterpolator(FastOutSlowInInterpolator())
                // Here we use the log-distance to calculate the duration of the animation.
                // Longer distance should be only slightly longer animation and vice-versa
                .setDuration(distance.toLong() * l.effects.animateToHomeDurationMultiplier)
                .withEndAction {
                    l.z = C.MIDDLE_LAYER
                    l.state = HOME
                    l.currentPos.x = l.pos.x
                    l.currentPos.y = l.pos.y
                    assertIdenticalPositions(l)
                }
                .start()
        }
        private fun assertIdenticalPositions(l:Letter)
        {
            val byLetterToString = mutableMapOf<IBox, String>()
            val homePosits = mutableMapOf<IBox, String>()
            val byLetterToPos = l.director.boxes
                .filterIsInstance<Letter>()
                .associateWithTo(byLetterToString) {
                    "${it.pos.x},${it.pos.y}"
                }
            val hompos = l.director.boxes
                .filterIsInstance<Letter>()
                .associateWithTo(homePosits) {
                    "${it.homePos.x};${it.homePos.y} "
                }
            val sameElements = byLetterToPos.values.toSet().size < byLetterToPos.values.size
            assert(!sameElements) { "Identical positions for objects not allowed: ${byLetterToPos}. Letter homes: $hompos" }
        }
    };
}



