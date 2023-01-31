package com.indest.hah.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.R
import com.indest.hah.databinding.StagesFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.LevelSelectType
import com.indest.hah.other.appendSmallSuperscript
import com.indest.hah.other.safeNavigate
import com.indest.hah.persistance.level.LevelProgress
import com.indest.hah.views.PromoButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class StagesFragment : FullBottomSheetDialogFragment() {
    private val TAG = "StagesFragment"
    private var _binding: StagesFragmentBinding? = null
    private val binding get() = _binding!!
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                val isAllowedToSubmit = generalViewModel.isAllowedToSubmitPalindrome()
                levelViewModel.accountStatus.collect { accountStatus ->
                    binding.submitYourLevelButton.visibility = View.VISIBLE
                    if (accountStatus == AccountStatus.SIGNED_IN_WITH_FIREBASE) {
                        binding.submitYourLevelButton.setOnClickListener {
                            if (isAllowedToSubmit) {
                                findNavController().safeNavigate(R.id.action_navigation_stages_to_navigation_submission)
                            } else {
                                showLimitedSubmissionsToast()
                            }
                        }
                        binding.playerLevelsButton.visibility = View.VISIBLE
                    } else {
                        binding.submitYourLevelButton.setOnClickListener(null)
                        binding.submitYourLevelButton.visibility = View.GONE
                        if (!generalViewModel.isPlayerLevelsAvailable()) {
                            binding.playerLevelsButton.visibility = View.GONE
                        }
                    }
                }

            }
        }

        binding.newPromoButton.visibility = View.GONE
        levelViewModel.promoMenuButtonData?.let { promoData ->
            if (promoData.activated == true) {
                val promoButtonTitle =
                    binding.newPromoButton.findViewById<AppCompatTextView>(R.id.promo_button_title)
                promoButtonTitle.text = promoData.title
                binding.newPromoButton.setOnClickListener {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(promoData.link)
                        setPackage(getString(R.string.vending_package))
                        if (context?.let { it1 -> this.resolveActivity(it1.packageManager) } != null) {
                            startActivity(this)
                        } else {
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(promoData.link)
                            }
                            startActivity(browserIntent)
                        }
                    }
                    levelViewModel.logMenuPromoClicked()
                }
                binding.newPromoButton.visibility = View.VISIBLE;

            } else {
                binding.newPromoButton.visibility = View.GONE;
            }
        }

        /* TODO: Need to be rewritten VM once notification system in place

                lifecycleScope.launch {
            levelViewModel.countOfNewPlayerLevelsAndLanguageLevels()
                .collect { (newPlayerLevelCount, newLanguageLevelCount) ->
                    val superScriptPlayer = "+$newPlayerLevelCount"
                    val playerLevelSelectButtonTitle =
                        binding.playerLevelsButton.findViewById<AppCompatTextView>(R.id.any_button_title)
                    val finalPlayerTitle = playerLevelSelectButtonTitle.text.toString()
                        .appendSmallSuperscript(superScriptPlayer)

                    if (newPlayerLevelCount > 0) {
                        playerLevelSelectButtonTitle.text = finalPlayerTitle
                    }

                    val superScriptLanguage = "+$newLanguageLevelCount"
                    val langLevelSelectButtonTitle =
                        binding.internationalLevelsButton.findViewById<AppCompatTextView>(R.id.any_button_title)
                    val langPlayerTitle = langLevelSelectButtonTitle.text.toString()
                        .appendSmallSuperscript(superScriptLanguage)

                    if (newLanguageLevelCount > 0) {
                        langLevelSelectButtonTitle.text = langPlayerTitle
                    }
                }
        }
         */
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = StagesFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        binding.gameLevelsButton.setOnClickListener {
            findNavController().safeNavigate(
                R.id.action_navigation_stages_to_navigation_level_select,
                LevelSelectType.GAME_LEVELS
            )
        }
        binding.playerLevelsButton.setOnClickListener {
            findNavController().safeNavigate(
                R.id.action_navigation_stages_to_navigation_level_select,
                LevelSelectType.PLAYER_LEVELS
            )
        }
        binding.internationalLevelsButton.setOnClickListener {
            findNavController().safeNavigate(R.id.action_navigation_stages_to_navigation_international)
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    private fun showLimitedSubmissionsToast() {
        val toast = Toast.makeText(
            requireContext().applicationContext,
            resources.getString(R.string.submit_once_per_day_limit),
            Toast.LENGTH_LONG
        )
        toast.show()
    }

    override fun onDestroyView() {
        activity?.let {
            val stagesBadge = it.findViewById<BottomNavigationView>(R.id.bottomNavView)
                .getOrCreateBadge(R.id.navigation_stages)
            stagesBadge.isVisible = false
        }
        super.onDestroyView()
    }

}