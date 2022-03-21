package com.indest.hah.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.indest.hah.R

class AnyButton : LinearLayoutCompat {

    private lateinit var title: String
    private var icon: Int = -1

    constructor(context: Context) : super(context) {
        initializeViews(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnyButton)
        initResources(typedArray)
        initializeViews(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnyButton)
        initResources(typedArray)
        initializeViews(context)
    }

    private fun initResources(typedArray: TypedArray) {
        title = typedArray.getString(R.styleable.AnyButton_any_button_title).toString()
        icon = typedArray.getResourceId(R.styleable.AnyButton_any_button_icon, -1)
        typedArray.recycle()
    }

    private fun initializeViews(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.any_button, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findViewById<AppCompatTextView>(R.id.any_button_title).text = title
        findViewById<AppCompatImageView>(R.id.any_button_icon).apply {
            this.setImageDrawable(AppCompatResources.getDrawable(context, icon))
            this.setColorFilter(
                ContextCompat.getColor(context, R.color.colorOnPrimary),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }
}
