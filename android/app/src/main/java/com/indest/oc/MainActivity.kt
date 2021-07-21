package com.indest.oc

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.*
import android.util.Log
import android.view.Window
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.indest.oc.util.Util

class MainActivity : AppCompatActivity() {

    private val TAG = "com.indest.oc.MainActivity"
    private lateinit var mainContainer: ConstraintLayout
    private lateinit var playButton: Button

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        super.onCreate(savedInstanceState)
        with(window) {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            enterTransition = ChangeTransform()
            exitTransition = ChangeTransform()
        }
        setContentView(R.layout.activity_main)

        mainContainer = findViewById(R.id.main_container)
        Util.activateFullscreen(this, mainContainer)

        playButton = findViewById(R.id.play_button)
        playButton.setOnClickListener{
            val intent = Intent(this, GameActivity::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Util.activateFullscreen(this, mainContainer)
    }
}

