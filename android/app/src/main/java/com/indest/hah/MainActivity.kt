package com.indest.hah

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
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
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.ExitGameDialogFragment
import com.indest.hah.game.ContainerState
import com.indest.hah.game.LevelViewModel
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.game.levelcreation.WindowSizeClass
import com.indest.hah.other.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ExitGameDialogFragment.ExitGameDialogListener {

    private val TAG = "MainActivity"
    lateinit var binding: ActivityMainBinding
    private val levelViewModel: LevelViewModel by viewModels()
    private val generalViewModel: GeneralViewModel by viewModels()
    @Inject lateinit var signInObserver : SignInLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateFoldModeInfo()

        lifecycle.addObserver(signInObserver)
        signInObserver.observe(this) {
            it?.let { accountStatus ->
                levelViewModel.setAccountStatus(accountStatus)
                generalViewModel.setAccountStatus(accountStatus)
            }
        }

        MobileAds.initialize(applicationContext) {
        }

        val testDeviceIds = listOf("6C6A719C306FBA0A0FBE4D81600B8AB1", "1C8B057289566E4B36BA860D0A8281CB", "06F9B7BCACB746B35A407A17B4D161D9")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
        generalViewModel.toString()

        applyCustomWindowInsets()
        windowSizeClassUpdater()
        setUpNavController()
        progressBarUpdater()
    }

    override fun onResume() {
        super.onResume()
        levelViewModel.startSavingGame()
        levelViewModel.setUiStateWhenOnResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        levelViewModel.stopSavingGame()
        levelViewModel.setUiStateWhenOnPause()
    }

    private fun updateFoldModeInfo()
    {
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { newLayoutInfo ->
                        val foldingFeature: FoldingFeature? =
                            newLayoutInfo.displayFeatures.find { it is FoldingFeature } as? FoldingFeature
                        val isBookMode = foldingFeature?.state == FoldingFeature.State.FLAT
                        if (isBookMode) levelViewModel.setBookMode(BookMode.BOOK_MODE)
                        else levelViewModel.setBookMode(BookMode.NOPE)
                    }
            }
        }
    }

    private fun windowSizeClassUpdater() {
        binding.container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
    }

    private fun applyCustomWindowInsets() {
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
        levelViewModel.setWindowSizeClass(widthWindowSizeClass)
    }

    private fun progressBarUpdater() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                levelViewModel.currentLevel.distinctUntilChangedBy { it.levelInfo.levelId }.collect {
                    setLevelProgressIndicator(it.levelInfo)
                }
            }
        }
    }

    private fun setLevelProgressIndicator(level: LevelData) {
        val levelsCount = 100
        binding.levelNumber.text = level.levelId.toString()
        binding.gameProgress.progress =
            (level.levelId.toFloat() / levelsCount.toFloat() * 100.0).toInt()
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

  /*  override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        if (isTopResumedActivity && levelViewModel.uiState.value.state == ContainerState.PLAY)
        {
            levelViewModel.setUiStateMenuAction()
        }
        else {
            levelViewModel.setUiStateWhenOnResume()
        }
        Log.d(TAG, "onTopResumedActivityChanged")
    }*/

}