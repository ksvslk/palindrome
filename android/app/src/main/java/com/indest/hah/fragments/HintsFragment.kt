package com.indest.hah.fragments

import android.animation.Animator
import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.indest.hah.R
import com.indest.hah.ads.RewardType
import com.indest.hah.ads.RewardType.AD_FAILED
import com.indest.hah.ads.RewardType.CLUE
import com.indest.hah.ads.RewardType.LETTER
import com.indest.hah.ads.RewardType.NONE
import com.indest.hah.ads.RewardType.PICTURE
import com.indest.hah.ads.RewardType.SAD_AD_FAILED
import com.indest.hah.ads.RewardType.SAD_MODE
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.fx.SoundPlayer
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.views.ButtonState.AD
import com.indest.hah.views.ButtonState.NO_AD
import com.indest.hah.views.ButtonState.PREMIUM
import com.indest.hah.views.PurchaseButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class HintsFragment : FullBottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private var _binding: HintsFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                launch {
                    viewModel.buttonState.collect {
                        if (!viewModel.currentLevel.value.levelInfo.pictureRewarded)
                        {
                            binding.rewardPictureButton.updateButtonUi(it)
                        }
                        binding.rewardCategoryButton.updateButtonUi(it)
                        binding.rewardPlaceLetter.updateButtonUi(it)
                        binding.purchaseButtonPictures.updateButtonUi(it)
                        binding.purchaseButtonClues.updateButtonUi(it)
                        binding.purchaseButtonLetters.updateButtonUi(it)
                        binding.purchaseNoSadMode.updateButtonUi(it)
                        binding.purchaseAllInclusivePack2?.updateButtonUi(it)
                    }
                }
                launch {
                    viewModel.rewardState.distinctUntilChangedBy { it }.collect {
                        handleRewards(it)
                    }
                }
                launch {
                    levelViewModel.pictureHintDownloaded.collectLatest {
                        Log.d(TAG, "pictureHintDownloaded: $it")
                        if (it) binding.rewardPictureButton.removeDownloadingState()
                        else binding.rewardPictureButton.addDownloadingState()
                    }
                }
            }
        }

        val hintInLocale = levelViewModel.getCurrentHintInLocale()
        binding.rewardCategoryButton.setClue(hintInLocale)
        if (viewModel.currentLevel.value.levelInfo.categoryRewarded) {
            binding.rewardCategoryButton.showClue()
        }
        if (viewModel.currentLevel.value.levelInfo.pictureRewarded)
        {
            binding.rewardPictureButton.updateToPremium()
        }

        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonPictures)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonClues)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonLetters)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseNoSadMode)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseAllInclusivePack2)

        binding.rewardPictureButton.setOnClickListener {
            if (levelViewModel.currentLevel.value.levelInfo.progress == LevelProgress.INTERMEDIATE)
            {
                levelIsCompletedToast()
                return@setOnClickListener
            }

            if (!viewModel.currentLevel.value.levelInfo.pictureRewarded) {
                when (binding.rewardPictureButton.buttonState) {
                    AD -> {
                        (activity as? AppCompatActivity)?.let {
                            viewModel.showRewardedVideo(it, PICTURE)
                        }
                    }
                    PREMIUM -> {
                        revealPicture()
                        viewModel.updatePictureRewarded()
                        levelViewModel.logPremiumHintUsage(PICTURE)
                    }
                    NO_AD -> {  /* No action. Button should be disabled in this state. */
                    }
                }
            }
            else {
                revealPicture()
            }

        }

        binding.rewardCategoryButton.setOnClickListener {
            if (levelViewModel.currentLevel.value.levelInfo.progress == LevelProgress.INTERMEDIATE)
            {
                levelIsCompletedToast()
                return@setOnClickListener
            }
            if (!viewModel.currentLevel.value.levelInfo.categoryRewarded) {
                when (binding.rewardCategoryButton.buttonState) {
                    AD -> {
                        (activity as? AppCompatActivity)?.let {
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
            if (levelViewModel.currentLevel.value.levelInfo.progress == LevelProgress.INTERMEDIATE)
            {
                levelIsCompletedToast()
                return@setOnClickListener
            }
            when (binding.rewardPlaceLetter.buttonState) {
                AD -> {
                    dismissAllowingStateLoss()
                    (activity as? AppCompatActivity)?.let {
                        viewModel.showRewardedVideo(it, LETTER)
                    }
                }
                PREMIUM -> {
                    levelViewModel.logPremiumHintUsage(LETTER)
                    viewModel.updateRewardState(LETTER)
                    dismissAllowingStateLoss()
                }
                NO_AD -> { /* No action. Button should be disabled in this state. */
                }
            }
        }
    }

    private fun levelIsCompletedToast() {
        val toast = Toast.makeText(
            requireContext().applicationContext,
            resources.getString(R.string.level_already_completed),
            Toast.LENGTH_SHORT
        )
        toast.show()
    }

    private fun handleRewards(rewardType: RewardType) {
        when (rewardType) {
            NONE -> {/*_*/
            }
            PICTURE -> {
                revealPicture()
                levelViewModel.saveGameImmediately()
                viewModel.updateRewardState(NONE)
                levelViewModel.logRewardGiven(rewardType)
                binding.rewardPictureButton.updateToPremium()
                binding.rewardPictureButton.setOnClickListener(null)
                binding.rewardPictureButton.setOnClickListener { revealPicture() }
            }
            CLUE -> {
                revealClue()
                levelViewModel.saveGameImmediately()
                viewModel.updateRewardState(NONE)
                levelViewModel.logRewardGiven(rewardType)
            }
            LETTER -> {
                levelViewModel.logRewardGiven(rewardType)
                viewModel.updateRewardState(LETTER)
                dismissAllowingStateLoss()
            }
            AD_FAILED -> {
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

    private fun revealPicture() {
        showPictureHintForCurrentLevel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                    (activity as? AppCompatActivity)?.let {
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

    private fun showPictureHintForCurrentLevel() {
        soundPlayer.play(soundPlayer.sounds.letterRewardedSoundId)
        val pictureDialogFragment = PictureDialogFragment()
        Log.d(TAG, "CURRENT: ${levelViewModel.currentRelativeLevelNumber}")
        Log.d(TAG, "CC: ${levelViewModel.currentLevel.value.levelInfo.countryCode}")
        pictureDialogFragment.setDrawable(
            requireContext(),
            levelViewModel.currentRelativeLevelNumber,
            levelViewModel.currentLevel.value.levelInfo.countryCode
        )
        pictureDialogFragment.show(childFragmentManager, PictureDialogFragment.TAG)
    }

}