package com.indest.hah.fragments

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.games.PlayGames
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.EpilogueFragmentBinding
import com.indest.hah.databinding.EpiloguePlayerFragmentBinding
import com.indest.hah.fx.Animations
import com.indest.hah.general.AccountStatus
import com.indest.hah.general.GeneralViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PlayerEpilogueFragment : Fragment(R.layout.epilogue_player_fragment) {

    @Inject
    lateinit var animations: Animations
    private lateinit var binding: EpiloguePlayerFragmentBinding
    private var trainAnimator: ObjectAnimator? = null
    private val generalViewModel: GeneralViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EpiloguePlayerFragmentBinding.inflate(layoutInflater, container, false)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED)
            {
                val isAllowedToSubmit = generalViewModel.isAllowedToSubmitPalindrome()
                generalViewModel.internet.combine(generalViewModel.accountStatus) { i, a ->
                    Pair(i, a)
                }.collect { (internet, accountStatus) ->
                    when (internet) {
                        true -> {
                            binding.submitYourOwn.visibility = View.VISIBLE
                            if (accountStatus == AccountStatus.SIGNED_IN_WITH_FIREBASE) {
                                binding.submitYourOwn.setOnClickListener {
                                    if (isAllowedToSubmit) {
                                        findNavController().popBackStack()
                                        parentFragmentManager.commit {
                                            addToBackStack(SubmissionFragment().tag)
                                            add(SubmissionFragment(), SubmissionFragment().tag)
                                        }
                                    } else {
                                        val toast = Toast.makeText(
                                            requireContext().applicationContext,
                                            "You can submit once per day.",
                                            Toast.LENGTH_LONG
                                        )
                                        toast.show()
                                    }
                                }
                            } else {
                                binding.submitYourOwn.setOnClickListener(null)
                                binding.submitYourOwn.visibility = View.GONE
                            }
                            binding.shareButtonPlayer.visibility = View.VISIBLE
                            binding.feedbackButtonPlayer.visibility = View.VISIBLE
                        }
                        false -> {
                            binding.shareButtonPlayer.visibility = View.GONE
                            binding.shareButtonPlayer.setOnClickListener(null)
                            binding.feedbackButtonPlayer.visibility = View.GONE
                            binding.feedbackButtonPlayer.setOnClickListener(null)
                            binding.submitYourOwn.visibility = View.GONE
                            binding.submitYourOwn.setOnClickListener(null)
                        }
                    }

                    binding.feedbackButtonPlayer.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(getString(R.string.app_package_url))
                            setPackage(getString(R.string.vending_package))
                        }
                        context?.let {
                            if (intent.resolveActivity(it.packageManager) != null) {
                                startActivity(intent)
                            }
                        }
                    }
                    binding.shareButtonPlayer.setOnClickListener {
                        val shareIntent = generalViewModel.getShareIntent("EpilogueFragment")
                        startActivity(shareIntent)
                    }
                }
            }
        }

        (activity as? MainActivity)?.let {
            it.binding.bottomNavView.menu.findItem(R.id.navigation_goods).isEnabled = false
            it.binding.bottomNavView.menu.findItem(R.id.navigation_goods)
                .setIcon(R.drawable.ic_baseline_disabled_by_default_24)
            val parade = animations.getParade()
            it.binding.konfettiView.start(parade)
            playTrainAnimation()
        }
    }


    override fun onDestroyView() {
        (activity as? MainActivity)?.binding?.konfettiView?.stopGracefully()
        trainAnimator?.cancel()
        trainAnimator?.removeAllListeners()
        super.onDestroyView()
    }

}