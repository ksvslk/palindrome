package com.indest.heh.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.heh.databinding.HintsFragmentBinding

class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private lateinit var binding: HintsFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "onCreateView")
        binding = HintsFragmentBinding.inflate(layoutInflater, container, false)
        binding.categoryRevealButton.setOnClickListener {
            Toast.makeText(requireContext(), "Reveal category rewarded.", Toast.LENGTH_LONG).show()
        }
        binding.letterPlaceButton.setOnClickListener {
            Toast.makeText(requireContext(), "Place letter rewarded.", Toast.LENGTH_LONG).show()
        }
        binding.allCategoriesButton.setOnClickListener {
            Toast.makeText(requireContext(), "All categories IAP.", Toast.LENGTH_LONG).show()
        }
        binding.unlockAllHintsButton.setOnClickListener {
            Toast.makeText(requireContext(), "All hints IAP.", Toast.LENGTH_LONG).show()
        }
        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        return binding.root
    }

}