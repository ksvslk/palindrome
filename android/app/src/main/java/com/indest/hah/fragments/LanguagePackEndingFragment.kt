package com.indest.hah.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.indest.hah.MainActivity
import com.indest.hah.R
import com.indest.hah.databinding.LanguagePackEndingFragmentBinding
import com.indest.hah.databinding.TutorialFragmentBinding
import com.indest.hah.fx.Animations
import com.indest.hah.game.viewmodel.LevelViewModel
import com.indest.hah.other.countryCodeToEmoji
import com.indest.hah.other.mapCountryCodeToLanguageAndFlag
import com.indest.hah.other.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LanguagePackEndingFragment : FullBottomSheetDialogFragment() {

    @Inject
    lateinit var animations: Animations
    private var _binding: LanguagePackEndingFragmentBinding? = null
    private val binding get() = _binding!!
    private var trainAnimator: ObjectAnimator? = null
    private val levelViewModel: LevelViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LanguagePackEndingFragmentBinding.inflate(layoutInflater, container, false)
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
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.languagePackContinueButton.setOnClickListener {
            dismissAllowingStateLoss()
        }
        val cc = levelViewModel.currentLevel.value.levelInfo.countryCode
        val language = cc.mapCountryCodeToLanguageAndFlag(requireContext()).first
        val str = resources.getString(R.string.language_pack_completed, language)
        binding.languagePackCompletedText.text = str

        (activity as? MainActivity)?.let {
            val parade = animations.getParade()
            it.binding.konfettiView.start(parade)
        }
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.let {
            it.binding.konfettiView.stopGracefully()
            playTrainAnimation()
        }
        super.onDestroyView()
    }

}