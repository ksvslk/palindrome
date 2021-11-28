package com.indest.hah.letter

import android.view.MotionEvent
import android.view.View

sealed interface ILetterState {
    fun draw(l: Letter)
    fun executeActionDownBehaviour(l: Letter, v: View, e: MotionEvent)
    fun executeActionMoveBehaviour(l: Letter, v: View, e: MotionEvent)
    fun executeActionUpBehaviour(l: Letter)
    fun executeAnimationBehaviour(l: Letter, moveByX: Int, moveByY: Int)
}