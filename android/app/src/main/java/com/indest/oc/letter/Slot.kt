package com.indest.oc.letter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.nfc.Tag
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.indest.oc.R
import com.indest.oc.mediator.IBoxDirector
import java.lang.System.out

@SuppressLint("ViewConstructor", "AppCompatCustomView")
class Slot @RequiresApi(Build.VERSION_CODES.O) constructor(
    context: Context?,
    boxDirector: IBoxDirector,
    defStyleRes: Int = R.style.EmptyBoxView
) : TextView(context, null, 0, defStyleRes), IBox {

    private var TAG: String
    private var director: IBoxDirector = boxDirector

    init {
        TAG = "Slot ${View.generateViewId()}"
        text = TAG
    }

    override fun coords(): Pair<Int, Int> {
        val coords = IntArray(2)
        getLocationOnScreen(coords)
        val centerX = coords[0] + width / 2
        val centerY = coords[0] + height / 2
        return Pair(centerX, centerY)
    }

    override fun rect(): Rect {
        val rect = Rect()
        getGlobalVisibleRect(rect)
        return rect
    }

    override fun toString(): String {
        return TAG
    }
}