package com.indest.hah.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewFlipper
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.indest.hah.R
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.views.ButtonState.*

class RewardButton : LinearLayoutCompat {

    private lateinit var mainTitle: String
    private lateinit var unlockingSkus: MutableSet<String>
    private lateinit var subtitleRewardUnlocked: String
    private lateinit var subtitleAdAvailable: String
    private lateinit var subtitleAdUnavailable: String
    private var customIcon: Int = -1
    var buttonState: ButtonState = NO_AD

    constructor(context: Context) : super(context) {
        initializeViews(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RewardButton)
        initResources(typedArray)
        initializeViews(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RewardButton)
        initResources(typedArray)
        initializeViews(context)
    }

    private fun initResources(typedArray: TypedArray) {
        mainTitle = typedArray.getString(R.styleable.RewardButton_main_title).toString()
        subtitleAdAvailable =
            typedArray.getString(R.styleable.RewardButton_subtitle_ad_available).toString()
        subtitleAdUnavailable =
            typedArray.getString(R.styleable.RewardButton_subtitle_ad_unavailable).toString()
        subtitleRewardUnlocked =
            typedArray.getString(R.styleable.RewardButton_subtitle_reward_unlocked).toString()
        unlockingSkus = typedArray.getTextArray(R.styleable.RewardButton_skus).map { it.toString() }
            .toMutableSet()
        customIcon = typedArray.getResourceId(R.styleable.RewardButton_custom_icon, -1)
        typedArray.recycle()
    }

    private fun initializeViews(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.reward_button, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findViewById<AppCompatTextView>(R.id.reward_button_flipper_hidden_title).text = mainTitle
        findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_title).text = mainTitle
        findViewById<AppCompatImageView>(R.id.reward_button_icon).apply {
            this.setImageDrawable(AppCompatResources.getDrawable(context, customIcon))
        }
    }

    fun showClue()
    {
        findViewById<ViewFlipper>(R.id.reward_button_flipper).showNext()
        this.isClickable = false
    }

    fun setClue(clue: String)
    {
        findViewById<AppCompatTextView>(R.id.reward_button_flipper_hidden_title).text = clue
    }

    fun updateButtonUi(info: GeneralViewModel.ButtonInfo) {
        val state = if (info.skus.values
                .filter { it.isPurchased }
                .map { it.sku }
                .intersect(unlockingSkus)
                .isNotEmpty()
        ) PREMIUM else info.state

        when (state) {
            AD -> {
                this.isClickable = true
                buttonState = AD
                findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_subtitle).apply {
                    this.text = subtitleAdAvailable
                    setBackgroundColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.RED))
                    setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, Color.RED))
                }
            }
            NO_AD -> {
                this.isClickable = false
                buttonState = NO_AD
                findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_subtitle).apply {
                    if (info.internet) this.text = subtitleAdUnavailable
                    else this.text = context.getString(R.string.no_internet)

                    setBackgroundColor(
                        MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.RED)
                    )
                    setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondary, Color.RED))
                }
            }
            PREMIUM -> {
                this.isClickable = true
                buttonState = PREMIUM
                findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_subtitle).apply {
                    this.text = subtitleRewardUnlocked
                    setBackgroundColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, Color.RED))
                    setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.RED))
                }
            }
        }
    }
}

enum class ButtonState {
    AD,
    NO_AD,
    PREMIUM
}
