package com.indest.hah.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.leaderboard.LeaderboardVariant.COLLECTION_PUBLIC
import com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_ALL_TIME
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.ScoreFragmentBinding
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel

class ScoresFragment : BottomSheetDialogFragment() {
    private var _binding: ScoreFragmentBinding? = null
    private val binding get() = _binding!!
    private val TAG = "ScoresFragment"
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = ScoreFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        layoutInflater.inflate(R.layout.ciruclar_progress, binding.scoreFragment)
        return b.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scoresDialogTitle1.setOnClickListener {
            dismiss()
        }
        binding.scoresDialogTitle2.setOnClickListener {
            dismiss()
        }

        if (levelViewModel.accountStatus.value in setOf(AccountStatus.SIGNED_IN, AccountStatus.SIGNED_IN_WITH_FIREBASE)) {

            val leaderboardId = if (generalViewModel.currentLevel.value.levelInfo.levelId > 1000) {
                getString(R.string.leaderboard_player_levels_leaderboard)
            } else {
                getString(R.string.leaderboard_crme_de_la_crme)
            }


            activity?.let { activity ->
                PlayGames.getLeaderboardsClient(activity)
                    .loadPlayerCenteredScores(
                        leaderboardId,
                        TIME_SPAN_ALL_TIME,
                        COLLECTION_PUBLIC,
                        3,
                        true
                    ).addOnSuccessListener(activity) { result ->
                        val leaderBoardScores = result.get()
                        if (leaderBoardScores != null && isAdded) {
                            binding.scoreFragment.removeViewAt(1)
                            leaderBoardScores.scores.forEachIndexed { index, score ->
                                if (score.scoreHolderDisplayName == levelViewModel.currentlySignedInPlayerName) {
                                    val scoreLayout = layoutInflater.inflate(
                                        R.layout.score_item_player,
                                        binding.scoreFragment,
                                        false
                                    )
                                    val imgManager =
                                        ImageManager.create(activity.applicationContext)
                                    imgManager.loadImage(
                                        scoreLayout.findViewById<ImageView>(R.id.icon),
                                        score.scoreHolderIconImageUri
                                    )

                                    scoreLayout.findViewById<AppCompatTextView>(R.id.rank).text =
                                        score.rank.toString()
                                    scoreLayout.findViewById<AppCompatTextView>(R.id.name).text =
                                        score.scoreHolderDisplayName
                                    scoreLayout.findViewById<AppCompatTextView>(R.id.score).text =
                                        "Score: ${score.rawScore}"

                                    if (index == 0) {
                                        binding.playerScoreImproveMessage.text =
                                            "You rank first!"
                                    } else {
                                        binding.playerScoreImproveMessage.text =
                                            "Earn ${leaderBoardScores.scores[index - 1].rawScore - score.rawScore + 1} points to rank up!"
                                    }
                                    binding.scoreFlipper.showNext()
                                    binding.scoreFragment.addView(scoreLayout)
                                } else {
                                    val scoreLayout = layoutInflater.inflate(
                                        R.layout.score_item,
                                        binding.scoreFragment,
                                        false
                                    )
                                    val imgManager =
                                        ImageManager.create(activity.applicationContext)
                                    imgManager.loadImage(
                                        scoreLayout.findViewById<ImageView>(R.id.icon),
                                        score.scoreHolderIconImageUri
                                    )
                                    scoreLayout.findViewById<AppCompatTextView>(R.id.rank).text =
                                        score.rank.toString()
                                    scoreLayout.findViewById<AppCompatTextView>(R.id.name).text =
                                        score.scoreHolderDisplayName
                                    scoreLayout.findViewById<AppCompatTextView>(R.id.score).text =
                                        "Score: ${score.rawScore}"
                                    binding.scoreFragment.addView(scoreLayout)
                                }
                            }
                        }
                        leaderBoardScores?.release()
                    }.addOnFailureListener {
                        dismiss()
                    }
            }
        } else {
            dismiss()
        }
    }
}