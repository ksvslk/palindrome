package com.indest.oc.letter

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.indest.oc.util.BOTTOM_LAYER
import kotlin.math.roundToInt

class Slot constructor(
    context: Context?
) : AppCompatTextView(context!!, null), IBox {
    override var homeIndex: Int = -666

    private var TAG: String = "Slot ${View.generateViewId()}"
    private var friend: Letter? = null

    init {
        z = BOTTOM_LAYER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Log.i(TAG, "onSizeChanged")
        homeIndex = (parent as androidx.gridlayout.widget.GridLayout).indexOfChild(this)
        super.onSizeChanged(w, h, oldw, oldh)
    }

override val pos: GlobalPosition
        get() {
            return GlobalPosition(
                (x + width / 2).roundToInt(),
                (y + height / 2).roundToInt()
            )
        }

    override val rect: Rect
        get() {
            val rect = Rect()
            getGlobalVisibleRect(rect)
            return rect
        }

    override fun hasFriend(): Boolean {
        return friend != null
    }

    override fun getFriend(): Letter? {
        return if (hasFriend()) friend
        else null
    }

    override fun setFriend(box: IBox?) {
        friend = box as Letter?
    }

    override fun toString(): String {
        return TAG
    }
}