package com.indest.heh.letter

import android.content.Context
import android.view.View
import com.indest.heh.util.C.BOTTOM_LAYER

class Slot constructor(
    context: Context?
) : Cell<Slot, Letter>(context!!) {

    private var TAG: String = "Slot ${View.generateViewId()}"

    init {
        z = BOTTOM_LAYER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        homeIndex = (parent as androidx.gridlayout.widget.GridLayout).indexOfChild(this)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun setFriend(cell: Cell<Letter, Slot>?) {
        friend = cell as Letter?
    }

    override fun toString(): String {
        return TAG
    }
}