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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.PlayerLevelSelectFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.LevelItemAdapter
import com.indest.hah.other.safeNavigate
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PlayerLevelSelectFragment : BottomSheetDialogFragment() {
    private val TAG = "PlayerLevelSelectFrag"
    private var _binding: PlayerLevelSelectFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = PlayerLevelSelectFragmentBinding.inflate(layoutInflater, container, false)
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
                levelViewModel.allPlayerLevelsFlow().collect { playerLevels ->
                    val currentLevel = levelViewModel.currentLevel.replayCache.firstOrNull()?.levelInfo?.levelId
                    val currentLevelIcon = getString(R.string.level_unknown)
                    binding.playerLevelSelectRecyclerview.adapter = LevelItemAdapter(currentLevelIcon, currentLevel, playerLevels, true) {
                        it?.levelId?.let { nr ->
                            if (nr != currentLevel) {
                                levelViewModel.changeLevel(nr)
                                findNavController().safeNavigate(R.id.action_navigation_player_level_select_to_navigation_level)
                            }
                        }
                    }
                }
        }
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
                levelViewModel.currentLevel.replayCache.firstOrNull()?.let {
                    binding.playerLevelSelectRecyclerview.scrollToPosition(it.levelInfo.levelId - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.playerLevelSelectRecyclerview.adapter = null
        _binding = null
        super.onDestroyView()
    }

}