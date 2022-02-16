package com.indest.hah.game

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.LinearLayoutCompat

class CellContainer : LinearLayoutCompat {

    var notClickable:Boolean = false

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return notClickable
    }
}