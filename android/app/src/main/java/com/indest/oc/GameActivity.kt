package com.indest.oc

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.LinearLayoutCompat
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import androidx.gridlayout.widget.GridLayout
import com.indest.oc.letter.Letter
import com.indest.oc.letter.LetterResources
import com.indest.oc.mediator.BoxDirector
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT
import com.google.android.gms.ads.nativead.NativeAdView
import com.indest.oc.util.Util
import kotlinx.android.synthetic.main.activity_game.*

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

        val builder = AdLoader.Builder(this, "ca-app-pub-5386053272091445/8861866959")
            .forNativeAd { nativeAd : NativeAd ->
                val adView = layoutInflater
                    .inflate(R.layout.native_ad_layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView)
                ad_frame.removeAllViews()
                ad_frame.addView(adView)
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
                .setAdChoicesPlacement(ADCHOICES_TOP_RIGHT)
                .build())
            .build()

        builder.loadAd(AdRequest.Builder().build())
    }

    override fun onResume() {
        Log.i(TAG,"onResume")
        super.onResume()
        Util.activateFullscreen(this, gameContainer)
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.iconView = adView.findViewById(R.id.ad_icon)
        adView.starRatingView = adView.findViewById(R.id.ad_rating)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

        (adView.headlineView as TextView).text = nativeAd.headline

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.GONE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}