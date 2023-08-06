package com.indest.hah.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.games.PlayGames
import com.indest.hah.R
import com.indest.hah.databinding.GlobalLeaderboardsFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.LanguageItemAdapter
import kotlinx.coroutines.launch

class GlobalLeaderboardsFragment : FullBottomSheetDialogFragment() {

    private val TAG = "GlobalLeaderboardsFragment"
    private var _binding: GlobalLeaderboardsFragmentBinding? = null
    private val binding get() = _binding!!
    private val levelViewModel: LevelViewModel by activityViewModels()
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                levelViewModel.allUniqueCountries().collect { countries ->
                    binding.globalLeaderboardsRecyclerview.addItemDecoration(
                        DividerItemDecoration(
                            context,
                            DividerItemDecoration.VERTICAL
                        )
                    )
                    val countriesModifiedForAdapter = countries.map {
                        Pair(it, 1)
                    }
                    binding.globalLeaderboardsRecyclerview.adapter =
                        LanguageItemAdapter(requireContext(), true, countriesModifiedForAdapter) {
                             it?.let { cc ->
                                 val id = getCountryCodeLeaderboardId(cc)
                                 showLeaderBoard(id)
                             }
                        }
                }

            }
        }
    }


    private fun getCountryCodeLeaderboardId(cc:String) : String {
        when(cc)
        {
            "cn" -> return getString(R.string.leaderboard_chinese)
            "ee" -> return getString(R.string.leaderboard_estonian)
            "fi" -> return getString(R.string.leaderboard_finnish)
            "fr" -> return getString(R.string.leaderboard_french)
            "de" -> return getString(R.string.leaderboard_german)
            "in" -> return getString(R.string.leaderboard_hindi)
            "id" -> return getString(R.string.leaderboard_indonesian)
            "it" -> return getString(R.string.leaderboard_italian)
            "jp" -> return getString(R.string.leaderboard_japanese)
            "kr" -> return getString(R.string.leaderboard_korean)
            "br" -> return getString(R.string.leaderboard_portuguese)
            "ru" -> return getString(R.string.leaderboard_russian)
            "es" -> return getString(R.string.leaderboard_spanish)
            "se" -> return getString(R.string.leaderboard_swedish)
            "tr" -> return getString(R.string.leaderboard_turkish)
            "pl" -> return getString(R.string.leaderboard_polish)
            "ro" -> return getString(R.string.leaderboard_romanian)
            "nl" -> return getString(R.string.leaderboard_dutch)
            "ua" -> return getString(R.string.leaderboard_ukrainian)
        }
        return getString(R.string.global_leaderboards)
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = GlobalLeaderboardsFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }


}