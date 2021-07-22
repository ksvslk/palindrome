package com.indest.oc.mediator

import android.util.Log
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.ViewCompat
import com.indest.oc.letter.IBox
import com.indest.oc.letter.Letter
import com.indest.oc.letter.Slot
import kotlin.math.abs

object BoxDirector : IBoxDirector {
    private val TAG = "BoxDirector"
    override val boxes: MutableList<IBox> = mutableListOf()
    override fun handleCollisions(released: Letter): Boolean {

        println("BoxDirector was notified by $released. to handle collisions!")
        var result = false
        val candidateBoxes = mutableListOf<IBox>()

        for (droppedTo in boxes) {
                if (released.rect().intersect(droppedTo.rect())&& released != droppedTo) {
                    Log.i(TAG, "Adding $droppedTo to candidateBoxes list. Apparently $released hit it!")
                    candidateBoxes.add(droppedTo)
                }
        }

        var closest = candidateBoxes.minByOrNull {
            abs(released.coords().first - it.coords().first) +
            abs(released.coords().second - it.coords().second)
        }

        candidateBoxes.forEach { m -> Log.i(TAG, (abs(released.coords().first - m.coords().first) +
                abs(released.coords().second - m.coords().second)).toString()) }

        Log.i(TAG, candidateBoxes.toString())

        when (closest) {
                is Letter -> {
                    switchPositions(released, closest)
                    result = true
                }
                is Slot -> {
                    TODO("Method for placing letter in empty slot")
                }
        }

        candidateBoxes.clear()
        return result
        TODO("Handle dropping onto empty box")
    }

    private fun switchPositions(released: Letter, droppedTo: Letter) {
        Log.i(TAG, "Switching ${released.text} home position with ${droppedTo.text}!")
        val tempX = released.oriX
        val tempY = released.oriY
        released.oriX = droppedTo.oriX
        released.oriY = droppedTo.oriY
        droppedTo.oriX = tempX
        droppedTo.oriY = tempY
        ViewCompat.setTranslationZ((droppedTo.parent.parent as LinearLayoutCompat), 0.5f)
        droppedTo.z = 0.5f
        ViewCompat.setTranslationZ((released.parent.parent as LinearLayoutCompat), 1f)
        released.z = 1f
        released.sendToNewPosition()
        droppedTo.sendToNewPosition()
    }
}
