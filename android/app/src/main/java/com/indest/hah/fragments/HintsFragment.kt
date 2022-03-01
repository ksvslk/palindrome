package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.views.ButtonState.*
import com.indest.hah.GeneralViewModel
import com.indest.hah.databinding.HintsFragmentBinding
import com.indest.hah.views.PurchaseButton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class HintsFragment : BottomSheetDialogFragment() {
    private val TAG = "HintsFragment"
    private var _binding: HintsFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GeneralViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                viewModel.buttonState.collect() {
                    Log.d(TAG, "Update buttons!")
                    binding.rewardCategoryButton.updateButtonUi(it)
                    binding.rewardPlaceLetter.updateButtonUi(it)
                    binding.purchaseButtonClues.updateButtonUi(it)
                    binding.purchaseButtonLetters.updateButtonUi(it)
                    binding.purchaseIntermediateAds.updateButtonUi(it)
                    binding.purchaseFullAds.updateButtonUi(it)
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
        val b = HintsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b

        requireActivity().window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonClues)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseButtonLetters)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseIntermediateAds)
        addBillingFlowOnClickToPurchaseButton(binding.purchaseFullAds)

        binding.rewardCategoryButton.setOnClickListener {
            when (binding.rewardCategoryButton.buttonState)
            {
                AD -> {
                    Toast.makeText(context,"CATEGORY: Show rewarded ad.", Toast.LENGTH_SHORT).show()
                }
                NOTHING -> {
                    Toast.makeText(context,"CATEGORY: Purchased - show category.", Toast.LENGTH_SHORT).show()
                }
                NO_AD -> { }
            }
        }

        binding.rewardPlaceLetter.setOnClickListener {
            when (binding.rewardPlaceLetter.buttonState)
            {
                AD -> {
                    Toast.makeText(context,"LETTER: Show rewarded ad.", Toast.LENGTH_SHORT).show()
                }
                NOTHING -> {
                    Toast.makeText(context,"LETTER: Purchased - put letter to place.", Toast.LENGTH_SHORT).show()
                }
                NO_AD -> { }

            }
        }
        return b.root
    }

    private fun addBillingFlowOnClickToPurchaseButton(view: View?)
    {
        view?.setOnClickListener {
            (view as? PurchaseButton)?.let { pb ->
                pb.sku?.let { sku ->
                    viewModel.makePurchase(requireActivity(), sku)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.setAd(isChecked)
        }
       /* binding.internetSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.setInternet(isChecked)
        }
        binding.addRemoveClues.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) viewModel.addPurchase(getString(R.string.instant_clues))
            else viewModel.removePurchase(getString(R.string.instant_clues))
        }
        binding.addRemoveLetters.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) viewModel.addPurchase(getString(R.string.instant_letters))
            else viewModel.removePurchase(getString(R.string.instant_letters))
        }
        binding.addRemoveFull.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) viewModel.addPurchase(getString(R.string.full_no_ads))
            else viewModel.removePurchase(getString(R.string.full_no_ads))
        }
        binding.addRemoveIntermediate.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) viewModel.addPurchase(getString(R.string.no_intermediate_ads))
            else viewModel.removePurchase(getString(R.string.no_intermediate_ads))
        }*/
    }

}