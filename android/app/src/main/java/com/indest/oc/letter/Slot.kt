package com.indest.oc.letter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.indest.oc.util.C
import com.indest.oc.R

@SuppressLint( "AppCompatCustomView", "ViewConstructor")
class Slot @RequiresApi(Build.VERSION_CODES.O) constructor(
    context: Context?,
    defStyleRes: Int = R.style.EmptyBoxView
) : TextView(context, null, 0, defStyleRes), IBox {

    private var TAG: String = "Slot ${View.generateViewId()}"
    private var friend: Letter? = null

    init {
        //text = TAG
        z = C.BOTTOM_LAYER
    }

    override val pos: GlobalPosition
        get() {
            val coordinates = IntArray(2)
            getLocationOnScreen(coordinates)
            return GlobalPosition(
                coordinates[0] + width / 2,
                coordinates[1] + height / 2)
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