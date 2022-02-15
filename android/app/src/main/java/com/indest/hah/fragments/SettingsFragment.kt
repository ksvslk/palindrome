package com.indest.hah.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
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
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.enums.InternetState
import kotlinx.coroutines.launch


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
        Log.d(TAG, "onCreateView")
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
                if (gameViewModel.internetState.value.state == InternetState.ONLINE) {
                    signInToGoogle()
                } else {
                    Toast.makeText(requireContext().applicationContext,getString(R.string.no_internet),Toast.LENGTH_SHORT).show()
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
            lifecycleScope.launch {
                gameViewModel.increaseIntervalCount(getString(R.string.player_centered_scores))
            }
            return@setOnClickListener
            if (gameViewModel.internetState.value.state == InternetState.ONLINE) {
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
                Toast.makeText(requireContext().applicationContext,getString(R.string.no_internet),Toast.LENGTH_SHORT).show()
            }
        }

        binding.soundButton.setOnClickListener {
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
        binding.tutorialButton.setOnClickListener {
            lifecycleScope.launch {
                gameViewModel.increaseIntervalCount(getString(R.string.hint_suggestion))
            }
            return@setOnClickListener
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<TutorialFragment>(R.id.fragments_container)
            }
            dismiss()
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
                Log.d(TAG, "it.resultCode == Activity.RESULT_OK")
                dismiss()
            }
            else {
                dismiss()
                Log.d(TAG, "Activity left without RESULT_OK ${it.resultCode}")
            }
        }

    private fun signInToGoogle() {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(Scope(Scopes.DRIVE_APPFOLDER))
                .build()

        val signInClient = GoogleSignIn.getClient(
            requireActivity(),
            signInOptions
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
                Toast.makeText(requireContext().applicationContext,"Successfully signed out.",Toast.LENGTH_LONG).show()
                dismiss()
            } else {
                Toast.makeText(requireContext().applicationContext,"Something went wrong when trying to sign out.",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun changePreference(setting: String, value: Boolean) {
        requireActivity().getSharedPreferences(
            getString(R.string.app_preferences),
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
            getString(R.string.app_preferences),
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