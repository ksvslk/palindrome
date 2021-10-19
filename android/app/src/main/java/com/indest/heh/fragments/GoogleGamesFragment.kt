package com.indest.heh.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.heh.databinding.GoogleGamesFragmentBinding

class GoogleGamesFragment : BottomSheetDialogFragment() {

    private val TAG = "GoogleGamesFragment"
    private val startActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    private lateinit var binding: GoogleGamesFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i(TAG, "onCreateView")
        binding = GoogleGamesFragmentBinding.inflate(layoutInflater, container, false)
        binding.achievementsButton.setOnClickListener {
            showAchievements()
        }
        binding.leaderboardsButton.setOnClickListener {
            showLeaderBoard()
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

        leaderboardsClient?.allLeaderboardsIntent?.continueWith {
            if (it.isSuccessful)
            {
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
            if (it.isSuccessful)
            {
                startActivity.launch(it.result)
            }
        }
    }

}