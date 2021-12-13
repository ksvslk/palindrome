package com.indest.hah.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.adapters.LevelItemAdapter
import com.indest.hah.databinding.LevelSelectFragmentBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.persistance.LevelProgress
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.indest.hah.R
import com.indest.hah.persistance.dto.Level
import com.indest.hah.persistance.dto.LevelData
import kotlin.math.max

@AndroidEntryPoint
class LevelSelectFragment : BottomSheetDialogFragment() {
    private val TAG = "LevelSelectFragment"
    private lateinit var binding: LevelSelectFragmentBinding
    private val gameViewModel: GameViewModel by activityViewModels()
    private var lastUnlockedAndExpand = Pair(1, false)

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
                val lastUnlocked = levels.last {
                    it.progress > LevelProgress.TODO
                }.levelId
                lastUnlockedAndExpand = Pair(lastUnlocked, lastUnlocked >= levels.size - 9)
                binding.levelSelectRecyclerview.adapter = LevelItemAdapter(levels) {
                    it?.levelId?.let { nr ->
                        parentFragmentManager.popBackStackImmediate()
                        gameViewModel.changeLevel(nr)
                    }
                }
               // val lastLevelPlayed = levels.maxWithOrNull(compareBy { it.timeModified })?.levelId ?: 1
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i(TAG, "onCreateDialog")
        Log.i(TAG, lastUnlockedAndExpand.toString())
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val bsd = dialog as BottomSheetDialog
            if (lastUnlockedAndExpand.second) bsd.behavior.state = STATE_EXPANDED
            binding.levelSelectRecyclerview.scrollToPosition(lastUnlockedAndExpand.first - 1)
        }
        return bottomSheetDialog
    }

}