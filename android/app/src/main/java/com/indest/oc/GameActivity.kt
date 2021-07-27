package com.indest.oc

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT
import com.google.android.gms.ads.nativead.NativeAdView
import com.indest.oc.letter.Letter
import com.indest.oc.letter.LetterResources
import com.indest.oc.letter.Slot
import com.indest.oc.letter.State
import com.indest.oc.mediator.BoxDirector
import com.indest.oc.util.C
import com.indest.oc.util.Util
import kotlinx.android.synthetic.main.activity_game.*

class GameActivity : AppCompatActivity() {
    private val TAG = "GameActivity"
    lateinit var test: LinearLayoutCompat
    lateinit var up: TableRow
    lateinit var down: TableRow

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test)

        test = findViewById(R.id.test)
        up = findViewById(R.id.letters_tablerow)
        down = findViewById(R.id.solution_tablerow)
        up.translationZ = C.MIDDLE_LAYER
        down.translationZ = C.BOTTOM_LAYER
        val boxDirector = BoxDirector
        Util.activateFullscreen(this, test)

        val puzzle = "INDREK".uppercase()

        if (puzzle.length > 25) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val data = LetterResources(applicationContext, resources)

        val params = TableRow.LayoutParams(
            (resources.displayMetrics.scaledDensity * 50).toInt(),
            (resources.displayMetrics.scaledDensity * 50).toInt(),
        )
        val gap = (resources.displayMetrics.scaledDensity * 4).toInt()
        params.setMargins(gap)

        val lettersInRow = 5
        val lettersCount = 20

        test.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_CANCEL -> {
                    Log.i(TAG, "We should handle this! $v")
                }
              }
            false
        }
        down.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_CANCEL -> {
                    Log.i(TAG, "We should handle this! $v")
                }
            }
            false
        }

        for (i in 1..lettersCount step lettersInRow) {
            print("[$i]") // add first view to row
            val leftInRow = if (lettersCount - i > 4) 4 else lettersCount - i
            for (j in i+1..i+leftInRow) {
                print("[$j]") // add other views to row
            }
            println() // create and add next row
        }
        for (l in puzzle) {
            up.addView(
                Letter(this, l.toString(), data, this, boxDirector, State.HOME, R.style.LetterView)
                    .also {
                        boxDirector.boxes.add(it)
                        it.layoutParams = params
                    })
        }
        for (l in puzzle) {
            down.addView(Slot(this, R.style.EmptyBoxView)
                .also {
                    boxDirector.boxes.add(it)
                    it.layoutParams = params
                })
        }

        val builder = AdLoader.Builder(this, "ca-app-pub-5386053272091445/8861866959")
            .forNativeAd { nativeAd: NativeAd ->
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
                    .build()
            )
            .build()

        //builder.loadAd(AdRequest.Builder().build()) TODO("Uncomment before merging with develop")
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        Util.activateFullscreen(this, test)
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


