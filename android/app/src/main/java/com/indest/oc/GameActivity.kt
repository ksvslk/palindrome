package com.indest.oc

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

class GameActivity : AppCompatActivity() {
    private val TAG = "GameActivity"
    private lateinit var gameContainer: ConstraintLayout

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game)
        activateFullscreen()
    }

    override fun onResume() {
        super.onResume()
        activateFullscreen()
    }

    private fun activateFullscreen() {

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        gameContainer = findViewById(R.id.game_container)
        gameContainer.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}