package com.indest.hah

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
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.databinding.ActivityMainBinding
import com.indest.hah.fragments.ExitGameDialogFragment
import com.indest.hah.game.LevelViewModel
import com.indest.hah.persistance.dto.LevelData
import com.indest.hah.other.SignInLifecycleObserver
import com.indest.hah.other.C
import com.indest.hah.game.levelcreation.WindowSizeClass
import com.indest.hah.other.hideSystemBars
import com.indest.hah.other.saveBooleanPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ExitGameDialogFragment.ExitGameDialogListener {

    private val TAG = "MainActivity"
    lateinit var binding: ActivityMainBinding
    private val levelViewModel: LevelViewModel by viewModels()
    private val generalViewModel: GeneralViewModel by viewModels()
    private lateinit var signInObserver : SignInLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signInObserver = SignInLifecycleObserver(activityResultRegistry)
        lifecycle.addObserver(signInObserver)
        RequestConfiguration.Builder().setTestDeviceIds(
            listOf(
                "6C6A719C306FBA0A0FBE4D81600B8AB1",
                "1C8B057289566E4B36BA860D0A8281CB"
            )
        )
        MobileAds.initialize(applicationContext) {}
        generalViewModel.toString()

        applyCustomWindowInsets()
        windowSizeClassUpdater()
        setUpNavController()
        progressBarUpdater()
    }

    override fun onResume() {
        super.onResume()
        tryToSignInSilently()
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
        levelViewModel.windowSizeClassWidth = widthWindowSizeClass
    }

    private fun progressBarUpdater() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                levelViewModel.latestDistinctLevelFlow.collect {
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

    private fun tryToSignInSilently() {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(Games.SCOPE_GAMES_SNAPSHOTS)
                .build()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            if (account != null) {
                Games.getGamesClient(this, account)
                    .setViewForPopups(findViewById(android.R.id.content))
            }
        } else {
            // Haven't been signed-in before. Try the silent sign-in first.
            val signInClient = GoogleSignIn.getClient(this, signInOptions)
            signInClient
                .silentSignIn()
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {
                        // The signed in account is stored in the task's result.
                        val signedInAccount = task.result
                        Games.getGamesClient(this, signedInAccount)
                            .setViewForPopups(findViewById(android.R.id.content))
                        Log.d(TAG, "Signed in successfully.")
                    } else {
                        Log.d(TAG, "Sign-in explicitly using via UI. ${task.exception}")
                        val oldPreference = saveBooleanPreference(
                            getString(R.string.interactive_sign_in_done),
                            true
                        )
                        if (!oldPreference) {
                            signInObserver.signIn(signInClient.signInIntent)
                        }
                        // else preferences were already saved (true) and we do not display sign
                        // in dialog for user anymore as per requirements
                    }
                }
        }
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