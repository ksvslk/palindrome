package com.indest.hah.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.indest.hah.R

fun AppCompatActivity.hideSystemBars(viewGroup: ViewGroup) {
    val insetsController = WindowInsetsControllerCompat(window, viewGroup)
    insetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
}

fun Fragment.hideSystemBars(viewGroup: ViewGroup) {
    val insetsController = WindowInsetsControllerCompat(requireActivity().window, viewGroup)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
}

fun SoundPool.playWhileSoundOn(
    activity: Activity,
    sound: Int,
    priority: Int,
    loop: Int = 0,
    audioManager: AudioManager
) {
    activity.getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
            !it.getBoolean(activity.getString(R.string.sound_on_pref), true)
        ) return
        play(sound, 1.0f, 1.0f, priority, 0, 1.0f)
    }

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