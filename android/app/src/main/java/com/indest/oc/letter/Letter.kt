package com.indest.oc.letter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt
import android.content.Context
import android.media.AudioManager
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.ViewCompat
import com.indest.oc.R
import com.indest.oc.mediator.IBoxDirector

@SuppressLint("ViewConstructor", "AppCompatCustomView")
class Letter : TextView, IBox {

    private var TAG: String = "Letter"
    private var dX: Float = 0.0f
    private var dY: Float = 0.0f
    private var oriX: Float = Float.NaN
    private var oriY: Float = Float.NaN
    private lateinit var director: IBoxDirector
    private lateinit var lr: LetterResources

    @RequiresApi(Build.VERSION_CODES.O)
    constructor(
        context: Context?,
        letter: String?,
        letterResources: LetterResources,
        boxDirector: IBoxDirector,
        defStyleRes: Int = R.style.LetterView
    ) : super(context, null, 0, defStyleRes) {
        lr = letterResources
        director = boxDirector
        id = View.generateViewId()
        TAG = "Letter$id"
        text = letter
        setOnTouchListener(touchListen)
    }

    override fun onDraw(canvas: Canvas?) {
        if (oriX.isNaN() && oriY.isNaN()) {
            oriX = x
            oriY = y
        }
        super.onDraw(canvas)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    private val touchListen = OnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                // Calculate the distance moved
                dX = v.x - e.rawX
                dY = v.y - e.rawY

                // Draggable view and its parents parent should be always on top
                ViewCompat.setTranslationZ((parent.parent as LinearLayoutCompat), 1.0f)
                z = 1.0f

                // Sound effects
                addShortVibration()
                val id = lr.soundPool.load(context, R.raw.start_drag, 1)
                lr.soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f)

                // Color effects
                ObjectAnimator.ofArgb(this, "backgroundColor", lr.viewStartColor, lr.viewEndColor).apply {
                    duration = lr.onClickColorChangeDuration
                    start()
                }
                ObjectAnimator.ofArgb(this, "textColor", lr.textStartColor, lr.textEndColor).apply {
                    duration = lr.onClickColorChangeDuration
                    start()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Calculate the distance moved
                v.x = e.rawX + dX
                v.y = e.rawY + dY
            }
            MotionEvent.ACTION_UP -> {
                director.handleCollisions(this)
                // Logarithm (to get some value that represents how much was moved ignoring the
                // actual physical distance) applied to Euclidean distance between start point
                // and end point
                val distance = log2(sqrt((dX - oriX).pow(2) + (dY - oriY).pow(2)))

                // Color effects
                ObjectAnimator.ofArgb(this, "backgroundColor", lr.viewEndColor, lr.viewStartColor).apply {
                    duration = lr.onReleaseColorChangeDuration
                    start()
                }
                ObjectAnimator.ofArgb(this, "textColor", lr.textEndColor, lr.textStartColor).apply {
                    duration = lr.onReleaseColorChangeDuration
                    start()
                }

                // Here we use the log-distance to calculate the duration of the animation
                // longer distance should be only slightly longer animation and vice-versa
                v.animate()
                    .x(oriX)
                    .y(oriY)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(distance.toLong() * lr.animateToHomeDurationMultiplier)
                    .withEndAction { z = 0.0f }
                    .start()
            }
        }

        true
    }

    private fun collision(v1: View, v2: View): Triple<Boolean, String, String> {
        val r1 = Rect()
        val r2 = Rect()
        v1.getHitRect(r1)
        v2.getHitRect(r2)
        return Triple(
            r1.intersect(r2),
            (v1 as Letter).text.toString(),
            (v2 as Letter).text.toString()
        )
    }

    private fun addShortVibration(length: Long = 20) {
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
}