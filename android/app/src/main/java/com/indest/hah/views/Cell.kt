package com.indest.hah.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.gridlayout.widget.GridLayout
import com.indest.hah.R
import com.indest.hah.game.director.data.GlobalPosition
import kotlin.math.roundToInt

abstract class Cell<T, F>(context: Context) : AppCompatTextView(context) {

    var homeIndex: Int = -666
    private var TAG: String = "${javaClass.name} ${View.generateViewId()}"

    init {
        val params = GridLayout.LayoutParams()
        val dp = context.resources.getDimensionPixelSize(R.dimen._2sdp)
        params.setMargins(dp, dp, dp, dp)
        layoutParams = params
    }

    val pos: GlobalPosition
        get() {
            return GlobalPosition(
                (x + width / 2).roundToInt(),
                (y + height / 2).roundToInt()
            )

        }

    var friend: F? = null

    val rect: Rect
        get() {
            val rect = Rect()
            getGlobalVisibleRect(rect)
            return rect
        }

    fun hasFriend(): Boolean {
        return friend != null
    }

    fun getTheFriend(): F? {
        return if (hasFriend()) friend
        else null
    }

    abstract fun setFriend(cell: Cell<F, T>?)

    override fun toString(): String {
        return TAG
    }
}