package com.indest.hah.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.indest.hah.R
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.enums.LoadingState
import com.indest.hah.util.C.GAMEPLAY_FRAGMENT_TAG
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoadingFragment : Fragment(R.layout.home_fragment) {
    private val TAG = "LoadingFragment"
    private val gameViewModel: GameViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            gameViewModel.loadingState.collectLatest { latest ->
                if (latest.state == LoadingState.READY) {
                    parentFragmentManager.commit(allowStateLoss = true) {
                        setReorderingAllowed(true)
                        replace<GamePlayFragment>(
                            R.id.fragments_container,
                            tag = GAMEPLAY_FRAGMENT_TAG
                        )
                    }
                }
            }
        }
    }
}