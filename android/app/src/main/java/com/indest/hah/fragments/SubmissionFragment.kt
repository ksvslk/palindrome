package com.indest.hah.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.indest.hah.R
import com.indest.hah.databinding.SubmissionFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.C
import com.indest.hah.other.isPalindrome
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay

class SubmissionFragment : FullBottomSheetDialogFragment() {
    private val TAG = "SubmissionFragment"
    private var _binding: SubmissionFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val b = SubmissionFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    private fun showLanguageSelectionDialog()
    {
        val languages = resources.getStringArray(R.array.languages_array)
        val languagesReversed = C.languages.entries.associate { it.value to it.key }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.select_language))
            .setItems(languages) { _, index ->
                // correct index, languages_array has one more English as the first element.
                // so if index is always -1 and if it is -1 then it is English
                val selectedLanguage = languages[index]
                val countryCode = if (selectedLanguage == "English") "gb" else languagesReversed[selectedLanguage]
                Log.d(TAG, "Selected is $selectedLanguage with country code $countryCode")
                levelViewModel.tempCountrySelected = countryCode!!
                refreshSelectedLanguage()
            }
            .show()
    }

    private fun refreshSelectedLanguage()
    {
        val language = if (levelViewModel.tempCountrySelected == "gb") "English"
                       else C.languages[levelViewModel.tempCountrySelected]
        binding.currentlySelectedLanguage.text = language
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sendLevelButton.isEnabled = false
        binding.sendLevelButton.alpha = 0.5f

        binding.languageSelectionButton.setOnClickListener {
            showLanguageSelectionDialog()
        }

        refreshSelectedLanguage()

        lifecycleScope.launchWhenCreated {
            levelViewModel.allPalindromesFlow().collect { palindromes ->
                binding.enterPalindromeTextInputEditText.addTextChangedListener(object :
                    TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        //Log.d(TAG, "beforeTextChanged: $p0")
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        //Log.d(TAG, "onTextChanged: $p0")
                    }
                    override fun afterTextChanged(p0: Editable?) {
                        levelViewModel.tempSubmissionPalindrome = p0.toString()
                        val errors = mutableListOf<String>()
                        if (p0.toString().isEmpty()) {
                            errors.clear()
                            binding.enterPalindromeTextInputLayout.error = null
                            binding.enterPalindromeTextInputLayout.endIconDrawable = null
                            binding.enterPalindromeTextInputLayout.errorIconDrawable = null
                        } else if (p0.toString().length < resources.getInteger(R.integer.minUserPalindromeLength)) {
                            errors.add(getString(R.string.min_length_error) + resources.getInteger(R.integer.minUserPalindromeLength))
                            binding.enterPalindromeTextInputLayout.endIconDrawable = null
                        } else if (palindromes.contains(
                                p0?.toString()?.replace("[!?’,.;:'\\-\"\\s]".toRegex(), "")
                            )
                        ) {
                            errors.add(getString(R.string.palindrome_exists))
                            binding.enterPalindromeTextInputLayout.endIconDrawable = null
                        } else if (p0.toString().length > resources.getInteger(R.integer.maxUserPalindromeLength)) {
                            errors.add(getString(R.string.max_length_error) + resources.getInteger(R.integer.maxUserPalindromeLength))
                            binding.enterPalindromeTextInputLayout.endIconDrawable = null
                        }
                        else if (p0.toString().isDigitsOnly()) {
                            errors.add(getString(R.string.all_digits_error))
                            binding.enterPalindromeTextInputLayout.endIconDrawable = null
                        } else {
                            errors.remove(
                                getString(R.string.min_length_error) + resources.getInteger(
                                    R.integer.minUserPalindromeLength
                                )
                            )
                            errors.remove(
                                getString(R.string.max_length_error) + resources.getInteger(
                                    R.integer.maxUserPalindromeLength
                                )
                            )
                            errors.remove(getString(R.string.palindrome_exists))
                            if (p0.toString().isPalindrome()) {
                                errors.remove(getString(R.string.not_a_palindrome_error))
                                binding.enterPalindromeTextInputLayout.error = null
                                binding.enterPalindromeTextInputLayout.errorIconDrawable = null
                                binding.enterPalindromeTextInputLayout.endIconDrawable =
                                    ContextCompat.getDrawable(
                                        requireContext(),
                                        R.drawable.ic_baseline_check_circle_24
                                    )
                            } else {
                                errors.add(getString(R.string.not_a_palindrome_error))
                                binding.enterPalindromeTextInputLayout.endIconDrawable = null
                                binding.enterPalindromeTextInputLayout.error = "Not a palindrome"
                                binding.enterPalindromeTextInputLayout.errorIconDrawable =
                                    ContextCompat.getDrawable(
                                        requireContext(),
                                        R.drawable.ic_baseline_error_24
                                    )
                            }
                        }
                        if (errors.size > 1) {
                            binding.enterPalindromeTextInputLayout.error =
                                errors.joinToString(", ")
                        } else {
                            binding.enterPalindromeTextInputLayout.error = errors.joinToString()
                            binding.enterPalindromeTextInputLayout.errorIconDrawable =
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_baseline_error_24
                                )
                        }

                        if (errors.size == 0 && p0.toString().isNotEmpty()) {
                            binding.enterPalindromeTextInputLayout.errorIconDrawable = null
                            binding.sendLevelButton.alpha = 1f
                            binding.sendLevelButton.isEnabled = true
                        } else {
                            binding.sendLevelButton.alpha = 0.5f
                            binding.sendLevelButton.isEnabled = false
                        }
                    }
                })
            }
            if (levelViewModel.tempSubmissionPalindrome.isNotEmpty())
                binding.enterPalindromeTextInputLayout.editText?.setText(levelViewModel.tempSubmissionPalindrome)
        }

        binding.sendLevelButton.setOnClickListener {
            lifecycleScope.launchWhenCreated {
                binding.enterPalindromeTextInputLayout.visibility = View.GONE
                binding.sendLevelFlipper.showNext()
                levelViewModel.submissionEventFlow.collect { event ->
                    when (event) {
                        LevelViewModel.SubmissionEvent.Succeeded -> {
                            binding.sendLevelFlipper.showNext()
                            binding.sendLevelSuccessView.visibility = View.VISIBLE
                            binding.enterPalindromeTextInputLayout.visibility = View.GONE
                            lifecycleScope.launchWhenCreated {
                                delay(3000)
                                dismissAllowingStateLoss()
                            }
                            this.cancel()
                        }
                        LevelViewModel.SubmissionEvent.Failed -> {
                            binding.sendLevelFlipper.showNext()
                            binding.sendLevelFailedView.visibility = View.VISIBLE
                            binding.sendLevelFailedButton.setOnClickListener {
                                binding.sendLevelFailedView.visibility = View.GONE
                                binding.sendLevelFlipper.showNext()
                                binding.enterPalindromeTextInputLayout.visibility = View.VISIBLE
                            }
                            this.cancel()
                        }
                    }
                }
            }
            lifecycleScope.launchWhenCreated {
                levelViewModel.submitUserLevel(binding.enterPalindromeTextInputLayout.editText?.text.toString())
            }
        }
    }


}