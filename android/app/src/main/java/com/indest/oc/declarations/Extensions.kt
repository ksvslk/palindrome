package com.indest.oc.declarations

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

fun AppCompatActivity.getInsetsController(viewGroup: ViewGroup) : WindowInsetsControllerCompat {
    val insetsController = WindowInsetsControllerCompat(window, viewGroup)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    return insetsController
}