package com.indest.hah.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Lifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.images.ImageManager
import com.google.android.gms.games.Games
import com.google.android.gms.games.leaderboard.LeaderboardVariant.COLLECTION_PUBLIC
import com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_ALL_TIME
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.R
import com.indest.hah.databinding.ScoreFragmentBinding

class ScoresFragment : BottomSheetDialogFragment() {
    private var _binding: ScoreFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = ScoreFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        layoutInflater.inflate(R.layout.ciruclar_progress, binding.scoreFragment)
        return b.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GoogleSignIn.getLastSignedInAccount(requireActivity())?.let { googleSignInAccount ->
            Games.getLeaderboardsClient(requireActivity(), googleSignInAccount)
                .loadPlayerCenteredScores(
                    getString(R.string.leaderboard_crme_de_la_crme),
                    TIME_SPAN_ALL_TIME,
                    COLLECTION_PUBLIC,
                    3,
                    true
                ).addOnSuccessListener { data ->
                    val lifeCycleValid =
                        lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.DESTROYED
                    if (lifeCycleValid) {
                        binding.let { binding ->
                            binding.scoreFragment.removeViewAt(1)
                            data.get()?.let {
                                it.scores.forEachIndexed { index, score ->
                                    val isCurrentPlayer =
                                        score.scoreHolder?.relationshipInfo?.friendStatus

                                    if (isCurrentPlayer == -1) {
                                        val scoreLayout = layoutInflater.inflate(
                                            R.layout.score_item_player,
                                            binding.scoreFragment,
                                            false
                                        )
                                        val imgManager = ImageManager.create(requireContext())
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
                                                "Earn ${it.scores[index - 1].rawScore - score.rawScore + 1} points to rank up!"
                                        }

                                        binding.scoreFlipper.showNext()
                                        binding.scoreFragment.addView(scoreLayout)
                                    } else {
                                        val scoreLayout = layoutInflater.inflate(
                                            R.layout.score_item,
                                            binding.scoreFragment,
                                            false
                                        )
                                        val imgManager = ImageManager.create(requireContext())
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
                                it.release()
                            }
                        }
                    }
                }.addOnFailureListener {
                    dismiss()
                }
        }
    }
}