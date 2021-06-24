package com.indest.oc

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.gridlayout.widget.GridLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.indest.oc.util.Util

class GameActivity : AppCompatActivity() {
    private val TAG = "GameActivity"
    private lateinit var gameContainer: GridLayout

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG,"onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameContainer = findViewById(R.id.grid_layout)
        Util.activateFullscreen(this, gameContainer)

        for (l in "INDREK LUTS")
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                gameContainer.addView(Letter(this, l.toString()))
            }
            else {
                throw NotImplementedError("Creating Letter views is not implemented with SDKs lower than 26 (O)")
            }
        }

        val adLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad : NativeAd ->
                // Show the ad.
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "onAdFailedToLoad")
                }
                override fun onAdLoaded() {
                    Log.d(TAG, "onAdLoaded")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                // Methods in the NativeAdOptions.Builder class can be
                // used here to specify individual options settings.
                .build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

    }

    override fun onResume() {
        Log.i(TAG,"onResume")
        super.onResume()
        Util.activateFullscreen(this, gameContainer)
    }
}