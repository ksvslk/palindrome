package com.indest.hah.letter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View.OnTouchListener
import com.indest.hah.letter.effects.Effects
import com.indest.hah.letter.effects.SoundSelector
import com.indest.hah.util.C.NEUTRAL_LAYER
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("ViewConstructor")
@AndroidEntryPoint
class Letter @Inject constructor(
    context: Context,
    letter: String?,
    initialState: LetterState
) : Cell<Letter, Slot>(context) {

    @Inject
    lateinit var effects: Effects
    lateinit var currentPos: GlobalPosition
    lateinit var homePos: GlobalPosition
    var dX: Float = 0.0f
    var dY: Float = 0.0f
    var friendIndex: Int = -666
    var state: LetterState = initialState

    override fun onDraw(canvas: Canvas?) {
        state.draw(this)
        super.onDraw(canvas)
    }

    private val touchListen = OnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                if (state == LetterState.HOME) {
                    effects.addShortVibration()
                    effects.playSound(SoundSelector.DRAGGING_LETTER)
                    state = LetterState.DRAGGING
                    state.executeActionDownBehaviour(this, v, e)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (state == LetterState.DRAGGING) {
                    state.executeActionMoveBehaviour(this, v, e)
                }
            }
            else -> {
                if (state == LetterState.DRAGGING) {
                    state = LetterState.RESOLVING
                    state.executeActionUpBehaviour(this)
                }
                performClick()
            }
        }
        true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        homePos = pos
        currentPos = pos
        homeIndex = (parent as androidx.gridlayout.widget.GridLayout).indexOfChild(this)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun setFriend(cell: Cell<Slot, Letter>?) {
        if (cell != null) {
            friend = cell as Slot?
            friendIndex = cell.homeIndex
        } else {
            friend = cell
            friendIndex = -666
        }
    }

    init {
        setOnTouchListener(touchListen)
        z = NEUTRAL_LAYER
        text = letter
    }
}