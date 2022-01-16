package com.indest.hah.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.indest.hah.R
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.OverallState
import com.indest.hah.util.C.GAMEPLAY_FRAGMENT_TAG
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoadingFragment : Fragment(R.layout.home_fragment) {
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            gameViewModel.overAllState.collectLatest { latest ->
                if (latest.state != OverallState.LOADING) {
                    requireActivity().getSharedPreferences(
                        getString(R.string.preferences),
                        Context.MODE_PRIVATE
                    )?.let {
                        val tutorialPassed =
                            it.getBoolean(getString(R.string.tutorial_passed), false)
                        if (tutorialPassed) {
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                replace<GamePlayFragment>(R.id.fragments_container, tag = GAMEPLAY_FRAGMENT_TAG)
                            }
                        } else {
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                replace<TutorialFragment>(R.id.fragments_container)
                            }
                        }
                    }
                }
            }
        }
    }
}