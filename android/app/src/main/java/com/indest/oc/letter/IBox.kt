package com.indest.oc.letter

import android.graphics.Rect

sealed interface IBox {
    fun hasFriend(): Boolean
    fun getFriend(): IBox?
    fun setFriend(box: IBox?)
    fun pos(): GlobalPosition
    fun rect(): Rect
}