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
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.OverallState


class SettingsFragment : BottomSheetDialogFragment() {
    private val TAG = "SettingsFragment"
    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        val b = SettingsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b

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
                if (gameViewModel.overAllState.value.state == OverallState.ONLINE) {
                    signInToGoogle()
                } else {
                    Toast.makeText(
                        requireContext().applicationContext,
                        getString(R.string.no_internet),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
            if (gameViewModel.overAllState.value.state == OverallState.ONLINE) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(
                        getString(R.string.app_package_url)
                    )
                    setPackage(getString(R.string.vending_package))
                }
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                }
            } else {
                Toast.makeText(
                    requireContext().applicationContext,
                    getString(R.string.no_internet),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.soundButton.setOnClickListener {
            //TODO: remove next 2 lines in production
            val config: Configuration = resources.configuration
            Toast.makeText(requireContext().applicationContext, "DPI: ${config.densityDpi}", Toast.LENGTH_LONG).show()
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
        return b.root
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                dismiss()
            } else if (it.resultCode != Activity.RESULT_CANCELED) {
                Toast.makeText(
                    requireContext().applicationContext,
                    "Something went wrong. Do you have internet connection?",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            }
        }

    private fun signInToGoogle() {
        gameViewModel.changeState(OverallState.LOADING)
        parentFragmentManager.commit {
            setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_top
            )
            setReorderingAllowed(true)
            replace<LoadingFragment>(R.id.fragments_container)
        }
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
            if (it.isSuccessful) {
                Toast.makeText(
                    requireContext().applicationContext,
                    "Successfully signed out.",
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            } else {
                Toast.makeText(
                    requireContext().applicationContext,
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

    override fun onDestroyView() {
        activityResultLauncher.unregister()
        _binding = null
        super.onDestroyView()
    }
}