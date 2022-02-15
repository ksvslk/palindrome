package com.indest.hah.letter

import android.content.Context
import android.view.View
import com.indest.hah.util.C.BOTTOM_LAYER

class Slot constructor(
    context: Context?, realFriendChar: Char
) : Cell<Slot, Letter>(context!!) {

    constructor(context: Context) : this(context, '-')

    var realChar: Char = realFriendChar
    private var TAG: String = "Slot ${View.generateViewId()}"

    init {
        translationZ = BOTTOM_LAYER
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