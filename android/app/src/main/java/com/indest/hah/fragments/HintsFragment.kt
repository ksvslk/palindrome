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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.ads.RewardType.*
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.C
import com.indest.hah.views.ButtonState.*
import com.indest.hah.views.PurchaseButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private var _binding: HintsFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                launch {
                    viewModel.buttonState.collect {
                        Log.d(TAG, "Update buttons!: ${it}")
                        binding.rewardCategoryButton.updateButtonUi(it)
                        binding.rewardPlaceLetter.updateButtonUi(it)
                        binding.purchaseButtonClues.updateButtonUi(it)
                        binding.purchaseButtonLetters.updateButtonUi(it)
                        binding.purchaseNoSadMode.updateButtonUi(it)
                        binding.purchaseAllInclusivePack.updateButtonUi(it)
                    }
                }
                launch {
                    viewModel.rewardState.distinctUntilChangedBy { it }.collect {
                        Log.d(TAG, "Reward actually given: $it")
                        handleRewards(it)
                    }
                }
            }
        }

        val uniqueHints = viewModel.currentLevel.value.solutions.map { it.hint }.toSet()
        if (uniqueHints.isNotEmpty()) {
            val hints =
                if (uniqueHints.size > 1) uniqueHints.joinToString(" / ") { it } else uniqueHints.first()
            binding.rewardCategoryButton.setClue(hints, uniqueHints.size > 1)
        }
        if (viewModel.currentLevel.value.levelInfo.categoryRewarded) {
            binding.rewardCategoryButton.showClue()
        }

        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonClues)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonLetters)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseNoSadMode)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseAllInclusivePack)

        binding.rewardCategoryButton.setOnClickListener {
            if (!viewModel.currentLevel.value.levelInfo.categoryRewarded) {
                when (binding.rewardCategoryButton.buttonState) {
                    AD -> {
                        levelViewModel.setUiStateBusy()
                        activity?.let {
                            viewModel.showRewardedVideo(it, CLUE)
                        }
                    }
                    PREMIUM -> {
                        revealClue()
                        viewModel.updateCategoryRewarded()
                        levelViewModel.logPremiumHintUsage(CLUE)
                    }
                    NO_AD -> {  /* No action. Button should be disabled in this state. */
                    }
                }
            }
        }

        binding.rewardPlaceLetter.setOnClickListener {
            when (binding.rewardPlaceLetter.buttonState) {
                AD -> {
                    levelViewModel.setUiStateBusy()
                    activity?.let {
                        viewModel.showRewardedVideo(it, LETTER)
                    }
                }
                PREMIUM -> {
                    levelViewModel.logPremiumHintUsage(LETTER)
                    setFragmentResult(
                        C.HINTS_REQUEST_KEY,
                        bundleOf(
                            C.LETTER_PREMIUM_BUNDLE to true
                        )
                    )
                    dismiss()
                }
                NO_AD -> { /* No action. Button should be disabled in this state. */
                }
            }
        }
    }

    private fun handleRewards(rewardType: RewardType) {
        when (rewardType) {
            NONE -> {/*_*/
            }
            CLUE -> {
                revealClue()
                levelViewModel.saveGameImmediately()
                viewModel.updateRewardState(NONE)
                levelViewModel.logRewardGiven(rewardType)
            }
            LETTER -> {
                levelViewModel.logRewardGiven(rewardType)
                setFragmentResult(
                    C.HINTS_REQUEST_KEY,
                    bundleOf(C.LETTER_REWARD_TYPE_BUNDLE to rewardType.ordinal)
                )
                viewModel.updateRewardState(NONE)
                dismiss()
            }
            AD_FAILED -> {
                levelViewModel.setUiStatePlayWhenBusy()
                viewModel.updateRewardState(NONE)
            }
            SAD_MODE -> {}
            SAD_AD_FAILED -> {}
        }
    }

    private fun revealClue() {
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

        b.root.layoutTransition.disableTransitionType(LayoutTransition.CHANGING);
        b.root.layoutTransition.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
        b.root.layoutTransition.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        b.root.layoutTransition.setAnimator(LayoutTransition.APPEARING, appearingScaleY)
        b.root.layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, disappearingScaleY)

        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        return b.root
    }

    private fun addBillingFlowOnClickToPurchaseButton(view: View?) {
        view?.setOnClickListener {
            (view as? PurchaseButton)?.let { pb ->
                pb.sku?.let { sku ->
                    levelViewModel.setUiStateMenuAction()
                    Log.d(TAG, "launchBillingFlow: $sku")
                    activity?.let {
                        viewModel.launchBillingFlow(it, sku)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        activity?.let {
            val hintBadge = it.findViewById<BottomNavigationView>(R.id.bottomNavView)
                .getOrCreateBadge(R.id.navigation_goods)
            hintBadge.isVisible = false
        }
        super.onDestroyView()
    }


}