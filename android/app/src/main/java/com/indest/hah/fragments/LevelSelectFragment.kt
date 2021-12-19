package com.indest.hah.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.adapters.LevelItemAdapter
import com.indest.hah.databinding.LevelSelectFragmentBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.persistance.LevelProgress
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog

@AndroidEntryPoint
class LevelSelectFragment : BottomSheetDialogFragment() {
    private val TAG = "LevelSelectFragment"
    private lateinit var binding: LevelSelectFragmentBinding
    private val gameViewModel: GameViewModel by activityViewModels()
    private var lastUnlocked = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        binding = LevelSelectFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        gameViewModel.levels.observe(viewLifecycleOwner, { levels ->
            if (levels != null) {
                val last = levels.last {
                    it.progress > LevelProgress.TODO
                }.levelId
                lastUnlocked = last
                binding.levelSelectRecyclerview.adapter = LevelItemAdapter(levels) {
                    it?.levelId?.let { nr ->
                        parentFragmentManager.popBackStackImmediate()
                        gameViewModel.changeLevel(nr)
                    }
                }
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i(TAG, "onCreateDialog")
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val bsd = dialog as BottomSheetDialog
            bsd.behavior.state = STATE_EXPANDED
            binding.levelSelectRecyclerview.scrollToPosition(lastUnlocked - 1)
        }
        return bottomSheetDialog
    }

}