package com.indest.hah.game

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.LinearLayoutCompat

class CellContainer : LinearLayoutCompat {

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.d("TEST", "onInterceptTouchEvent")
        return false
/*        val fragment = findFragment<GamePlayFragment>()
        if (fragment.gameState == GameState.READY) {
            return false
        }
        return true*/
    }
}