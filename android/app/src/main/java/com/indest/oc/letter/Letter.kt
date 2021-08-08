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
import com.indest.oc.R
import com.indest.oc.letter.effects.Effects
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.C
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
@SuppressLint("AppCompatCustomView", "ViewConstructor")
class Letter @Inject constructor(
    context: Context?,
    letter: String?,
    initialState: State,
    defStyleRes: Int = R.style.LetterView
) : TextView(context, null, 0, defStyleRes), IBox {

    private var TAG: String = "Letter $letter"
    @Inject lateinit var director: BoxDirector
    @Inject lateinit var  effects: Effects
    var dX: Float = 0.0f
    var dY: Float = 0.0f
    var state: State by Delegates.observable(initialState) {
            _, oldValue, newValue ->
            Log.i(TAG, "$oldValue -> $newValue")
    }
    lateinit var currentPos: GlobalPosition
    lateinit var homePos: GlobalPosition
    private var friend: Slot? = null

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

    override fun onDraw(canvas: Canvas?) {
        state.draw(this)
        super.onDraw(canvas)
    }

    private val touchListen = OnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                if (state == State.HOME) {
                    state = State.DRAGGING
                    state.executeActionDownBehaviour(this, v, e)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (state == State.DRAGGING) {
                    state.executeActionMoveBehaviour(this, v, e)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (state == State.DRAGGING) {
                    state = State.RESOLVING
                    state.executeActionUpBehaviour(this)
                }
            }
            else -> {
                performClick()
            }
        }
        true
    }

    fun addShortVibration(length: Long = 20) {
        if (effects.am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= 26) {
                effects.vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        length,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                effects.vibrator.vibrate(length)
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

    override fun toString(): String {
        return TAG
    }

    init {
        setOnTouchListener(touchListen)
        z = C.NEUTRAL_LAYER
        text = letter
    }
}