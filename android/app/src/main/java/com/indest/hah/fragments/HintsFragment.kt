package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.GameActivity
import com.indest.hah.R
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.LatestLevelEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.StringBuilder

class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private lateinit var binding: HintsFragmentBinding
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "onCreateView")
        binding = HintsFragmentBinding.inflate(layoutInflater, container, false)
        binding.categoryRevealButton.setOnClickListener {
            if (!gameViewModel.isCategoryRewarded()) {
                /* TODO: how do we call the showRewardedVideo? Loading the ad cannot be in
                    this fragment because it starts to load too late. We must have a way to access the parent
                     to call it. And in the show it should change the layout of this fragment. */
                val gamePlayFragment = parentFragmentManager.findFragmentById(R.id.navigation_gameplay) as GamePlayFragment
                gamePlayFragment.showRewardedVideo(
                    getString(R.string.category_rewarded_ad_unit_id)
                )
            }
        }
        binding.letterPlaceButton.setOnClickListener {
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
                                    "HINT: ",
                                    uiState.currentLevel.levelInfo.category
                                )
                            if (uiState.currentLevel.levelInfo.categoryRewarded) {
                                binding.revealCategoryFlipper.showNext()
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

}