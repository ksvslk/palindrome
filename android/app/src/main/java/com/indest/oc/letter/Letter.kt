package com.indest.oc.letter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.indest.oc.R
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.C
import kotlin.properties.Delegates

@SuppressLint("AppCompatCustomView", "ViewConstructor")
class Letter @RequiresApi(Build.VERSION_CODES.O) constructor(
    context: Context?,
    letter: String?,
    letterResources: LetterResources,
    boxDirector: BoxDirector,
    initialState: State,
    defStyleRes: Int = R.style.LetterView
) : TextView(context, null, 0, defStyleRes), IBox {

    private var TAG: String = "Letter $letter"
    var director: BoxDirector = boxDirector
    var dX: Float = 0.0f
    var dY: Float = 0.0f
    var lr: LetterResources = letterResources
    var state: State by Delegates.observable(initialState) {
            _, oldValue, newValue ->
            Log.i(TAG, "$oldValue -> $newValue")
    }
    var currentX: Int = Int.MAX_VALUE
    var currentY: Int = Int.MAX_VALUE
    var homeX: Int = 0
    var homeY: Int = 0
    private var friend: Slot? = null

    override fun onDraw(canvas: Canvas?) {
        state.draw(this)
        super.onDraw(canvas)
    }

    private val touchListen = OnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                state = State.DRAGGING
                state.executeActionDownBehaviour(this, v, e)
            }
            MotionEvent.ACTION_MOVE -> {
                state.executeActionMoveBehaviour(this, v, e)
            }
            MotionEvent.ACTION_UP -> {
                state = State.RESOLVING
                state.executeActionUpBehaviour(this)
            }
            else -> {
                performClick()
            }
        }
        true
    }

    fun addShortVibration(length: Long = 20) {
        if (lr.am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= 26) {
                lr.vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        length,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                lr.vibrator.vibrate(length)
            }
        }
    }

    override fun hasFriend(): Boolean {
        return friend != null
    }

    override fun getFriend(): Slot? {
        return if (hasFriend()) friend
        else null
    }

    override fun setFriend(box: IBox?) {
        friend = box as Slot?
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

    init {
        setOnTouchListener(touchListen)
        z = C.MIDDLE_LAYER
        text = letter
    }
}