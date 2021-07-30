package com.indest.oc.mediator

import android.util.Log
import androidx.core.view.ViewCompat
import com.indest.oc.letter.IBox
import com.indest.oc.letter.Letter
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.util.C
import kotlin.math.abs

object BoxDirector : IBoxDirector {
    private const val TAG = "BoxDirector"
    override val boxes: MutableList<IBox> = mutableListOf()
    var pairs:MutableMap<Slot, Letter> = mutableMapOf()

    override fun handleCollisions(box: Letter): Boolean {
        Log.i(TAG, "$box called handleCollisions.")
        var result = false
        val candidateBoxes = mutableListOf<IBox>()

        for (droppedTo in boxes) {
                if (box.rect().intersect(droppedTo.rect()) && box != droppedTo) {
                    if ((droppedTo is Letter) && droppedTo.state == State.HOME)
                    {
                        candidateBoxes.add(droppedTo)
                    }
                    else if (droppedTo is Slot){
                        candidateBoxes.add(droppedTo)
                    }
                }
        }
        Log.i(TAG, "Candidate boxes: $candidateBoxes")

        val closest = candidateBoxes.minByOrNull {
            abs(box.pos().gX - it.pos().gX) +
            abs(box.pos().gY - it.pos().gY)
        }.also { if (it == null) {
            Log.i(TAG, "${it ?: "No letter was touching so nothing"} was chosen!")
            animateLetterHome(box)
            result = true
            }
        }


        when(closest)
        {
            is Letter -> {
                if (closest.state == State.HOME)
                {
                    switchLetterPositions(box, closest)
                    result = true
                }
            }
            is Slot -> {
                if(!pairs.containsKey(closest))
                {
                    addLetterSlotPair(box, closest)
                    placeLetterInEmptyBox(box, closest)
                    result = true
                }
            }
        }

        return result
    }

    private fun addLetterSlotPair(letter: Letter, slot: Slot)
    {
        Log.i(TAG, "Add pair $slot -> $letter")
        pairs[slot] = letter
    }

    private fun clearPairsOfLetter(l: Letter)
    {
        if (pairs.containsValue(l)) {
            Log.i(TAG, "Deleting $l from pairs!")
            pairs.keys.find { s -> pairs[s] == l }.also { pairs.remove(it) }
        }
    }

    private fun animateLetterHome(l: Letter)
    {
        Log.i(TAG, "Animating $l to home position!")
        clearPairsOfLetter(l)
        l.state = State.ANIMATION
        l.state.executeAnimationBehaviour(l, (l.homeX - l.pos().gX), (l.homeY - l.pos().gY))
    }

    private fun placeLetterInEmptyBox(letter: Letter, slot: Slot) {
        Log.i(TAG, "Placing $letter to empty $slot!")

        val MINI_TAG ="placeLetter begin"
        Log.i(MINI_TAG, "$letter current position: ${letter.currentX},${letter.currentY}")
        Log.i(MINI_TAG, "$letter home position: ${letter.homeX},${letter.homeY}")
        Log.i(MINI_TAG, "$slot position: ${slot.pos()}")
        Log.i(MINI_TAG, "$letter position: ${letter.pos()}")
        letter.currentX = slot.pos().gX
        letter.currentY = slot.pos().gY

        val distanceToMoveX = slot.pos().gX - letter.pos().gX
        val distanceToMoveY = slot.pos().gY - letter.pos().gY
        ViewCompat.setTranslationZ((letter.parent as androidx.gridlayout.widget.GridLayout), C.TOP_LAYER)
        letter.z = C.TOP_LAYER
        letter.state = State.ANIMATION
        letter.state.executeAnimationBehaviour(letter, distanceToMoveX, distanceToMoveY)
    }

    private fun switchLetterPositions(top: Letter, bottom: Letter) {
        Log.i(TAG, "Switching $top home position with $bottom!")

        val distanceToMoveX = bottom.pos().gX - top.pos().gX
        val distanceToMoveY = bottom.pos().gY - top.pos().gY
        val droppedToMoveX = top.currentX - bottom.currentX
        val droppedToMoveY = top.currentY - bottom.currentY
        val tempX = bottom.currentX
        val tempY = bottom.currentY
        bottom.currentX = top.currentX
        bottom.currentY = top.currentY
        top.currentX = tempX
        top.currentY = tempY

        val slot = pairs.keys.find { s -> pairs[s] == bottom }
        if (slot != null)
        {
            clearPairsOfLetter(bottom)
            addLetterSlotPair(top, slot)
        }

        ViewCompat.setTranslationZ((bottom.parent as androidx.gridlayout.widget.GridLayout), C.MIDDLE_LAYER)
        bottom.z = C.MIDDLE_LAYER
        ViewCompat.setTranslationZ((top.parent as androidx.gridlayout.widget.GridLayout), C.TOP_LAYER)
        top.z = C.TOP_LAYER
        top.state = State.ANIMATION
        top.state.executeAnimationBehaviour(top,distanceToMoveX, distanceToMoveY)
        bottom.state = State.ANIMATION
        bottom.state.executeAnimationBehaviour(bottom, droppedToMoveX, droppedToMoveY)
    }

}
