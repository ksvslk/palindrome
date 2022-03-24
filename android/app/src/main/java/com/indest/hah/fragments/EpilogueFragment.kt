package com.indest.hah.fragments

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.indest.hah.BuildConfig
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.EpilogueFragmentBinding
import com.indest.hah.fx.Animations
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import java.io.ByteArrayOutputStream
import javax.inject.Inject


@AndroidEntryPoint
class EpilogueFragment : Fragment(R.layout.epilogue_fragment) {

    @Inject
    lateinit var animations: Animations
    private lateinit var binding: EpilogueFragmentBinding
    private var trainAnimator: ObjectAnimator? = null
    private val generalViewModel: GeneralViewModel by activityViewModels()
    private val startActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EpilogueFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    private fun playTrainAnimation() {
        val trainLinearLayout = animations.getTrainAnimation(fixed = true, fixedValue = 10)
        trainLinearLayout.doOnNextLayout {
            val offscreenX = resources.displayMetrics.widthPixels + trainLinearLayout.width
            trainAnimator = ObjectAnimator.ofFloat(
                trainLinearLayout,
                "x",
                (0f - it.width),
                offscreenX.toFloat()
            )
            trainAnimator?.duration = 60000
            trainAnimator?.doOnEnd {
                activity?.findViewById<LinearLayoutCompat>(R.id.trainLinearLayout)
                    ?.removeAllViews()
            }
            trainAnimator?.start()
        }
    }

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
                            if (accountStatus == AccountStatus.SIGNED_IN) {
                                binding.showLeaderboardsButton.visibility = View.VISIBLE
                                binding.showAchievementsButton.visibility = View.VISIBLE
                            } else {
                                binding.showLeaderboardsButton.visibility = View.GONE
                                binding.showAchievementsButton.visibility = View.GONE
                            }
                            binding.shareButton.visibility = View.VISIBLE
                            binding.feedbackButton.visibility = View.VISIBLE
                        }
                        false -> {
                            binding.showLeaderboardsButton.visibility = View.GONE
                            binding.showAchievementsButton.visibility = View.GONE
                            binding.shareButton.visibility = View.GONE
                            binding.feedbackButton.visibility = View.GONE
                        }
                    }

                    binding.feedbackButton.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(getString(R.string.app_package_url))
                            setPackage(getString(R.string.vending_package))
                        }
                        if (intent.resolveActivity(requireContext().packageManager) != null) {
                            startActivity(intent)
                        }
                    }
                    binding.showLeaderboardsButton.setOnClickListener {
                        showLeaderBoard()
                    }
                    binding.showAchievementsButton.setOnClickListener {
                        showAchievements()
                    }
                    binding.shareButton.setOnClickListener {
                        val shareIntent = generalViewModel.getShareIntent("EpilogueFragment")
                        startActivity(shareIntent)
                    }
                    binding.levelSelectButton.setOnClickListener {
                        parentFragmentManager.commit {
                            addToBackStack(LevelSelectFragment().tag)
                            add(LevelSelectFragment(), LevelSelectFragment().tag)
                        }
                    }
                }
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity() as? MainActivity)?.let {
            it.binding.bottomNavView.menu.findItem(R.id.navigation_goods).isEnabled = false
            it.binding.bottomNavView.menu.findItem(R.id.navigation_goods).setIcon(R.drawable.ic_baseline_not_interested_24)
            val parade = animations.getParade()
            it.binding.konfettiView.start(parade)
            playTrainAnimation()
        }
    }

    private fun showLeaderBoard() {
        val lastSignedInAccount =
            GoogleSignIn.getLastSignedInAccount(requireContext().applicationContext)
        val leaderboardsClient = if (lastSignedInAccount != null)
            Games.getLeaderboardsClient(
                requireContext().applicationContext,
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
        val lastSignedInAccount =
            GoogleSignIn.getLastSignedInAccount(requireContext().applicationContext)
        val achievementsClient = if (lastSignedInAccount != null)
            Games.getAchievementsClient(
                requireContext().applicationContext,
                lastSignedInAccount
            )
        else null
        achievementsClient?.achievementsIntent?.continueWith {
            if (it.isSuccessful) {
                startActivity.launch(it.result)
            }
        }
    }

    override fun onDestroyView() {
        (requireActivity() as? MainActivity)?.binding?.konfettiView?.stopGracefully()
        trainAnimator?.cancel()
        trainAnimator?.removeAllListeners()
        super.onDestroyView()
    }

}