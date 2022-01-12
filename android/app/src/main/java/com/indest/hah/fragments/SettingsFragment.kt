package com.indest.hah.fragments

import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.play.core.review.ReviewManagerFactory
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding
import androidx.test.core.app.ApplicationProvider.getApplicationContext

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient.getPackageName
import androidx.test.core.app.ApplicationProvider


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

        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(requireActivity())
        if (lastSignedInAccount != null) {
            binding.googleSignInButton.visibility = View.GONE
            binding.googleSignOutButton.visibility = View.VISIBLE
            binding.googleSignOutButton.setOnClickListener {
                signOut()
            }
        } else {
            binding.googleSignOutButton.visibility = View.GONE
            binding.googleSignInButton.visibility = View.VISIBLE
            binding.googleSignInButton.setOnClickListener {
                signInToGoogle()
            }
        }

        binding.levelSelectButton.setOnClickListener {
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(LevelSelectFragment().tag)
                add(LevelSelectFragment(), LevelSelectFragment().tag)
            }
        }

        binding.rateGameButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    getString(R.string.app_package_url))
                setPackage(getString(R.string.vending_package))
            }
            if(intent.resolveActivity(requireContext().packageManager) != null)
            {
                startActivity(intent)
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
        binding.soundSwitch.setOnCheckedChangeListener { _, b ->
            changePreference(getString(R.string.sound_on_pref), b)
        }
        binding.vibrationSwitch.setOnCheckedChangeListener { _, b ->
            changePreference(getString(R.string.vibration_on_pref), b)
        }

        setInitialPreference(binding.soundSwitch, getString(R.string.sound_on_pref))
        setInitialPreference(binding.vibrationSwitch, getString(R.string.vibration_on_pref))

        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                dismiss()
            }
            else if (it.resultCode != Activity.RESULT_CANCELED) {
                Toast.makeText(
                    requireContext(),
                    "Something went wrong. Do you have internet connection?",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            }
        }

    private fun signInToGoogle() {
        val signInClient = GoogleSignIn.getClient(
            requireActivity(),
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        )
        activityResultLauncher.launch(signInClient.signInIntent)
    }

    private fun signOut() {
        val signInClient = GoogleSignIn.getClient(
            requireActivity(),
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        )
        signInClient.signOut().addOnCompleteListener {
            if (it.isSuccessful)
            {
                Toast.makeText(
                    requireContext(),
                    "Successfully signed out.",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            }
            else {
                Toast.makeText(
                    requireContext(),
                    "Something went wrong when trying to sign out.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun changePreference(setting: String, value: Boolean) {
        requireActivity().getSharedPreferences(
            getString(R.string.preferences),
            Context.MODE_PRIVATE
        )?.let {
            with(it.edit()) {
                putBoolean(setting, value)
                apply()
            }
        }
    }

    private fun setInitialPreference(button: CompoundButton, setting: String) {
        requireActivity().getSharedPreferences(
            getString(R.string.preferences),
            Context.MODE_PRIVATE
        )?.let {
            button.isChecked = it.getBoolean(setting, true)
        }
    }
}