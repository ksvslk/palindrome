package com.indest.hah.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.SubmissionFragmentBinding


class SubmissionFragment : BottomSheetDialogFragment() {
    private val TAG = "SubmissionFragment"
    private var _binding: SubmissionFragmentBinding? = null
    private val binding get() = _binding!!

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

    private fun isPalindrome(string: String): Boolean {
        val testString = string.lowercase()
            .replace("""[\W+]""".toRegex(), "")
        return testString == testString.reversed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendLevelButton.isEnabled = false
        binding.enterPalindromeTextInputEditText.setOnFocusChangeListener { _, b ->
            if (!b) {
                if (binding.enterPalindromeTextInputEditText.text.toString().trim().length < 3) {
                    binding.enterPalindromeTextInputLayout.error = "Length must be >= 3"
                }
            }
        }
        binding.enterPalindromeTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                Log.d(TAG, "beforeTextChanged: $p0")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                Log.d(TAG, "onTextChanged: $p0")
            }

            override fun afterTextChanged(p0: Editable?) {
                val errors = mutableListOf<String>()
                if (p0.toString().length < 3) {
                    errors.add(getString(R.string.length_3_error))
                    binding.enterPalindromeTextInputLayout.endIconDrawable = null
                }
                else if (p0.toString().length > 24) {
                    errors.add(getString(R.string.length_24_error))
                    binding.enterPalindromeTextInputLayout.endIconDrawable = null
                }
                else {
                    errors.remove(getString(R.string.length_3_error))
                    errors.remove(getString(R.string.length_24_error))
                    if (isPalindrome(p0.toString())) {
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
                if (errors.size > 1) binding.enterPalindromeTextInputLayout.error =
                    errors.joinToString(", ")
                else binding.enterPalindromeTextInputLayout.error = errors.joinToString()

                binding.sendLevelButton.isEnabled = errors.size == 0
            }
        })
/*        val db = FirebaseFirestore.getInstance()

        db.collection("user_submitted_levels")
            .whereEqualTo("is_featured", true)
            .addSnapshotListener(EventListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "listen:error", e)
                    return@EventListener
                }
                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> Log.d(TAG, "New featured level: " + dc.document.data)
                        DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified level: " + dc.document.data)
                        DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed level: " + dc.document.data)
                    }
                }
            })*/

/*        binding.sendLevelButton.setOnClickListener {
            // Create a new user with a first, middle, and last name
            val level = hashMapOf(
                "is_featured" to false,
                "level" to binding.playerSubmittedLevel.text.toString(),
                "timestamp" to FieldValue.serverTimestamp(),
                "user_name" to "Mina"
            )

            // Add a new document with a generated ID
            db.collection("user_submitted_levels")
                .add(level)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                }
        }*/
    }

}