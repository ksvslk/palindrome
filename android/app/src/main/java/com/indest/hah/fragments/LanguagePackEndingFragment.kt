package com.indest.hah.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.LanguagePackEndingFragmentBinding
import com.indest.hah.fx.Animations
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.C
import com.indest.hah.other.LevelSelectType
import com.indest.hah.other.mapCountryCodeToLanguageAndFlag
import com.indest.hah.other.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LanguagePackEndingFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var animations: Animations
    private lateinit var binding: LanguagePackEndingFragmentBinding
    private var trainAnimator: ObjectAnimator? = null
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LanguagePackEndingFragmentBinding.inflate(layoutInflater, container, false)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        (activity as? MainActivity)?.binding?.bottomNavView?.let {
            it.visibility = View.INVISIBLE
        }
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
        binding.mainLanguagePackView.setOnClickListener {
            val navHostFragment =
                (requireActivity() as MainActivity).supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
            val navController = navHostFragment.navController
            levelViewModel.changeLevel(1)
            navController.safeNavigate(R.id.action_navigation_language_pack_ending_to_navigation_level)
        }
        val language = levelViewModel.currentLevel.value.levelInfo.countryCode
            .mapCountryCodeToLanguageAndFlag(requireContext()).first
        binding.languagePackCompletedText.text =
            resources.getString(R.string.language_pack_completed, language)

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