package com.indest.oc.letter

import android.view.MotionEvent
import android.view.View

sealed interface IState {
    fun draw(l: Letter)
    fun executeActionDownBehaviour(l:Letter, v: View, e:MotionEvent)
    fun executeActionMoveBehaviour(l:Letter, v: View, e:MotionEvent)
    fun executeActionUpBehaviour(l: Letter, s: Slot? = null)
    fun executeAnimationBehaviour(l: Letter, other:Letter?, s:Slot? = null, moveByX: Int, moveByY: Int)
}