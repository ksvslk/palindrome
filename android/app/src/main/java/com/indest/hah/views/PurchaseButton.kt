package com.indest.hah.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.indest.hah.R
import com.indest.hah.general.GeneralViewModel

private const val REGULAR_VISIBILITY_CASES = 1
private const val SPECIAL_VISIBILITY_CASES = 5
private const val SENTENCE_WORD_COUNT = 2

class PurchaseButton : LinearLayoutCompat {

    private var visibilityChangingSkus: MutableList<Set<String>> = mutableListOf()
    private var bestLabel: String? = null
    var sku: String? = null

    constructor(context: Context) : super(context) {
        initializeViews(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PurchaseButton)
        initResources(typedArray)
        initializeViews(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PurchaseButton)
        initResources(typedArray)
        initializeViews(context)
    }

    private fun initResources(typedArray: TypedArray) {
        val case1 = typedArray.getTextArray(R.styleable.PurchaseButton_visibility_case_1)
        val case2 = typedArray.getTextArray(R.styleable.PurchaseButton_visibility_case_2)
        val case3 = typedArray.getTextArray(R.styleable.PurchaseButton_visibility_case_3)
        val case4 = typedArray.getTextArray(R.styleable.PurchaseButton_visibility_case_4)
        val case5 = typedArray.getTextArray(R.styleable.PurchaseButton_visibility_case_5)
        for (case in listOf(case1, case2, case3, case4, case5)) {
            if (case != null) {
                visibilityChangingSkus.add(case.map { it.toString() }.toSet())
            }
        }
        bestLabel = typedArray.getString(R.styleable.PurchaseButton_best_label)
        sku = typedArray.getString(R.styleable.PurchaseButton_sku)
        typedArray.recycle()
    }

    private fun initializeViews(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.purchase_button, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findViewById<AppCompatImageView>(R.id.purchase_button_icon).apply {
            this.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.baseline_lock_black_36
                )
            )

        }
        showLabelIfApplicable()
        this.visibility = View.GONE
    }

    private fun showLabelIfApplicable()
    {
        if (bestLabel != null) {
            val label = findViewById<AppCompatTextView>(R.id.purchase_button_label)
            label.visibility = View.VISIBLE
            label.text = bestLabel
        }
        else {
            hideLabel()
        }
    }

    private fun hideLabel()
    {
        findViewById<AppCompatTextView>(R.id.purchase_button_label).visibility = View.GONE
    }

    private fun setTitle(title: String)
    {
        findViewById<AppCompatTextView>(R.id.purchase_button_title).text = title
    }

    private fun setPrice(price: String)
    {
        findViewById<AppCompatTextView>(R.id.purchase_button_subtitle).text = price
    }

    fun updateButtonUi(info: GeneralViewModel.ButtonInfo) {
        info.skus[sku]?.let {
            setTitle(it.title)
            setPrice(it.price)
        }
        val skus = info.skus.values.filter { it.isPurchased }.map { it.sku }
        var purchasedInSomeWay = false

        // Regular purchase button
        if (visibilityChangingSkus.size == REGULAR_VISIBILITY_CASES) {
            purchasedInSomeWay = skus.intersect(visibilityChangingSkus[0]).isNotEmpty()
        }
        // Full ads purchase button
        else if (visibilityChangingSkus.size == SPECIAL_VISIBILITY_CASES) {
            if (skus.isNotEmpty()) hideLabel()
            else showLabelIfApplicable()
            visibilityChangingSkus.forEach {
                if (it.intersect(skus.toSet()) == it) {
                    purchasedInSomeWay = true
                    return@forEach
                }
            }
        }

        when {
            purchasedInSomeWay -> {
                this.setOnClickListener(null)
                this.visibility = View.GONE
            }
            info.skus[sku]?.canPurchase == true -> {
                this.visibility = View.VISIBLE
            }
            info.skus[sku]?.canPurchase == false -> {
                this.visibility = View.GONE
            }
        }
    }
}
