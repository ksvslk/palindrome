package com.indest.oc

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdOptions.ADCHOICES_TOP_RIGHT
import com.google.android.gms.ads.nativead.NativeAdView
import com.indest.oc.util.Util
import kotlinx.android.synthetic.main.activity_game.*

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