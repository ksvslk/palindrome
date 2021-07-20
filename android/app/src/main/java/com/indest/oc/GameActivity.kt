package com.indest.oc

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.gridlayout.widget.GridLayout
import com.indest.oc.letter.Letter
import com.indest.oc.letter.LetterResources
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.Util

class GameActivity : AppCompatActivity() {
    private val TAG = "GameActivity"
    private lateinit var gameContainer: LinearLayoutCompat
    private lateinit var letters: GridLayout
    private lateinit var solution: GridLayout

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameContainer = findViewById(R.id.game_container)
        letters = findViewById(R.id.letters_gridlayout)
        solution = findViewById(R.id.solution_gridlayout)

        val letterDirector = BoxDirector
        Util.activateFullscreen(this, gameContainer)

        val puzzle = "XYZ".uppercase()

        if (puzzle.length > 25) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val data = LetterResources(applicationContext, resources)

        for (l in puzzle)
        {
            letters.addView(
                Letter(this, l.toString(), data, letterDirector)
                    .also {
                        letterDirector.boxes.add(it)
                    })
            solution.addView(TextView(this, null, 0, R.style.EmptyBoxView))
        }

    }

    override fun onResume() {
        Log.i(TAG,"onResume")
        super.onResume()
        Util.activateFullscreen(this, gameContainer)
    }
}