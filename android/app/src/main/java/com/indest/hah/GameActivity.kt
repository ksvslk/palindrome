package com.indest.hah

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.net.*
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.games.Games
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.indest.hah.databinding.ActivityGameBinding
import com.indest.hah.game.GameViewModel
import com.indest.hah.game.enums.InternetState.*
import com.indest.hah.game.enums.RewardType
import com.indest.hah.util.ConnectionLiveData
import com.indest.hah.util.WindowSizeClass
import com.indest.hah.util.hideSystemBars
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection


@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"
    private var _binding: ActivityGameBinding? = null
    val binding get() = _binding!!
    private lateinit var connectionLiveData: ConnectionLiveData
    private val gameViewModel: GameViewModel by viewModels()
    private var _rewardedAd: RewardedAd? = null
    private var _rewardEarned = false
    lateinit var consentInformation: ConsentInformation
    lateinit var consentForm: ConsentForm
    private var adReloadTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        connectionLiveData = ConnectionLiveData(applicationContext)

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

        b.container.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
        when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> {
                Log.d(TAG, "Screen: SMALL, ")
            }
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> {
                Log.d(TAG, "Screen: NORMAL, DPI: ${resources.configuration.densityDpi}")
            }
            Configuration.SCREENLAYOUT_SIZE_LARGE -> {
                Log.d(TAG, "Screen: LARGE, DPI: ${resources.configuration.densityDpi}")
            }
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> {
                Log.d(TAG, "Screen: XLARGE, DPI: ${resources.configuration.densityDpi}")
            }
        }
        requestConsentInformation()
        MobileAds.initialize(applicationContext) {}
        RequestConfiguration.Builder().setTestDeviceIds(
            listOf(
                "6C6A719C306FBA0A0FBE4D81600B8AB1",
                "1C8B057289566E4B36BA860D0A8281CB"
            )
        )
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

    override fun onResume() {
        super.onResume()
        setUpNavController()
        signInSilently()
        gameViewModel.startBillingConnection(applicationContext)
        gameViewModel.startSaveGameTimer()
        if (_rewardedAd == null) loadRewardedAd()
    }

    private fun requestConsentInformation() {
        // Set tag for underage of consent. false means users are not underage.
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(applicationContext)
        //consentInformation.reset()
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                // The consent information state was updated.
                // You are now ready to check if a form is available.
                if (consentInformation.isConsentFormAvailable) {
                    loadForm()
                }
            },
            {
                // Handle the error.
            })
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            applicationContext,
            { consentForm ->
                this.consentForm = consentForm
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(
                        this
                    ) { // Handle dismissal by reloading form.
                        loadForm()
                    }
                }
            }
        ) {
            /// Handle Error.
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        Log.d(TAG, "onWindowFocusChanged")
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars(binding.root)
    }

    private fun startObservingNetworkStatus() {
        connectionLiveData.observe(this@GameActivity, { hasNetwork ->
            lifecycleScope.launch(Dispatchers.Default)
            {
                if (hasNetwork) {
                    if (isConnectedToInternet()) {
                        gameViewModel.changeInternetState(ONLINE)
                        Log.d(TAG, "Internet connection: $ONLINE.")
                    } else {
                        gameViewModel.changeInternetState(OFFLINE)
                        Log.d(TAG, "Internet connection: $OFFLINE.")
                    }
                } else {
                    gameViewModel.changeInternetState(OFFLINE)
                    Log.d(TAG, "Internet connection: $OFFLINE.")
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
        Log.d(TAG, "Trying to sign in silently.")
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(Scope(Scopes.DRIVE_APPFOLDER))
                .build()
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            // Already signed in.
            // The signed in account is stored in the 'account' variable.
            if (account != null) {
                Games.getGamesClient(this, account)
                    .setViewForPopups(findViewById(android.R.id.content))
            }
            lifecycleScope.launch {
                gameViewModel.loadSnapshot(this@GameActivity)
            }
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
                        Log.d(TAG, "Signed in successfully.")
                        lifecycleScope.launch {
                            gameViewModel.loadSnapshot(this@GameActivity)
                        }
                        startObservingNetworkStatus()
                    } else if (task.isComplete) {
                        gameViewModel.startGame()
                        startObservingNetworkStatus()
                        Log.d(
                            TAG,
                            "Player needs to sign-in explicitly using via UI. ${task.exception}"
                        )
                        getSharedPreferences(
                            getString(R.string.app_preferences),
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
                Log.d(
                    TAG,
                    "Something went wrong with ActivityResult. Code Not OK and not CANCELED! "
                )
            }
        }

    private fun signInToGoogle() {
        val signInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN,
        )
        activityResultLauncher.launch(signInClient.signInIntent)
    }

    private fun startAdReloadTimer() {
        adReloadTimer?.cancel()
        adReloadTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                Log.d(TAG, "Ad reload timer completed.")
                loadRewardedAd()
            }
        }.start()
    }

    // CLEAN
    private fun loadRewardedAd() {
        if (_rewardedAd == null) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                applicationContext, getString(R.string.admob_rewarded_ad_unit), adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, adError.message)
                        gameViewModel.setRewardedAdAvailability(false)
                        startAdReloadTimer()
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.d(TAG, "Ad was loaded.")
                        gameViewModel.setRewardedAdAvailability(true)
                        _rewardedAd = rewardedAd
                    }
                }
            )
        }
    }

    fun showRewardedVideo(rewardedType: RewardType) {
        if (gameViewModel.rewardedAdAvailable.value) {
            Log.d(TAG, "showRewardedVideo")
            _rewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        _rewardedAd = null
                        loadRewardedAd()
                        if (_rewardEarned) {
                            _rewardEarned = false
                            when (rewardedType) {
                                RewardType.HINT -> {
                                    gameViewModel.setCategoryRewarded(true)
                                    gameViewModel.rewardUser(rewardedType)
                                }
                                RewardType.LETTER -> {
                                    gameViewModel.rewardUser(rewardedType)
                                }
                                else -> {
                                    Log.d(TAG, "Reward type does not match available ones.")
                                }
                            }
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        _rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }
            _rewardedAd?.show(
                this
            ) {
                Log.d(TAG, "User earned the reward.")
                _rewardEarned = true
            }
        }
    }

    private val RC_SAVED_GAMES = 9009

    fun showSavedGamesUI() {
        GoogleSignIn.getLastSignedInAccount(applicationContext)?.let {
            val snapshotsClient = Games.getSnapshotsClient(
                applicationContext,
                it
            )
            val maxNumberOfSavedGamesToShow = 5
            val intentTask: Task<Intent> = snapshotsClient.getSelectSnapshotIntent(
                "See My Saves", true, true, maxNumberOfSavedGamesToShow
            )
            intentTask.addOnSuccessListener { intent ->
                ActivityCompat.startActivityForResult(
                    this,
                    intent,
                    RC_SAVED_GAMES,
                    null
                )
            }
        }
    }

    override fun onStop() {
        adReloadTimer?.cancel()
        adReloadTimer = null
        gameViewModel.endBillingConnection()
        gameViewModel.cancelSaveGameTimer()
        super.onStop()
    }

    override fun onDestroy() {
        _binding = null
        adReloadTimer?.cancel()
        adReloadTimer = null
        gameViewModel.endBillingConnection()
        gameViewModel.destroySaveGameTimer()
        super.onDestroy()
    }

}
