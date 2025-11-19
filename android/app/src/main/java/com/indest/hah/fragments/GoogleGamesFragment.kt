package com.indest.hah.fragments

import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
import com.google.android.gms.games.PlayGames
import com.indest.hah.R
import com.indest.hah.databinding.GoogleGamesFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import com.indest.hah.other.safeNavigate
import kotlinx.coroutines.launch

class GoogleGamesFragment : FullBottomSheetDialogFragment() {

    private val TAG = "GoogleGamesFragment"
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }
    private var _binding: GoogleGamesFragmentBinding? = null
    private val binding get() = _binding!!
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                levelViewModel.accountStatus.collect { accountStatus ->
                            if (accountStatus !in setOf(AccountStatus.SIGNED_IN, AccountStatus.SIGNED_IN_WITH_FIREBASE)) {
                                binding.alternativeScreen.findViewById<AppCompatTextView>(R.id.alternative_text).text =
                                    getString(R.string.please_sign_in)
                                binding.achievementsButton.visibility = View.GONE
                                binding.leaderboardsButton.visibility = View.GONE
                                binding.globalLeaderboardsButton.visibility = View.GONE
                                binding.playerLevelsLeaderboardButton.visibility = View.GONE
                            } else {
                                binding.alternativeScreen.visibility = View.GONE
                                binding.globalLeaderboardsButton.setOnClickListener {
                                    findNavController().safeNavigate(R.id.action_navigation_google_games_to_navigation_global_leaderboards)
                                }
                                binding.achievementsButton.setOnClickListener {
                                    showAchievements()
                                }
                                binding.leaderboardsButton.setOnClickListener {
                                    showLeaderBoard(getString(R.string.leaderboard_crme_de_la_crme))
                                }
                                binding.playerLevelsLeaderboardButton.setOnClickListener {
                                    showLeaderBoard(getString(R.string.leaderboard_player_levels_leaderboard))
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
        val b = GoogleGamesFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }


    private fun showLeaderBoard(id:String) {
        activity?.let { activity ->
            val leaderboardsClient = PlayGames.getLeaderboardsClient(activity)
            leaderboardsClient.getLeaderboardIntent(id)
                .continueWith {
                    if (it.isSuccessful) {
                        startActivity.launch(it.result)
                    }
                }
        }
    }

    private fun showAchievements() {
        activity?.let { activity ->
            val achievementsClient = PlayGames.getAchievementsClient(activity)
            achievementsClient.achievementsIntent.continueWith {
                if (it.isSuccessful) {
                    startActivity.launch(it.result)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}