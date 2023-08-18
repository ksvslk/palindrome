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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.LevelSelectFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.LevelItemAdapter
import com.indest.hah.other.LevelSelectType
import com.indest.hah.other.LevelSelectType.*
import com.indest.hah.other.StandardLevelItemAdapter
import com.indest.hah.other.safeNavigate
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.persistance.level.LevelProgress
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LevelSelectFragment : FullBottomSheetDialogFragment() {

    private val TAG = "LevelSelectFragment"
    private var _binding: LevelSelectFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LevelSelectFragmentBinding.inflate(layoutInflater, container, false)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val levelSelectType = arguments?.getString("levelSelectType")?.let {
            valueOf(it)
        } ?: NONE
        val selectedLanguage = arguments?.getString("selectedLanguage")
        setUpRecyclerView(levelSelectType, selectedLanguage)
    }

    private fun setUpRecyclerView(levelSelectType: LevelSelectType, language: String? = null) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                when (levelSelectType) {
                    GAME_LEVELS -> {
                        levelViewModel.allGameLevelsFlow().collect { levels ->
                            handleLevelsCollection(levels)
                            binding.progressIndicator.visibility = View.GONE
                            binding.levelSelectRecyclerview.visibility = View.VISIBLE
                        }
                    }
                    PLAYER_LEVELS -> {
                        levelViewModel.allPlayerLevelsFlow().collect { playerLevels ->
                            val currentLevel =
                                levelViewModel.currentLevel.replayCache.firstOrNull()?.levelInfo?.levelId
                            val currentLevelIcon = getCurrentLevelIcon(currentLevel)
                            binding.levelSelectRecyclerview.adapter =
                                LevelItemAdapter(
                                    currentLevelIcon,
                                    currentLevel,
                                    playerLevels,
                                    true
                                ) {
                                    it?.levelId?.let { nr ->
                                        if (nr != currentLevel) {
                                            levelViewModel.changeLevel(nr)
                                            findNavController().safeNavigate(R.id.action_navigation_level_select_to_navigation_level)
                                        }
                                    }
                                }
                            binding.progressIndicator.visibility = View.GONE
                            binding.levelSelectRecyclerview.visibility = View.VISIBLE
                        }
                    }

                    LANGUAGE_LEVELS -> {
                        levelViewModel.getLevelsForCountry(language!!).collect { languageLevels ->
                            val currentLevel =
                                levelViewModel.currentLevel.replayCache.firstOrNull()?.levelInfo?.levelId
                            binding.levelSelectRecyclerview.adapter =
                                StandardLevelItemAdapter(languageLevels) {
                                    it?.levelId?.let { nr ->
                                        if (nr != currentLevel) {
                                            levelViewModel.changeLevel(nr)
                                            findNavController().safeNavigate(R.id.action_navigation_level_select_to_navigation_level)
                                        }
                                    }
                                }
                            binding.progressIndicator.visibility = View.GONE
                            binding.levelSelectRecyclerview.visibility = View.VISIBLE
                        }
                    }

                    NONE -> throw NoSuchElementException("Case $levelSelectType should no occur in LevelSelectFragment.")
                }
            }
        }
    }

    private fun handleLevelsCollection(levels: List<Pair<Boolean, LevelData>>) {
        val currentLevel =
            levelViewModel.currentLevel.replayCache.firstOrNull()?.levelInfo?.levelId
        val currentLevelIcon = getCurrentLevelIcon(currentLevel)
        binding.levelSelectRecyclerview.adapter =
            LevelItemAdapter(currentLevelIcon, currentLevel, levels) {
                it?.levelId?.let { nr ->
                    if (nr != currentLevel) {
                        levelViewModel.changeLevel(nr)
                        findNavController().safeNavigate(R.id.action_navigation_level_select_to_navigation_level)
                    }
                }
            }

    }

    private fun getCurrentLevelIcon(level: Int?): String {
        level?.let {
            return when (it) {
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
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        scrollToPositionOnShow(bottomSheetDialog)
        return bottomSheetDialog
    }

    private fun scrollToPositionOnShow(bottomSheetDialog: BottomSheetDialog) {
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val bsd = dialog as BottomSheetDialog
            bsd.behavior.state = STATE_EXPANDED
            if (isVisible) {
                levelViewModel.currentLevel.replayCache.firstOrNull()?.let {
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