package com.indest.heh.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.fragment.app.setFragmentResult
import com.indest.heh.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.home_fragment) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO: Load level from cloud/persistence and pass along

        val result = 2
        // Use the Kotlin extension in the fragment-ktx artifact
        setFragmentResult("requestKey", bundleOf("bundleKey" to result))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            replace<GamePlayFragment>(R.id.fragments_container)
        }
    }
}
