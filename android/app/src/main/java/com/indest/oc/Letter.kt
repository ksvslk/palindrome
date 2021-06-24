package com.indest.oc

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt
import android.content.Context
import android.media.SoundPool


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
    private val paint = Paint()
    private val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val soundPool: SoundPool = SoundPool.Builder().build()

    @RequiresApi(Build.VERSION_CODES.O)
    constructor(
        context: Context?,
        letter: String?,
        defStyleRes: Int = R.style.LetterView
    ) : super(context, null, 0, defStyleRes) {
        Log.d(TAG,"constructor")
        id =
            View.generateViewId()
        TAG = "Letter$id"
        text = letter
        setOnTouchListener(touchListen)
        paint.color = ResourcesCompat.getColor(resources, R.color.zeus, null)
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
                addShortVibration()
                val id = soundPool.load(context, R.raw.start_drag, 1)
                Log.d(TAG, id.toString())
                soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f)
                z = 1.0f
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
                    .withEndAction { z = 0.0f }
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
        //Log.i(TAG, r1.flattenToString())
        //Log.i(TAG, r2.flattenToString())
        return r1.intersect(r2)
    }

    private fun addShortVibration(length: Long = 20) {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(length, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(length)
        }
    }
}