package com.indest.oc

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
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
import android.media.AudioManager
import android.media.SoundPool
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.ViewCompat

@SuppressLint("ViewConstructor", "AppCompatCustomView")
class Letter : TextView {

    private var TAG: String = "Letter"
    private var dX: Float = 0.0f
    private var dY: Float = 0.0f
    private var oriX: Float = Float.NaN
    private var oriY: Float = Float.NaN
    private val onClickColorChangeDuration: Long = 200L
    private val onReleaseColorChangeDuration: Long = 200L
    private val animateToHomeDurationMultiplier = 50
    private val viewStartColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
    private val viewEndColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorSecondary, null)
    private val textStartColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorOnPrimary, null)
    private val textEndColor: Int =
        ResourcesCompat.getColor(resources, R.color.colorOnSecondary, null)
    private val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val soundPool: SoundPool = SoundPool.Builder().build()
    private var am: AudioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @RequiresApi(Build.VERSION_CODES.O)
    constructor(
        context: Context?,
        letter: String?,
        defStyleRes: Int = R.style.LetterView
    ) : super(context, null, 0, defStyleRes) {
        Log.d(TAG, "constructor")
        id =
            View.generateViewId()
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

                ViewCompat.setTranslationZ((parent.parent as LinearLayoutCompat), 1.0f)

                addShortVibration()
                val id = soundPool.load(context, R.raw.start_drag, 1)
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
                val result = collision(this, v)
                if (result.first)
                {
                    Log.i(TAG, "${result.second} intersects with ${result.third}!")
                }

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
        if (am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        length,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(length)
            }
        }
    }
}