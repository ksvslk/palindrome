package com.indest.hah.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import com.indest.hah.other.C.BOTTOM_LAYER

@SuppressLint("ViewConstructor", "RestrictedApi")
class Slot constructor(
    context: Context?, realFriendChar: Char
) : Cell<Slot, Letter>(context!!) {

    constructor(context: Context) : this(context, '-')

    var realChar: Char = realFriendChar
    private var TAG: String = "Slot ${View.generateViewId()}"
    var isHyphen = false

    init {
        translationZ = BOTTOM_LAYER
        setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
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