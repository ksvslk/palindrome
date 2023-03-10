package com.indest.hah.views

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration.*
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.indest.hah.R
import com.indest.hah.other.findSuitableParent
import com.indest.hah.views.MessageSnackBar.Companion.TextSize.*


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

    private var _callback: BaseCallback<MessageSnackBar>? = null

    override fun addCallback(callback: BaseCallback<MessageSnackBar>?): MessageSnackBar {
        _callback = callback
        return super.addCallback(callback)
    }

    fun removeMessageCallback()
    {
        removeCallback(_callback)
    }


    companion object {

        private var tapToContinueAnimation:ObjectAnimator? = null

        fun make(
            view: View,
            message: String,
            duration: Int,
            textSize: TextSize,
            animateText: Boolean = false,
            isViewEnabled:Boolean = true
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
            textView.textSize = getTextSize(view.context, textSize)
            val snackBar = MessageSnackBar(parent, customView)
            snackBar.view.isEnabled = isViewEnabled
            snackBar.duration = duration
            if (animateText) addBlinkingTextAnimation(snackBar.view)
            customView.y = 10f
            return snackBar
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
            textSize: TextSize
        ): Float {

            //val textBig = context.resources.getDimension(R.dimen.winning_message_size)
            //val textSmall = context.resources.getDimension(R.dimen.other_message_size)
            val smallestWidth = context.resources.configuration.smallestScreenWidthDp
            val divider = when {
                smallestWidth < 600f -> 12 // compact
                (600f <= smallestWidth && smallestWidth < 840f) -> 20 // medium
                smallestWidth > 840f -> 30 // expanded
                else -> 12
            }

            val widthDivided: Int = context.resources.displayMetrics.widthPixels / divider
            val textBig = widthDivided / context.resources.displayMetrics.density
            val textSmall = widthDivided / context.resources.displayMetrics.density * 0.8f

            return when (textSize) {
                SMALL -> textSmall
                BIG -> textBig
            }
        }

        enum class TextSize {
            SMALL,
            BIG
        }
    }
}
