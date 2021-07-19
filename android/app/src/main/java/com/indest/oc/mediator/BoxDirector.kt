package com.indest.oc.mediator

import android.graphics.Rect
import android.util.Log
import com.indest.oc.letter.IBox
import com.indest.oc.letter.Letter
import kotlin.math.abs

object BoxDirector : IBoxDirector {
    private val TAG = "BoxDirector"
    override val boxes: MutableList<IBox> = mutableListOf()
    override fun handleCollisions(released: IBox): Boolean {
        println("BoxDirector was notified by $released. to handle collisions!")

        var result = false
        var candidateBoxes = mutableListOf<Letter>()
        var rel = released as Letter

        for (droppedTo in boxes) {
            if (droppedTo is Letter) {
                val r1 = Rect()
                val r2 = Rect()
                released.getHitRect(r1)
                droppedTo.getHitRect(r2)
                if (r1.intersect(r2) && r1 != r2) {
                    candidateBoxes.add(droppedTo)
                }
            }
        }

        candidateBoxes.minByOrNull {
            abs(rel.x - it.x) + abs(rel.y - it.y)
        }.also {
            if (it != null) {
                switchPositions(rel, it)
                result = true
            }
        }

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
