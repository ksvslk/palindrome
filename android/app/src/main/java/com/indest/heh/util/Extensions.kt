package com.indest.heh.util

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

fun AppCompatActivity.hideSystemBars(viewGroup: ViewGroup) {
    val insetsController = WindowInsetsControllerCompat(window, viewGroup)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
}

fun Fragment.hideSystemBars(viewGroup: ViewGroup) {
    val insetsController = WindowInsetsControllerCompat(requireActivity().window, viewGroup)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
}