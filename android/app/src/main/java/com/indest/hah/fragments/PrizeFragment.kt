package com.indest.hah.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.PrizeFragmentBinding
import com.indest.hah.general.GeneralViewModel

class PrizeFragment(private val promoLink: String = "") : BottomSheetDialogFragment() {
    private var _binding: PrizeFragmentBinding? = null
    private val binding get() = _binding!!
    private val TAG = "PrizeFragment"
    private val generalViewModel: GeneralViewModel by activityViewModels()

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
        if (promoLink.isNotEmpty()) {
            binding.redeemButton.visibility = View.VISIBLE
            binding.prizeDialogTitle.text = getString(R.string.congrats_25_prize_title)
            binding.competitionCompleteText.text = getString(R.string.congrats_25_prize_text)
            binding.redeemButton.setOnClickListener {
                redeemCode()
            }
        } else {
            binding.redeemButton.visibility = View.GONE
            binding.prizeDialogTitle.text = getString(R.string.congrats_25_no_prize_title)
            binding.competitionCompleteText.text = getString(R.string.congrats_25_no_prize_text)
        }
        binding.prizeCloseButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    private fun redeemCode() {
        generalViewModel.refreshPurchases = true
        activity?.let {
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(promoLink)
            }
            if (playStoreIntent.resolveActivity(it.packageManager) != null) {
                startActivity(playStoreIntent)
                dismiss()
            }
            else {
                Toast.makeText(it, "Something went wrong. Promo code should be still in your clipboard!", Toast.LENGTH_LONG).show()
            }
        }
        dismiss()
    }

}