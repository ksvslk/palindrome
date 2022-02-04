package com.indest.hah.fragments

import android.content.DialogInterface
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.billingclient.api.BillingFlowParams
import com.google.android.gms.games.Game
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.GameActivity
import com.indest.hah.R
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.LevelEvent
import com.indest.hah.game.SkuDetailsWrapper
import com.indest.hah.game.enums.RewardType
import com.indest.hah.util.C.HINT_FRAGMENT_TAG
import com.indest.hah.util.C.HINT_REQUEST_KEY
import kotlinx.coroutines.flow.collect

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private var _binding: HintsFragmentBinding? = null
    private val binding get() = _binding!!
    private val gameViewModel: GameViewModel by activityViewModels()
    private var bundle: Bundle = bundleOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "onCreateView")
        val b = HintsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b

        binding.categoryRevealButton.setOnClickListener {
            if (!gameViewModel.isCategoryRewarded()) {
                if (isAdded) {
                    (requireActivity() as GameActivity).showRewardedVideo(RewardType.HINT)
                }
            }
        }
        binding.letterPlaceButton.setOnClickListener {
            dismiss()
            if (isAdded) {
                (requireActivity() as GameActivity).showRewardedVideo(RewardType.LETTER)
            }
        }

        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    gameViewModel.uiState.collectLatest { uiState ->
                        when (uiState) {
                            is LevelEvent.Success -> {
                                binding.revealCategoryShown.hintRevealedTextview.text =
                                    uiState.currentLevel.levelInfo.category
                                if (uiState.currentLevel.levelInfo.categoryRewarded) {
                                    Log.i(TAG, "revealCategoryFlipper.showNext()")
                                    if (!binding.revealCategoryShown.root.isShown) {
                                        binding.revealCategoryFlipper.showNext()
                                    }
                                    binding.categoryRevealButton.isEnabled = false
                                }
                            }
                            is LevelEvent.Error -> {
                                Log.i(TAG, "Error getting level from gameViewModel")
                            }
                        }
                    }
                }

                launch {
                    gameViewModel.skuDetails.collect { skuDetailsWrapperList ->
                        if (skuDetailsWrapperList.isNotEmpty()) {
                            skuDetailsWrapperList.forEach {
                                val lastIndex = it.skuDetails.title.indexOf("(")
                                val title = if (lastIndex != -1) it.skuDetails.title.subSequence(
                                    0,
                                    lastIndex
                                ) else ""

                                when (it.skuDetails.sku) {
                                    getString(R.string.remove_letter_ads_sku) -> {
                                        setUpPurchaseButton(
                                            titleTextView = binding.instantLettersTitle,
                                            subtitleTextView = binding.instantLettersSubtitle,
                                            button = binding.instantLettersButton,
                                            skuDetailsWrapper = it,
                                            lastIndex = lastIndex,
                                            title = title
                                        )
                                    }
                                    getString(R.string.remove_clue_ads_sku) -> {
                                        setUpPurchaseButton(
                                            titleTextView = binding.instantCluesTitle,
                                            subtitleTextView = binding.instantCluesSubtitle,
                                            button = binding.instantCluesButton,
                                            skuDetailsWrapper = it,
                                            lastIndex = lastIndex,
                                            title = title
                                        )
                                    }
                                    getString(R.string.remove_inbetween_ads_sku) -> {
                                        setUpPurchaseButton(
                                            titleTextView = binding.removeIntermediateTitle,
                                            subtitleTextView = binding.removeIntermediateSubtitle,
                                            button = binding.noIntermediateAdsButton,
                                            skuDetailsWrapper = it,
                                            lastIndex = lastIndex,
                                            title = title
                                        )
                                    }
                                    getString(R.string.remove_all_ads_sku) -> {
                                        setUpPurchaseButton(
                                            titleTextView = binding.fullNoAdsTitle,
                                            subtitleTextView = binding.fullNoAdsSubtitle,
                                            button = binding.fullNoAdsButton,
                                            skuDetailsWrapper = it,
                                            lastIndex = lastIndex,
                                            title = title
                                        )
                                    }
                                }
                            }
                        } else {
                            hidePurchaseButtons()
                        }
                    }
                }


                launch {
                    gameViewModel.rewardedAdAvailable.collectLatest { rewardedAdAvailable ->
                        if (!rewardedAdAvailable) {
                            val text = getString(R.string.no_ad_available)
                            val textColor =
                                ContextCompat.getColor(requireContext(), R.color.colorOnError)
                            val subtitleBackgroundColor = ContextCompat.getColor(
                                requireContext(),
                                R.color.hintsNotificationColor
                            )
                            binding.revealCategoryHidden.revealClueSubtitle.setTextColor(textColor)
                            binding.revealCategoryHidden.revealClueSubtitle.text = text
                            binding.revealCategoryHidden.revealClueSubtitle.setBackgroundColor(
                                subtitleBackgroundColor
                            )
                            binding.placeLetterSubtitle.setTextColor(textColor)
                            binding.placeLetterSubtitle.text = text
                            binding.placeLetterSubtitle.setBackgroundColor(subtitleBackgroundColor)
                        } else {
                            val text = getString(R.string.watch_rewarded_video)
                            val textColor =
                                ContextCompat.getColor(requireContext(), R.color.colorOnPrimary)
                            val subtitleBackgroundColor =
                                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                            binding.revealCategoryHidden.revealClueSubtitle.setTextColor(textColor)
                            binding.revealCategoryHidden.revealClueSubtitle.text = text
                            binding.revealCategoryHidden.revealClueSubtitle.setBackgroundColor(
                                subtitleBackgroundColor
                            )
                            binding.placeLetterSubtitle.setTextColor(textColor)
                            binding.placeLetterSubtitle.text = text
                            binding.placeLetterSubtitle.setBackgroundColor(subtitleBackgroundColor)
                        }
                    }
                }
            }
        }

        return b.root
    }

    private fun hidePurchaseButtons() {
        listOf(
            binding.instantCluesButton,
            binding.instantLettersButton,
            binding.noIntermediateAdsButton,
            binding.fullNoAdsButton
        ).forEach {
            it.visibility = View.GONE
        }
    }

    private fun setUpPurchaseButton(
        titleTextView: AppCompatTextView,
        subtitleTextView: AppCompatTextView,
        button: LinearLayoutCompat,
        skuDetailsWrapper: SkuDetailsWrapper,
        lastIndex: Int,
        title: CharSequence
    ) {
        if (skuDetailsWrapper.purchased) {
            button.setOnClickListener(null)
            button.visibility = View.GONE
        } else {
            if (lastIndex != -1) titleTextView.text = title
            subtitleTextView.text = skuDetailsWrapper.skuDetails.price
            button.setOnClickListener {
                if (isAdded) {
                    gameViewModel.launchPurchaseFlow(
                        skuDetailsWrapper.skuDetails,
                        requireActivity()
                    )
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        bundle.putString(HINT_FRAGMENT_TAG, HINT_FRAGMENT_TAG)
        setFragmentResult(
            HINT_REQUEST_KEY,
            bundle
        )
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}