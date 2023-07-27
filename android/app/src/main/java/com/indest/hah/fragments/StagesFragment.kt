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
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.StagesFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.LevelSelectType
import com.indest.hah.other.safeNavigate
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
                generalViewModel.internet.combine(levelViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    when (internet) {
                        true -> {
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
                        false -> {
                            if (!generalViewModel.isPlayerLevelsAvailable()) {
                                binding.playerLevelsButton.visibility = View.GONE
                            }
                            binding.submitYourLevelButton.setOnClickListener(null)
                            binding.submitYourLevelButton.visibility = View.GONE
                        }
                    }
                }
            }
        }

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
            "You can submit once per day.",
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