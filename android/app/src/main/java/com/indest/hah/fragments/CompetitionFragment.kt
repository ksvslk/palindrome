package com.indest.hah.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import com.indest.hah.databinding.CompetitionFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel

class CompetitionFragment : FullBottomSheetDialogFragment() {
    private var _binding: CompetitionFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = CompetitionFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        val competition = levelViewModel.getCompetitionStatus()
        if (competition.show && !competition.accessedFromMenu || true) {
           // binding.limitedAmountText.text = "Limited amount - ${competition.prizesLeft} left!"
            binding.competitionAnnouncement.visibility = View.VISIBLE
        }
        else {
            binding.competitionAnnouncement.visibility = View.GONE
        }
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueButton.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

}