package com.indest.heh.game

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.findFragment
import com.indest.heh.fragments.GamePlayFragment

class CellContainer : LinearLayoutCompat {

    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val fragment = findFragment<GamePlayFragment>()
        if (fragment.gameState == GameState.BUSY) {
            return true
        }
        return false
    }

}