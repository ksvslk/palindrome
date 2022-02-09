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
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.adapters.LevelItemAdapter
import com.indest.hah.databinding.LevelSelectFragmentBinding
import com.indest.hah.game.GameViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.indest.hah.R
import com.indest.hah.util.C
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LevelSelectFragment : BottomSheetDialogFragment() {
    private val TAG = "LevelSelectFragment"
    private var _binding: LevelSelectFragmentBinding? = null
    private val binding get() = _binding!!
    private val gameViewModel: GameViewModel by activityViewModels()
    private var lastUnlocked = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        val b = LevelSelectFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        gameViewModel.levels.observe(viewLifecycleOwner, { levels ->
            if (levels != null) {
                binding.levelSelectRecyclerview.adapter = LevelItemAdapter(levels) {
                    it?.levelId?.let { nr ->
                        gameViewModel.realSolutionAndGuess.postValue(Triple("", "2", null))
                        gameViewModel.changeLevel(nr)

                        lifecycleScope.launch {
                            gameViewModel.setCountTo(getString(R.string.hint_suggestion))
                        }
                        parentFragmentManager.commit {
                            setCustomAnimations(
                                R.anim.slide_in_bottom,
                                R.anim.slide_out_top
                            )
                            setReorderingAllowed(true)
                            replace<GamePlayFragment>(R.id.fragments_container, tag = C.GAMEPLAY_FRAGMENT_TAG)
                        }
                        dismiss()
                    }
                }
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog")
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val bsd = dialog as BottomSheetDialog
            bsd.behavior.state = STATE_EXPANDED
            val lastPlayed = gameViewModel.getLevelNumber()
            binding.levelSelectRecyclerview.scrollToPosition(lastPlayed - 1)
        }
        return bottomSheetDialog
    }

    override fun onDestroyView() {
        _binding?.levelSelectRecyclerview?.adapter = null
        _binding = null
        super.onDestroyView()
    }

}