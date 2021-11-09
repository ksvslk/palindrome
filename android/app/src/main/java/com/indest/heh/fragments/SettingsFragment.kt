package com.indest.heh.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.SignInButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.heh.databinding.SettingsFragmentBinding

class SettingsFragment : BottomSheetDialogFragment() {
    private val TAG = "SettingsFragment"
    private lateinit var binding: SettingsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        binding = SettingsFragmentBinding.inflate(layoutInflater, container, false)
        binding.levelSelectButton.setOnClickListener {
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(LevelSelectFragment().tag)
                add(LevelSelectFragment(), LevelSelectFragment().tag)
            }
        }
        binding.soundButton.setOnClickListener {
            val config: Configuration = resources.configuration
            Toast.makeText(requireContext(), "DPI: ${config.densityDpi}", Toast.LENGTH_LONG).show()
        }
        binding.vibrationButton.setOnClickListener {
            Toast.makeText(requireContext(), "Vibration ON/OFF", Toast.LENGTH_LONG).show()
        }
        binding.uiModeButton.setOnClickListener {
            Toast.makeText(requireContext(), "Change UI mode.", Toast.LENGTH_LONG).show()
        }
        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(requireContext(), "Sign in to google.", Toast.LENGTH_LONG).show()
        }
        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

}