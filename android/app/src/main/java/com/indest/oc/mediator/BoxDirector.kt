package com.indest.oc.mediator

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.util.Log
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.coroutineScope
import com.indest.oc.letter.*
import com.indest.oc.letter.effects.ColorSelector
import com.indest.oc.letter.effects.SoundSelector
import com.indest.oc.persistance.Level
import com.indest.oc.persistance.LevelRepository
import com.indest.oc.util.MIDDLE_LAYER
import com.indest.oc.util.TOP_LAYER
import javax.inject.Inject
import kotlin.math.abs
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ActivityScoped
class BoxDirector @Inject constructor(
    val levelRepository: LevelRepository
): LifecycleObserver {
    private val TAG = "BoxDirector"
    var boxes: MutableList<IBox> = ArrayList()
    lateinit var currentLevel: Level
    lateinit var scope: CoroutineScope

    fun registerLifeCycleOwner(lifecycle: Lifecycle)
    {
        lifecycle.addObserver(this)
        scope = lifecycle.coroutineScope
    }

    fun handleCollisions(box: Letter) {
        Log.i(TAG, "$box called handleCollisions.")
        Log.i(TAG, "$boxes")
        when (val closestBox = getClosestBox(to = box, from = getCandidateBoxes(box))) {
            is Letter -> {
                Log.i(TAG, "Closest is $closestBox and its friend is ${closestBox.getFriend()}!")
                if (closestBox.state == LetterState.HOME) {
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

        sendLetterHome(box)
    }

    private fun getCandidateBoxes(box: Letter): MutableList<IBox> {
        val candidateBoxes = mutableListOf<IBox>()

        for (droppedTo in boxes) {
            if (box.rect.intersect(droppedTo.rect) && box != droppedTo) {
                if ((droppedTo is Letter) && droppedTo.state == LetterState.HOME) {
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

        scope.launch {
            levelRepository.updateLetter(
                homeIndex = topIndex,
                letter = bottomLetter,
                level = currentLevel.levelId
            )
            levelRepository.updateLetter(
                homeIndex = bottomIndex,
                letter = topLetter,
                level = currentLevel.levelId
            )
        }

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
        Log.i(TAG, "Placing $letter to empty $slot!")
        letter.effects.playSound(SoundSelector.PLACE_LETTER_IN_EMPTY_BOX)
        letter.getFriend()?.setFriend(null)
        slot.setFriend(letter)
        letter.setFriend(slot)
        letter.currentPos.x = slot.pos.x
        letter.currentPos.y = slot.pos.y
        letter.z = TOP_LAYER
        letter.elevation = TOP_LAYER
        letter.state = LetterState.ANIMATION
        letter.state.executeAnimationBehaviour(
            letter,
            (slot.pos.x - letter.pos.x),
            (slot.pos.y - letter.pos.y)
        )
        scope.launch {
            levelRepository.updateFriendIndex(
                homeIndex = letter.homeIndex,
                newFriendIndex = slot.homeIndex,
                level = currentLevel.levelId
            )
        }
    }

    fun checkSolution()
    {
        val letters = boxes.filterIsInstance<Slot>().mapNotNull {
            if (it.getFriend()?.text?.contains("^([?!.,’])".toRegex()) == false) it.getFriend()
            else null
        }
        val odd = letters.size % 2 != 0
        val pairs = letters.foldIndexed(mutableListOf(), {
                index: Int,
                acc: List<Pair<Letter,Any>>,
                l: Letter ->
            if (index < letters.size / 2) acc + Pair(l, letters[letters.size - 1 - index])
            else if (index == letters.size / 2 && odd) acc + Pair(l, "*")
            else acc
        })

        val animatorSet = AnimatorSet()
        for (p in pairs) {
            val colorCurrent = p.first.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND).viewStart
            val textColorCurrent = p.first.effects.getColors(ColorSelector.ACTION_DOWN_FRIEND).textStart
            val animations = animatorSet.childAnimations
            if (p.second is Letter) {
                val color = if (p.first.text == (p.second as Letter).text) Color.GREEN else Color.BLACK
                val textColor = if (p.first.text == (p.second as Letter).text) textColorCurrent else Color.WHITE
                val a1 = ObjectAnimator.ofArgb(p.first, "backgroundColor", colorCurrent, color).apply {
                    interpolator = FastOutLinearInInterpolator()
                    duration = 350 }
                val a2 = ObjectAnimator.ofArgb(p.second, "backgroundColor", colorCurrent, color).apply {   interpolator = FastOutLinearInInterpolator()
                    duration = 350 }
                val a1t = ObjectAnimator.ofArgb(p.first, "textColor", textColorCurrent, textColor).apply {   interpolator = FastOutLinearInInterpolator()
                    duration = 350 }
                val a2t = ObjectAnimator.ofArgb(p.second, "textColor", textColorCurrent, textColor).apply {   interpolator = FastOutLinearInInterpolator()
                    duration = 350 }
                if (animations.size > 0) animatorSet.play(a1).with(a2).with(a1t).with(a2t).after(animations.last())
                else animatorSet.playTogether(a1, a2, a1t, a2t)
                if (color == Color.BLACK) break
            }
            else {
                val a = ObjectAnimator.ofArgb(p.first, "backgroundColor", colorCurrent, Color.GREEN).apply {   interpolator = FastOutLinearInInterpolator()
                    duration = 350 }
                if (animations.size > 0) animatorSet.play(a).after(animations.last())
                else animatorSet.playTogether(a)
            }
        }

        Log.i(TAG, "Animations: ${animatorSet.childAnimations}")
        animatorSet.start()
    }

    private fun sendLetterHome(letter: Letter) {
        if (letter.hasFriend()) {
            Log.i(TAG, "Going to real home!")
            letter.getFriend()?.setFriend(null)
            letter.setFriend(null)

            scope.launch {
                levelRepository.updateFriendIndex(
                    homeIndex = letter.homeIndex,
                    newFriendIndex = -666,
                    level = currentLevel.levelId
                )
            }

            letter.state = LetterState.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.homePos.x - letter.pos.x),
                (letter.homePos.y - letter.pos.y)
            )
        } else {
            Log.i(TAG, "No collisions detected (or Letter/Slot not good) - going to current home!")
            letter.state = LetterState.ANIMATION
            letter.state.executeAnimationBehaviour(
                letter,
                (letter.currentPos.x - letter.pos.x),
                (letter.currentPos.y - letter.pos.y)
            )
        }
    }

    private fun getClosestBox(to: Letter, from: MutableList<IBox>): IBox? {
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