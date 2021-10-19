package com.indest.heh.letter

import android.graphics.Rect

sealed interface IBox {
    var homeIndex: Int
    val pos: GlobalPosition
    val rect: Rect
    fun hasFriend(): Boolean
    fun getFriend(): IBox?
    fun setFriend(box: IBox?)
}