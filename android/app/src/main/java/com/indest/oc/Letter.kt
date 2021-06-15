package com.indest.oc

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("ViewConstructor", "AppCompatCustomView")
class Letter : TextView {

    private var TAG: String = "Letter"
    private var dX: Float = 0.0f
    private var dY: Float = 0.0f
    private var oriX: Float = Float.NaN
    private var oriY: Float = Float.NaN
    private val onClickColorChangeDuration: Long = 300L
    private val onReleaseColorChangeDuration: Long = 500L
    private val animateToHomeDurationMultiplier = 50
    private val viewStartColor: Int = ResourcesCompat.getColor(resources, R.color.sandal, null)
    private val viewEndColor: Int = ResourcesCompat.getColor(resources, R.color.sandstone, null)
    private val textStartColor: Int = ResourcesCompat.getColor(resources, R.color.zeus, null)
    private val textEndColor: Int = ResourcesCompat.getColor(resources, R.color.wafer, null)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        letter: String?,
        defStyleRes: Int = R.style.LetterView
    ) : super(context, null, 0, defStyleRes) {
        id =
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                View.generateViewId()
            } else {
                ViewCompat.generateViewId()
            }
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility")
    private val touchListen = OnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                ObjectAnimator.ofArgb(this, "backgroundColor", viewStartColor, viewEndColor).apply {
                    duration = onClickColorChangeDuration
                    start()
                }
                ObjectAnimator.ofArgb(this, "textColor", textStartColor, textEndColor).apply {
                    duration = onClickColorChangeDuration
                    start()
                }
                dX = v.x - e.rawX
                dY = v.y - e.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                v.x = e.rawX + dX
                v.y = e.rawY + dY
            }
            MotionEvent.ACTION_UP -> {
                val distance = log2(sqrt((dX - oriX).pow(2) + (dY - oriY).pow(2)))

                ObjectAnimator.ofArgb(this, "backgroundColor", viewEndColor, viewStartColor).apply {
                    duration = onReleaseColorChangeDuration
                    start()
                }
                ObjectAnimator.ofArgb(this, "textColor", textEndColor, textStartColor).apply {
                    duration = onReleaseColorChangeDuration
                    start()
                }
                val intersects = collision(this, v)
                Log.i(TAG, "Intersects: $intersects, with ${(v as Letter).TAG}")

                v.animate()
                    .x(oriX)
                    .y(oriY)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(distance.toLong() * animateToHomeDurationMultiplier)
                    .start()
            }
        }
        true
    }

    private fun collision(v1: View, v2: View): Boolean {
        val r1 = Rect()
        val r2 = Rect()
        v1.getHitRect(r1)
        v2.getHitRect(r2)
        Log.i(TAG, r1.flattenToString())
        Log.i(TAG, r2.flattenToString())
        return r1.intersect(r2)
    }
}