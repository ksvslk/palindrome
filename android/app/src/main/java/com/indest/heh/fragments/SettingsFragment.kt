package com.indest.heh.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.SignInButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.heh.R
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
            //TODO: remove next 2 lines in production
            val config: Configuration = resources.configuration
            Toast.makeText(requireContext(), "DPI: ${config.densityDpi}", Toast.LENGTH_LONG).show()
            binding.soundSwitch.performClick()
        }
        binding.vibrationButton.setOnClickListener {
            binding.vibrationSwitch.performClick()
        }
        binding.uiModeButton.setOnClickListener {
            binding.uiSwitch.performClick()
        }
        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(requireContext(), "Sign in to google.", Toast.LENGTH_LONG).show()
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, b ->
            changePreference(getString(R.string.sound_on_pref), b)
        }
        binding.vibrationSwitch.setOnCheckedChangeListener { _, b ->
            changePreference(getString(R.string.vibration_on_pref), b)
        }
        binding.uiSwitch.setOnCheckedChangeListener { _, b ->
            changePreference(getString(R.string.ui_night_pref), b)
        }

        setInitialPreference(binding.soundSwitch, getString(R.string.sound_on_pref))
        setInitialPreference(binding.vibrationSwitch, getString(R.string.vibration_on_pref))
        setInitialPreference(binding.uiSwitch, getString(R.string.ui_night_pref))

        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    private fun changePreference(setting:String, value:Boolean)
    {
        requireActivity().getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {
            with(it.edit()) {
                putBoolean(setting, value)
                apply()
            }
        }
    }

    private fun setInitialPreference(button: CompoundButton, setting:String)
    {
        requireActivity().getSharedPreferences("heh_pref", Context.MODE_PRIVATE)?.let {
            button.isChecked = it.getBoolean(setting, true)
        }
    }

}