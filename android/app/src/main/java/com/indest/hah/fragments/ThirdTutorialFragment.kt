package com.indest.hah.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.R
import com.indest.hah.databinding.ThirdTutorialFragmentBinding

class ThirdTutorialFragment : Fragment() {
    private var _binding: ThirdTutorialFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val b = ThirdTutorialFragmentBinding.inflate(layoutInflater, container, false)
        _binding = b
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return b.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView).visibility =
            View.INVISIBLE

        binding.startButton.setOnClickListener {

            parentFragmentManager.commit {
                requireActivity().getSharedPreferences(getString(R.string.app_preferences), Context.MODE_PRIVATE)?.let {
                    with(it.edit()) {
                        putBoolean(getString(R.string.tutorial_passed), true)
                        apply()
                    }
                }
                setReorderingAllowed(true)
                setCustomAnimations(R.anim.fade_in_short, R.anim.fade_out_short)
                replace<GamePlayFragment>(R.id.fragments_container)
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView).visibility =
                    View.VISIBLE
            }
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.commit {
                setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                setReorderingAllowed(true)
                replace<SecondTutorialFragment>(R.id.fragments_container)
            }
        }

    }


}