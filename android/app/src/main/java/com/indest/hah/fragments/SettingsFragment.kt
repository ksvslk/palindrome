package com.indest.hah.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.games.Games.SCOPE_GAMES_SNAPSHOTS
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.BuildConfig
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.SignInLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BottomSheetDialogFragment() {
    private val TAG = "SettingsFragment"
    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!
    private val generalViewModel: GeneralViewModel by activityViewModels()
    @Inject
    lateinit var signInObserver: SignInLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(signInObserver)
        lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                generalViewModel.internet.combine(generalViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    binding.rateGameButton.setOnClickListener {
                        when (internet) {
                            true -> {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(getString(R.string.app_package_url))
                                    setPackage(getString(R.string.vending_package))
                                }
                                if (intent.resolveActivity(requireContext().packageManager) != null) {
                                    startActivity(intent)
                                }
                            }
                            false -> {
                                Toast.makeText(
                                    requireContext().applicationContext,
                                    getString(R.string.no_internet),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    when (accountStatus) {
                        AccountStatus.SIGNED_IN -> {
                            binding.googleSignInButton.visibility = View.GONE
                            binding.googleSignOutButton.visibility = View.VISIBLE
                            binding.googleSignOutButton.setOnClickListener {
                                signOut()
                            }
                        }
                        AccountStatus.SIGNED_OUT -> {
                            binding.googleSignOutButton.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.VISIBLE
                        }
                        AccountStatus.NOT_SET -> {}
                    }

                    binding.googleSignInButton.setOnClickListener {
                        when (internet) {
                            true -> {
                                signInToGoogle()
                            }
                            false -> {
                                Toast.makeText(
                                    requireContext().applicationContext,
                                    getString(R.string.no_internet),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
        val b = SettingsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b

        binding.levelSelectButton.setOnClickListener {
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(LevelSelectFragment().tag)
                add(LevelSelectFragment(), LevelSelectFragment().tag)
            }
        }

        binding.soundButton.setOnClickListener {
            binding.soundSwitch.performClick()
        }
        binding.vibrationButton.setOnClickListener {
            binding.vibrationSwitch.performClick()
        }
        binding.soundSwitch.setOnCheckedChangeListener { _, b1 ->
            changePreference(getString(R.string.sound_on_pref), b1)
        }
        binding.vibrationSwitch.setOnCheckedChangeListener { _, b2 ->
            changePreference(getString(R.string.vibration_on_pref), b2)
        }
        binding.tutorialButton.setOnClickListener {
            parentFragmentManager.commit {
                addToBackStack(TutorialFragment().tag)
                add(TutorialFragment(), TutorialFragment().tag)
            }
            dismiss()
        }
        binding.versionInfo.text = "${BuildConfig.VERSION_CODE}\n${BuildConfig.VERSION_NAME}\n${BuildConfig.GIT_HASH}"
        binding.settingsDialogTitle.setOnLongClickListener {
            binding.versionInfo.visibility =
                if (binding.versionInfo.visibility == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
            true
        }

        setInitialPreference(binding.soundSwitch, getString(R.string.sound_on_pref))
        setInitialPreference(binding.vibrationSwitch, getString(R.string.vibration_on_pref))

        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    private fun signInToGoogle() {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(SCOPE_GAMES_SNAPSHOTS)
                .build()

        val signInClient = GoogleSignIn.getClient(
            requireContext().applicationContext,
            signInOptions
        )
        signInObserver.signIn(signInClient.signInIntent)
    }

    private fun signOut() {
        val client =
            GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
        client.signOut().addOnCompleteListener(requireActivity()) {
            if (it.isSuccessful) {
                signInObserver.setSignedOutState()
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
                dismiss()
            }
        }
    }

    private fun changePreference(setting: String, value: Boolean) {
        requireContext().applicationContext.getSharedPreferences(
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
        requireContext().applicationContext.getSharedPreferences(
            getString(R.string.app_preferences),
            Context.MODE_PRIVATE
        )?.let {
            button.isChecked = it.getBoolean(setting, true)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}