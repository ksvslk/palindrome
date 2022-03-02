package com.indest.hah.ads

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.indest.hah.R

class AdsLiveData (private val context: Context) : LiveData<Pair<Boolean, RewardType>>() {
    private val TAG = "AdsLiveData"

    private var _adReloadTimer: CountDownTimer? = null
    private var _rewardedAd: RewardedAd? = null
    private var _rewardEarned = false

    init {
        postValue(Pair(false, RewardType.NONE))
    }

    override fun onActive() {
        Log.d(TAG, "onActive")
        loadRewardedAd()
    }

    override fun onInactive() {
        Log.d(TAG, "onInactive")
        _rewardedAd = null
        _adReloadTimer?.cancel()
        _adReloadTimer = null
        postValue(Pair(false, RewardType.NONE))
    }

    private fun startAdReloadTimer() {
        _adReloadTimer?.cancel()
        _adReloadTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                Log.d(TAG, "Ad reload timer completed.")
                loadRewardedAd()
            }
        }.start()
    }

    // CLEAN
    private fun loadRewardedAd() {
        if (_rewardedAd == null) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context, context.getString(R.string.admob_rewarded_ad_unit), adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        startAdReloadTimer()
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        _rewardedAd = rewardedAd
                        postValue(Pair(true, RewardType.NONE))
                    }
                }
            )
        }
    }

    fun showRewardedVideo(activity: Activity, rewardedType: RewardType) {
        Log.d(TAG, "showRewardedVideo")
        _rewardedAd?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    _rewardedAd = null
                    loadRewardedAd()
                    if (_rewardEarned) {
                        _rewardEarned = false
                        postValue(Pair(false, rewardedType))
                    }
                    else {
                        postValue(Pair(false, RewardType.NONE))
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    _rewardedAd = null
                    postValue(Pair(false, RewardType.NONE))
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
        _rewardedAd?.show(
            activity
        ) {
            Log.d(TAG, "User earned the reward.")
            _rewardEarned = true
        }
    }

}