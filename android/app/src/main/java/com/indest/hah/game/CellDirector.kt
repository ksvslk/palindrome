package com.indest.hah.game

import android.util.Log
import com.indest.hah.fragments.GamePlayFragment
import com.indest.hah.letter.*
import com.indest.hah.letter.effects.ColorSelector
import com.indest.hah.letter.effects.SoundSelector
import com.indest.hah.persistance.dto.Level
import com.indest.hah.util.C.MIDDLE_LAYER
import com.indest.hah.util.C.NEUTRAL_LAYER
import com.indest.hah.util.C.REWARDED_ANIM_MUL
import com.indest.hah.util.C.REWARDED_ANIM_MUL_DEFAULT
import com.indest.hah.util.C.TOP_LAYER
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

    fun handleCollisions(letter: Letter) {
        Log.i(TAG,"Here")
        when (val closestCell = getClosestCell(to = letter, from = getCandidateCells(letter))) {
            is Letter -> {
                if (closestCell.state == LetterState.HOME && !closestCell.isLocked) {
                    switchLetterPositions(letter, closestCell)
                    return
                }
            }
            is Slot -> {
                if (!closestCell.hasFriend()) {
                    placeLetterInEmptyBox(letter, closestCell)
                    return
                }
                else if (closestCell.getTheFriend() == letter)
                {
                    letter.effects.playSound(SoundSelector.PLACE_LETTER_IN_EMPTY_BOX)
                    letter.currentPos.x = closestCell.pos.x
                    letter.currentPos.y = closestCell.pos.y
                    letter.z = NEUTRAL_LAYER
                    letter.elevation = NEUTRAL_LAYER
                    letter.state = LetterState.ANIMATION
                    letter.state.executeAnimationBehaviour(
                        letter,
                        (closestCell.pos.x - letter.pos.x),
                        (closestCell.pos.y - letter.pos.y),
                    )
                    return
                }
            }

        }
        sendLetterHome(letter)
    }

    private fun getCandidateCells(letter: Letter): MutableList<Cell<*, *>> {
        val candidateCells = mutableListOf<Cell<*, *>>()

        for (droppedTo in cells) {
            if (letter.rect.intersect(droppedTo.rect) && letter != droppedTo) {
                if ((droppedTo is Letter) && droppedTo.state == LetterState.HOME) {
                    candidateCells.add(droppedTo)
                } else if (droppedTo is Slot) {
                    candidateCells.add(droppedTo)
                }
            }
        }
        Log.i(TAG, candidateCells.firstOrNull().toString())
        return candidateCells
    }

    fun switchLetterPositions(top: Letter, bottom: Letter, isRewarded: Boolean = false) {
        if (isRewarded){
            top.effects.playSound(SoundSelector.LETTER_REWARDED)
        }
        else {
            top.effects.playSound(SoundSelector.SWITCH_LETTERS)
        }

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

        if (top.text.first() != top.friend?.realChar) gf.gameViewModel.increaseMistakes()
        if (bottom.text.first() != bottom.friend?.realChar) gf.gameViewModel.increaseMistakes()

        // Set the layers accordingly - which letter is FRONT and which is BEHIND
        bottom.z = MIDDLE_LAYER
        bottom.elevation = MIDDLE_LAYER
        top.z = TOP_LAYER
        top.elevation = TOP_LAYER

        // Change state to animation
        top.state = LetterState.ANIMATION
        bottom.state = LetterState.ANIMATION

        // Execute animation
        val durationModifier = if (isRewarded) REWARDED_ANIM_MUL else REWARDED_ANIM_MUL_DEFAULT
        top.state.executeAnimationBehaviour(top, distanceToMoveX, distanceToMoveY, durationModifier)
        bottom.state.executeAnimationBehaviour(bottom, droppedToMoveX, droppedToMoveY)
    }

    fun placeLetterInEmptyBox(letter: Letter, slot: Slot, isRewarded: Boolean = false, isLastRewardedLetter:Boolean = false) {
        if (isRewarded){
            letter.effects.playSound(SoundSelector.LETTER_REWARDED)
        }
        else {
            letter.effects.playSound(SoundSelector.PLACE_LETTER_IN_EMPTY_BOX)
        }
        letter.getTheFriend()?.setFriend(null)
        slot.setFriend(letter)
        letter.setFriend(slot)
        letter.currentPos.x = slot.pos.x
        letter.currentPos.y = slot.pos.y
        letter.z = NEUTRAL_LAYER
        letter.elevation = NEUTRAL_LAYER
        letter.state = LetterState.ANIMATION

        val durationModifier = if (isRewarded && !isLastRewardedLetter) REWARDED_ANIM_MUL else REWARDED_ANIM_MUL_DEFAULT
        letter.state.executeAnimationBehaviour(
            letter,
            (slot.pos.x - letter.pos.x),
            (slot.pos.y - letter.pos.y),
            durationModifier
        )

        gf.gameViewModel.updateFriendIndex(
            index = letter.homeIndex,
            newIndex = slot.homeIndex
        )
        if (isRewarded) gf.gameViewModel.setLetterLocked(letter.homeIndex)

        val isCorrectMove = slot.friend?.text?.first()?.equals(slot.realChar)
        if (isCorrectMove == false) gf.gameViewModel.increaseMistakes()
    }

    fun sendLetterHome(letter: Letter) {
        if (letter.hasFriend()) {
            letter.getTheFriend()?.setFriend(null)
            letter.setFriend(null)
            gf.gameViewModel.increaseMistakes()

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
        } else {
            letter.state = LetterState.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.currentPos.x - letter.pos.x),
                (letter.currentPos.y - letter.pos.y)
            )
        }
    }

    private fun getClosestCell(to: Letter, from: MutableList<Cell<*, *>>): Cell<*, *>? {
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
            topFriend?.setFriend(bottom)
            bottomFriend?.setFriend(top)
        } else if (bottom.hasFriend() && !top.hasFriend()) {
            top.setFriend(bottomFriend)
            bottomFriend?.setFriend(top)
            bottom.setFriend(null)
        } else if (top.hasFriend() && !bottom.hasFriend()) {
            bottom.setFriend(topFriend)
            topFriend?.setFriend(bottom)
            top.setFriend(null)
        }
    }

    //TODO:Double check
    fun restoreSlotPlacements(info: Level) {
        when {
            info.lettersProperties.isNotEmpty() -> {
                info.lettersProperties.forEach { letterProperties ->
                    if (letterProperties.friendIndex != -666) {
                        val slot = cells.filterIsInstance<Slot>()
                            .find { it.homeIndex == letterProperties.friendIndex } as Slot
                        val letter = cells.filterIsInstance<Letter>()
                            .find { it.homeIndex == letterProperties.homeIndex } as Letter
                        letter.x = slot.x
                        letter.y = slot.y
                        letter.setFriend(slot)
                        slot.setFriend(letter)
                        letter.friendIndex = slot.homeIndex
                        letter.currentPos = slot.pos
                        letter.isLocked = letterProperties.locked
                        val colors = letter.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND)
                        if (letterProperties.locked) {
                            letter.setBackgroundColor(letter.effects.getLockedColor())
                        } else {
                            letter.setBackgroundColor(colors.viewEnd)
                        }
                    }
                }
            }
        }

    }
}