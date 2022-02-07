package com.indest.hah.fragments

import android.content.DialogInterface
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.GameActivity
import com.indest.hah.R
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.SkuDetailsWrapper
import com.indest.hah.util.C.HINT_FRAGMENT_TAG
import com.indest.hah.util.C.HINT_REQUEST_KEY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import com.indest.hah.fragments.HintsFragment.AvailablePurchase.PLACE
import com.indest.hah.fragments.HintsFragment.AvailablePurchase.CLUE
import com.indest.hah.fragments.HintsFragment.AvailablePurchase.INTER
import com.indest.hah.game.enums.RewardType
import com.indest.hah.persistance.dto.Sku

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private var _binding: HintsFragmentBinding? = null
    private val binding get() = _binding!!
    private val gameViewModel: GameViewModel by activityViewModels()
    private var bundle: Bundle = bundleOf()
    private var skuDetailsCollectionJob: Job? = null
    private var rewardedAdCollectionJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.d(TAG, "onCreateView")
        val b = HintsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b

        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        skuDetailsCollectionJob = lifecycleScope.launch {
            gameViewModel.skuDetails.collect { skuDetailsWrapperList ->
                Log.i(TAG, skuDetailsWrapperList.toString())
                val isCategoryRewarded = gameViewModel.isCategoryRewarded()

                val setOfAvailablePurchases: MutableSet<AvailablePurchase> =
                    if (skuDetailsWrapperList.isNotEmpty()) {
                        Log.d(TAG, "All sku details not there")
                        setUpAvailablePurchases(skuDetailsWrapperList)
                    } else {
                        Log.d(TAG, "Set offline sku details")
                        val skus = gameViewModel.getAllSkus()
                        setUpAvailablePurchasesOffline(skus)
                    }

                when (setOfAvailablePurchases) {
                    setOf(INTER) -> {
                        setUpPurchasedGoods(RewardType.LETTER)
                        setUpPurchasedGoods(RewardType.HINT, isCategoryRewarded)
                        terminateButton(binding.fullNoAdsButton)
                    }
                    setOf(CLUE) -> {
                        setUpPurchasedGoods(RewardType.LETTER)
                        terminateButton(binding.fullNoAdsButton)
                    }
                    setOf(PLACE) -> {
                        setUpPurchasedGoods(RewardType.HINT)
                        terminateButton(binding.fullNoAdsButton)
                    }
                    setOf(PLACE, INTER) -> {
                        setUpPurchasedGoods(RewardType.HINT, isCategoryRewarded)
                        binding.fullAdsSavingsBadge.visibility = View.GONE
                    }
                    setOf(PLACE, CLUE) -> {
                        // All taken care of in setUpAvailablePurchases
                        binding.fullAdsSavingsBadge.visibility = View.GONE
                    }
                    setOf(CLUE, INTER) -> {
                        terminateButton(binding.fullNoAdsButton)
                        setUpPurchasedGoods(RewardType.LETTER)
                    }
                    setOf(PLACE, CLUE, INTER) -> {
                        // All taken care of in setUpAvailablePurchases
                    }
                    setOf<AvailablePurchase>() -> {
                        Log.d(TAG, "All good, got 0 set.")
                        setUpPurchasedGoods(RewardType.LETTER)
                        setUpPurchasedGoods(RewardType.HINT, isCategoryRewarded)
                        listOf(
                            binding.instantCluesButton,
                            binding.instantLettersButton,
                            binding.noIntermediateAdsButton,
                            binding.fullNoAdsButton
                        ).forEach {
                            terminateButton(it)
                        }
                    }
                }
            }
        }

        rewardedAdCollectionJob = lifecycleScope.launch {
            gameViewModel.rewardedAdAvailable.collectLatest { rewardedAdAvailable ->
                Log.d(TAG, "Set up the video buttons.: $rewardedAdAvailable")
                var cluesPurchased = false
                var lettersPurchased = false
                var allAdsRemoved = false

                if (gameViewModel.skuDetails.value.isNotEmpty())
                {
                    gameViewModel.skuDetails.value.forEach {
                        when (it.skuDetails.sku) {
                            getString(R.string.instant_clues) -> {
                                cluesPurchased = it.purchased
                            }
                            getString(R.string.instant_letters) -> {
                                lettersPurchased = it.purchased
                            }
                            getString(R.string.full_no_ads) -> {
                                allAdsRemoved = it.purchased
                            }
                        }
                    }
                } else {
                    val skus = gameViewModel.getAllSkus()
                    skus.forEach {
                        when (it.sku) {
                            getString(R.string.instant_clues) -> {
                                cluesPurchased = it.purchased
                            }
                            getString(R.string.instant_letters) -> {
                                lettersPurchased = it.purchased
                            }
                            getString(R.string.full_no_ads) -> {
                                allAdsRemoved = it.purchased
                            }
                        }
                    }
                }
                Log.d(TAG, "$cluesPurchased, $lettersPurchased, $allAdsRemoved")

                if ((cluesPurchased && lettersPurchased) || allAdsRemoved) {
                    rewardedAdCollectionJob?.cancel()
                    return@collectLatest
                }

                val isCategoryRewarded = gameViewModel.isCategoryRewarded()

                if (!cluesPurchased) {
                    setUpRewardedButtonSubtitle(
                        binding.revealCategoryHidden.revealClueSubtitle,
                        rewardedAdAvailable
                    )
                    binding.categoryRevealButton.setOnClickListener {
                        if (rewardedAdAvailable) {
                            if (!isCategoryRewarded) {
                                if (isAdded) {
                                    (requireActivity() as GameActivity).showRewardedVideo(RewardType.HINT)
                                }
                            }
                        }
                    }
                }
                if (!lettersPurchased) {
                    setUpRewardedButtonSubtitle(binding.placeLetterSubtitle, rewardedAdAvailable)
                    binding.letterPlaceButton.setOnClickListener {
                        if (rewardedAdAvailable) {
                            dismiss()
                            if (isAdded) {
                                (requireActivity() as GameActivity).showRewardedVideo(RewardType.LETTER)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            skuDetailsCollectionJob?.start()
            val cluesPurchased =
                gameViewModel.skuDetails.value.find { it.skuDetails.sku == getString(R.string.instant_clues) }?.purchased
            val lettersPurchased =
                gameViewModel.skuDetails.value.find { it.skuDetails.sku == getString(R.string.instant_letters) }?.purchased
            if (cluesPurchased == false || lettersPurchased == false) {
                rewardedAdCollectionJob?.start()
            }
        }
    }

    private fun setUpRewardedButtonSubtitle(subtitle: AppCompatTextView, adAvailable: Boolean) {

        when (adAvailable) {
            true -> {
                val text = getString(R.string.watch_rewarded_video)
                val textColor =
                    ContextCompat.getColor(requireContext(), R.color.colorOnPrimary)
                val subtitleBackgroundColor =
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary)

                subtitle.setTextColor(textColor)
                subtitle.text = text
                subtitle.setBackgroundColor(subtitleBackgroundColor)
            }
            false -> {
                val text = getString(R.string.no_ad_available)
                val textColor = ContextCompat.getColor(requireContext(), R.color.colorOnError)
                val subtitleBackgroundColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.hintsNotificationColor
                )

                subtitle.setTextColor(textColor)
                subtitle.text = text
                subtitle.setBackgroundColor(subtitleBackgroundColor)
            }
        }
    }

    private fun setUpPurchasedGoods(rewardType: RewardType, categoryRevealed: Boolean = false) {
        val subtitleText = getString(R.string.unlimited)
        val subtitleTextColor =
            ContextCompat.getColor(
                requireContext(),
                R.color.successTextColor
            )
        val subtitleBackgroundColor = ContextCompat.getColor(
            requireContext(),
            R.color.successBoxColor
        )

        when (rewardType) {
            RewardType.LETTER -> {
                binding.placeLetterSubtitle.setTextColor(subtitleTextColor)
                binding.placeLetterSubtitle.text = subtitleText
                binding.placeLetterSubtitle.setBackgroundColor(subtitleBackgroundColor)

                binding.letterPlaceButton.setOnClickListener {
                    dismiss()
                    if (isAdded) {
                        gameViewModel.rewardUser(rewardType = RewardType.LETTER)
                    }
                }
            }
            RewardType.HINT -> {
                binding.revealCategoryShown.hintRevealedTextview.text = gameViewModel.getHint()
                binding.revealCategoryHidden.revealClueSubtitle.text =
                    subtitleText
                binding.revealCategoryHidden.revealClueSubtitle.setTextColor(
                    subtitleTextColor
                )
                binding.revealCategoryHidden.revealClueSubtitle.setBackgroundColor(
                    subtitleBackgroundColor
                )
                if (categoryRevealed) {
                    if (!binding.revealCategoryShown.root.isShown) {
                        binding.revealCategoryFlipper.showNext()
                    }
                    binding.categoryRevealButton.isEnabled = false
                } else {
                    binding.categoryRevealButton.setOnClickListener {
                        gameViewModel.setCategoryRewarded(true)
                        if (!binding.revealCategoryShown.root.isShown) {
                            binding.revealCategoryFlipper.showNext()
                        }
                        binding.categoryRevealButton.isEnabled = false
                    }
                }
            }
            RewardType.NONE -> {}
        }

    }

    private fun setUpObtainableButton(
        titleTextView: AppCompatTextView,
        subtitleTextView: AppCompatTextView,
        button: LinearLayoutCompat,
        skuDetailsWrapper: SkuDetailsWrapper,
        lastIndex: Int,
        title: CharSequence
    ) {

        if (skuDetailsWrapper.purchased) {
            terminateButton(button)
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

    private fun terminateButton(button: LinearLayoutCompat) {
        button.setOnClickListener(null)
        button.visibility = View.GONE
    }

    private fun setUpAvailablePurchasesOffline(skus: List<Sku>): MutableSet<AvailablePurchase> {
        val purchasesAvailable = mutableSetOf<AvailablePurchase>()
        skus.forEach {
            when (it.sku) {
                getString(R.string.instant_clues) -> {
                    if (!it.purchased) purchasesAvailable.add(CLUE)
                    else terminateButton(binding.instantCluesButton)
                }
                getString(R.string.instant_letters) -> {
                    if (!it.purchased) purchasesAvailable.add(PLACE)
                    else terminateButton(binding.instantLettersButton)
                }
                getString(R.string.no_intermediate_ads) -> {
                    if (!it.purchased) purchasesAvailable.add(INTER)
                    else terminateButton(binding.noIntermediateAdsButton)
                }
                getString(R.string.full_no_ads) -> {
                    if (it.purchased) terminateButton(binding.fullNoAdsButton)
                }
            }
        }
        return purchasesAvailable
    }

    private fun setUpAvailablePurchases(skuDetails: List<SkuDetailsWrapper>): MutableSet<AvailablePurchase> {
        val purchasesAvailable = mutableSetOf<AvailablePurchase>()
        if (skuDetails.find { it.skuDetails.sku == getString(R.string.full_no_ads) }?.purchased == true) {
            return purchasesAvailable
        }
        skuDetails.forEach {
            val lastIndex = it.skuDetails.title.indexOf("(")
            val title = if (lastIndex != -1) it.skuDetails.title.subSequence(
                0,
                lastIndex
            ) else ""

            when (it.skuDetails.sku) {
                getString(R.string.instant_clues) -> {
                    if (!it.purchased) {
                        setUpObtainableButton(
                            titleTextView = binding.instantCluesTitle,
                            subtitleTextView = binding.instantCluesSubtitle,
                            button = binding.instantCluesButton,
                            skuDetailsWrapper = it,
                            lastIndex = lastIndex,
                            title = title
                        )
                        purchasesAvailable.add(CLUE)
                    } else {
                        terminateButton(binding.instantCluesButton)
                    }
                }
                getString(R.string.instant_letters) -> {
                    if (!it.purchased) {
                        setUpObtainableButton(
                            titleTextView = binding.instantLettersTitle,
                            subtitleTextView = binding.instantLettersSubtitle,
                            button = binding.instantLettersButton,
                            skuDetailsWrapper = it,
                            lastIndex = lastIndex,
                            title = title
                        )
                        purchasesAvailable.add(PLACE)
                    } else {
                        terminateButton(binding.instantLettersButton)
                    }
                }
                getString(R.string.no_intermediate_ads) -> {
                    if (!it.purchased) {
                        setUpObtainableButton(
                            titleTextView = binding.removeIntermediateTitle,
                            subtitleTextView = binding.removeIntermediateSubtitle,
                            button = binding.noIntermediateAdsButton,
                            skuDetailsWrapper = it,
                            lastIndex = lastIndex,
                            title = title
                        )
                        purchasesAvailable.add(INTER)
                    } else {
                        terminateButton(binding.noIntermediateAdsButton)
                    }
                }
                getString(R.string.full_no_ads) -> {
                    if (!it.purchased) {
                        setUpObtainableButton(
                            titleTextView = binding.fullNoAdsTitle,
                            subtitleTextView = binding.fullNoAdsSubtitle,
                            button = binding.fullNoAdsButton,
                            skuDetailsWrapper = it,
                            lastIndex = lastIndex,
                            title = title
                        )
                    } else {
                        terminateButton(binding.fullNoAdsButton)
                    }
                }
            }
        }
        return purchasesAvailable
    }

    enum class AvailablePurchase {
        PLACE,
        CLUE,
        INTER
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