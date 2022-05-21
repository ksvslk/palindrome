package com.indest.hah.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.SecondTutorialFragmentBinding
import com.indest.hah.game.viewmodel.FirebaseCustom
import com.indest.hah.game.viewmodel.LevelViewModel

class SecondTutorialFragment : BottomSheetDialogFragment() {
    private var _binding: SecondTutorialFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = SecondTutorialFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        val competition = levelViewModel.getCompetitionStatus()
        if (competition.show && !competition.accessedFromMenu) {
            binding.limitedAmountText.text = "Limited amount - ${competition.prizesLeft} left!"
            binding.competitionAnnouncement.visibility = View.VISIBLE
            binding.tutorialCompleteText.visibility = View.GONE
            levelViewModel.logCompetitionRelatedEvent(FirebaseCustom.Events.COMPETITION_25_ANNOUNCED)
        }
        else {
            binding.tutorialCompleteText.visibility = View.VISIBLE
            binding.competitionAnnouncement.visibility = View.GONE
        }
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueButton.setOnClickListener {
            parentFragmentManager.popBackStack()
            parentFragmentManager.commit {
                levelViewModel.logTutorialComplete()
                parentFragmentManager.popBackStack()
            }
        }

        this.dialog?.let { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBehavior.peekHeight = bottomSheet.height
        }
    }


}