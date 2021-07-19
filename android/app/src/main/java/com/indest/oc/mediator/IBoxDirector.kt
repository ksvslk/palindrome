package com.indest.oc.mediator

import com.indest.oc.letter.IBox

sealed interface IBoxDirector {
    val boxes: MutableList<IBox>
    fun handleCollisions(box: IBox)
}