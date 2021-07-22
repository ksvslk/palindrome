package com.indest.oc.letter

import android.graphics.Rect

sealed interface IBox {
    fun coords(): Pair<Int, Int>
    fun rect(): Rect
}