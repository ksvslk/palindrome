package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.adapters.LevelItemAdapter
import com.indest.hah.databinding.LevelSelectFragmentBinding
import com.indest.hah.game.GameViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LevelSelectFragment : BottomSheetDialogFragment() {
    private val TAG = "LevelSelectFragment"
    private lateinit var binding: LevelSelectFragmentBinding
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        binding = LevelSelectFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        gameViewModel.levels.observe(viewLifecycleOwner, { levels ->
            if (levels != null) {
                binding.levelSelectRecyclerview.adapter = LevelItemAdapter(levels) {
                    it?.levelId?.let { nr ->
                        parentFragmentManager.popBackStackImmediate()
                        gameViewModel.changeLevel(nr)
                    }
                }
            }
        })
        return binding.root
    }
}