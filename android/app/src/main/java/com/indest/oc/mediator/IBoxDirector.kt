package com.indest.oc.mediator

import com.indest.oc.letter.IBox
import com.indest.oc.letter.Letter

sealed interface IBoxDirector {
    val boxes: MutableList<IBox>
    fun handleCollisions(box: Letter) : Boolean
}