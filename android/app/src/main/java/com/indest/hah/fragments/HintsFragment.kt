package com.indest.hah.fragments

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.GeneralViewModel
import com.indest.hah.ads.RewardType
import com.indest.hah.ads.RewardType.*
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.other.C
import com.indest.hah.views.ButtonState.*
import com.indest.hah.views.PurchaseButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private var _binding: HintsFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GeneralViewModel by activityViewModels()
    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                viewModel.buttonState.collect() {
                    Log.d(TAG, "Update buttons!")
                    handleRewards(it.rewardType)
                    binding.rewardCategoryButton.updateButtonUi(it)
                    binding.rewardPlaceLetter.updateButtonUi(it)
                    binding.purchaseButtonClues.updateButtonUi(it)
                    binding.purchaseButtonLetters.updateButtonUi(it)
                    binding.purchaseIntermediateAds.updateButtonUi(it)
                    binding.purchaseFullAds.updateButtonUi(it)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rewardCategoryButton.setClue(viewModel.currentLevel.value.levelInfo.category)
        if (viewModel.currentLevel.value.levelInfo.categoryRewarded)
        {
            binding.rewardCategoryButton.showClue()
        }
    }

    private fun handleRewards(rewardType: RewardType)
    {
        when(rewardType)
        {
            NEXT_LEVEL -> {/*_*/}
            NONE -> {/*_*/}
            CLUE -> revealClue()
            LETTER -> {
                setFragmentResult(
                    C.HINTS_REQUEST_KEY,
                    bundleOf(C.LETTER_REWARD_TYPE_BUNDLE to rewardType.ordinal)
                )
                dismiss()
            }
        }
    }

    private fun revealClue()
    {
        soundPlayer.play(soundPlayer.sounds.letterRewardedSoundId)
        binding.rewardCategoryButton.showClue()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val b = HintsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b

        val appearingScaleY: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
        )
        val disappearingScaleY: Animator = ObjectAnimator.ofPropertyValuesHolder(
            null as Any?,
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
        )
        appearingScaleY.duration = 3000
        disappearingScaleY.duration = 3000

        binding.root.layoutTransition.disableTransitionType(LayoutTransition.CHANGING);
        binding.root.layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        binding.root.layoutTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        binding.root.layoutTransition.setAnimator(LayoutTransition.APPEARING, appearingScaleY)
        binding.root.layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, disappearingScaleY)

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonClues)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonLetters)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseIntermediateAds)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseFullAds)

        binding.rewardCategoryButton.setOnClickListener {
            if (!viewModel.currentLevel.value.levelInfo.categoryRewarded)
            {
                when (binding.rewardCategoryButton.buttonState)
                {
                    AD -> {
                        viewModel.showRewardedVideo(requireActivity(), CLUE)
                    }
                    PREMIUM -> {
                        revealClue()
                        viewModel.updateCategoryRewarded()
                    }
                    NO_AD -> {  /* No action. Button should be disabled in this state. */ }
                }
            }
        }

        binding.rewardPlaceLetter.setOnClickListener {
            when (binding.rewardPlaceLetter.buttonState)
            {
                AD -> {
                    viewModel.showRewardedVideo(requireActivity(), LETTER)
                }
                PREMIUM -> {
                    setFragmentResult(
                        C.HINTS_REQUEST_KEY,
                        bundleOf(
                            C.LETTER_PREMIUM_BUNDLE to true
                        )
                    )
                    dismiss()
                }
                NO_AD -> { /* No action. Button should be disabled in this state. */ }
            }
        }
        return b.root
    }

    private fun addBillingFlowOnClickToPurchaseButton(view: View?)
    {
        view?.setOnClickListener {
            (view as? PurchaseButton)?.let { pb ->
                pb.sku?.let { sku ->
                    viewModel.launchBillingFlow(requireActivity(), sku)
                }
            }
        }
    }

}