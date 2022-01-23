package com.indest.hah

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gms.ads.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.indest.hah.databinding.ActivityGameBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.OverallState.*
import com.indest.hah.game.RewardType
import com.indest.hah.util.ConnectionLiveData
import com.indest.hah.util.WindowSizeClass
import com.indest.hah.util.hideSystemBars
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection


@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"
    private var _binding: ActivityGameBinding? = null
    val binding get() = _binding!!
    private lateinit var connectionLiveData: ConnectionLiveData
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        connectionLiveData = ConnectionLiveData(applicationContext)
        UnityAds.initialize(
            applicationContext,
            getString(R.string.game_id),
            true,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    gameViewModel.setShouldLoadAd(true)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.i(TAG, "onInitializationFailed. Error: $error, Message: $message.")
                }

            })

        val b = ActivityGameBinding.inflate(layoutInflater)
        _binding = b

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

        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragments_container) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView: BottomNavigationView = binding.bottomNavView
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.menu.setGroupCheckable(0, true, false)
        bottomNavigationView.menu.forEach { it.isChecked = true }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    gameViewModel.rewardedAdState.collect { rewardedAdState ->
                        if (!rewardedAdState.adAvailable && rewardedAdState.shouldLoadNewAd && rewardedAdState.rewardType == RewardType.NONE && !rewardedAdState.userEarnedReward) {
                            loadRewardedAd(getString(R.string.ad_unit_rewarded))
                        }
                        else if (rewardedAdState.adAvailable && rewardedAdState.rewardType != RewardType.NONE)
                        {
                            showRewardedAd()
                        }
                    }
                }
            }
        }

        b.container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
        when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK)
        {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> {
                Log.i(TAG,"Screen: SMALL, ")
            }
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> {
                Log.i(TAG,"Screen: NORMAL, DPI: ${resources.configuration.densityDpi}")
            }
            Configuration.SCREENLAYOUT_SIZE_LARGE -> {
                Log.i(TAG,"Screen: LARGE, DPI: ${resources.configuration.densityDpi}")
            }
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> {
                Log.i(TAG,"Screen: XLARGE, DPI: ${resources.configuration.densityDpi}")
            }
        }

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

        gameViewModel.windowSizeClassWidth = widthWindowSizeClass

    }

    private fun loadRewardedAd(adUnit: String) {
        UnityAds.load(adUnit, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                Log.i(TAG, "onUnityAdsAdLoaded: $placementId")
                gameViewModel.setRewardedAdAvailability(true)
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                gameViewModel.resetRewardedAdState()
                Log.i(TAG, "onUnityAdsFailedToLoad ($placementId, $error, $message)")
            }

        })
    }

    private fun showRewardedAd() {
        UnityAds.show(
            this,
            getString(R.string.ad_unit_rewarded),
            object : IUnityAdsShowListener {

                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    Log.i(TAG, "onUnityAdsShowFailure: $message")
                }

                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.i(TAG, "onUnityAdsShowStart: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.i(TAG, "onUnityAdsShowClick: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    Log.i(TAG, "onUnityAdsShowComplete: $placementId. State: $state")
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        Log.i(TAG, "Reward user!")
                        gameViewModel.setUserEarnedReward(true)
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        signInSilently()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.i(TAG, "onWindowFocusChanged")
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars(binding.root)
    }

    private fun startObservingNetworkStatus() {
        connectionLiveData.observe(this@GameActivity, { hasNetwork ->
            lifecycleScope.launch(Dispatchers.IO)
            {
                if (hasNetwork) {
                    if (isConnectedToInternet()) {
                        gameViewModel.changeState(ONLINE)
                        Log.i(TAG, "Internet connection: $ONLINE.")
                    }
                    else {
                        gameViewModel.changeState(OFFLINE)
                        Log.i(TAG, "Internet connection: $OFFLINE.")
                    }
                } else {
                    gameViewModel.changeState(OFFLINE)
                    Log.i(TAG, "Internet connection: $OFFLINE.")
                }
            }
        })
    }

    private fun isConnectedToInternet(): Boolean {
        val url = URL("https://www.google.com")
        (url.openConnection() as? HttpURLConnection)?.runCatching {
            requestMethod = "GET"
            val hasInternet = responseCode == HttpsURLConnection.HTTP_OK
            inputStream.close()
            disconnect()
            return hasInternet
        }?.onFailure {
            Log.e(TAG, "URL openConnection onFailure: ${it.message}")
            return false
        }
        return false
    }

    private fun signInSilently() {
        Log.i(TAG, "Trying to sign in silently.")
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .build()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            if (account != null) {
                Games.getGamesClient(this, account)
                    .setViewForPopups(findViewById(android.R.id.content))
            }
            gameViewModel.changeState(ONLINE)
            startObservingNetworkStatus()
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
                        Log.i(TAG, "Signed in successfully.")
                        gameViewModel.changeState(ONLINE)
                        startObservingNetworkStatus()
                    } else if (task.isComplete) {
                        gameViewModel.changeState(ONLINE)
                        startObservingNetworkStatus()
                        Log.i(
                            TAG,
                            "Player needs to sign-in explicitly using via UI. ${task.exception}"
                        )
                        getSharedPreferences(
                            getString(R.string.preferences),
                            Context.MODE_PRIVATE
                        )?.let {
                            if (!it.getBoolean(
                                    getString(R.string.interactive_sign_in_done),
                                    false
                                )
                            ) {
                                with(it.edit()) {
                                    putBoolean(
                                        getString(R.string.interactive_sign_in_done),
                                        true
                                    )
                                    apply()
                                }
                                signInToGoogle()
                            }
                        }
                    }
                }
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK && it.resultCode != Activity.RESULT_CANCELED) {
                Log.i(TAG, "Something went wrong with ActivityResult. Code Not OK and not CANCELED! ")
            }
        }

    private fun signInToGoogle() {
        gameViewModel.changeState(LOADING)
        val signInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        )
        activityResultLauncher.launch(signInClient.signInIntent)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}
