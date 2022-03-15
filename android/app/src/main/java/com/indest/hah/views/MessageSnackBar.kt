package com.indest.hah.views

import android.animation.ObjectAnimator
import android.content.Context
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.indest.hah.R
import com.indest.hah.views.MessageSnackBar.Companion.TextSize.*
import com.indest.hah.game.levelcreation.WindowSizeClass
import com.indest.hah.other.BookMode
import com.indest.hah.other.findSuitableParent

class MessageSnackBar(
    parent: ViewGroup,
    content: MessageSnackView
) : BaseTransientBottomBar<MessageSnackBar>(parent, content, content) {

    init {
        getView().setBackgroundColor(
            ContextCompat.getColor(
                view.context,
                android.R.color.transparent
            )
        )
        val bottomPadding = view.context.resources.displayMetrics.heightPixels / 2
        getView().setPadding(0, 0, 0, bottomPadding)
        val params = getView().layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.CENTER
        getView().layoutParams = params
    }

    companion object {

        private var tapToContinueAnimation:ObjectAnimator? = null

        fun make(
            view: View,
            message: String,
            duration: Int,
            windowSizeClassWidth: WindowSizeClass,
            isBookMode: BookMode,
            textSize: TextSize,
            animateText: Boolean = false
        ): MessageSnackBar {

            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            val customView = LayoutInflater.from(view.context).inflate(
                R.layout.message_snackbar_view,
                parent,
                false
            ) as MessageSnackView
            val textView = customView.findViewById<TextView>(R.id.message)
            textView.text = message
            textView.textSize =
                getTextSize(view.context, textSize, windowSizeClassWidth, isBookMode)
            val snackBar = MessageSnackBar(parent, customView)
            snackBar.duration = duration
            if (animateText) addBlinkingTextAnimation(snackBar.view)
            customView.y = 10f
            return snackBar
        }

        fun removeBlinkingAnimation()
        {
            tapToContinueAnimation?.cancel()
            tapToContinueAnimation = null
        }

        private fun addBlinkingTextAnimation(view: SnackbarBaseLayout) {
           tapToContinueAnimation =
                ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.5f)
            tapToContinueAnimation?.let {
                it.duration = 1000
                it.repeatCount = ObjectAnimator.INFINITE
                it.repeatMode = ObjectAnimator.REVERSE
                it.start()
            }
        }

        private fun getTextSize(
            context: Context,
            textSize: TextSize,
            windowSizeClassWidth: WindowSizeClass,
            isBookMode: BookMode
        ): Float {

            var textBig = 0
            var textSmall = 0
            val densityDpi = context.resources?.displayMetrics?.densityDpi

            when (windowSizeClassWidth) {
                WindowSizeClass.COMPACT -> {
                    when {
                        densityDpi in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._20ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._19ssp)
                        }
                        densityDpi in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._14ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._13ssp)
                        }
                        densityDpi in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._10ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._9ssp)
                        }
                        densityDpi in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                            if (isBookMode == BookMode.BOOK_MODE) {
                                textBig = context.resources.getDimensionPixelSize(R.dimen._5ssp)
                                textSmall = context.resources.getDimensionPixelSize(R.dimen._4ssp)
                            } else {
                                textBig = context.resources.getDimensionPixelSize(R.dimen._8ssp)
                                textSmall = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                            }
                        }
                        densityDpi in DisplayMetrics.DENSITY_XXHIGH.rangeTo(DisplayMetrics.DENSITY_XXXHIGH) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                        }
                        densityDpi?.compareTo(DisplayMetrics.DENSITY_XXXHIGH) == 1 || densityDpi?.compareTo(
                            DisplayMetrics.DENSITY_XXXHIGH
                        ) == 0 -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._5ssp)
                        }
                        else -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._8ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                        }
                    }
                }
                WindowSizeClass.MEDIUM -> {
                    when (densityDpi) {
                        in DisplayMetrics.DENSITY_LOW.rangeTo(DisplayMetrics.DENSITY_MEDIUM) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._15ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._14ssp)
                        }
                        in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._11ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._10ssp)
                        }
                        in DisplayMetrics.DENSITY_HIGH.rangeTo(DisplayMetrics.DENSITY_XHIGH) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._8ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                        }
                        in DisplayMetrics.DENSITY_XHIGH.rangeTo(DisplayMetrics.DENSITY_XXHIGH) -> {
                            if (isBookMode == BookMode.BOOK_MODE) {
                                textBig = context.resources.getDimensionPixelSize(R.dimen._5ssp)
                                textSmall = context.resources.getDimensionPixelSize(R.dimen._4ssp)
                            } else {
                                textBig = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                                textSmall = context.resources.getDimensionPixelSize(R.dimen._5ssp)
                            }
                        }
                        else -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                        }
                    }
                }
                WindowSizeClass.EXPANDED -> {
                    when (densityDpi) {
                        in DisplayMetrics.DENSITY_MEDIUM.rangeTo(DisplayMetrics.DENSITY_HIGH) -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._10ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._9ssp)
                        }
                        else -> {
                            textBig = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                            textSmall = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                        }
                    }

                }
                WindowSizeClass.NOT_SET -> {
                    textBig = context.resources.getDimensionPixelSize(R.dimen._7ssp)
                    textSmall = context.resources.getDimensionPixelSize(R.dimen._6ssp)
                }
            }
            return when (textSize) {
                SMALL -> textSmall.toFloat()
                BIG -> textBig.toFloat()
            }
        }

        enum class TextSize {
            SMALL,
            BIG
        }
    }
}
