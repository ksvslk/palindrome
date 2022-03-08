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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.other.LevelItemAdapter
import com.indest.hah.databinding.LevelSelectFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.indest.hah.R
import com.indest.hah.game.LevelViewModel
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class LevelSelectFragment : BottomSheetDialogFragment() {
    private val TAG = "LevelSelectFragment"
    private var _binding: LevelSelectFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = LevelSelectFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView()
    {
        lifecycleScope.launchWhenCreated {
                levelViewModel.allLevelsFlow().collect { levels ->
                    val currentLevel = levelViewModel.currentLevel.replayCache.first()?.levelInfo?.levelId
                    val currentLevelIcon = getCurrentLevelIcon(currentLevel)
                    binding.levelSelectRecyclerview.adapter = LevelItemAdapter(currentLevelIcon, currentLevel, levels) {
                        it?.levelId?.let { nr ->
                            levelViewModel.pauseUiContainerState()
                            levelViewModel.changeLevel(nr)
                            dismiss()
                        }
                    }
                }
        }
    }

    private fun getCurrentLevelIcon(level:Int?):String
    {
        level?.let {
            return when(it) {
                in 1..20 -> getString(R.string.levels_1_20)
                in 21..40 -> getString(R.string.levels_21_40)
                in 41..60 -> getString(R.string.levels_41_60)
                in 61..80 -> getString(R.string.levels_61_80)
                in 81..100 -> getString(R.string.levels_81_100)
                else -> getString(R.string.level_unknown)
            }
        }
        return getString(R.string.level_unknown)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog")
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        scrollToPositionOnShow(bottomSheetDialog)
        return bottomSheetDialog
    }

    private fun scrollToPositionOnShow(bottomSheetDialog:BottomSheetDialog)
    {
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val bsd = dialog as BottomSheetDialog
            bsd.behavior.state = STATE_EXPANDED
            if (isVisible)
            {
                levelViewModel.currentLevel.replayCache.first().let {
                    binding.levelSelectRecyclerview.scrollToPosition(it.levelInfo.levelId - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.levelSelectRecyclerview.adapter = null
        _binding = null
        super.onDestroyView()
    }

}