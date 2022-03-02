package com.indest.hah.ads

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.indest.hah.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class Ads @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "Ads"

    private var adReloadTimer: CountDownTimer? = null
    private var _rewardedAd: RewardedAd? = null
    private var _rewardEarned = false

    private fun startAdReloadTimer() {
        adReloadTimer?.cancel()
        adReloadTimer = object : CountDownTimer(15000, 1000) {
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
                        // TODO() gameViewModel.setRewardedAdAvailability(false)
                        startAdReloadTimer()
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        // TODO() gameViewModel.setRewardedAdAvailability(true)
                        _rewardedAd = rewardedAd
                    }
                }
            )
        }
    }

    fun showRewardedVideo(activity: Activity, rewardedType: RewardType) {
       //TODO() if (gameViewModel.rewardedAdAvailable.value*/) { */
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
                            when (rewardedType) {
                                RewardType.CLUE -> {
                                    //TODO() gameViewModel.setCategoryRewarded(true)
                                    //TODO() gameViewModel.rewardUser(rewardedType)
                                }
                                RewardType.LETTER -> {
                                    //TODO() gameViewModel.rewardUser(rewardedType)
                                }
                                RewardType.NEXT_LEVEL -> {
                                    //TODO() gameViewModel.rewardUser(rewardedType)
                                }
                                else -> {
                                    Log.d(TAG, "Reward type does not match available ones.")
                                }
                            }
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        _rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
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