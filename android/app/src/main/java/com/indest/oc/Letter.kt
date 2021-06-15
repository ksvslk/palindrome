package com.indest.oc

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.log10
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        letter: String?,
        defStyleRes: Int = R.style.LetterView
    ) : super(context, null, 0, defStyleRes) {
        id =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
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

    @SuppressLint("ClickableViewAccessibility")
    private val touchListen = OnTouchListener { v, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = v.x - e.rawX
                dY = v.y - e.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                v.x = e.rawX + dX
                v.y = e.rawY + dY
            }
            MotionEvent.ACTION_UP -> {
                //this.isActivated = false
                val intersects = collision(this, v)
                Log.i(TAG, "Intersects: $intersects, with ${(v as Letter).TAG}")

                val distance = log2(sqrt((dX - oriX).pow(2) + (dY - oriY).pow(2)))

                v.animate()
                    .x(oriX)
                    .y(oriY)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setDuration(distance.toLong() * 50)
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