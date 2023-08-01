package com.indest.hah.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.SignInButton
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.indest.hah.BuildConfig

@AndroidEntryPoint
class SettingsFragment : FullBottomSheetDialogFragment() {
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
                generalViewModel.internet.combine(levelViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    binding.shareButtonSettings.visibility =
                        if (internet) View.VISIBLE else View.GONE
                    binding.shareButtonSettings.setOnClickListener {
                        val shareIntent = generalViewModel.getShareIntent("SettingsFragment")
                        startActivity(shareIntent)
                    }
                    binding.twitterButton?.setOnClickListener {
                        context?.let {
                            when (internet) {
                                true -> {
                                    openWebsite(getString(R.string.company_social_url), it)
                                }
                                false -> {
                                    showNoInternetToast(it.applicationContext)
                                }
                            }
                        }
                    }
                    binding.rateGameButton.setOnClickListener {
                        context?.let {
                            when (internet) {
                                true -> {
                                    openWebsite(getString(R.string.app_package_url), it)
                                }
                                false -> {
                                    showNoInternetToast(it.applicationContext)
                                }
                            }
                        }
                    }

                    when (accountStatus) {
                        AccountStatus.SIGNED_IN -> {
                            binding.progressCircular.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.GONE
                            binding.googleSignOutButton.visibility = View.VISIBLE
                            binding.googleSignOutButton.text = "${resources.getString(R.string.profile_person_symbol)} ${levelViewModel.currentlySignedInPlayerName}"
                        }
                        AccountStatus.SIGNED_OUT -> {
                            binding.progressCircular.visibility = View.GONE
                            binding.googleSignOutButton.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.VISIBLE
                        }
                        AccountStatus.LOADING -> {
                            binding.progressCircular.visibility = View.VISIBLE
                            binding.googleSignOutButton.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.GONE
                        }
                        AccountStatus.NOT_SET -> {}
                        AccountStatus.SIGNED_IN_WITH_FIREBASE -> {
                            binding.progressCircular.visibility = View.GONE
                            binding.googleSignInButton.visibility = View.GONE
                            binding.googleSignOutButton.visibility = View.VISIBLE
                            binding.googleSignOutButton.text = "${resources.getString(R.string.profile_person_symbol)} ${levelViewModel.currentlySignedInPlayerName}"
                        }
                    }

                    binding.googleSignInButton.setOnClickListener {
                        when (internet) {
                            true -> {
                                signInToGoogle()
                            }
                            false -> {
                                context?.let {
                                    showNoInternetToast(it)
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
            findNavController().safeNavigate(R.id.action_navigation_settings_to_navigation_tutorial)
        }

        binding.googleSignInOutButton.setOnLongClickListener {
            val info = "${BuildConfig.VERSION_CODE}; ${BuildConfig.VERSION_NAME}; ${BuildConfig.GIT_HASH}"
            Toast.makeText(
                context,
                info,
                Toast.LENGTH_LONG
            ).show()
            return@setOnLongClickListener true
        }

        setInitialPreference(binding.soundSwitch, getString(R.string.sound_on_pref))
        setInitialPreference(binding.vibrationSwitch, getString(R.string.vibration_on_pref))
        binding.googleSignInButton.setSize(SignInButton.SIZE_WIDE)
    }

    private fun openWebsite(url:String, context: Context)
    {
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage(getString(R.string.vending_package))
            if (this.resolveActivity(context.packageManager) != null) {
                startActivity(this)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(browserIntent)
            }
        }
    }

    private fun showNoInternetToast(context: Context)
    {
        Toast.makeText(
            context,
            getString(R.string.no_internet),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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