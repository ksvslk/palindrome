package com.indest.oc.letter

import android.annotation.SuppressLint
import android.content.Context
import android.nfc.Tag
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.indest.oc.R
import com.indest.oc.mediator.IBoxDirector

@SuppressLint("ViewConstructor", "AppCompatCustomView")
class Slot @RequiresApi(Build.VERSION_CODES.O) constructor(
    context: Context?,
    boxDirector: IBoxDirector,
    defStyleRes: Int = R.style.EmptyBoxView
) : TextView(context, null, 0, defStyleRes), IBox {

    private var TAG: String
    private var director: IBoxDirector = boxDirector

    init {
        TAG = "Slot ${View.generateViewId()}"
        text = TAG
    }

    override var posX: Float
        get() = x
        set(value) {x = value}

    override var posY: Float
        get() = y
        set(value) {y = value}

    override fun toString(): String {
        return TAG
    }
}