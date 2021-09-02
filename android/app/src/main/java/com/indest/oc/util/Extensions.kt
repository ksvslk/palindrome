package com.indest.oc.util

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun AppCompatActivity.initInsetsController(viewGroup: ViewGroup) : WindowInsetsControllerCompat {
    val insetsController = WindowInsetsControllerCompat(window, viewGroup)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
    return insetsController
}