package com.indest.oc.letter

import android.graphics.Rect

sealed interface IBox {
    fun pos(): GlobalPosition
    fun rect(): Rect
}