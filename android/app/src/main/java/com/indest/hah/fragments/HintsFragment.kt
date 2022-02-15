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
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.fragments.HintsFragment.AvailablePurchase.*
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.SkuDetailsWrapper
import com.indest.hah.util.C.HINT_FRAGMENT_TAG
import com.indest.hah.util.C.HINT_REQUEST_KEY
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import com.indest.hah.game.enums.InternetState
import com.indest.hah.game.enums.RewardType

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
        removeAllPurchaseButtons() // At first remove all to avoid flickering
        return b.root
    }

    override fun onResume() {
        super.onResume()
        binding.revealCategoryShown.hintRevealedTextview.text =
            gameViewModel.currentLevel.levelInfo.category
        if (gameViewModel.currentLevel.levelInfo.categoryRewarded) {
            Log.i(TAG, "revealCategoryFlipper.showNext()")
            if (!binding.revealCategoryShown.root.isShown) {
                binding.revealCategoryFlipper.showNext()
            }
            binding.categoryRevealButton.isEnabled = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        skuDetailsCollectionJob = lifecycleScope.launch {
            gameViewModel.skuDetails.collect { skuDetailsWrapperList ->
                Log.i(TAG, skuDetailsWrapperList.toString())
                val isCategoryRewarded = gameViewModel.isCategoryRewarded()
                if (gameViewModel.internetState.value.state == InternetState.ONLINE) {
                    Log.i(TAG, "Set purchases online.")
                    when (setUpAvailablePurchases(skuDetailsWrapperList)) {
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
                        setOf(UNKNOWN) -> { // old info from offline, not updated yet from online
                            removeAllPurchaseButtons()
                        }
                        setOf<AvailablePurchase>() -> {
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
                } else {
                    Log.i(TAG, "Set purchases offline.")
                    val skus = gameViewModel.getAllSkus()
                    removeAllPurchaseButtons()
                    skus.forEach {
                        when (it.sku) {
                            getString(R.string.instant_clues) -> {
                                if (it.purchased) {
                                    setUpPurchasedGoods(RewardType.HINT, isCategoryRewarded)
                                }
                            }
                            getString(R.string.instant_letters) -> {
                                if (it.purchased) {
                                    setUpPurchasedGoods(RewardType.LETTER)
                                }
                            }
                            getString(R.string.full_no_ads) -> {
                                if (it.purchased) {
                                    setUpPurchasedGoods(RewardType.HINT, isCategoryRewarded)
                                    setUpPurchasedGoods(RewardType.LETTER)
                                }
                            }
                        }
                    }
                }

            }
        }

        rewardedAdCollectionJob = lifecycleScope.launch {
            gameViewModel.rewardedAdAvailable.collectLatest { rewardedAdAvailable ->
                if (gameViewModel.internetState.value.state == InternetState.ONLINE) {
                    Log.d(TAG, "Set up the video buttons.: $rewardedAdAvailable")
                    var cluesPurchased = false
                    var lettersPurchased = false
                    var allAdsRemoved = false

                    if (gameViewModel.skuDetails.value.isNotEmpty()) {
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
                    }

                    Log.d(TAG, "$cluesPurchased, $lettersPurchased, $allAdsRemoved")

                    if ((cluesPurchased && lettersPurchased) || allAdsRemoved) {
                        rewardedAdCollectionJob?.cancel()
                        return@collectLatest
                    }

                    val isCatRew = gameViewModel.isCategoryRewarded()

                    if (rewardedAdAvailable && !cluesPurchased)
                    {
                        binding.categoryRevealButton.setOnClickListener {
                            if (!isCatRew) {
                                if (isAdded) {
                                    Log.d(TAG, "Set up clue reward button")
                                    //TODO: SHOW REWARDED VIDEO
                                 /*   (requireActivity() as MainActivity).showRewardedVideo(
                                        RewardType.HINT
                                    )*/
                                }
                            }
                        }
                    }
                   if (rewardedAdAvailable && !lettersPurchased)
                        {
                        binding.letterPlaceButton.setOnClickListener {
                            dismiss()
                            if (isAdded) {
                                Log.d(TAG, "Set up letters reward button")
                                //TODO: SHOW REWARDED VIDEO
                                //(requireActivity() as MainActivity).showRewardedVideo(RewardType.LETTER)
                            }
                        }
                    }

                    if (!cluesPurchased) {
                        setUpRewardedButtonSubtitle(
                            binding.revealCategoryHidden.revealClueSubtitle,
                            rewardedAdAvailable
                        )
                    }
                    if (!lettersPurchased) {
                        setUpRewardedButtonSubtitle(
                            binding.placeLetterSubtitle,
                            rewardedAdAvailable
                        )
                    }
                } else {
                    // We are offline, no ad can be available
                    setUpRewardedButtonSubtitle(
                        binding.revealCategoryHidden.revealClueSubtitle,
                        adAvailable = false
                    )
                    setUpRewardedButtonSubtitle(
                        binding.placeLetterSubtitle,
                        adAvailable = false
                    )
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

    private fun removeAllPurchaseButtons() {
        listOf(
            binding.instantCluesButton,
            binding.instantLettersButton,
            binding.noIntermediateAdsButton,
            binding.fullNoAdsButton
        ).forEach {
            terminateButton(it)
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

    private fun setUpAvailablePurchases(skuDetails: List<SkuDetailsWrapper>): MutableSet<AvailablePurchase> {
        val purchasesAvailable = mutableSetOf<AvailablePurchase>()
        if (skuDetails.isEmpty()) {
            purchasesAvailable.add(UNKNOWN)
        }
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
                        binding.instantCluesButton.visibility = View.VISIBLE
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
                        binding.instantLettersButton.visibility = View.VISIBLE
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
                        binding.noIntermediateAdsButton.visibility = View.VISIBLE
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
                        binding.fullNoAdsButton.visibility = View.VISIBLE
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
        INTER,
        UNKNOWN
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