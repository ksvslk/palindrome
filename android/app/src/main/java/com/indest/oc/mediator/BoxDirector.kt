package com.indest.oc.mediator

import android.util.Log
import androidx.core.view.ViewCompat
import com.indest.oc.letter.IBox
import com.indest.oc.letter.Letter
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.util.C
import kotlin.math.abs

object BoxDirector {
    private const val TAG = "BoxDirector"
    val boxes: MutableList<IBox> = mutableListOf()

    fun handleCollisions(box: Letter): Boolean {
        Log.i(TAG, "$box called handleCollisions.")
        Log.i(TAG, "??")
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
                if (box.hasFriend())
                {
                    box.getFriend()?.setFriend(null)
                    box.setFriend(null)
                }
                box.state = State.ANIMATION
                box.state.executeAnimationBehaviour(box, (box.homeX - box.pos().gX), (box.homeY - box.pos().gY))
                return true
            }
        }

        when(closest)
        {
            is Letter -> {
                Log.i(TAG, "Closest is Letter!")
                if (closest.state == State.HOME)
                {
                    switchLetterPositions(box, closest)
                    return true
                }
            }
            is Slot -> {
                Log.i(TAG, "Closest is $closest and its friend is ${closest.getFriend()}!")
                if(!closest.hasFriend())
                {
                    placeLetterInEmptyBox(box, closest)
                    return true
                }
            }
            else -> {
                Log.i(TAG, "Closest is Unknown: $closest!")
            }
        }

        return false
    }

    private fun placeLetterInEmptyBox(letter: Letter, slot: Slot) {
        Log.i(TAG, "Placing $letter to empty $slot!")

        letter.getFriend()?.setFriend(null)
        slot.setFriend(letter)
        letter.setFriend(slot)

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

        val tempHomeX = bottom.homeX
        val tempHomeY = bottom.homeY
        val bottomFriend = bottom.getFriend()
        val topFriend = top.getFriend()

        bottom.homeX = top.homeX
        bottom.homeY = top.homeY
        top.homeX = tempHomeX
        top.homeY = tempHomeY

        if (bottom.hasFriend() && top.hasFriend())
        {
            bottom.setFriend(topFriend)
            top.setFriend(bottomFriend)
        }
        else if (bottom.hasFriend() && !top.hasFriend())
        {
            top.setFriend(bottomFriend)
            bottom.setFriend(null)
        }
        else if (top.hasFriend() && !bottom.hasFriend())
        {
            bottom.setFriend(topFriend)
            top.setFriend(null)
        }

        ViewCompat.setTranslationZ((bottom.parent as androidx.gridlayout.widget.GridLayout), C.MIDDLE_LAYER)
        bottom.z = C.MIDDLE_LAYER
        ViewCompat.setTranslationZ((top.parent as androidx.gridlayout.widget.GridLayout), C.TOP_LAYER)
        top.z = C.TOP_LAYER
        top.state = State.ANIMATION
        bottom.state = State.ANIMATION
        top.state.executeAnimationBehaviour(top,distanceToMoveX, distanceToMoveY)
        bottom.state.executeAnimationBehaviour(bottom, droppedToMoveX, droppedToMoveY)
    }

}
