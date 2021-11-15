package com.indest.heh.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.heh.R

class HomeFragment : Fragment(R.layout.home_fragment) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            requireActivity().getPreferences(Context.MODE_PRIVATE)?.let {
                val tutorialPassed = it.getBoolean(getString(R.string.tutorial_passed), false)
                if (tutorialPassed)
                {
                    replace<GamePlayFragment>(R.id.fragments_container)
                }
                else {
                    replace<TutorialFragment>(R.id.fragments_container)
                }
            }
        }
    }
}