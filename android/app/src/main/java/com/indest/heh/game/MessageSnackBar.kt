package com.indest.heh.game

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.indest.heh.R
import com.indest.heh.util.findSuitableParent
import android.widget.FrameLayout
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.size

class MessageSnackBar(
    parent: ViewGroup,
    content: MessageSnackView
) : BaseTransientBottomBar<MessageSnackBar>(parent, content, content) {

    init {
        getView().setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.transparent))
        val bottomPadding = Resources.getSystem().displayMetrics.heightPixels / 2
        getView().setPadding(0, 0, 0, bottomPadding)
        val params = getView().layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.CENTER
        getView().layoutParams = params
    }

    companion object {

        fun make(view: View, message:String, duration:Int, textSize:Float?): MessageSnackBar {

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
            textSize?.let { textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, it) }
            val snackBar = MessageSnackBar(parent,customView)
            snackBar.duration = duration
            return snackBar
        }

    }


}
