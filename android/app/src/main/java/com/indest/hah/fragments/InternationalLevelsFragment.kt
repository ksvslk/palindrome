package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.indest.hah.R

import com.indest.hah.databinding.InternationalLevelsFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.LanguageItemAdapter
import com.indest.hah.other.LevelSelectType
import com.indest.hah.other.safeNavigate

import kotlinx.coroutines.launch

class InternationalLevelsFragment : FullBottomSheetDialogFragment() {

    private val TAG = "InterLevelsFragment"
    private var _binding: InternationalLevelsFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED)
            {
                levelViewModel.allUniqueCountriesWithLevelCount().collect { countries ->
                    binding.internationalLevelsRecyclerview.addItemDecoration(
                        DividerItemDecoration(
                            context,
                            DividerItemDecoration.VERTICAL
                        )
                    )
                    binding.internationalLevelsRecyclerview.adapter =
                        LanguageItemAdapter(requireContext(), false, countries) {
                             it?.let {cc ->
                                 findNavController().safeNavigate(
                                     R.id.action_navigation_international_to_navigation_level_select,
                                     LevelSelectType.LANGUAGE_LEVELS,
                                     cc
                                 )
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
        val b = InternationalLevelsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }


}