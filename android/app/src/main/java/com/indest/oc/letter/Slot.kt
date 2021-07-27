package com.indest.oc.letter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
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

    lateinit var filler:Letter
    var slotState: Boolean = false
    private var TAG: String = "Slot ${View.generateViewId()}"

    private val touchListen = OnTouchListener { v, e ->
        if (slotState) {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                filler.state = State.DRAGGING
                filler.state.executeActionDownBehaviour(filler, filler, e)
            }
            MotionEvent.ACTION_MOVE -> {
                filler.state.executeActionMoveBehaviour(filler, filler, e)
            }
            MotionEvent.ACTION_UP -> {
                filler.state = State.RESOLVING
                filler.state.executeActionUpBehaviour(filler, this)
            }
            else -> {
                performClick()
            }
        }
            true
        } else {
            false

        }

    }

    init {
        text = TAG
        z = C.BOTTOM_LAYER
        setOnTouchListener(touchListen)
    }

    override fun pos(): GlobalPosition {
        val coordinates = IntArray(2)
        getLocationOnScreen(coordinates)
        return GlobalPosition(
            coordinates[0] + width / 2,
            coordinates[1] + height / 2
        )
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