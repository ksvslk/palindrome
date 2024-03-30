package com.indest.hah.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

class HahConstraintLayout : ConstraintLayout {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context!!, attrs, defStyleAttr, defStyleRes)

    var isSadMode: Boolean = true
    private var paint: Paint

    init {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (isSadMode) {
            val count = childCount
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child.tag == "AdViewTag") {
                    // Draw this child with different saturation
                    canvas.save()
                    canvas.concat(child.matrix)
                    canvas.translate(child.left.toFloat(), child.top.toFloat())
                    child.draw(canvas)
                    canvas.restore()
                } else {
                    // Apply general saturation adjustment
                    canvas.saveLayer(null, paint)
                    canvas.concat(child.matrix)
                    canvas.translate(child.left.toFloat(), child.top.toFloat())
                    child.draw(canvas)
                    canvas.restore()
                }
            }
        }
        else {
            super.dispatchDraw(canvas)
        }
    }

}