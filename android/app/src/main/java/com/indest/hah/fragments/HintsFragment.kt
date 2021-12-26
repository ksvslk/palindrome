package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.games.Games
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.LatestLevelEvent
import com.indest.hah.util.C.CATEGORY
import com.indest.hah.util.C.HINT_REQUEST_KEY
import com.indest.hah.util.C.HINT_TYPE
import com.indest.hah.util.C.LETTER
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private lateinit var binding: HintsFragmentBinding
    private val gameViewModel: GameViewModel by activityViewModels()
    private var rewardEarned = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "onCreateView")
        binding = HintsFragmentBinding.inflate(layoutInflater, container, false)
        binding.categoryRevealButton.setOnClickListener {
            if (!gameViewModel.isCategoryRewarded()) {
                showRewardedVideo(
                    getString(
                        R.string.rewarded_ad_unit
                    ),
                    RewardedAdType.REVEAL_CATEGORY
                )
            }
        }
        binding.letterPlaceButton.setOnClickListener {
            showRewardedVideo(
                getString(
                    R.string.rewarded_ad_unit
                ),
                RewardedAdType.PLACE_LETTER
            )
            Toast.makeText(requireContext(), "Place letter rewarded.", Toast.LENGTH_LONG).show()
        }
        binding.allCategoriesButton.setOnClickListener {
            Toast.makeText(requireContext(), "All categories IAP.", Toast.LENGTH_LONG).show()
        }
        binding.unlockAllHintsButton.setOnClickListener {
            Toast.makeText(requireContext(), "All hints IAP.", Toast.LENGTH_LONG).show()
        }
        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gameViewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is LatestLevelEvent.Success -> {
                            binding.revealCategoryShown.hintRevealedTextview.text =
                                StringBuilder().append(
                                    uiState.currentLevel.levelInfo.category
                                )
                            if (uiState.currentLevel.levelInfo.categoryRewarded) {
                                Log.i(TAG, "revealCategoryFlipper.showNext()")
                                if (!binding.revealCategoryShown.root.isShown) {
                                    binding.revealCategoryFlipper.showNext()
                                }
                                binding.categoryRevealButton.isEnabled = false
                            }
                        }
                        is LatestLevelEvent.Error -> {
                            Log.i(TAG, "Error getting level from gameViewModel")
                        }
                    }
                }
            }
        }

        return binding.root
    }

    private fun showRewardedVideo(rewardedAdUnit: String, rewardedType: RewardedAdType) {
        if (gameViewModel.rewardedAd != null) {
            gameViewModel.rewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        gameViewModel.rewardedAd = null
                        loadRewardedAd(rewardedAdUnit)
                        if (rewardEarned) {
                            rewardEarned = false
                            when (rewardedType) {
                                RewardedAdType.REVEAL_CATEGORY -> {
                                    gameViewModel.setCategoryRewarded(true)
                                    binding.categoryRevealButton.isEnabled = false
                                    setFragmentResult(
                                        HINT_REQUEST_KEY,
                                        bundleOf(HINT_TYPE to CATEGORY)
                                    )
                                }
                                RewardedAdType.PLACE_LETTER -> {
                                    setFragmentResult(
                                        HINT_REQUEST_KEY,
                                        bundleOf(HINT_TYPE to LETTER)
                                    )
                                    dismiss()
                                }
                            }
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        gameViewModel.rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }
            gameViewModel.rewardedAd?.show(
                requireActivity()
            ) {
                rewardEarned = true
                Log.d(TAG, "User earned the reward.")
            }
        }
    }

    private fun loadRewardedAd(adUnit: String) {
        if (gameViewModel.rewardedAd == null) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                requireActivity(), adUnit, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        gameViewModel.rewardedAd = null
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        gameViewModel.rewardedAd = rewardedAd
                    }
                }
            )
        }
    }

    enum class RewardedAdType {
        REVEAL_CATEGORY,
        PLACE_LETTER
    }

}