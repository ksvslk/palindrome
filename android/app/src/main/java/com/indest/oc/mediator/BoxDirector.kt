package com.indest.oc.mediator

import android.graphics.Rect
import android.util.Log
import android.widget.TextView
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
                val r2 = Rect()
                val r1 = Rect()
                released.getGlobalVisibleRect(r1)
                (droppedTo as TextView).getGlobalVisibleRect(r2)
                if (r1.intersect(r2) && released != droppedTo) {
                    Log.i(TAG, "Adding $droppedTo to candidateBoxes list. Apparently $released hit it!")
                    candidateBoxes.add(droppedTo)
                }
        }

        candidateBoxes.sortedBy {
            abs(released.posX - it.posX) + abs(released.posY - it.posY)
        }
        Log.i(TAG, candidateBoxes.toString())

        when (val first = candidateBoxes.first()) {
                is Letter -> {
                    switchPositions(released, first)
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
        released.sendToNewPosition()
        droppedTo.sendToNewPosition()
    }
}
