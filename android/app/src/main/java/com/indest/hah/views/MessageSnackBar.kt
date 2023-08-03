package com.indest.hah.views

import android.animation.ObjectAnimator
import android.content.Context
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
            val snackBar = MessageSnackBar(parent, customView)
            snackBar.view.isEnabled = isViewEnabled
            snackBar.duration = duration
            if (animateText) addBlinkingTextAnimation(snackBar.view)
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
    }
}
