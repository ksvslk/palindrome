package com.indest.oc.mediator

import android.util.Log
import com.indest.oc.letter.*
import com.indest.oc.util.C
import kotlin.math.abs

object BoxDirector {
    private const val TAG = "BoxDirector"
    val boxes: MutableList<IBox> = mutableListOf()

    fun handleCollisions(box: Letter) {
        Log.i(TAG, "$box called handleCollisions.")

        when (val closestBox = getClosestBox(to=box, from=getCandidateBoxes(box))) {
            is Letter -> {
                Log.i(TAG, "Closest is $closestBox and its friend is ${closestBox.getFriend()}!")
                if (closestBox.state == State.HOME) {
                    switchLetterPositions(box, closestBox)
                    return
                }
            }
            is Slot -> {
                Log.i(TAG, "Closest is $closestBox and its friend is ${closestBox.getFriend()}!")
                if (!closestBox.hasFriend()) {
                    placeLetterInEmptyBox(box, closestBox)
                    return
                }
            }
        }

        // If there were no collisions
        sendLetterHome(box)
    }

    private fun getCandidateBoxes(box: Letter): MutableList<IBox> {
        val candidateBoxes = mutableListOf<IBox>()

        for (droppedTo in boxes) {
            if (box.rect().intersect(droppedTo.rect()) && box != droppedTo) {
                if ((droppedTo is Letter) && droppedTo.state == State.HOME) {
                    candidateBoxes.add(droppedTo)
                } else if (droppedTo is Slot) {
                    candidateBoxes.add(droppedTo)
                }
            }
        }
        Log.i(TAG, "Candidate boxes: $candidateBoxes")
        return candidateBoxes
    }

    private fun switchLetterPositions(top: Letter, bottom: Letter) {
        Log.i(TAG, "Switching $top home position with $bottom!")

        // Calculate distance for animation
        val distanceToMoveX = bottom.pos().x - top.pos().x
        val distanceToMoveY = bottom.pos().y - top.pos().y
        val droppedToMoveX = top.currentPos.x - bottom.currentPos.x
        val droppedToMoveY = top.currentPos.y - bottom.currentPos.y

        // Switch info
        switchCoordinates(bottom.currentPos, top.currentPos)
        switchCoordinates(bottom.homePos, top.homePos)
        switchFriends(bottom, top)

        // Set the layers accordingly - which letter is FRONT and which is BEHIND
        bottom.z = C.MIDDLE_LAYER
        top.z = C.TOP_LAYER

        // Change state to animation
        top.state = State.ANIMATION
        bottom.state = State.ANIMATION

        // Execute animation
        top.state.executeAnimationBehaviour(top, distanceToMoveX, distanceToMoveY)
        bottom.state.executeAnimationBehaviour(bottom, droppedToMoveX, droppedToMoveY)
    }

    private fun placeLetterInEmptyBox(letter: Letter, slot: Slot) {
        Log.i(TAG, "Placing $letter to empty $slot!")
        letter.getFriend()?.setFriend(null)
        slot.setFriend(letter)
        letter.setFriend(slot)
        letter.currentPos.x = slot.pos().x
        letter.currentPos.y = slot.pos().y
        letter.z = C.TOP_LAYER
        letter.state = State.ANIMATION
        letter.state.executeAnimationBehaviour(
            letter,
            (slot.pos().x - letter.pos().x),
            (slot.pos().y - letter.pos().y)
        )
    }

    private fun sendLetterHome(letter: Letter) {
        if (letter.hasFriend()) {
            Log.i(TAG, "Going to real home!")
            letter.getFriend()?.setFriend(null)
            letter.setFriend(null)

            letter.state = State.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.homePos.x - letter.pos().x),
                (letter.homePos.y - letter.pos().y)
            )
        } else{
            Log.i(TAG, "No collisions detected (or Letter/Slot not good) - going to current home!")
            letter.state = State.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.currentPos.x - letter.pos().x),
                (letter.currentPos.y - letter.pos().y)
            )
        }
    }

    private fun getClosestBox(to: Letter, from: MutableList<IBox>): IBox? {
        return from.minByOrNull {
            abs(to.pos().x - it.pos().x) +
                    abs(to.pos().y - it.pos().y)
        }
    }

    private fun switchCoordinates(bottomPos: GlobalPosition, topPos: GlobalPosition)
    {
        val tempHomeX = bottomPos.x
        val tempHomeY = bottomPos.y
        bottomPos.x = topPos.x
        bottomPos.y = topPos.y
        topPos.x = tempHomeX
        topPos.y = tempHomeY
    }

    private fun switchFriends(bottom: Letter, top: Letter)
    {
        val bottomFriend = bottom.getFriend()
        val topFriend = top.getFriend()
        if (bottom.hasFriend() && top.hasFriend()) {
            bottom.setFriend(topFriend)
            top.setFriend(bottomFriend)
        } else if (bottom.hasFriend() && !top.hasFriend()) {
            top.setFriend(bottomFriend)
            bottom.setFriend(null)
        } else if (top.hasFriend() && !bottom.hasFriend()) {
            bottom.setFriend(topFriend)
            top.setFriend(null)
        }
    }
}
