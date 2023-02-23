package com.indest.hah.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.indest.hah.R
import com.indest.hah.databinding.StagesFragmentBinding
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class StagesFragment : BottomSheetDialogFragmentCustom() {
    private val TAG = "StagesFragment"
    private var _binding: StagesFragmentBinding? = null
    private val binding get() = _binding!!
    private val generalViewModel: GeneralViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                generalViewModel.internet.combine(generalViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    when (internet) {
                        true -> {
                            if (accountStatus == AccountStatus.SIGNED_IN_WITH_FIREBASE) {
                                binding.submitYourLevelButton.visibility = View.VISIBLE
                                binding.submitYourLevelButton.setOnClickListener {
                                    launch {
                                        if (generalViewModel.isAllowedToSubmitPalindrome())
                                        {
                                            findNavController().popBackStack()
                                            parentFragmentManager.commit {
                                                addToBackStack(SubmissionFragment().tag)
                                                add(SubmissionFragment(), SubmissionFragment().tag)
                                            }
                                        }
                                        else {
                                            showLimitedSubmissionsToast()
                                        }
                                    }
                                }
                                binding.playerLevelsButton.visibility = View.VISIBLE
                            }
                            else {
                                binding.submitYourLevelButton.setOnClickListener(null)
                                binding.submitYourLevelButton.visibility = View.GONE
                                if (!generalViewModel.isPlayerLevelsAvailable())
                                {
                                    binding.playerLevelsButton.visibility = View.GONE
                                }
                            }
                        }
                        false -> {
                            if (!generalViewModel.isPlayerLevelsAvailable())
                            {
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
        Log.d(TAG, "onCreateView")
        val b = StagesFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        binding.gameLevelsButton.setOnClickListener {
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(LevelSelectFragment().tag)
                add(LevelSelectFragment(), LevelSelectFragment().tag)
            }
        }
        binding.playerLevelsButton.setOnClickListener {
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(PlayerLevelSelectFragment().tag)
                add(PlayerLevelSelectFragment(), PlayerLevelSelectFragment().tag)
            }
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    private fun showLimitedSubmissionsToast()
    {
        val toast = Toast.makeText(
            requireContext().applicationContext,
            "You can submit once per day.",
            Toast.LENGTH_LONG
        )
        toast.show()
    }

}