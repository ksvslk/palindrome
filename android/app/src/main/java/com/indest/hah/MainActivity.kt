package com.indest.hah

import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.ExitGameDialogFragment
import com.indest.hah.game.LevelViewModel
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.util.C
import com.indest.hah.util.WindowSizeClass
import com.indest.hah.util.hideSystemBars
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.ln

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ExitGameDialogFragment.ExitGameDialogListener {
    private val TAG = "MainActivity"
    lateinit var binding: ActivityMainBinding
    private val levelViewModel: LevelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MobileAds.initialize(applicationContext) {}
        RequestConfiguration.Builder().setTestDeviceIds(
            listOf(
                "6C6A719C306FBA0A0FBE4D81600B8AB1",
                "1C8B057289566E4B36BA860D0A8281CB"
            )
        )
        applyCustomWindowInsets()
        windowSizeClassUpdater()
        setUpNavController()
        progressBarUpdater()
    }

    private fun windowSizeClassUpdater()
    {
        binding.container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
    }

    private fun applyCustomWindowInsets()
    {
        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { _, insets ->
            WindowInsetsCompat.Builder().setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    0
                )
            ).build()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars(binding.root)
    }

    private fun setUpNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView: BottomNavigationView = binding.bottomNavView
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.menu.setGroupCheckable(0, true, false)
        bottomNavigationView.menu.forEach { it.isChecked = true }
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() /
                resources.displayMetrics.density
        val widthWindowSizeClass = when {
            widthDp < 600f -> WindowSizeClass.COMPACT
            widthDp < 840f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
        levelViewModel.windowSizeClassWidth = widthWindowSizeClass
    }

    private fun progressBarUpdater()
    {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                levelViewModel.latestLevelFlow().collect {
                    setLevelProgressIndicator(it.levelInfo)
                }
            }
        }
    }

    private fun setLevelProgressIndicator(level: LevelData) {
        val levelsCount = 100
        //val progressX = (level.levelId * resources.displayMetrics.widthPixels / levelsCount) - ln(level.levelId.toFloat()) * 15
        binding.levelNumber.text = level.levelId.toString()
        //val progressX = (level.levelId * resources.displayMetrics.widthPixels / levelsCount).toFloat()
        //binding.levelNumber.x = progressX
        binding.gameProgress.progress = (level.levelId.toFloat() / levelsCount.toFloat() * 100.0).toInt()
    }

    override fun onBackPressed() {
        val dialog = ExitGameDialogFragment()
        dialog.show(supportFragmentManager, C.EXIT_GAME_DIALOG_FRAGMENT)
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        finish()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

}