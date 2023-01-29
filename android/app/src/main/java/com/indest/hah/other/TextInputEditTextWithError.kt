package com.indest.hah.other

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TextInputEditTextWithError @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {

    private val parentRect = Rect()

    override fun getFocusedRect(rect: Rect?) {
        super.getFocusedRect(rect)
        rect?.let {
            getMyParent().getFocusedRect(parentRect)
            rect.bottom = parentRect.bottom
        }
    }

    override fun getGlobalVisibleRect(rect: Rect?, globalOffset: Point?): Boolean {
        val result = super.getGlobalVisibleRect(rect, globalOffset)
        rect?.let {
            getMyParent().getGlobalVisibleRect(parentRect, globalOffset)
            rect.bottom = parentRect.bottom
        }
        return result
    }

    override fun requestRectangleOnScreen(rect: Rect?): Boolean {
        val result = super.requestRectangleOnScreen(rect)
        val parent = getMyParent()
        // 10 is a random magic number to define a rectangle height.
        parentRect.set(0, parent.height - 10, parent.right, parent.height)
        parent.requestRectangleOnScreen(parentRect, true /*immediate*/)
        return result;
    }

    private fun getMyParent(): View {
        var myParent: ViewParent? = parent;
        while (!(myParent is TextInputLayout) && myParent != null) {
            myParent = myParent.parent
        }
        return if (myParent == null) this else myParent as View
    }
}