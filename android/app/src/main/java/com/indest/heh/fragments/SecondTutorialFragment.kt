package com.indest.heh.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.heh.R
import com.indest.heh.databinding.SecondTutorialFragmentBinding

class SecondTutorialFragment: Fragment() {
    private lateinit var binding:SecondTutorialFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SecondTutorialFragmentBinding.inflate(layoutInflater, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.startButton.setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                setCustomAnimations(R.anim.fade_in_short,R.anim.fade_out_short)
                replace<GamePlayFragment>(R.id.fragments_container)
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavView).visibility = View.VISIBLE
            }
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.commit {
                setCustomAnimations(R.anim.slide_in_bottom,R.anim.slide_out_top)
                setReorderingAllowed(true)
                replace<TutorialFragment>(R.id.fragments_container)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}