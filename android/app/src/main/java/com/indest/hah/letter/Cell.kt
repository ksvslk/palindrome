package com.indest.hah.letter

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.roundToInt

abstract class Cell<T, F>(context: Context) : AppCompatTextView(context) {

    var homeIndex: Int = -666
    private var TAG: String = "${javaClass.name} ${View.generateViewId()}"

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