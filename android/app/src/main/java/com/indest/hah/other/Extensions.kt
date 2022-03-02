package com.indest.hah.other

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.indest.hah.R

fun AppCompatActivity.hideSystemBars(viewGroup: ViewGroup) {
    val insetsController = WindowInsetsControllerCompat(window, viewGroup)
    insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
}

// Return old value
fun AppCompatActivity.saveBooleanPreference(preference: String, newValue: Boolean): Boolean {
    var oldValue = false
    getSharedPreferences(
        getString(R.string.app_preferences),
        Context.MODE_PRIVATE
    )?.let {
        oldValue = it.getBoolean(preference, false)
        with(it.edit()) {
            putBoolean(preference, newValue)
            apply()
        }
    }
    return oldValue
}

internal fun View?.findSuitableParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            // We've found a CoordinatorLayout, use it
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                // If we've hit the decor content view, then we didn't find a CoL in the
                // hierarchy, so use it.
                return view
            } else {
                // It's not the content view but we'll use it as our fallback
                fallback = view
            }
        }

        if (view != null) {
            // Else, we will loop and crawl up the view hierarchy and try to find a parent
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)

    // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
    return fallback
}
