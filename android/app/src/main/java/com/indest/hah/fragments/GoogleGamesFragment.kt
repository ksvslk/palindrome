package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.games.PlayGames
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.GoogleGamesFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine

class GoogleGamesFragment : BottomSheetDialogFragment() {

    private val TAG = "GoogleGamesFragment"
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }
    private var _binding: GoogleGamesFragmentBinding? = null
    private val binding get() = _binding!!
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                generalViewModel.internet.combine(generalViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    when (internet) {
                        true -> {
                            if (accountStatus != AccountStatus.SIGNED_IN) {
                                binding.alternativeScreen.findViewById<AppCompatTextView>(R.id.alternative_text).text =
                                    getString(R.string.please_sign_in)
                                binding.achievementsButton.visibility = View.GONE
                                binding.leaderboardsButton.visibility = View.GONE
                            } else {
                                binding.alternativeScreen.visibility = View.GONE
                                binding.achievementsButton.setOnClickListener {
                                    showAchievements()
                                }
                                binding.leaderboardsButton.setOnClickListener {
                                    showLeaderBoard()
                                }
                            }
                        }
                        false -> {
                            binding.achievementsButton.setOnClickListener(null)
                            binding.leaderboardsButton.setOnClickListener(null)
                            binding.achievementsButton.visibility = View.GONE
                            binding.leaderboardsButton.visibility = View.GONE
                            binding.alternativeScreen.findViewById<AppCompatTextView>(R.id.alternative_text).text =
                                getString(R.string.no_internet)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.d(TAG, "onCreateView")
        val b = GoogleGamesFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    private fun showLeaderBoard() {
        val leaderboardsClient = PlayGames.getLeaderboardsClient(requireActivity())
        leaderboardsClient.getLeaderboardIntent(getString(R.string.leaderboard_crme_de_la_crme))
            .continueWith {
                if (it.isSuccessful) {
                    levelViewModel.setUiStateMenuAction()
                    startActivity.launch(it.result)
                }
            }
    }

    private fun showAchievements() {
        val achievementsClient = PlayGames.getAchievementsClient(requireActivity())
        achievementsClient.achievementsIntent.continueWith {
            if (it.isSuccessful) {
                levelViewModel.setUiStateMenuAction()
                startActivity.launch(it.result)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}