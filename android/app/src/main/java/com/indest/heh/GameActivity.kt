package com.indest.heh

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.commit
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.heh.databinding.ActivityGameBinding
import com.indest.heh.fragments.GamePlayFragment
import com.indest.heh.util.hideSystemBars
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"
    private lateinit var binding: ActivityGameBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
        navController = navHostFragment.navController
        navView.setupWithNavController(navController)
        navView.menu.setGroupCheckable(0, true, false)
        navView.menu.forEach { it.isChecked = true }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.i(TAG, "onWindowFocusChanged")
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars(binding.root)
    }
}
