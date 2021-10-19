package com.indest.heh.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.SignInButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.heh.databinding.SettingsMenuDialogBinding

class SettingsDialogFragment : BottomSheetDialogFragment() {
    private val TAG = "SettingsDialogFragment"
    private val startActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    private lateinit var binding: SettingsMenuDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        binding = SettingsMenuDialogBinding.inflate(layoutInflater, container, false)
        binding.soundButton.setOnClickListener {
            Toast.makeText(requireContext(), "Turn sound ON/OFF", Toast.LENGTH_LONG).show()
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