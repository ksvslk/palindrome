package com.indest.hah.letter

import android.view.MotionEvent
import android.view.View
import com.indest.hah.util.C

sealed interface ILetterState {
    fun draw(letter: Letter)
    fun executeActionDownBehaviour(letter: Letter, view: View, event: MotionEvent)
    fun executeActionMoveBehaviour(letter: Letter, view: View, event: MotionEvent)
    fun executeActionUpBehaviour(letter: Letter)
    fun executeAnimationBehaviour(letter: Letter, moveByX: Int, moveByY: Int, durationModifier: Int = 1)
}