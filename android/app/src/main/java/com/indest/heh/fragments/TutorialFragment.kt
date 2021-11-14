package com.indest.heh.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.indest.heh.R

class TutorialFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tutorial_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val skipButton = view.findViewById<AppCompatButton>(R.id.skipButton)
        skipButton.setOnClickListener {
            requireActivity().getPreferences(Context.MODE_PRIVATE)?.let {
                with (it.edit()) {
                    putBoolean(getString(R.string.tutorial_passed), true)
                    apply()
                }
            }
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                replace<GamePlayFragment>(R.id.fragments_container)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}