package com.indest.oc

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Space
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import com.indest.oc.letter.Letter
import com.indest.oc.letter.LetterResources
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.Util

class GameActivity : AppCompatActivity() {
    private val TAG = "GameActivity"
    private lateinit var gameLayout: LinearLayoutCompat
    private lateinit var letters: androidx.gridlayout.widget.GridLayout
    private val colCount = 5

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameLayout = findViewById(R.id.game_container)
        letters = findViewById(R.id.boxes_gridlayout)
        letters.columnCount = colCount

        val boxDirector = BoxDirector
        Util.activateFullscreen(this, gameLayout)

        val puzzle = "INDREK".uppercase()

        if (puzzle.length > 25) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val data = LetterResources(applicationContext, resources)

        for (l in puzzle)
        {
            letters.addView(
                Letter(this, l.toString(), data, boxDirector, State.HOME, R.style.LetterView)
                    .also {
                        boxDirector.boxes.add(it)
                    })
        }
        for (i in 1..colCount - (puzzle.length / colCount)) {
            letters.addView(
                Space(this, null, R.layout.blank)
            )
        }
        for (l in puzzle)
        {
            letters.addView(
                Slot(this).also {
                    boxDirector.boxes.add(it)
                    it.text = " "
                })
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        Util.activateFullscreen(this, gameLayout)
    }
}


