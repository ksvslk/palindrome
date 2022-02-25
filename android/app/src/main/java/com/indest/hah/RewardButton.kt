package com.indest.hah

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat

class RewardButton : LinearLayoutCompat {

    private lateinit var mainTitle: String
    private lateinit var unlockingSkus: MutableSet<String>
    private lateinit var subtitleRewardUnlocked: String
    private lateinit var subtitleAdAvailable: String
    private lateinit var subtitleAdUnavailable: String
    private var customIcon: Int = -1

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
        unlockingSkus = typedArray.getTextArray(R.styleable.RewardButton_skus).map { it.toString() }.toMutableSet()
        customIcon = typedArray.getResourceId(R.styleable.RewardButton_custom_icon, -1)
        typedArray.recycle()
    }

    private fun initializeViews(context: Context) {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.reward_button, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findViewById<AppCompatTextView>(R.id.reward_button_flipper_hidden_title).text = mainTitle
        findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_title).text = mainTitle
        findViewById<AppCompatImageView>(R.id.reward_button_icon).apply {
            this.setImageDrawable(AppCompatResources.getDrawable(context, customIcon))
            this.setColorFilter(
                ContextCompat.getColor(context, R.color.colorOnPrimary),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    fun updateButtonUi(info: GeneralViewModel.ButtonInfo) {
        val purchased = info.skus.intersect(unlockingSkus).isNotEmpty()
        if (purchased) {
            findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_subtitle).apply {
                this.text = subtitleRewardUnlocked
                setBackgroundColor(ContextCompat.getColor(context, R.color.successBoxColor))
                setTextColor(ContextCompat.getColor(context, R.color.successTextColor))
            }
            return
        }

        when (info.state) {
            ButtonState.AD -> {
                findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_subtitle).apply {
                    this.text = subtitleAdAvailable
                    setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    setTextColor(ContextCompat.getColor(context, R.color.colorOnPrimary))
                }
            }
            ButtonState.NO_AD -> {
                findViewById<AppCompatTextView>(R.id.reward_button_flipper_revealed_subtitle).apply {
                    this.text = subtitleAdUnavailable
                    setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.hintsNotificationColor
                        )
                    )
                    setTextColor(ContextCompat.getColor(context, R.color.colorOnError))
                }
            }
        }
    }
}

enum class ButtonState {
    AD,
    NO_AD
}
