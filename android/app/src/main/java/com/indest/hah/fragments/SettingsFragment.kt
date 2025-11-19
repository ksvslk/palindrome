package com.indest.hah.fragments

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.LocaleListCompat
import androidx.core.util.toAndroidPair
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.indest.hah.BuildConfig
import com.indest.hah.R
import com.indest.hah.databinding.SettingsFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.getCountryTagOrDefault
import com.indest.hah.other.mapCountryCodeToLanguageAndFlag
import com.indest.hah.other.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale


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

                    binding.shareButtonSettings.setOnClickListener {
                        val shareIntent = generalViewModel.getShareIntent("SettingsFragment")
                        startActivity(shareIntent)
                    }
                    binding.twitterButton.setOnClickListener {
                        context?.let {

                                    openWebsite(getString(R.string.company_social_url), it)
                            }
                        }
                    binding.rateGameButton.setOnClickListener {
                        context?.let {
                                    openWebsite(getString(R.string.app_package_url), it)

                            }
                        }
                    binding.privacyPolicyButton.setOnClickListener {
                        context?.let {
                                    openWebsite(getString(R.string.privacy_policy_url), it)
                            }
                    }
                }
        }
        setCorrectLanguageToSelector()
        val flagImageView = binding.selectAppLocaleButton.findViewById<AppCompatImageView>(R.id.any_button_icon)
        flagImageView.imageTintList = null
        set50PercentIntensity(flagImageView)

        binding.selectAppLocaleButton.setOnClickListener {
            showLanguageSelectionDialog()
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
        binding.tutorialButton.findViewById<AppCompatImageView>(R.id.any_button_icon).setOnLongClickListener {
            val info = "${BuildConfig.VERSION_CODE}; ${BuildConfig.VERSION_NAME}; ${BuildConfig.GIT_COMMIT_ID}"
            Toast.makeText(
                context,
                info,
                Toast.LENGTH_LONG
            ).show()
            return@setOnLongClickListener true
        }

        setInitialPreference(binding.soundSwitch, getString(R.string.sound_on_pref))
        setInitialPreference(binding.vibrationSwitch, getString(R.string.vibration_on_pref))
    }

    private fun setCorrectLanguageToSelector()
    {
        val currentLocaleName = getCountryTagOrDefault()
        binding.selectAppLocaleButton.findViewById<AppCompatImageView>(R.id.any_button_icon).apply {
            val flag = AppCompatResources.getDrawable(
                context,
                currentLocaleName.mapCountryCodeToLanguageAndFlag(requireContext()).second)
            this.setImageDrawable(flag)
        }
    }

    private fun set50PercentIntensity(imageView: AppCompatImageView) {
        val matrix = ColorMatrix().apply {
            setSaturation(0.5f)
        }

        val colorFilter = ColorMatrixColorFilter(matrix)
        imageView.colorFilter = colorFilter
    }

    private fun showLanguageSelectionDialog()
    {
        // these to match in length
        val languages = resources.getStringArray(R.array.languages_array)
        val countryCodes = resources.getStringArray(R.array.country_codes_array)
        assert(languages.size == countryCodes.size)

        val selectionDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.select_language))
            .setItems(languages) { _, index ->
                var countryCode = countryCodes[index]
                if (countryCode == "gb") countryCode = "en"
                else if (countryCode == "cn") countryCode = "zh"
                val localeList = LocaleListCompat.forLanguageTags(countryCode)
                AppCompatDelegate.setApplicationLocales(localeList)
            }.create()
        selectionDialog.show()
    }

    private fun openWebsite(url:String, context: Context)
    {
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage(getString(R.string.vending_package))
            if (this.resolveActivity(context.packageManager) != null) {
                startActivity(this)
            } else {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                    }
                    startActivity(browserIntent)
                } catch (e:Exception) {
                    Toast.makeText(context, "Could not open website", Toast.LENGTH_SHORT).show()
                }
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