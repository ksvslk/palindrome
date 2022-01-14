package com.indest.hah.fragments

import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.games.Games
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.GoogleGamesFragmentBinding
import com.indest.hah.game.OverallState
import com.indest.hah.game.OverallViewModel

class GoogleGamesFragment : BottomSheetDialogFragment() {

    private val TAG = "GoogleGamesFragment"
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    private lateinit var binding: GoogleGamesFragmentBinding
    private val overallViewModel: OverallViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "onCreateView")
        binding = GoogleGamesFragmentBinding.inflate(layoutInflater, container, false)


        if (overallViewModel.uiState.value.state == OverallState.ONLINE) {
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(requireActivity())
            if (lastSignedInAccount == null) {
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
        } else {
            binding.achievementsButton.visibility = View.GONE
            binding.leaderboardsButton.visibility = View.GONE
            binding.alternativeScreen.findViewById<AppCompatTextView>(R.id.alternative_text).text =
                getString(R.string.no_internet)
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    private fun showLeaderBoard() {
        dismiss()
        Log.i(TAG, "showLeaderBoard")
        Toast.makeText(requireContext(), "Showing leaderboard!", Toast.LENGTH_LONG * 4).show()
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(requireActivity())
        val leaderboardsClient = if (lastSignedInAccount != null)
            Games.getLeaderboardsClient(
                requireActivity(),
                lastSignedInAccount
            )
        else null

        leaderboardsClient?.getLeaderboardIntent(getString(R.string.leaderboard_crme_de_la_crme))
            ?.continueWith {
                if (it.isSuccessful) {
                    startActivity.launch(it.result)
                }
            }
    }

    private fun showAchievements() {
        dismiss()
        Log.i(TAG, "showAchievements")
        Toast.makeText(requireContext(), "Showing achievements!", Toast.LENGTH_LONG * 4).show()
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(requireActivity())
        val achievementsClient = if (lastSignedInAccount != null)
            Games.getAchievementsClient(
                requireActivity(),
                lastSignedInAccount
            )
        else null

        achievementsClient?.achievementsIntent?.continueWith {
            if (it.isSuccessful) {
                startActivity.launch(it.result)
            }
        }
    }

}