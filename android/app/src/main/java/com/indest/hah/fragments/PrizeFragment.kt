package com.indest.hah.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.PrizeFragmentBinding

class PrizeFragment(private val promoLink:String = "") : BottomSheetDialogFragment() {
    private var _binding: PrizeFragmentBinding? = null
    private val binding get() = _binding!!
    private val TAG = "PrizeFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = PrizeFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(promoLink.isNotEmpty())
        {
            binding.redeemButton.visibility = View.VISIBLE
            binding.prizeDialogTitle.text = getString(R.string.congrats_25_prize_title)
            binding.competitionCompleteText.text = getString(R.string.congrats_25_prize_text)
            binding.redeemButton.setOnClickListener {
                redeemCode()
            }
        }
        else {
            binding.redeemButton.visibility = View.GONE
            binding.prizeDialogTitle.text = getString(R.string.congrats_25_no_prize_title)
            binding.competitionCompleteText.text = getString(R.string.congrats_25_no_prize_text)
        }
    }

    private fun redeemCode()
    {
        val browserIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(promoLink)
        }
        startActivity(browserIntent)
        dismiss()
    }

}