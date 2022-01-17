package com.indest.hah.game

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.findFragment
import com.indest.hah.fragments.GamePlayFragment

class CellContainer : LinearLayoutCompat {

    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.i("TEST", "onInterceptTouchEvent")
        val fragment = findFragment<GamePlayFragment>()
        if (fragment.gameState == GameState.READY) {
            return false
        }
        return true
    }
}