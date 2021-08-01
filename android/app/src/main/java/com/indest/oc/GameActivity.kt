package com.indest.oc

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Space
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import com.indest.oc.letter.Letter
import com.indest.oc.letter.Effects
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.Util

class GameActivity : AppCompatActivity() {
    private val TAG = "GameActivity"
    private lateinit var gameLayout: LinearLayoutCompat
    private lateinit var letters: androidx.gridlayout.widget.GridLayout
    private var colCount = 6

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameLayout = findViewById(R.id.game_container)
        letters = findViewById(R.id.boxes_gridlayout)

        val boxDirector = BoxDirector
        Util.activateFullscreen(this, gameLayout)

        val puzzle = "A nut for a jar of tuna".uppercase()
        Log.i(TAG, puzzle.length.toString())

        /*if (puzzle.length > 25) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            colCount = 12
        }*/
        letters.columnCount = colCount

        val data = Effects(applicationContext, resources)

        for (l in puzzle)
        {
            if (l.isWhitespace()) {
           /*     letters.addView(
                    Space(this, null, R.layout.blank)
                )*/
                continue
            }
            letters.addView(
                Letter(this, l.toString(), data, boxDirector, State.HOME, R.style.LetterView)
                    .also {
                        boxDirector.boxes.add(it)
                    })
        }
        val puzzleWithoutWhitespace = puzzle.replace("\\s".toRegex(), "")
        if (puzzle.length > colCount) {
            for (i in 1..colCount - (puzzleWithoutWhitespace.length - (puzzleWithoutWhitespace.length / colCount) * colCount)) {
                letters.addView(
                    Space(this, null, R.layout.blank)
                )
            }
        }
        else {
            for (i in 1..colCount - puzzle.length) {
                letters.addView(
                    Space(this, null, R.layout.blank)
                )
            }
        }


        for (l in puzzle)
        {
            if (l.isWhitespace()) {
                letters.addView(
                    Space(this, null, R.layout.blank)
                )
                continue
            }
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


