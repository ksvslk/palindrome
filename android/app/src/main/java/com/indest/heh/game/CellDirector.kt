package com.indest.heh.game

import android.util.Log
import com.indest.heh.fragments.GamePlayFragment
import com.indest.heh.letter.*
import com.indest.heh.letter.effects.ColorSelector
import com.indest.heh.letter.effects.SoundSelector
import com.indest.heh.persistance.dto.Level
import com.indest.heh.util.C.MIDDLE_LAYER
import com.indest.heh.util.C.NEUTRAL_LAYER
import com.indest.heh.util.C.TOP_LAYER
import kotlin.math.abs

/**
 * CellDirector is kind of a mediator object that will manage the interaction between
 * all the Letter and Slot objects. It will handle the collisions between the objects and the
 * following position (and other important info) changes.
 * State persistence to database is also handled from here (high level calls).
 */
class CellDirector constructor(gamePlayFragment: GamePlayFragment) {

    private val TAG = "CellDirector"
    private val gf: GamePlayFragment = gamePlayFragment
    var cells: MutableList<Cell<*, *>> = ArrayList()

    fun handleCollisions(cell: Letter) {
        when (val closestCell = getClosestBox(to = cell, from = getCandidateBoxes(cell))) {
            is Letter -> {
                if (closestCell.state == LetterState.HOME) {
                    switchLetterPositions(cell, closestCell)
                    return
                }
            }
            is Slot -> {
                if (!closestCell.hasFriend()) {
                    placeLetterInEmptyBox(cell, closestCell)
                    return
                }
            }
        }

        sendLetterHome(cell)
    }

    private fun getCandidateBoxes(box: Letter): MutableList<Cell<*, *>> {
        val candidateCells = mutableListOf<Cell<*, *>>()

        for (droppedTo in cells) {
            if (box.rect.intersect(droppedTo.rect) && box != droppedTo) {
                if ((droppedTo is Letter) && droppedTo.state == LetterState.HOME) {
                    candidateCells.add(droppedTo)
                } else if (droppedTo is Slot) {
                    candidateCells.add(droppedTo)
                }
            }
        }
        return candidateCells
    }

    private fun switchLetterPositions(top: Letter, bottom: Letter) {
        top.effects.playSound(SoundSelector.SWITCH_LETTERS)

        // Calculate distance for animation
        val distanceToMoveX = bottom.pos.x - top.pos.x
        val distanceToMoveY = bottom.pos.y - top.pos.y
        val droppedToMoveX = top.currentPos.x - bottom.currentPos.x
        val droppedToMoveY = top.currentPos.y - bottom.currentPos.y

        // Switch info
        switchCoordinates(bottom.currentPos, top.currentPos)
        switchCoordinates(bottom.homePos, top.homePos)

        val topIndex = top.homeIndex
        val bottomIndex = bottom.homeIndex
        val topLetter = top.text.first()
        val bottomLetter = bottom.text.first()

        gf.gameViewModel.updateLetters(
            topIndex = topIndex,
            bottomLetter = bottomLetter,
            bottomIndex = bottomIndex,
            topLetter = topLetter
        )

        switchFriends(bottom, top)
        switchIndex(bottom, top)

        // Set the layers accordingly - which letter is FRONT and which is BEHIND
        bottom.z = MIDDLE_LAYER
        bottom.elevation = MIDDLE_LAYER
        top.z = TOP_LAYER
        top.elevation = TOP_LAYER

        // Change state to animation
        top.state = LetterState.ANIMATION
        bottom.state = LetterState.ANIMATION

        // Execute animation
        top.state.executeAnimationBehaviour(top, distanceToMoveX, distanceToMoveY)
        bottom.state.executeAnimationBehaviour(bottom, droppedToMoveX, droppedToMoveY)
    }

    private fun placeLetterInEmptyBox(letter: Letter, slot: Slot) {
        letter.effects.playSound(SoundSelector.PLACE_LETTER_IN_EMPTY_BOX)
        letter.getTheFriend()?.setFriend(null)
        slot.setFriend(letter)
        letter.setFriend(slot)
        letter.currentPos.x = slot.pos.x
        letter.currentPos.y = slot.pos.y
        letter.z = NEUTRAL_LAYER
        letter.elevation = NEUTRAL_LAYER
        letter.state = LetterState.ANIMATION
        letter.state.executeAnimationBehaviour(
            letter,
            (slot.pos.x - letter.pos.x),
            (slot.pos.y - letter.pos.y)
        )

        gf.gameViewModel.updateFriendIndex(
            index = letter.homeIndex,
            newIndex = slot.homeIndex
        )
        Log.i(TAG, cells.toString())
    }

    fun sendLetterHome(letter: Letter) {
        if (letter.hasFriend()) {
            letter.getTheFriend()?.setFriend(null)
            letter.setFriend(null)

            gf.gameViewModel.updateFriendIndex(
                index = letter.homeIndex,
                newIndex = -666
            )

            letter.state = LetterState.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.homePos.x - letter.pos.x),
                (letter.homePos.y - letter.pos.y)
            )
            Log.i(TAG, cells.toString())
        } else {
            letter.state = LetterState.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.currentPos.x - letter.pos.x),
                (letter.currentPos.y - letter.pos.y)
            )
        }
    }

    private fun getClosestBox(to: Letter, from: MutableList<Cell<*, *>>): Cell<*, *>? {
        return from.minByOrNull {
            abs(to.pos.x - it.pos.x) +
                    abs(to.pos.y - it.pos.y)
        }
    }

    private fun switchIndex(bottom: Letter, top: Letter) {
        val bottomIndex = bottom.homeIndex
        bottom.homeIndex = top.homeIndex
        top.homeIndex = bottomIndex
    }

    private fun switchCoordinates(bottomPos: GlobalPosition, topPos: GlobalPosition) {
        val tempHomeX = bottomPos.x
        val tempHomeY = bottomPos.y
        bottomPos.x = topPos.x
        bottomPos.y = topPos.y
        topPos.x = tempHomeX
        topPos.y = tempHomeY
    }

    private fun switchFriends(bottom: Letter, top: Letter) {
        val bottomFriend = bottom.getTheFriend()
        val topFriend = top.getTheFriend()
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

    fun restoreSlotPlacements(info: Level) {
        when {
            info.lettersProperties.isNotEmpty() -> {
                info.lettersProperties.forEach { li ->
                    if (li.friendIndex != -666) {
                        val slot = cells.find { it.homeIndex == li.friendIndex } as Slot
                        val letter = cells.filterIsInstance<Letter>()
                            .find { it.homeIndex == li.homeIndex } as Letter
                        letter.x = slot.x
                        letter.y = slot.y
                        letter.setFriend(slot)
                        slot.setFriend(letter)
                        letter.friendIndex = slot.homeIndex
                        letter.currentPos = slot.pos
                        val colors = letter.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND)
                        letter.setBackgroundColor(colors.viewEnd)
                    }
                }
            }
        }

    }
}