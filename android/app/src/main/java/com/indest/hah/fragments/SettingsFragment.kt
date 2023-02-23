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
import com.google.android.gms.common.SignInButton
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.Player
import com.google.android.gms.tasks.Task
import com.indest.hah.BuildConfig
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : BottomSheetDialogFragmentCustom() {
    private val TAG = "SettingsFragment"
    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                generalViewModel.internet.combine(generalViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    binding.shareButtonSettings.visibility =
                        if (internet) View.VISIBLE else View.GONE
                    binding.shareButtonSettings.setOnClickListener {
                        val shareIntent = generalViewModel.getShareIntent("SettingsFragment")
                        startActivity(shareIntent)
                    }
                    binding.rateGameButton.setOnClickListener {
                        context?.let {
                            when (internet) {
                                true -> {
                                    val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(getString(R.string.app_package_url))
                                        setPackage(getString(R.string.vending_package))
                                    }
                                    if (playStoreIntent.resolveActivity(it.packageManager) != null) {
                                        startActivity(playStoreIntent)
                                    } else {
                                        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(getString(R.string.app_package_url))
                                        }
                                        startActivity(browserIntent)
                                    }
                                }
                                false -> {
                                    Toast.makeText(
                                        it.applicationContext,
                                        getString(R.string.no_internet),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }

                    when (accountStatus) {
                        AccountStatus.SIGNED_IN -> {
                            Log.d(TAG, "Signed in.")
                            binding.progressCircular.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.GONE
                            binding.googleSignOutButton.visibility = View.VISIBLE
                            binding.googleSignOutButton.text = "Signed in as ${levelViewModel.currentlySignedInPlayerName}"
                        }
                        AccountStatus.SIGNED_OUT -> {
                            Log.d(TAG, "Signed out.")
                            binding.progressCircular.visibility = View.GONE
                            binding.googleSignOutButton.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.VISIBLE
                        }
                        AccountStatus.LOADING -> {
                            binding.progressCircular.visibility = View.VISIBLE
                            binding.googleSignOutButton.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.GONE
                            Log.d(TAG, "Loading, signing in!")
                        }
                        AccountStatus.NOT_SET -> {}
                    }

                    binding.googleSignInButton.setOnClickListener {
                        when (internet) {
                            true -> {
                                signInToGoogle()
                            }
                            false -> {
                                context?.let {
                                    Toast.makeText(
                                        it.applicationContext,
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
            levelViewModel.setCompetitionStatus(
                levelViewModel.getCompetitionStatus().copy(
                    accessedFromMenu = true
                )
            )
            findNavController().popBackStack()
            parentFragmentManager.commit {
                addToBackStack(TutorialFragment().tag)
                add(TutorialFragment(), TutorialFragment().tag)
            }
        }
        binding.versionInfo.text =
            "${BuildConfig.VERSION_CODE}\n${BuildConfig.VERSION_NAME}\n${BuildConfig.GIT_HASH}"
        binding.settingsDialogTitle.setOnLongClickListener {
            binding.versionInfo.visibility =
                if (binding.versionInfo.visibility == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
            true
        }

        setInitialPreference(binding.soundSwitch, getString(R.string.sound_on_pref))
        setInitialPreference(binding.vibrationSwitch, getString(R.string.vibration_on_pref))
        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val b = SettingsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    private fun signInToGoogle() {
        activity?.let { activity ->
            (activity as MainActivity).signInManually()
        }
    }

    private fun changePreference(setting: String, value: Boolean) {
        context?.applicationContext?.getSharedPreferences(
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
        context?.applicationContext?.getSharedPreferences(
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