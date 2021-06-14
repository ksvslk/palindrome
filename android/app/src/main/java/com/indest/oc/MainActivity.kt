package com.indest.oc

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.*
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var mainContainer: ConstraintLayout
    private lateinit var playButton: Button

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode())
        super.onCreate(savedInstanceState)
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            enterTransition = ChangeTransform()
            exitTransition = ChangeTransform()
        }

        setContentView(R.layout.activity_main)
        activateFullscreen()

        playButton = findViewById(R.id.play_button)
        playButton.setOnClickListener{
            val intent = Intent(this, GameActivity::class.java)
            // Check if we're running on Android 5.0 or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activateFullscreen()
    }

    private fun activateFullscreen() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        mainContainer = findViewById(R.id.main_container)
        mainContainer.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}

