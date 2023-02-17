package com.indest.hah.views

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.indest.hah.R

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

    override fun dispatchDraw(canvas: Canvas?) {
        if (isSadMode) canvas?.saveLayer(null, paint)
        super.dispatchDraw(canvas)
        if (isSadMode) canvas?.restore()
    }

}