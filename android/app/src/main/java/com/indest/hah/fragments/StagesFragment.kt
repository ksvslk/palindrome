package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.commit
import com.google.android.gms.common.SignInButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.BuildConfig
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding
import com.indest.hah.databinding.StagesFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.findNavController

class StagesFragment : BottomSheetDialogFragment()  {
    private val TAG = "StagesFragment"
    private var _binding: StagesFragmentBinding? = null
    private val binding get() = _binding!!

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
        binding.submitYourLevelButton.setOnClickListener {
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(SubmissionFragment().tag)
                add(SubmissionFragment(), SubmissionFragment().tag)
            }
        }
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

}