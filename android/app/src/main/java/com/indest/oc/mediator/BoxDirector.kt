package com.indest.oc.mediator

import com.indest.oc.letter.IBox

object BoxDirector : IBoxDirector {
    override val boxes: MutableList<IBox> = mutableListOf()
    override fun handleCollisions(box: IBox) {
        println("BoxDirector was notified by $box. to handle collisions!")
    }
}
