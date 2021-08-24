package com.indest.oc.letter

import android.graphics.Rect

sealed interface IBox {
    var currentIndex: Int
    val pos: GlobalPosition
    val rect: Rect
    fun hasFriend(): Boolean
    fun getFriend(): IBox?
    fun setFriend(box: IBox?)
}