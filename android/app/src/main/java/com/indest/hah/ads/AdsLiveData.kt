@file:Suppress("PrivatePropertyName")

package com.indest.hah.ads

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.indest.hah.R
import kotlin.math.min

private const val AD_RELOAD_TIMER_START_MILLISECONDS = 1L * 1000L
private const val AD_RELOAD_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 5L // 5 minutes

@Suppress("PrivatePropertyName")
class AdsLiveData (private val context: Context) : LiveData<Pair<Boolean, RewardType>>() {
    private val TAG = "AdsLiveData"

    private var _rewardedAd: RewardedAd? = null
    private var _rewardEarned = false
    private val handler = Handler(Looper.getMainLooper())
    private var adReloadMilliseconds = AD_RELOAD_TIMER_START_MILLISECONDS

    init {
        postValue(Pair(false, RewardType.NONE))
    }

    override fun onActive() {
        loadRewardedAd()
    }

    override fun onInactive() {
        _rewardedAd = null
        postValue(Pair(false, RewardType.NONE))
    }

    private fun loadRewardedAd() {
        if (_rewardedAd == null) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context, context.getString(R.string.admob_rewarded_ad_unit), adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        retryReloadingAdsWithExponentialBackoff()
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        _rewardedAd = rewardedAd
                        postValue(Pair(true, RewardType.NONE))
                    }
                }
            )
        }
    }

    private fun retryReloadingAdsWithExponentialBackoff() {
        if (hasActiveObservers())
        {
            handler.postDelayed(
                {
                    loadRewardedAd()
                },
                adReloadMilliseconds
            )
            adReloadMilliseconds = min(
                adReloadMilliseconds * 2,
                AD_RELOAD_TIMER_MAX_TIME_MILLISECONDS
            )
        }
    }

    fun showRewardedVideo(activity: FragmentActivity, rewardedType: RewardType) {
        _rewardedAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    _rewardedAd = null
                    loadRewardedAd()
                    if (_rewardEarned) {
                        _rewardEarned = false
                        postValue(Pair(false, rewardedType))
                    }
                    else {
                        if (rewardedType == RewardType.SAD_MODE) postValue(Pair(false, RewardType.SAD_AD_FAILED))
                        else postValue(Pair(false, RewardType.AD_FAILED))
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    _rewardedAd = null
                    if (rewardedType == RewardType.SAD_MODE) postValue(Pair(false, RewardType.SAD_AD_FAILED))
                    else postValue(Pair(false, RewardType.AD_FAILED))
                }

                override fun onAdShowedFullScreenContent() {

                }
            }
        _rewardedAd?.show(
            activity
        ) {
            _rewardEarned = true
        }
    }

}